package com.kinpulse.app.model

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvTest {

    private val utc = ZoneId.of("UTC")

    private fun millisAt(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, utc).toInstant().toEpochMilli()

    private fun sugarReading(
        takenAt: Long,
        mgDl: Int = 110,
        context: SugarContext = SugarContext.FASTING,
        note: String = "",
        addedByName: String = "Alex",
    ) = Reading(
        type = ReadingType.SUGAR,
        takenAt = takenAt,
        sugarMgDl = mgDl,
        sugarContext = context,
        note = note,
        addedByName = addedByName,
    )

    private fun bpReading(
        takenAt: Long,
        systolic: Int = 118,
        diastolic: Int = 76,
        pulse: Int? = 70,
        note: String = "",
        addedByName: String = "Sam",
    ) = Reading(
        type = ReadingType.BP,
        takenAt = takenAt,
        systolic = systolic,
        diastolic = diastolic,
        pulse = pulse,
        note = note,
        addedByName = addedByName,
    )

    @Test
    fun `header row matches expected columns`() {
        val csv = Csv.export(emptyList(), utc)
        val header = csv.lines().first()
        assertEquals("Date,Type,Sugar (mg/dL),Sugar context,Systolic,Diastolic,Pulse,Status,Note,Added by", header)
    }

    @Test
    fun `empty list produces only the header line`() {
        val csv = Csv.export(emptyList(), utc)
        assertEquals(listOf("Date,Type,Sugar (mg/dL),Sugar context,Systolic,Diastolic,Pulse,Status,Note,Added by"), csv.trim().lines())
    }

    @Test
    fun `note containing a comma is quoted`() {
        val reading = sugarReading(millisAt(2026, 1, 5, 8, 0), note = "before breakfast, felt fine")
        val row = Csv.export(listOf(reading), utc).lines()[1]
        assertTrue(row.contains("\"before breakfast, felt fine\""))
    }

    @Test
    fun `note containing quotes is escaped and quoted`() {
        val reading = sugarReading(millisAt(2026, 1, 5, 8, 0), note = "doctor said \"looks good\"")
        val row = Csv.export(listOf(reading), utc).lines()[1]
        assertTrue(row.contains("\"doctor said \"\"looks good\"\"\""))
    }

    @Test
    fun `note with no special characters is not quoted`() {
        val reading = sugarReading(millisAt(2026, 1, 5, 8, 0), note = "felt fine")
        val row = Csv.export(listOf(reading), utc).lines()[1]
        val cells = row.split(",")
        // Note is the second-to-last column; unquoted means it appears verbatim.
        assertEquals("felt fine", cells[cells.size - 2])
    }

    @Test
    fun `rows are ordered chronologically regardless of input order`() {
        val earliest = sugarReading(millisAt(2026, 1, 1, 7, 0))
        val middle = bpReading(millisAt(2026, 1, 2, 7, 0))
        val latest = sugarReading(millisAt(2026, 1, 3, 7, 0))

        val csv = Csv.export(listOf(latest, earliest, middle), utc)
        val dataRows = csv.trim().lines().drop(1)

        assertEquals(3, dataRows.size)
        assertTrue(dataRows[0].startsWith("2026-01-01"))
        assertTrue(dataRows[1].startsWith("2026-01-02"))
        assertTrue(dataRows[2].startsWith("2026-01-03"))
    }

    @Test
    fun `date is formatted in the given zone`() {
        val reading = sugarReading(millisAt(2026, 3, 15, 23, 30))
        val row = Csv.export(listOf(reading), utc).lines()[1]
        assertTrue(row.startsWith("2026-03-15 23:30"))
    }

    @Test
    fun `sugar row includes sugar fields and blank BP fields`() {
        val reading = sugarReading(millisAt(2026, 1, 1, 7, 0), mgDl = 95, context = SugarContext.FASTING)
        val row = Csv.export(listOf(reading), utc).lines()[1]
        val cells = row.split(",")
        assertEquals("Sugar", cells[1])
        assertEquals("95", cells[2])
        assertEquals("Fasting", cells[3])
        assertEquals("", cells[4]) // systolic
        assertEquals("", cells[5]) // diastolic
        assertEquals("", cells[6]) // pulse
    }

    @Test
    fun `bp row includes bp fields and blank sugar fields`() {
        val reading = bpReading(millisAt(2026, 1, 1, 7, 0), systolic = 150, diastolic = 95, pulse = 80)
        val row = Csv.export(listOf(reading), utc).lines()[1]
        val cells = row.split(",")
        assertEquals("BP", cells[1])
        assertEquals("", cells[2]) // sugar mg/dl
        assertEquals("", cells[3]) // sugar context
        assertEquals("150", cells[4])
        assertEquals("95", cells[5])
        assertEquals("80", cells[6])
        assertEquals(Level.STAGE_2.label, cells[7])
    }
}
