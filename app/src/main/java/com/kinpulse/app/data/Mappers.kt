package com.kinpulse.app.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.kinpulse.app.model.Invite
import com.kinpulse.app.model.Profile
import com.kinpulse.app.model.Reading
import com.kinpulse.app.model.ReadingType
import com.kinpulse.app.model.Role
import com.kinpulse.app.model.SugarContext
import java.util.Date

// Documents are mapped by hand so the schema is explicit and matches firestore.rules.

internal inline fun <reified T : Enum<T>> String?.toEnumOrNull(): T? =
    this?.let { name -> enumValues<T>().firstOrNull { it.name == name } }

internal fun DocumentSnapshot.toProfile(): Profile? {
    val name = getString("name") ?: return null
    val ownerId = getString("ownerId") ?: return null
    val roles = (get("roles") as? Map<*, *>).orEmpty()
        .mapNotNull { (k, v) -> (v as? String).toEnumOrNull<Role>()?.let { k.toString() to it } }
        .toMap()
    val names = (get("memberNames") as? Map<*, *>).orEmpty()
        .mapNotNull { (k, v) -> (v as? String)?.let { k.toString() to it } }
        .toMap()
    return Profile(id = id, name = name, ownerId = ownerId, roles = roles, memberNames = names)
}

internal fun DocumentSnapshot.toReading(): Reading? {
    val type = getString("type").toEnumOrNull<ReadingType>() ?: return null
    val takenAt = getTimestamp("takenAt")?.toDate()?.time ?: return null
    return Reading(
        id = id,
        type = type,
        takenAt = takenAt,
        sugarMgDl = getLong("sugarMgDl")?.toInt(),
        sugarContext = getString("sugarContext").toEnumOrNull<SugarContext>(),
        systolic = getLong("systolic")?.toInt(),
        diastolic = getLong("diastolic")?.toInt(),
        pulse = getLong("pulse")?.toInt(),
        note = getString("note").orEmpty(),
        addedBy = getString("addedBy").orEmpty(),
        addedByName = getString("addedByName").orEmpty(),
    )
}

internal fun Reading.toMap(): Map<String, Any?> = mapOf(
    "type" to type.name,
    "takenAt" to Timestamp(Date(takenAt)),
    "sugarMgDl" to sugarMgDl,
    "sugarContext" to sugarContext?.name,
    "systolic" to systolic,
    "diastolic" to diastolic,
    "pulse" to pulse,
    "note" to note,
    "addedBy" to addedBy,
    "addedByName" to addedByName,
)

internal fun DocumentSnapshot.toInvite(): Invite? = Invite(
    profileId = getString("profileId") ?: return null,
    profileName = getString("profileName").orEmpty(),
    email = getString("email") ?: return null,
    role = getString("role").toEnumOrNull<Role>() ?: return null,
    invitedByName = getString("invitedByName").orEmpty(),
)
