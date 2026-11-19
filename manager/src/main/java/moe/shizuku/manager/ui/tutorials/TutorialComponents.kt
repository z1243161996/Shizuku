package moe.shizuku.manager.ui.tutorials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import moe.shizuku.manager.ui.glass.GlassButton
import moe.shizuku.manager.ui.glass.GlassCard
import moe.shizuku.manager.ui.glass.GlassIcon
import moe.shizuku.manager.ui.glass.GlassType
import moe.shizuku.manager.ui.glass.LocalGlassPalette
import moe.shizuku.manager.ui.glass.liquidGlass

fun monoAnnotated(text: String, vararg monoParts: String): AnnotatedString {
    val monoStyle = SpanStyle(fontFamily = FontFamily.Monospace)
    return buildAnnotatedString {
        append(text)
        for (part in monoParts) {
            if (part.isEmpty()) continue
            var start = 0
            while (start < text.length) {
                val index = text.indexOf(part, start)
                if (index < 0) break
                addStyle(monoStyle, index, index + part.length)
                start = index + part.length
            }
        }
    }
}

@Composable
fun CommandBlock(command: String, backdrop: LayerBackdrop) {
    val palette = LocalGlassPalette.current
    SelectionContainer {
        Text(
            text = command,
            style = GlassType.mono,
            color = palette.textSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(backdrop, RoundedCornerShape(12.dp), palette.controlTint, blurRadius = 12.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

@Composable
fun TutorialStep(
    iconRes: Int,
    title: String,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    titleMono: Array<String> = emptyArray(),
    description: String? = null,
    descriptionMono: Array<String> = emptyArray(),
    command: String? = null,
    buttonText: String? = null,
    buttonIcon: Int? = null,
    onButtonClick: (() -> Unit)? = null,
) {
    val palette = LocalGlassPalette.current
    Row(modifier.fillMaxWidth()) {
        GlassIcon(iconRes, size = 24, tint = palette.textSecondary)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = monoAnnotated(title, *titleMono),
                style = GlassType.body,
                color = palette.textPrimary,
            )
            if (description != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = monoAnnotated(description, *descriptionMono),
                    style = GlassType.body,
                    color = palette.textSecondary,
                )
            }
            if (command != null) {
                Spacer(Modifier.height(10.dp))
                CommandBlock(command, backdrop)
            }
            if (buttonText != null && onButtonClick != null) {
                Spacer(Modifier.height(10.dp))
                GlassButton(
                    text = buttonText,
                    backdrop = backdrop,
                    onClick = onButtonClick,
                    iconRes = buttonIcon,
                )
            }
        }
    }
}

@Composable
fun GlassInfoCard(
    backdrop: LayerBackdrop,
    iconRes: Int,
    modifier: Modifier = Modifier,
    iconTint: Color = LocalGlassPalette.current.textPrimary,
    title: String? = null,
    body: AnnotatedString? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val palette = LocalGlassPalette.current
    GlassCard(backdrop, modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            GlassIcon(iconRes, size = 22, tint = iconTint)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                if (title != null) {
                    Text(title, style = GlassType.cardTitle, color = palette.textPrimary)
                    Spacer(Modifier.height(6.dp))
                }
                if (body != null) {
                    Text(body, style = GlassType.body, color = palette.textSecondary)
                }
                content()
            }
        }
    }
}
