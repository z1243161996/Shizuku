package moe.shizuku.manager.ui.glass

import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

/** API level at which RenderEffect based blur/refraction is usable. */
private val supportsGlassEffects: Boolean get() = Build.VERSION.SDK_INT >= 33

/**
 * The core glass surface: samples [backdrop], blurs + refracts it, lays a thin
 * veil of [tint] on top and closes the shape with a hairline highlight.
 */
fun Modifier.liquidGlass(
    backdrop: Backdrop,
    shape: Shape,
    tint: Color,
    blurRadius: Dp = 20.dp,
    refractionHeight: Dp = 10.dp,
    refractionAmount: Dp = 24.dp,
    hairline: Color? = null,
): Modifier = this.drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    effects = {
        if (supportsGlassEffects) {
            vibrancy()
            blur(blurRadius.toPx())
            if (refractionHeight > 0.dp) {
                lens(refractionHeight.toPx(), refractionAmount.toPx())
            }
        }
    },
    onDrawSurface = {
        drawRect(tint)
        val strokeColor = hairline ?: Color.White.copy(alpha = 0.35f)
        drawRect(
            brush = Brush.verticalGradient(
                0f to strokeColor.copy(alpha = (strokeColor.alpha * 1.6f).coerceAtMost(1f)),
                0.4f to strokeColor.copy(alpha = strokeColor.alpha * 0.35f),
                1f to strokeColor.copy(alpha = strokeColor.alpha * 0.8f),
            ),
            topLeft = Offset(0.5f, 0.5f),
            size = Size((size.width - 1f).coerceAtLeast(0f), (size.height - 1f).coerceAtLeast(0f)),
            style = Stroke(width = 1f),
        )
    },
)

@Composable
fun GlassCard(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    tint: Color = LocalGlassPalette.current.cardTint,
    elevation: Dp = 10.dp,
    contentPadding: Dp = 18.dp,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalGlassPalette.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.985f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cardScale",
    )
    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(elevation, shape, clip = false, ambientColor = palette.shadow, spotColor = palette.shadow)
            .liquidGlass(backdrop, shape, if (enabled) tint else tint.copy(alpha = tint.alpha * 0.6f))
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        enabled = enabled,
                        onClick = onClick,
                    )
                } else Modifier
            )
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun GlassTitle(text: String, modifier: Modifier = Modifier, color: Color = LocalGlassPalette.current.textPrimary) {
    Text(text = text, modifier = modifier, style = GlassType.cardTitle, color = color)
}

@Composable
fun GlassBody(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalGlassPalette.current.textSecondary,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(text = text, modifier = modifier, style = GlassType.body, color = color, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

@Composable
fun GlassCaption(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalGlassPalette.current.textSecondary,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(text = text, modifier = modifier, style = GlassType.caption, color = color, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

@Composable
fun GlassChip(
    text: String,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    tint: Color = LocalGlassPalette.current.controlTint,
    dotColor: Color? = null,
    textColor: Color = LocalGlassPalette.current.textPrimary,
) {
    Row(
        modifier = modifier
            .liquidGlass(backdrop, CircleShape, tint)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dotColor != null) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(dotColor))
            Spacer(Modifier.width(7.dp))
        }
        Text(text = text, style = GlassType.label, color = textColor)
    }
}

enum class GlassButtonStyle { Glass, Prominent, Destructive }

@Composable
fun GlassButton(
    text: String,
    backdrop: Backdrop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    style: GlassButtonStyle = GlassButtonStyle.Glass,
    enabled: Boolean = true,
    fillWidth: Boolean = false,
) {
    val palette = LocalGlassPalette.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "buttonScale",
    )
    val tint = when (style) {
        GlassButtonStyle.Glass -> palette.controlTint
        GlassButtonStyle.Prominent -> palette.accent
        GlassButtonStyle.Destructive -> palette.danger
    }
    // Filled buttons keep a single accessible pairing: dark ink on the saturated
    // fill in dark appearance, white ink on it in light appearance.
    val contentColor = when {
        style == GlassButtonStyle.Glass -> palette.textPrimary
        palette.isDark -> Color(0xFF10151C)
        else -> Color.White
    }
    val shape = CircleShape
    Row(
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.45f
            }
            .shadow(6.dp, shape, clip = false, ambientColor = palette.shadow, spotColor = palette.shadow)
            .liquidGlass(backdrop, shape, tint)
            .clip(shape)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .height(48.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (fillWidth) Arrangement.Center else Arrangement.Start,
    ) {
        if (iconRes != null) {
            GlassIcon(iconRes, size = 18, tint = contentColor)
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = GlassType.button, color = contentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun GlassIconButton(
    iconRes: Int,
    backdrop: Backdrop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    tint: Color = LocalGlassPalette.current.controlTint,
) {
    val palette = LocalGlassPalette.current
    Box(
        modifier = modifier
            .size(width = maxOf(size, 48.dp), height = maxOf(size, 48.dp))
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .liquidGlass(backdrop, CircleShape, tint),
            contentAlignment = Alignment.Center,
        ) {
            GlassIcon(iconRes, size = (size.value * 0.5f).toInt(), tint = palette.textPrimary)
        }
    }
}

@Composable
fun LiquidSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val palette = LocalGlassPalette.current
    val trackTint = if (checked) palette.accent.copy(alpha = 0.85f) else palette.controlTint
    val thumbOffset by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "thumb",
    )
    Box(
        modifier = modifier
            .size(width = 56.dp, height = 48.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 32.dp)
                .liquidGlass(backdrop, CircleShape, trackTint),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .offset(x = thumbOffset)
                    .size(28.dp)
                    .shadow(4.dp, CircleShape, clip = false, ambientColor = palette.shadow, spotColor = palette.shadow)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.96f))
            )
        }
    }
}

@Composable
fun GlassDivider(modifier: Modifier = Modifier, color: Color = LocalGlassPalette.current.hairline) {
    Box(modifier.fillMaxWidth().height(1.dp).background(color.copy(alpha = color.alpha * 0.5f)))
}

/** A settings/apps style row: leading icon, title + summary, trailing slot. */
@Composable
fun GlassRow(
    backdrop: Backdrop,
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    iconRes: Int? = null,
    iconTint: Color = LocalGlassPalette.current.textPrimary,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    val palette = LocalGlassPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconRes != null) {
            Box(
                Modifier
                    .size(38.dp)
                    .liquidGlass(backdrop, RoundedCornerShape(12.dp), palette.controlTint),
                contentAlignment = Alignment.Center,
            ) {
                GlassIcon(iconRes, size = 20, tint = iconTint)
            }
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = GlassType.body,
                color = if (enabled) palette.textPrimary else palette.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = GlassType.caption,
                    color = palette.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
        }
    }
}