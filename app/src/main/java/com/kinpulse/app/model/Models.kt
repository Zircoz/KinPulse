package com.kinpulse.app.model

enum class ReadingType { SUGAR, BP }

enum class SugarContext(val label: String) {
    FASTING("Fasting"),
    BEFORE_MEAL("Before meal"),
    AFTER_MEAL("After meal (2h)"),
    RANDOM("Random"),
    BEDTIME("Bedtime"),
}

/** A single measurement. Sugar fields are set for [ReadingType.SUGAR], BP fields for [ReadingType.BP]. */
data class Reading(
    val id: String = "",
    val type: ReadingType,
    val takenAt: Long,
    val sugarMgDl: Int? = null,
    val sugarContext: SugarContext? = null,
    val systolic: Int? = null,
    val diastolic: Int? = null,
    val pulse: Int? = null,
    val note: String = "",
    val addedBy: String = "",
    val addedByName: String = "",
)

enum class Role(val label: String) {
    OWNER("Owner"),
    EDITOR("Can add & view"),
    VIEWER("Can view only");

    val canEdit: Boolean get() = this == OWNER || this == EDITOR
}

/** The person whose readings are tracked, e.g. "Dad". Shared with family members via [roles]. */
data class Profile(
    val id: String,
    val name: String,
    val ownerId: String,
    val roles: Map<String, Role>,
    val memberNames: Map<String, String>,
) {
    fun roleOf(uid: String): Role? = roles[uid]
}

data class Invite(
    val profileId: String,
    val profileName: String,
    val email: String,
    val role: Role,
    val invitedByName: String,
)
