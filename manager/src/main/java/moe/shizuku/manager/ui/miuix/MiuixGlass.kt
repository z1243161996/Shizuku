package moe.shizuku.manager.ui.miuix

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.glassPanel
import top.yukonga.miuix.kmp.material.GlassDefaults
import top.yukonga.miuix.kmp.material.GlassShape
import top.yukonga.miuix.kmp.material.GlassShadows
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Continuous-corner glass silhouette with one radius for all four corners. */
fun glassShape(radius: Dp): GlassShape = GlassShape(
    CornerSize(radius),
    CornerSize(radius),
    CornerSize(radius),
    CornerSize(radius),
)

/** Whether the current miuix palette is a dark one. */
@Composable
fun isDarkColorScheme(): Boolean = MiuixTheme.colorScheme.background.luminance() < 0.5f

/**
 * A card that samples the screen backdrop through [glassPanel]; without a backdrop or runtime
 * shader support it degrades to an opaque surface card.
 */
@Composable
fun MiuixGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    contentPadding: Dp = 18.dp,
    backdrop: Backdrop? = LocalMiuixBackdrop.current,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = glassShape(cornerRadius)
    val surface = MiuixTheme.colorScheme.surface
    Box(
        modifier
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(cornerRadius))
                        .clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .glassPanel(
            backdrop = backdrop,
            shape = shape,
            style = GlassDefaults.style(isDarkColorScheme()),
            shadow = GlassShadows.Regular,
            fallback = Modifier
                .clip(RoundedCornerShape(cornerRadius))
                .background(surface),
        )
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}
