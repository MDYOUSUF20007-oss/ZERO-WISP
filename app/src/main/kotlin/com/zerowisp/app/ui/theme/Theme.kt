package com.zerowisp.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ColorBackground = Color(0xFF000000)
val ColorSurface = Color(0xFF0D0D0F)
val ColorSurfaceVariant = Color(0xFF141417)
val ColorBorder = Color(0xFF222226)
val ColorPrimary = Color(0xFF38BDF8)
val ColorOnBackground = Color(0xFFFFFFFF)
val ColorOnSurface = Color(0xFFFFFFFF)
val ColorOnSurfaceVariant = Color(0xFF9A9AA3)
val ColorOnPrimary = Color(0xFF000000)
val ColorSecondary = Color(0xFF2A2A2E)

private val ZeroWispColorScheme = darkColorScheme(
    primary = ColorPrimary,
    onPrimary = ColorOnPrimary,
    background = ColorBackground,
    onBackground = ColorOnBackground,
    surface = ColorSurface,
    onSurface = ColorOnSurface,
    surfaceVariant = ColorSurfaceVariant,
    onSurfaceVariant = ColorOnSurfaceVariant,
    secondary = ColorSecondary,
    onSecondary = ColorOnSurface,
    outline = ColorBorder
)

@Composable
fun ZeroWispTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ZeroWispColorScheme,
        content = content
    )
}
