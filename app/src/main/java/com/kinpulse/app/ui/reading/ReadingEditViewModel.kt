package com.kinpulse.app.ui.reading

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinpulse.app.AppContainer
import com.kinpulse.app.data.SessionUser
import com.kinpulse.app.model.HealthRanges
import com.kinpulse.app.model.Level
import com.kinpulse.app.model.Reading
import com.kinpulse.app.model.ReadingType
import com.kinpulse.app.model.SugarContext
import com.kinpulse.app.ui.components.userMessage
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.launch

class ReadingEditViewModel(
    private val container: AppContainer,
    private val user: SessionUser,
    private val profileId: String,
    private val readingId: String?,
    initialType: ReadingType,
) : ViewModel() {

    /** The reading being edited, once loaded; null for a new reading. Its id/addedBy/addedByName are preserved on save. */
    private var original: Reading? = null

    var loading by mutableStateOf(readingId != null)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var type by mutableStateOf(initialType)
        private set

    var sugarText by mutableStateOf("")
        private set

    var sugarContext by mutableStateOf(
        if (Instant.now().atZone(ZoneId.systemDefault()).hour < 10) SugarContext.FASTING else SugarContext.RANDOM,
    )
        private set

    var systolicText by mutableStateOf("")
        private set

    var diastolicText by mutableStateOf("")
        private set

    var pulseText by mutableStateOf("")
        private set

    var note by mutableStateOf("")
        private set

    var takenAt by mutableStateOf(System.currentTimeMillis())
        private set

    val isEditing: Boolean get() = readingId != null

    init {
        if (readingId != null) {
            viewModelScope.launch {
                try {
                    val reading = container.health.reading(profileId, readingId)
                    if (reading == null) {
                        error = "This reading no longer exists."
                    } else {
                        applyLoaded(reading)
                    }
                } catch (e: Exception) {
                    error = e.userMessage()
                } finally {
                    loading = false
                }
            }
        }
    }

    private fun applyLoaded(reading: Reading) {
        original = reading
        type = reading.type
        sugarText = reading.sugarMgDl?.toString().orEmpty()
        sugarContext = reading.sugarContext ?: sugarContext
        systolicText = reading.systolic?.toString().orEmpty()
        diastolicText = reading.diastolic?.toString().orEmpty()
        pulseText = reading.pulse?.toString().orEmpty()
        note = reading.note
        takenAt = reading.takenAt
    }

    fun onTypeChange(value: ReadingType) {
        if (!isEditing) type = value
    }

    fun onSugarTextChange(value: String) {
        sugarText = value.filter { it.isDigit() }.take(4)
    }

    fun onSugarContextChange(value: SugarContext) {
        sugarContext = value
    }

    fun onSystolicTextChange(value: String) {
        systolicText = value.filter { it.isDigit() }.take(4)
    }

    fun onDiastolicTextChange(value: String) {
        diastolicText = value.filter { it.isDigit() }.take(4)
    }

    fun onPulseTextChange(value: String) {
        pulseText = value.filter { it.isDigit() }.take(4)
    }

    fun onNoteChange(value: String) {
        note = value
    }

    fun onTakenAtChange(value: Long) {
        takenAt = value
    }

    // ---- Parsing ----

    private val sugarValue: Int? get() = sugarText.toIntOrNull()
    private val systolicValue: Int? get() = systolicText.toIntOrNull()
    private val diastolicValue: Int? get() = diastolicText.toIntOrNull()
    private val pulseValue: Int? get() = pulseText.toIntOrNull()

    // ---- Validation ----

    val sugarError: String?
        get() {
            if (type != ReadingType.SUGAR) return null
            val v = sugarValue ?: return if (sugarText.isBlank()) "Enter a value" else "Enter a number"
            return if (v !in HealthRanges.SUGAR_RANGE) {
                "Must be ${HealthRanges.SUGAR_RANGE.first}–${HealthRanges.SUGAR_RANGE.last} mg/dL"
            } else {
                null
            }
        }

    val systolicError: String?
        get() {
            if (type != ReadingType.BP) return null
            val v = systolicValue ?: return if (systolicText.isBlank()) "Enter a value" else "Enter a number"
            if (v !in HealthRanges.SYSTOLIC_RANGE) {
                return "Must be ${HealthRanges.SYSTOLIC_RANGE.first}–${HealthRanges.SYSTOLIC_RANGE.last}"
            }
            val d = diastolicValue
            if (d != null && v <= d) return "Must be higher than diastolic"
            return null
        }

    val diastolicError: String?
        get() {
            if (type != ReadingType.BP) return null
            val v = diastolicValue ?: return if (diastolicText.isBlank()) "Enter a value" else "Enter a number"
            return if (v !in HealthRanges.DIASTOLIC_RANGE) {
                "Must be ${HealthRanges.DIASTOLIC_RANGE.first}–${HealthRanges.DIASTOLIC_RANGE.last}"
            } else {
                null
            }
        }

    val pulseError: String?
        get() {
            if (type != ReadingType.BP || pulseText.isBlank()) return null
            val v = pulseValue ?: return "Enter a number"
            return if (v !in HealthRanges.PULSE_RANGE) {
                "Must be ${HealthRanges.PULSE_RANGE.first}–${HealthRanges.PULSE_RANGE.last} bpm"
            } else {
                null
            }
        }

    val takenAtError: String?
        get() = if (takenAt > System.currentTimeMillis() + 5 * 60_000L) "Can't be in the future" else null

    val isValid: Boolean
        get() = takenAtError == null && when (type) {
            ReadingType.SUGAR -> sugarError == null
            ReadingType.BP -> systolicError == null && diastolicError == null && pulseError == null
        }

    /** Live preview of the level for the value currently entered, or null while it's incomplete/invalid. */
    val previewLevel: Level?
        get() = if (!isValid) {
            null
        } else {
            when (type) {
                ReadingType.SUGAR -> sugarValue?.let { HealthRanges.sugar(it, sugarContext) }
                ReadingType.BP -> {
                    val s = systolicValue
                    val d = diastolicValue
                    if (s != null && d != null) HealthRanges.bloodPressure(s, d) else null
                }
            }
        }

    // ---- Save / delete ----

    /** [saveReading] is offline-friendly and not suspending, so this returns immediately. */
    fun save(): Boolean {
        if (!isValid) return false
        val base = original
        val reading = Reading(
            id = base?.id.orEmpty(),
            type = type,
            takenAt = takenAt,
            sugarMgDl = if (type == ReadingType.SUGAR) sugarValue else null,
            sugarContext = if (type == ReadingType.SUGAR) sugarContext else null,
            systolic = if (type == ReadingType.BP) systolicValue else null,
            diastolic = if (type == ReadingType.BP) diastolicValue else null,
            pulse = if (type == ReadingType.BP) pulseValue else null,
            note = note.trim(),
            addedBy = base?.addedBy ?: user.uid,
            addedByName = base?.addedByName ?: user.name,
        )
        return try {
            container.health.saveReading(profileId, reading)
            true
        } catch (e: Exception) {
            error = e.userMessage()
            false
        }
    }

    fun delete(onDone: () -> Unit) {
        val id = readingId ?: return
        try {
            container.health.deleteReading(profileId, id)
            onDone()
        } catch (e: Exception) {
            error = e.userMessage()
        }
    }

    fun clearError() {
        error = null
    }
}
