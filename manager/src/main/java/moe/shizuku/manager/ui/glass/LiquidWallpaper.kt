package moe.shizuku.manager.ui.glass

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlin.math.cos
import kotlin.math.sin

/**
 * The wall behind the glass: a vertical cyan -> blue wash with slow, soft
 * organic blobs. Everything drawn here is recorded into the backdrop layer,
 * which is what the glass surfaces sample and refract.
 */
@Composable
fun LiquidWallpaper(modifier: Modifier = Modifier) {
    val palette = LocalGlassPalette.current
    val transition = rememberInfiniteTransition(label = "wallpaper")

    val drift1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(28_000, easing = LinearEasing), RepeatMode.Restart),
        label = "drift1",
    )
    val drift2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(41_000, easing = LinearEasing), RepeatMode.Restart),
        label = "drift2",
    )
    val breathe by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(17_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "breathe",
    )

    val a1 = drift1 * 2f * Math.PI.toFloat()
    val a2 = drift2 * 2f * Math.PI.toFloat()

    Canvas(modifier.fillMaxSize()) {
        drawRect(
            Brush.linearGradient(
                colors = palette.wallpaper,
                start = Offset(size.width * 0.1f, 0f),
                end = Offset(size.width * 0.9f, size.height),
            )
        )

        val min = size.minDimension
        blob(
            center = Offset(
                size.width * (0.16f + 0.10f * sin(a1)),
                size.height * (0.13f + 0.05f * cos(a1 * 0.7f)),
            ),
            radius = min * (0.62f + 0.06f * breathe),
            color = palette.blobs[0],
        )
        blob(
            center = Offset(
                size.width * (0.92f + 0.06f * cos(a2)),
                size.height * (0.26f + 0.06f * sin(a2)),
            ),
            radius = min * 0.52f,
            color = palette.blobs[1],
        )
        blob(
            center = Offset(
                size.width * (0.74f + 0.08f * sin(a2 * 1.3f)),
                size.height * (0.74f + 0.05f * cos(a1)),
            ),
            radius = min * 0.70f,
            color = palette.blobs[2],
        )
        blob(
            center = Offset(
                size.width * (0.18f + 0.05f * cos(a1 * 1.1f)),
                size.height * (0.88f + 0.04f * sin(a2 * 0.9f)),
            ),
            radius = min * 0.58f,
            color = palette.blobs[3],
        )

        // light sheen sweeping down the wall
        drawRect(
            Brush.linearGradient(
                0f to Color.White.copy(alpha = if (palette.isDark) 0.05f else 0.16f),
                0.45f to Color.Transparent,
                1f to Color.Transparent,
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height * 0.85f),
            )
        )

        // top scrim keeps the white bar text legible against the pale upper edge
        drawRect(
            Brush.verticalGradient(
                0f to Color.Black.copy(alpha = if (palette.isDark) 0.32f else 0.22f),
                0.26f to Color.Transparent,
                startY = 0f,
                endY = size.height,
            )
        )

        // vignette so the glass edges have something to sit against
        drawRect(
            Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = if (palette.isDark) 0.55f else 0.22f)),
                center = Offset(size.width * 0.5f, size.height * 0.42f),
                radius = size.maxDimension * 0.78f,
            )
        )
    }
}

private fun DrawScope.blob(center: Offset, radius: Float, color: Color) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, color.copy(alpha = 0f)),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

/**
 * Hosts the recorded wallpaper layer plus screen content. [content] receives
 * the [LayerBackdrop] every glass surface must sample.
 */
@Composable
fun LiquidBackdropHost(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(LayerBackdrop) -> Unit,
) {
    val backdrop = rememberLayerBackdrop()
    Box(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            LiquidWallpaper(Modifier.fillMaxSize())
        }
        content(backdrop)
    }
}
