package moe.shizuku.manager.ui.glass

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop

/** A floating glass toolbar that content scrolls underneath. */
@Composable
fun GlassTopBar(
    title: String,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val palette = LocalGlassPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .shadow(14.dp, CircleShape, clip = false, ambientColor = palette.shadow, spotColor = palette.shadow)
            .liquidGlass(backdrop, CircleShape, palette.barTint, blurRadius = 26.dp, refractionHeight = 8.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                GlassChevron(direction = GlassChevronDirection.Start, color = palette.textPrimary, size = 20.dp)
            }
        } else {
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f).padding(start = if (onBack != null) 4.dp else 0.dp)) {
            Text(
                text = title,
                style = GlassType.title,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = GlassType.caption,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
        Spacer(Modifier.width(4.dp))
    }
}

/**
 * Screen frame: backdrop driven wallpaper, floating top bar, scrollable body
 * and an optional floating layer (snackbars, dialogs).
 */
@Composable
fun GlassScaffold(
    title: String,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    scrollable: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    floating: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
        Column(Modifier.fillMaxSize()) {
            GlassTopBar(
                title = title,
                backdrop = backdrop,
                subtitle = subtitle,
                onBack = onBack,
                actions = actions,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            )
            val body = if (scrollable) {
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            } else {
                Modifier.fillMaxSize()
            }
            Column(body.padding(horizontal = 14.dp)) {
                content()
                Spacer(Modifier.height(28.dp))
            }
        }
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth(), content = floating)
    }
}

enum class GlassChevronDirection { Start, End }

/** Chevron drawn in code so no extra drawable is needed. */
@Composable
fun GlassChevron(
    direction: GlassChevronDirection,
    color: Color = LocalGlassPalette.current.textSecondary,
    size: Dp = 16.dp,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            if (direction == GlassChevronDirection.Start) {
                moveTo(w * 0.62f, h * 0.16f)
                lineTo(w * 0.32f, h * 0.5f)
                lineTo(w * 0.62f, h * 0.84f)
            } else {
                moveTo(w * 0.38f, h * 0.16f)
                lineTo(w * 0.68f, h * 0.5f)
                lineTo(w * 0.38f, h * 0.84f)
            }
        }
        drawPath(path, color = color, style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}

data class GlassSnackbarMessage(
    val text: String,
    val actionText: String? = null,
    val onAction: (() -> Unit)? = null,
    val onDismiss: (() -> Unit)? = null,
)

@Composable
fun GlassSnackbarHost(
    message: GlassSnackbarMessage?,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    val palette = LocalGlassPalette.current
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier,
    ) {
        val current = message ?: return@AnimatedVisibility
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .shadow(18.dp, RoundedCornerShape(22.dp), clip = false, ambientColor = palette.shadow, spotColor = palette.shadow)
                .liquidGlass(backdrop, RoundedCornerShape(22.dp), palette.barTint, blurRadius = 24.dp)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = current.text,
                style = GlassType.body,
                color = palette.textPrimary,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (current.actionText != null && current.onAction != null) {
                Spacer(Modifier.width(14.dp))
                Text(
                    text = current.actionText,
                    style = GlassType.button,
                    color = palette.accent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            current.onAction.invoke()
                            current.onDismiss?.invoke()
                        }
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                )
            }
        }
    }
}

/** In-window dialog so the glass panel can sample the same backdrop layer. */
@Composable
fun GlassDialogOverlay(
    backdrop: Backdrop,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalGlassPalette.current
    BackHandler(onBack = onDismiss)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.scrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 26.dp)
                .fillMaxWidth()
                .shadow(30.dp, RoundedCornerShape(28.dp), clip = false, ambientColor = palette.shadow, spotColor = palette.shadow)
                .liquidGlass(backdrop, RoundedCornerShape(28.dp), palette.dialogTint, blurRadius = 34.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(24.dp),
            content = content,
        )
    }
}

@Composable
fun GlassDialogTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = GlassType.title,
        color = LocalGlassPalette.current.textPrimary,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun GlassDialogText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = GlassType.body,
        color = LocalGlassPalette.current.textSecondary,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun GlassDialogActions(
    backdrop: Backdrop,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String? = null,
    onDismiss: (() -> Unit)? = null,
    destructive: Boolean = false,
) {
    Column(Modifier.fillMaxWidth()) {
        GlassButton(
            text = confirmText,
            backdrop = backdrop,
            onClick = onConfirm,
            style = if (destructive) GlassButtonStyle.Destructive else GlassButtonStyle.Prominent,
            fillWidth = true,
        )
        if (dismissText != null && onDismiss != null) {
            Spacer(Modifier.height(10.dp))
            GlassButton(
                text = dismissText,
                backdrop = backdrop,
                onClick = onDismiss,
                fillWidth = true,
            )
        }
    }
}
