package moe.shizuku.manager.ui.miuix

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
import androidx.compose.ui.graphics.lerp
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
 * Two-tone wash built from the palette containers, so the background belongs to the same hue
 * family as the accent instead of introducing a second, unrelated color. Glass surfaces need some
 * tonal variation behind them to look like glass rather than a flat card, hence the two blobs.
 */
@Composable
fun MiuixWallpaper() {
    val colors = MiuixTheme.colorScheme
    val base = colors.background
    val dark = base.luminance() < 0.5f
    val top = remember(colors, dark) {
        lerp(base, colors.primary, if (dark) 0.30f else 0.20f)
    }
    val bottom = remember(colors, dark) {
        lerp(base, colors.secondary, if (dark) 0.25f else 0.16f)
    }
    val warmBlob = remember(colors, dark) {
        lerp(base, colors.primary, if (dark) 0.45f else 0.30f)
    }
    val coolBlob = remember(colors, dark) {
        lerp(base, colors.secondary, if (dark) 0.35f else 0.26f)
    }
    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    Brush.linearGradient(
                        colors = listOf(top, base, bottom),
                        start = Offset(0f, 0f),
                        end = Offset(size.width * 0.85f, size.height),
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(
                            warmBlob.copy(alpha = if (dark) 0.45f else 0.55f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.08f, size.height * 0.05f),
                        radius = size.width * 0.95f,
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(
                            coolBlob.copy(alpha = if (dark) 0.35f else 0.40f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.95f, size.height * 0.75f),
                        radius = size.width * 0.85f,
                    )
                )
            }
    )
}
