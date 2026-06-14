/*
 * WinDroid - Theme
 * Created by Raunak Singh
 */

package com.raunaksingh.windroid.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// WinDroid Color Palette — deep navy + electric blue + white
val NavyDeep      = Color(0xFF0A0E1A)
val NavyCard      = Color(0xFF111827)
val NavyBorder    = Color(0xFF1E293B)
val ElectricBlue  = Color(0xFF3B82F6)
val CyanAccent    = Color(0xFF06B6D4)
val PurpleAccent  = Color(0xFF8B5CF6)
val SuccessGreen  = Color(0xFF10B981)
val WarningAmber  = Color(0xFFF59E0B)
val ErrorRed      = Color(0xFFEF4444)
val TextPrimary   = Color(0xFFF1F5F9)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted     = Color(0xFF475569)

private val WinDroidColorScheme = darkColorScheme(
    primary         = ElectricBlue,
    onPrimary       = Color.White,
    secondary       = CyanAccent,
    onSecondary     = NavyDeep,
    tertiary        = PurpleAccent,
    background      = NavyDeep,
    onBackground    = TextPrimary,
    surface         = NavyCard,
    onSurface       = TextPrimary,
    surfaceVariant  = NavyBorder,
    onSurfaceVariant= TextSecondary,
    outline         = NavyBorder,
    error           = ErrorRed,
    onError         = Color.White,
)

@Composable
fun WinDroidTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WinDroidColorScheme,
        typography  = WinDroidTypography,
        content     = content
    )
}
