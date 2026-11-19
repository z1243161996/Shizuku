package moe.shizuku.manager.ui.glass

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image

/**
 * Colour + elevation tokens for the liquid glass skin, modelled on the
 * AndroidLiquidGlass sample: saturated cyan/blue wallpaper, barely tinted
 * white glass, hairline highlights and an iOS-style green accent.
 */
@Immutable
data class GlassPalette(
    val isDark: Boolean,
    val wallpaper: List<Color>,
    val blobs: List<Color>,
    val cardTint: Color,
    val barTint: Color,
    val controlTint: Color,
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
    cardTint = Color(0x24FFFFFF),
    barTint = Color(0x30FFFFFF),
    controlTint = Color(0x33FFFFFF),
    textPrimary = Color(0xFFFAFAFC),
    textSecondary = Color(0xCCFFFFFF),
    textTertiary = Color(0x8AFFFFFF),
    accent = Color(0xFF34C759),
    warning = Color(0xFFFFB020),
    danger = Color(0xFFFF453A),
    hairline = Color(0x66FFFFFF),
    shadow = Color(0x40103060),
)

val DarkGlassPalette = GlassPalette(
    isDark = true,
    wallpaper = listOf(Color(0xFF0A1330), Color(0xFF14315C), Color(0xFF02060F)),
    blobs = listOf(Color(0x4D7FD8FF), Color(0x402F6BFF), Color(0x338B5CF6), Color(0x1AFFFFFF)),
    cardTint = Color(0x1FFFFFFF),
    barTint = Color(0x26FFFFFF),
    controlTint = Color(0x26FFFFFF),
    textPrimary = Color(0xFFF5F6FA),
    textSecondary = Color(0xC7E8EDF7),
    textTertiary = Color(0x80E8EDF7),
    accent = Color(0xFF30D158),
    warning = Color(0xFFFFD60A),
    danger = Color(0xFFFF6961),
    hairline = Color(0x40FFFFFF),
    shadow = Color(0x99000000),
)

val LocalGlassPalette = staticCompositionLocalOf { LightGlassPalette }

object GlassType {
    val hero = TextStyle(fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.6).sp)
    val title = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp)
    val cardTitle = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
    val body = TextStyle(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal)
    val caption = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
    val label = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
    val mono = TextStyle(fontSize = 12.sp, lineHeight = 17.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
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
