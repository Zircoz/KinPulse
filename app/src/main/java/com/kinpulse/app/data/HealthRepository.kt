package com.kinpulse.app.data

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kinpulse.app.model.Invite
import com.kinpulse.app.model.Profile
import com.kinpulse.app.model.Reading
import com.kinpulse.app.model.Role
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Firestore layout (enforced by firestore.rules):
 *
 *   profiles/{profileId}                      name, ownerId, memberIds[], roles{uid: Role}, memberNames{uid: name}
 *   profiles/{profileId}/readings/{readingId} one sugar or BP measurement
 *   profiles/{profileId}/invites/{email}      pending invite for a family member, keyed by lower-case email
 */
class HealthRepository(private val db: FirebaseFirestore) {

    private val profiles get() = db.collection("profiles")
    private fun readings(profileId: String) = profiles.document(profileId).collection("readings")
    private fun invites(profileId: String) = profiles.document(profileId).collection("invites")

    // ---- Profiles ----

    fun profilesFor(uid: String): Flow<List<Profile>> =
        profiles.whereArrayContains("memberIds", uid).asFlow()
            .map { snap -> snap.documents.mapNotNull { it.toProfile() }.sortedBy { it.name.lowercase() } }

    fun profile(profileId: String): Flow<Profile?> =
        profiles.document(profileId).asFlow().map { it.toProfile() }

    /** Not awaited, like [saveReading], so it works offline; the local cache shows the profile at once. */
    fun createProfile(name: String, owner: SessionUser): String {
        val doc = profiles.document()
        doc.set(
            mapOf(
                "name" to name.trim(),
                "ownerId" to owner.uid,
                "memberIds" to listOf(owner.uid),
                "roles" to mapOf(owner.uid to Role.OWNER.name),
                "memberNames" to mapOf(owner.uid to owner.name),
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        )
        return doc.id
    }

    suspend fun renameProfile(profileId: String, name: String) {
        profiles.document(profileId).update("name", name.trim()).await()
    }

    /** Deletes the profile together with its readings and invites (owner only). */
    suspend fun deleteProfile(profileId: String) {
        for (collection in listOf(readings(profileId), invites(profileId))) {
            val docs = collection.get().await().documents
            docs.chunked(400).forEach { chunk ->
                db.batch().apply { chunk.forEach { delete(it.reference) } }.commit().await()
            }
        }
        profiles.document(profileId).delete().await()
    }

    // ---- Members ----

    suspend fun changeRole(profileId: String, uid: String, role: Role) {
        profiles.document(profileId).update(FieldPath.of("roles", uid), role.name).await()
    }

    /** Removes a member; used by the owner to revoke access and by a member to leave. */
    suspend fun removeMember(profileId: String, uid: String) {
        profiles.document(profileId).update(
            FieldPath.of("memberIds"), FieldValue.arrayRemove(uid),
            FieldPath.of("roles", uid), FieldValue.delete(),
            FieldPath.of("memberNames", uid), FieldValue.delete(),
        ).await()
    }

    // ---- Invites ----

    suspend fun invite(profile: Profile, email: String, role: Role, invitedBy: SessionUser) {
        val key = email.trim().lowercase()
        invites(profile.id).document(key).set(
            mapOf(
                "email" to key,
                "role" to role.name,
                "profileId" to profile.id,
                "profileName" to profile.name,
                "invitedBy" to invitedBy.uid,
                "invitedByName" to invitedBy.name,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    fun pendingInvites(profileId: String): Flow<List<Invite>> =
        invites(profileId).asFlow().map { snap -> snap.documents.mapNotNull { it.toInvite() } }

    fun invitesForEmail(email: String): Flow<List<Invite>> =
        db.collectionGroup("invites").whereEqualTo("email", email.lowercase()).asFlow()
            .map { snap -> snap.documents.mapNotNull { it.toInvite() } }

    suspend fun cancelInvite(profileId: String, email: String) {
        invites(profileId).document(email).delete().await()
    }

    /** Joins the profile with the invited role and consumes the invite, atomically. */
    suspend fun acceptInvite(invite: Invite, user: SessionUser) {
        val profileRef = profiles.document(invite.profileId)
        db.batch()
            .update(
                profileRef,
                FieldPath.of("memberIds"), FieldValue.arrayUnion(user.uid),
                FieldPath.of("roles", user.uid), invite.role.name,
                FieldPath.of("memberNames", user.uid), user.name,
            )
            .delete(invites(invite.profileId).document(invite.email))
            .commit()
            .await()
    }

    // ---- Readings ----

    fun readings(profileId: String, limit: Long = 1000): Flow<List<Reading>> =
        readings(profileId).orderBy("takenAt", Query.Direction.DESCENDING).limit(limit).asFlow()
            .map { snap -> snap.documents.mapNotNull { it.toReading() } }

    suspend fun reading(profileId: String, readingId: String): Reading? =
        readings(profileId).document(readingId).get().await().toReading()

    /**
     * Saves a reading. Writes are not awaited on purpose: Firestore applies them to the local
     * cache immediately and syncs when back online, so saving works without a connection.
     */
    fun saveReading(profileId: String, reading: Reading) {
        val collection = readings(profileId)
        val doc = if (reading.id.isEmpty()) collection.document() else collection.document(reading.id)
        doc.set(reading.toMap())
    }

    fun deleteReading(profileId: String, readingId: String) {
        readings(profileId).document(readingId).delete()
    }
}
