package com.kinpulse.app.model

import org.junit.Assert.assertEquals
import org.junit.Test

class HealthRangesTest {

    // ---- Sugar: fasting / before-meal thresholds (70, 100, 126) ----

    @Test
    fun `sugar fasting below 70 is low`() {
        assertEquals(Level.LOW, HealthRanges.sugar(69, SugarContext.FASTING))
    }

    @Test
    fun `sugar fasting at 70 is normal`() {
        assertEquals(Level.NORMAL, HealthRanges.sugar(70, SugarContext.FASTING))
    }

    @Test
    fun `sugar fasting at 99 is normal`() {
        assertEquals(Level.NORMAL, HealthRanges.sugar(99, SugarContext.FASTING))
    }

    @Test
    fun `sugar fasting at 100 is elevated`() {
        assertEquals(Level.ELEVATED, HealthRanges.sugar(100, SugarContext.FASTING))
    }

    @Test
    fun `sugar fasting at 125 is elevated`() {
        assertEquals(Level.ELEVATED, HealthRanges.sugar(125, SugarContext.FASTING))
    }

    @Test
    fun `sugar fasting at 126 is high`() {
        assertEquals(Level.HIGH, HealthRanges.sugar(126, SugarContext.FASTING))
    }

    @Test
    fun `sugar before meal follows the same thresholds as fasting`() {
        assertEquals(Level.NORMAL, HealthRanges.sugar(99, SugarContext.BEFORE_MEAL))
        assertEquals(Level.ELEVATED, HealthRanges.sugar(100, SugarContext.BEFORE_MEAL))
        assertEquals(Level.HIGH, HealthRanges.sugar(126, SugarContext.BEFORE_MEAL))
    }

    // ---- Sugar: other contexts (after meal / random / bedtime / none), thresholds (70, 140, 200) ----

    @Test
    fun `sugar random below 70 is low`() {
        assertEquals(Level.LOW, HealthRanges.sugar(69, SugarContext.RANDOM))
    }

    @Test
    fun `sugar random at 70 is normal`() {
        assertEquals(Level.NORMAL, HealthRanges.sugar(70, SugarContext.RANDOM))
    }

    @Test
    fun `sugar random at 139 is normal`() {
        assertEquals(Level.NORMAL, HealthRanges.sugar(139, SugarContext.RANDOM))
    }

    @Test
    fun `sugar random at 140 is elevated`() {
        assertEquals(Level.ELEVATED, HealthRanges.sugar(140, SugarContext.RANDOM))
    }

    @Test
    fun `sugar random at 199 is elevated`() {
        assertEquals(Level.ELEVATED, HealthRanges.sugar(199, SugarContext.RANDOM))
    }

    @Test
    fun `sugar random at 200 is high`() {
        assertEquals(Level.HIGH, HealthRanges.sugar(200, SugarContext.RANDOM))
    }

    @Test
    fun `sugar with no context uses the non-fasting thresholds`() {
        assertEquals(Level.NORMAL, HealthRanges.sugar(139, null))
        assertEquals(Level.ELEVATED, HealthRanges.sugar(140, null))
    }

    // ---- Blood pressure ----

    @Test
    fun `blood pressure normal`() {
        assertEquals(Level.NORMAL, HealthRanges.bloodPressure(115, 75))
    }

    @Test
    fun `blood pressure elevated`() {
        assertEquals(Level.ELEVATED, HealthRanges.bloodPressure(125, 78))
    }

    @Test
    fun `blood pressure stage 1 by systolic`() {
        assertEquals(Level.STAGE_1, HealthRanges.bloodPressure(132, 78))
    }

    @Test
    fun `blood pressure stage 1 by diastolic`() {
        assertEquals(Level.STAGE_1, HealthRanges.bloodPressure(118, 84))
    }

    @Test
    fun `blood pressure stage 2 by systolic`() {
        assertEquals(Level.STAGE_2, HealthRanges.bloodPressure(145, 85))
    }

    @Test
    fun `blood pressure stage 2 by diastolic`() {
        assertEquals(Level.STAGE_2, HealthRanges.bloodPressure(125, 95))
    }

    @Test
    fun `blood pressure crisis by systolic`() {
        assertEquals(Level.CRISIS, HealthRanges.bloodPressure(185, 100))
    }

    @Test
    fun `blood pressure crisis by diastolic`() {
        assertEquals(Level.CRISIS, HealthRanges.bloodPressure(150, 125))
    }

    @Test
    fun `blood pressure low`() {
        assertEquals(Level.LOW, HealthRanges.bloodPressure(85, 55))
    }
}
