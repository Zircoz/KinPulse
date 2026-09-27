package com.kinpulse.app.model

enum class Level(val label: String) {
    LOW("Low"),
    NORMAL("Normal"),
    ELEVATED("Elevated"),
    HIGH("High"),
    STAGE_1("High (stage 1)"),
    STAGE_2("High (stage 2)"),
    CRISIS("Very high – seek care"),
}

/**
 * Reference ranges for adults (ADA for glucose, ACC/AHA 2017 for blood pressure).
 * They are a general guide only; a doctor may set different targets for a person.
 */
object HealthRanges {

    fun sugar(mgDl: Int, context: SugarContext?): Level = when {
        mgDl < 70 -> Level.LOW
        context == SugarContext.FASTING || context == SugarContext.BEFORE_MEAL -> when {
            mgDl < 100 -> Level.NORMAL
            mgDl < 126 -> Level.ELEVATED
            else -> Level.HIGH
        }
        else -> when {
            mgDl < 140 -> Level.NORMAL
            mgDl < 200 -> Level.ELEVATED
            else -> Level.HIGH
        }
    }

    fun bloodPressure(systolic: Int, diastolic: Int): Level = when {
        systolic > 180 || diastolic > 120 -> Level.CRISIS
        systolic >= 140 || diastolic >= 90 -> Level.STAGE_2
        systolic >= 130 || diastolic >= 80 -> Level.STAGE_1
        systolic < 90 || diastolic < 60 -> Level.LOW
        systolic >= 120 -> Level.ELEVATED
        else -> Level.NORMAL
    }

    fun of(reading: Reading): Level? = when (reading.type) {
        ReadingType.SUGAR -> reading.sugarMgDl?.let { sugar(it, reading.sugarContext) }
        ReadingType.BP -> if (reading.systolic != null && reading.diastolic != null) {
            bloodPressure(reading.systolic, reading.diastolic)
        } else {
            null
        }
    }

    /** mg/dL → mmol/L, for people used to the other unit. */
    fun toMmol(mgDl: Int): Double = mgDl / 18.0

    // Plausible input bounds, mirrored in firestore.rules.
    val SUGAR_RANGE = 10..1000
    val SYSTOLIC_RANGE = 40..300
    val DIASTOLIC_RANGE = 20..200
    val PULSE_RANGE = 20..250
}
