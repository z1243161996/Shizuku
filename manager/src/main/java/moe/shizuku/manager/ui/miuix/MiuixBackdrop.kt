package moe.shizuku.manager.ui.miuix

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Backdrop sampled by the miuix glass surfaces of a screen. Null outside a [MiuixBackdropHost].
 */
val LocalMiuixBackdrop = compositionLocalOf<LayerBackdrop?> { null }

/**
 * Draws the app background, records it into a [LayerBackdrop] and provides that backdrop to the
 * content. Only the background is recorded, so glass surfaces above it sample the background
 * without creating a sampling cycle.
 */
@Composable
fun MiuixBackdropHost(content: @Composable () -> Unit) {
    val backdrop = rememberLayerBackdrop()
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
            MiuixWallpaper()
        }
        CompositionLocalProvider(LocalMiuixBackdrop provides backdrop) {
            content()
        }
    }
}

/**
 * Hue taken from the theme primary, so the wallpaper follows the brand color and the dynamic
 * color setting; the palette tones are picked here because the theme containers are too pale for
 * glass surfaces to have anything to refract.
 */
private fun wallpaperHue(primary: Color): Float {
    val hsv = FloatArray(3)
    AndroidColor.RGBToHSV(
        (primary.red * 255f).toInt(),
        (primary.green * 255f).toInt(),
        (primary.blue * 255f).toInt(),
        hsv,
    )
    return hsv[0]
}

@Composable
fun MiuixWallpaper() {
    val colors = MiuixTheme.colorScheme
    val base = colors.background
    val dark = base.luminance() < 0.5f
    val hue = remember(colors.primary) { wallpaperHue(colors.primary) }
    val wash = remember(hue, dark) {
        if (dark) {
            listOf(
                Color.hsv(hue, 0.50f, 0.26f),
                Color.hsv(hue, 0.55f, 0.09f),
                Color.hsv((hue + 40f) % 360f, 0.45f, 0.30f),
            )
        } else {
            listOf(
                Color.hsv(hue, 0.38f, 1f),
                Color.hsv(hue, 0.12f, 1f),
                Color.hsv((hue - 35f + 360f) % 360f, 0.30f, 1f),
            )
        }
    }
    val glow = remember(hue, dark) {
        if (dark) Color.hsv((hue + 20f) % 360f, 0.55f, 0.40f) else Color.hsv((hue + 15f) % 360f, 0.45f, 1f)
    }
    val accentBlob = remember(hue, dark) {
        if (dark) Color.hsv((hue + 55f) % 360f, 0.50f, 0.45f) else Color.hsv((hue + 55f) % 360f, 0.35f, 1f)
    }
    val softBlob = remember(hue, dark) {
        if (dark) Color.hsv((hue + 300f) % 360f, 0.45f, 0.40f) else Color.hsv((hue + 300f) % 360f, 0.30f, 1f)
    }
    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    Brush.linearGradient(
                        colors = wash,
                        start = Offset(0f, 0f),
                        end = Offset(size.width * 0.85f, size.height),
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(glow.copy(alpha = if (dark) 0.5f else 0.75f), Color.Transparent),
                        center = Offset(size.width * 0.10f, size.height * 0.06f),
                        radius = size.width * 0.95f,
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(accentBlob.copy(alpha = if (dark) 0.35f else 0.55f), Color.Transparent),
                        center = Offset(size.width * 0.92f, size.height * 0.42f),
                        radius = size.width * 0.75f,
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(softBlob.copy(alpha = if (dark) 0.30f else 0.45f), Color.Transparent),
                        center = Offset(size.width * 0.25f, size.height * 0.88f),
                        radius = size.width * 0.85f,
                    )
                )
            }
    )
}
