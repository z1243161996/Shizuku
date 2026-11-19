package moe.shizuku.manager.ui.tutorials

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import moe.shizuku.manager.R
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AdbPairingTutorialScreen(
    notificationEnabled: Boolean,
    isMiui: Boolean,
    onBack: () -> Unit,
    onDeveloperOptions: () -> Unit,
    onNotificationOptions: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val text = MiuixTheme.textStyles
    TutorialScaffold(
        title = stringResource(R.string.adb_pairing),
        onBack = onBack,
    ) {
        Spacer(Modifier.height(6.dp))
        if (notificationEnabled) {
            TutorialInfoCard(
                iconRes = R.drawable.ic_outline_notifications_active_24,
                body = AnnotatedString(stringResource(R.string.adb_pairing_tutorial_content_notification)),
            )
        } else {
            TutorialInfoCard(
                iconRes = R.drawable.ic_outline_info_24,
                body = AnnotatedString(stringResource(R.string.adb_pairing_tutorial_content_notification_blocked)),
            ) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onNotificationOptions,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        color = colors.error,
                        contentColor = colors.onError,
                    ),
                ) {
                    Icon(painterResource(R.drawable.ic_outline_open_in_new_24), null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.notification_settings), style = text.button)
                }
            }
        }
        if (notificationEnabled) {
            Spacer(Modifier.height(CardSpacing))
            TutorialInfoCard(
                iconRes = R.drawable.ic_help_outline_24dp,
                body = AnnotatedString(stringResource(R.string.adb_pairing_tutorial_content_network)),
            ) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.adb_pairing_tutorial_content_network_limation_not_foreground),
                    style = text.footnote1,
                    color = colors.onSurfaceVariantSummary,
                )
            }
        }
        if (isMiui) {
            Spacer(Modifier.height(CardSpacing))
            TutorialInfoCard(
                iconRes = R.drawable.ic_warning_24,
                body = AnnotatedString(stringResource(R.string.adb_pairing_tutorial_content_miui)),
            ) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.adb_pairing_tutorial_content_miui_2),
                    style = text.footnote1,
                    color = colors.onSurfaceVariantSummary,
                )
            }
        }
        if (notificationEnabled) {
            Spacer(Modifier.height(22.dp))
            TutorialStep(
                iconRes = R.drawable.ic_numeric_1_circle_outline_24,
                title = stringResource(R.string.adb_pairing_tutorial_content_steps),
                buttonText = stringResource(R.string.development_settings),
                buttonIcon = R.drawable.ic_outline_open_in_new_24,
                onButtonClick = onDeveloperOptions,
            )
            Spacer(Modifier.height(CardSpacing))
            TutorialStep(
                iconRes = R.drawable.ic_numeric_2_circle_outline_24,
                title = stringResource(R.string.adb_pairing_tutorial_content_enter_pairing_code),
            )
            Spacer(Modifier.height(CardSpacing))
            TutorialStep(
                iconRes = R.drawable.ic_numeric_3_circle_outline_24,
                title = stringResource(R.string.adb_pairing_tutorial_content_finish),
            )
        }
    }
}
