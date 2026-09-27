package com.kinpulse.app.model

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object Csv {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun export(readings: List<Reading>, zone: ZoneId = ZoneId.systemDefault()): String = buildString {
        appendLine("Date,Type,Sugar (mg/dL),Sugar context,Systolic,Diastolic,Pulse,Status,Note,Added by")
        readings.sortedBy { it.takenAt }.forEach { r ->
            val cells = listOf(
                formatter.format(Instant.ofEpochMilli(r.takenAt).atZone(zone)),
                if (r.type == ReadingType.SUGAR) "Sugar" else "BP",
                r.sugarMgDl?.toString().orEmpty(),
                r.sugarContext?.label.orEmpty(),
                r.systolic?.toString().orEmpty(),
                r.diastolic?.toString().orEmpty(),
                r.pulse?.toString().orEmpty(),
                HealthRanges.of(r)?.label.orEmpty(),
                r.note,
                r.addedByName,
            )
            appendLine(cells.joinToString(",") { escape(it) })
        }
    }

    private fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' }) "\"" + value.replace("\"", "\"\"") + "\"" else value
}
