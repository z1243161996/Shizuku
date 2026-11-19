package moe.shizuku.manager.ui.glass

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Colour + elevation tokens for the liquid glass skin.
 *
 * Readability contract: the glass is bright, so light appearance paints dark
 * ink on it and dark appearance paints near-white ink, the same way Apple's
 * liquid glass flips label colours with the appearance. Every text token is
 * chosen to clear 4.5:1 against the glass it sits on (see [GlassPalette]).
 */
@Immutable
data class GlassPalette(
    val isDark: Boolean,
    val wallpaper: List<Color>,
    val blobs: List<Color>,
    val cardTint: Color,
    val barTint: Color,
    val controlTint: Color,
    /** Near-opaque fill for in-window dialogs, which must beat the wallpaper. */
    val dialogTint: Color,
    /** Dim layer behind dialogs. */
    val scrim: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val warning: Color,
    val danger: Color,
    val hairline: Color,
    val shadow: Color,
)

val LightGlassPalette = GlassPalette(
    isDark = false,
    wallpaper = listOf(Color(0xFFB8F1F2), Color(0xFF5FB8F0), Color(0xFF1E6FD9)),
    blobs = listOf(Color(0xB3FFFFFF), Color(0x807FFFD4), Color(0x805B8CFF), Color(0x4D00D2FF)),
    cardTint = Color(0x4DFFFFFF),
    barTint = Color(0x66FFFFFF),
    controlTint = Color(0x5CFFFFFF),
    dialogTint = Color(0xF2FBFDFF),
    scrim = Color(0x3D0B1B33),
    textPrimary = Color(0xFF0B1B2B),
    textSecondary = Color(0xFF22344A),
    textTertiary = Color(0xFF3D5169),
    accent = Color(0xFF0B7A3B),
    warning = Color(0xFF8A5300),
    danger = Color(0xFFB3261E),
    hairline = Color(0x9EFFFFFF),
    shadow = Color(0x33103060),
)

val DarkGlassPalette = GlassPalette(
    isDark = true,
    wallpaper = listOf(Color(0xFF0A1330), Color(0xFF14315C), Color(0xFF02060F)),
    blobs = listOf(Color(0x4D7FD8FF), Color(0x402F6BFF), Color(0x338B5CF6), Color(0x1AFFFFFF)),
    cardTint = Color(0x26FFFFFF),
    barTint = Color(0x33FFFFFF),
    controlTint = Color(0x2EFFFFFF),
    dialogTint = Color(0xF01A2030),
    scrim = Color(0x73000000),
    textPrimary = Color(0xFFF7F9FC),
    textSecondary = Color(0xFFE3EAF4),
    textTertiary = Color(0xFFC4CEDE),
    accent = Color(0xFF30D158),
    warning = Color(0xFFFFD60A),
    danger = Color(0xFFFF6961),
    hairline = Color(0x59FFFFFF),
    shadow = Color(0x99000000),
)

val LocalGlassPalette = staticCompositionLocalOf { LightGlassPalette }

/**
 * Type scale. Sizes track Material 3 body/label steps but run one step larger
 * than the first draft so dense glass panels stay legible at arm's length.
 */
object GlassType {
    val hero = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.6).sp)
    val title = TextStyle(fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp)
    val cardTitle = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
    val body = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal)
    val caption = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
    val label = TextStyle(fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
    val button = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
    val mono = TextStyle(fontSize = 13.sp, lineHeight = 19.sp, fontFamily = FontFamily.Monospace)
}

@Composable
fun GlassTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = remember(darkTheme) { if (darkTheme) DarkGlassPalette else LightGlassPalette }
    CompositionLocalProvider(LocalGlassPalette provides palette) {
        MaterialTheme(
            colorScheme = if (darkTheme) {
                darkColorScheme(primary = palette.accent, secondary = palette.accent, background = Color.Transparent, surface = Color.Transparent, onSurface = palette.textPrimary)
            } else {
                lightColorScheme(primary = palette.accent, secondary = palette.accent, background = Color.Transparent, surface = Color.Transparent, onSurface = palette.textPrimary)
            },
            content = content,
        )
    }
}

/** Tinted vector icon; the manager resources are single-path masks. */
@Composable
fun GlassIcon(
    resourceId: Int,
    modifier: Modifier = Modifier,
    size: Int = 24,
    tint: Color = LocalGlassPalette.current.textPrimary,
) {
    Image(
        painter = painterResource(resourceId),
        contentDescription = null,
        modifier = modifier.size(size.dp),
        colorFilter = ColorFilter.tint(tint),
    )
}