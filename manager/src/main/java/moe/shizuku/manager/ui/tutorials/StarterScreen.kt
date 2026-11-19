package moe.shizuku.manager.ui.tutorials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import moe.shizuku.manager.R
import moe.shizuku.manager.ui.miuix.MiuixGlassCard
import moe.shizuku.manager.ui.miuix.MiuixTopBar
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun StarterScreen(
    log: String,
    errorMessage: String?,
    onBack: () -> Unit,
    onDismissError: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val text = MiuixTheme.textStyles
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            MiuixTopBar(
                title = stringResource(R.string.starter),
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(6.dp))
            val scrollState = rememberScrollState()
            LaunchedEffect(Unit) {
                snapshotFlow { scrollState.maxValue }.collect { scrollState.scrollTo(it) }
            }
            MiuixGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = 14.dp,
            ) {
                SelectionContainer {
                    Text(
                        text = log,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                        style = text.body2.copy(fontFamily = FontFamily.Monospace),
                        color = colors.onSurface,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        errorMessage?.let {
            OverlayDialog(
                show = true,
                summary = it,
                onDismissRequest = onDismissError,
            ) {
                Button(
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    onClick = onDismissError,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(android.R.string.ok), style = text.button)
                }
            }
        }
    }
}
