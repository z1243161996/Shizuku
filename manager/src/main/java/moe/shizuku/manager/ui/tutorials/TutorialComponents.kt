package moe.shizuku.manager.ui.tutorials

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import moe.shizuku.manager.ui.miuix.MiuixGlassCard
import moe.shizuku.manager.ui.miuix.MiuixTopBar
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal val CardSpacing = 14.dp

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
fun BackChevronIcon(
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.onSurface,
    size: Dp = 20.dp,
) {
    Canvas(modifier.size(size)) {
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        val w = this.size.width
        val h = this.size.height
        drawLine(
            color = color,
            start = Offset(w * 0.62f, h * 0.22f),
            end = Offset(w * 0.38f, h * 0.50f),
            strokeWidth = stroke.width,
            cap = stroke.cap,
        )
        drawLine(
            color = color,
            start = Offset(w * 0.38f, h * 0.50f),
            end = Offset(w * 0.62f, h * 0.78f),
            strokeWidth = stroke.width,
            cap = stroke.cap,
        )
    }
}

@Composable
fun TutorialScaffold(
    title: String,
    onBack: () -> Unit,
    dialogs: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            MiuixTopBar(
                title = title,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        BackChevronIcon()
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            content = content,
        )
        dialogs()
    }
}

@Composable
fun CommandBlock(command: String, modifier: Modifier = Modifier) {
    SelectionContainer {
        Text(
            text = command,
            style = MiuixTheme.textStyles.body2.copy(fontFamily = FontFamily.Monospace),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MiuixTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

@Composable
fun TutorialStep(
    iconRes: Int,
    title: String,
    modifier: Modifier = Modifier,
    titleMono: Array<String> = emptyArray(),
    description: String? = null,
    descriptionMono: Array<String> = emptyArray(),
    command: String? = null,
    buttonText: String? = null,
    buttonIcon: Int? = null,
    onButtonClick: (() -> Unit)? = null,
) {
    val colors = MiuixTheme.colorScheme
    val text = MiuixTheme.textStyles
    Row(modifier.fillMaxWidth()) {
        Icon(painterResource(iconRes), null, Modifier.size(24.dp), tint = colors.onSurfaceVariantSummary)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = monoAnnotated(title, *titleMono),
                style = text.body1,
                color = colors.onSurface,
            )
            if (description != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = monoAnnotated(description, *descriptionMono),
                    style = text.body2,
                    color = colors.onSurfaceVariantSummary,
                )
            }
            if (command != null) {
                Spacer(Modifier.height(10.dp))
                CommandBlock(command)
            }
            if (buttonText != null && onButtonClick != null) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onButtonClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                ) {
                    buttonIcon?.let { icon ->
                        Icon(painterResource(icon), null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(buttonText, style = text.button)
                }
            }
        }
    }
}

@Composable
fun TutorialInfoCard(
    iconRes: Int,
    modifier: Modifier = Modifier,
    iconTint: Color = MiuixTheme.colorScheme.onSurface,
    title: String? = null,
    body: AnnotatedString? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val colors = MiuixTheme.colorScheme
    val text = MiuixTheme.textStyles
    MiuixGlassCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(painterResource(iconRes), null, Modifier.size(22.dp), tint = iconTint)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                if (title != null) {
                    Text(title, style = text.title4, color = colors.onSurface)
                    Spacer(Modifier.height(6.dp))
                }
                if (body != null) {
                    Text(body, style = text.body2, color = colors.onSurfaceVariantSummary)
                }
                content()
            }
        }
    }
}
