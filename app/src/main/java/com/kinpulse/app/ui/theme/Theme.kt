package com.kinpulse.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.kinpulse.app.model.Level

private val Teal = Color(0xFF00696B)
private val TealLight = Color(0xFF4CDADB)

private val LightColors = lightColorScheme(primary = Teal, secondary = Color(0xFF4A6363))
private val DarkColors = darkColorScheme(primary = TealLight, secondary = Color(0xFFB0CCCC))

@Composable
fun KinPulseTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}

/** Status colours stay fixed (not dynamic) so "red means high" holds on every device. */
@Composable
fun Level.color(): Color {
    val dark = isSystemInDarkTheme()
    return when (this) {
        Level.LOW -> if (dark) Color(0xFF8AB4F8) else Color(0xFF1A73E8)
        Level.NORMAL -> if (dark) Color(0xFF81C995) else Color(0xFF188038)
        Level.ELEVATED -> if (dark) Color(0xFFFDD663) else Color(0xFFB06000)
        Level.HIGH, Level.STAGE_1 -> if (dark) Color(0xFFFCAD70) else Color(0xFFD14D00)
        Level.STAGE_2, Level.CRISIS -> if (dark) Color(0xFFF28B82) else Color(0xFFC5221F)
    }
}
