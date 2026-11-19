package moe.shizuku.manager.ui.tutorials

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import moe.shizuku.manager.R
import moe.shizuku.manager.ui.glass.GlassButton
import moe.shizuku.manager.ui.glass.GlassButtonStyle
import moe.shizuku.manager.ui.glass.GlassCaption
import moe.shizuku.manager.ui.glass.GlassScaffold

@Composable
fun AdbPairingTutorialScreen(
    backdrop: LayerBackdrop,
    notificationEnabled: Boolean,
    isMiui: Boolean,
    onBack: () -> Unit,
    onDeveloperOptions: () -> Unit,
    onNotificationOptions: () -> Unit,
) {
    GlassScaffold(
        title = stringResource(R.string.adb_pairing),
        backdrop = backdrop,
        onBack = onBack,
    ) {
        Spacer(Modifier.height(6.dp))
        if (notificationEnabled) {
            GlassInfoCard(
                backdrop = backdrop,
                iconRes = R.drawable.ic_outline_notifications_active_24,
                body = AnnotatedString(stringResource(R.string.adb_pairing_tutorial_content_notification)),
            )
        } else {
            GlassInfoCard(
                backdrop = backdrop,
                iconRes = R.drawable.ic_outline_info_24,
                body = AnnotatedString(stringResource(R.string.adb_pairing_tutorial_content_notification_blocked)),
            ) {
                Spacer(Modifier.height(10.dp))
                GlassButton(
                    text = stringResource(R.string.notification_settings),
                    backdrop = backdrop,
                    onClick = onNotificationOptions,
                    iconRes = R.drawable.ic_outline_open_in_new_24,
                    style = GlassButtonStyle.Destructive,
                )
            }
        }
        if (notificationEnabled) {
            Spacer(Modifier.height(14.dp))
            GlassInfoCard(
                backdrop = backdrop,
                iconRes = R.drawable.ic_help_outline_24dp,
                body = AnnotatedString(stringResource(R.string.adb_pairing_tutorial_content_network)),
            ) {
                Spacer(Modifier.height(8.dp))
                GlassCaption(stringResource(R.string.adb_pairing_tutorial_content_network_limation_not_foreground))
            }
        }
        if (isMiui) {
            Spacer(Modifier.height(14.dp))
            GlassInfoCard(
                backdrop = backdrop,
                iconRes = R.drawable.ic_warning_24,
                body = AnnotatedString(stringResource(R.string.adb_pairing_tutorial_content_miui)),
            ) {
                Spacer(Modifier.height(8.dp))
                GlassCaption(stringResource(R.string.adb_pairing_tutorial_content_miui_2))
            }
        }
        if (notificationEnabled) {
            Spacer(Modifier.height(22.dp))
            TutorialStep(
                iconRes = R.drawable.ic_numeric_1_circle_outline_24,
                title = stringResource(R.string.adb_pairing_tutorial_content_steps),
                backdrop = backdrop,
                buttonText = stringResource(R.string.development_settings),
                buttonIcon = R.drawable.ic_outline_open_in_new_24,
                onButtonClick = onDeveloperOptions,
            )
            Spacer(Modifier.height(14.dp))
            TutorialStep(
                iconRes = R.drawable.ic_numeric_2_circle_outline_24,
                title = stringResource(R.string.adb_pairing_tutorial_content_enter_pairing_code),
                backdrop = backdrop,
            )
            Spacer(Modifier.height(14.dp))
            TutorialStep(
                iconRes = R.drawable.ic_numeric_3_circle_outline_24,
                title = stringResource(R.string.adb_pairing_tutorial_content_finish),
                backdrop = backdrop,
            )
        }
    }
}
