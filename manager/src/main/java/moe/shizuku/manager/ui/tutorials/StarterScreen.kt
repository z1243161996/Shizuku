package moe.shizuku.manager.ui.tutorials

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import moe.shizuku.manager.R
import moe.shizuku.manager.ui.glass.GlassCard
import moe.shizuku.manager.ui.glass.GlassDialogActions
import moe.shizuku.manager.ui.glass.GlassDialogOverlay
import moe.shizuku.manager.ui.glass.GlassDialogText
import moe.shizuku.manager.ui.glass.GlassScaffold
import moe.shizuku.manager.ui.glass.GlassType
import moe.shizuku.manager.ui.glass.LocalGlassPalette

@Composable
fun StarterScreen(
    backdrop: LayerBackdrop,
    log: String,
    errorMessage: String?,
    onBack: () -> Unit,
    onDismissError: () -> Unit,
) {
    val palette = LocalGlassPalette.current
    GlassScaffold(
        title = stringResource(R.string.starter),
        backdrop = backdrop,
        onBack = onBack,
        scrollable = false,
    ) {
        Spacer(Modifier.height(6.dp))
        val scrollState = rememberScrollState()
        LaunchedEffect(Unit) {
            snapshotFlow { scrollState.maxValue }.collect { scrollState.scrollTo(it) }
        }
        GlassCard(
            backdrop = backdrop,
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = 14.dp,
        ) {
            SelectionContainer {
                Text(
                    text = log,
                    modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
                    style = GlassType.mono,
                    color = palette.textPrimary,
                )
            }
        }
    }
    errorMessage?.let {
        GlassDialogOverlay(backdrop, onDismiss = onDismissError) {
            GlassDialogText(it)
            GlassDialogActions(
                backdrop = backdrop,
                confirmText = stringResource(android.R.string.ok),
                onConfirm = onDismissError,
            )
        }
    }
}
