package moe.shizuku.manager.ui.miuix

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** One snackbar request, mirroring the previous glass message shape. */
data class MiuixSnackbarMessage(
    val text: String,
    val actionText: String? = null,
    val onAction: (() -> Unit)? = null,
    val onDismiss: (() -> Unit)? = null,
)

/** Bottom snackbar with an optional action; stays until the host clears it. */
@Composable
fun MiuixSnackbarHost(message: MiuixSnackbarMessage?, modifier: Modifier = Modifier) {
    if (message == null) return
    Card(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .fillMaxWidth()
            .widthIn(max = 560.dp),
        cornerRadius = 20.dp,
        insideMargin = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                message.text,
                modifier = Modifier.weight(1f),
                style = MiuixTheme.textStyles.body2,
            )
            message.actionText?.let { action ->
                Text(
                    action,
                    modifier = Modifier
                        .clickable { message.onAction?.invoke() }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    color = MiuixTheme.colorScheme.primary,
                    style = MiuixTheme.textStyles.button,
                )
            }
        }
    }
}
