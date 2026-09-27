package com.kinpulse.app.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.kinpulse.app.model.Reading
import com.kinpulse.app.model.ReadingType

/** A series of values to plot, oldest first. */
private data class Series(val label: String, val values: List<Int>, val color: Color)

/**
 * Line chart of the most recent readings, drawn directly on a Canvas to avoid a charting dependency.
 * Dashed lines mark the upper end of the normal range.
 */
@Composable
fun TrendChart(readings: List<Reading>, type: ReadingType, modifier: Modifier = Modifier) {
    val recent = readings.take(30).reversed()
    if (recent.size < 2) return

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val series = when (type) {
        ReadingType.SUGAR -> listOf(Series("Sugar mg/dL", recent.mapNotNull { it.sugarMgDl }, primary))
        ReadingType.BP -> listOf(
            Series("Systolic", recent.mapNotNull { it.systolic }, primary),
            Series("Diastolic", recent.mapNotNull { it.diastolic }, tertiary),
        )
    }
    val guides = when (type) {
        ReadingType.SUGAR -> listOf(70, 140)
        ReadingType.BP -> listOf(80, 120)
    }

    val all = series.flatMap { it.values } + guides
    val min = (all.min() - 10).coerceAtLeast(0)
    val max = all.max() + 10

    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val guideColor = MaterialTheme.colorScheme.outlineVariant

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(160.dp)) {
            val left = 36.dp.toPx()
            fun y(value: Int) = size.height - (value - min).toFloat() / (max - min) * size.height

            guides.forEach { g ->
                drawLine(
                    guideColor,
                    Offset(left, y(g)),
                    Offset(size.width, y(g)),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
                )
                drawLabel(measurer, g.toString(), labelStyle, Offset(0f, y(g) - 8.dp.toPx()))
            }
            series.forEach { s -> drawSeries(s, left, ::y) }
        }
        if (series.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                series.forEach { Text("● ${it.label}", color = it.color, style = MaterialTheme.typography.labelMedium) }
            }
        }
        Text(
            "Last ${recent.size} readings",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun DrawScope.drawSeries(series: Series, left: Float, y: (Int) -> Float) {
    val values = series.values
    if (values.size < 2) return
    val step = (size.width - left) / (values.size - 1)
    val path = Path()
    values.forEachIndexed { i, v ->
        val point = Offset(left + i * step, y(v))
        if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    drawPath(path, series.color, style = Stroke(width = 2.dp.toPx()))
    values.forEachIndexed { i, v -> drawCircle(series.color, 3.dp.toPx(), Offset(left + i * step, y(v))) }
}

private fun DrawScope.drawLabel(
    measurer: androidx.compose.ui.text.TextMeasurer,
    text: String,
    style: TextStyle,
    topLeft: Offset,
) {
    drawText(measurer, text, topLeft, style)
}
