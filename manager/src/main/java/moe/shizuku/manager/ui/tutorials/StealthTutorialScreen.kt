package moe.shizuku.manager.ui.tutorials

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import moe.shizuku.manager.R
import moe.shizuku.manager.stealth.Action
import moe.shizuku.manager.stealth.UiState
import moe.shizuku.manager.stealth.validatePackageName
import moe.shizuku.manager.ui.glass.GlassBody
import moe.shizuku.manager.ui.glass.GlassButton
import moe.shizuku.manager.ui.glass.GlassButtonStyle
import moe.shizuku.manager.ui.glass.GlassCard
import moe.shizuku.manager.ui.glass.GlassDialogActions
import moe.shizuku.manager.ui.glass.GlassDialogOverlay
import moe.shizuku.manager.ui.glass.GlassDialogText
import moe.shizuku.manager.ui.glass.GlassDialogTitle
import moe.shizuku.manager.ui.glass.GlassScaffold
import moe.shizuku.manager.ui.glass.LocalGlassPalette

enum class StealthDialog { None, ChooseFolder, Uninstall, Error, Cancel }

@Composable
fun StealthTutorialScreen(
    backdrop: LayerBackdrop,
    state: UiState,
    lastIdleAction: Action,
    dialog: StealthDialog,
    dialogMessage: String?,
    onBack: () -> Unit,
    onAction: (Action, String?) -> Unit,
    onCopyCompatibility: () -> Unit,
    onDismissDialog: () -> Unit,
    onChooseFolderConfirm: () -> Unit,
    onUninstallConfirm: () -> Unit,
    onCancelConfirm: () -> Unit,
) {
    val palette = LocalGlassPalette.current
    val context = LocalContext.current
    var packageName by remember { mutableStateOf("") }
    var packageNameError by remember { mutableStateOf<String?>(null) }

    val loading = state is UiState.Loading || state is UiState.Pending
    val showPackageName = lastIdleAction == Action.HIDE
    val fabEnabled = packageNameError == null

    BackHandler(onBack = onBack)

    GlassScaffold(
        title = stringResource(R.string.home_stealth_title),
        backdrop = backdrop,
        onBack = onBack,
        floating = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (showPackageName) {
                    PackageNameCard(
                        backdrop = backdrop,
                        packageName = packageName,
                        error = packageNameError,
                        onValueChange = { input ->
                            packageName = input
                            packageNameError = input.validatePackageName()?.let { context.getString(it) }
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.padding(4.dp), color = palette.accent)
                    } else {
                        GlassButton(
                            text = stringResource(
                                if (lastIdleAction == Action.UNHIDE) R.string.stealth_unhide else R.string.stealth_hide
                            ),
                            backdrop = backdrop,
                            onClick = { onAction(lastIdleAction, packageName.ifEmpty { null }) },
                            iconRes = if (lastIdleAction == Action.UNHIDE) {
                                R.drawable.ic_visibility_on_filled_24
                            } else {
                                R.drawable.ic_visibility_off_filled_24
                            },
                            style = GlassButtonStyle.Prominent,
                            enabled = fabEnabled,
                        )
                    }
                }
            }
        },
    ) {
        Spacer(Modifier.height(6.dp))
        GlassInfoCard(
            backdrop = backdrop,
            iconRes = R.drawable.ic_warning_24,
            title = stringResource(R.string.stealth_warning_compatibility),
            onClick = onCopyCompatibility,
        ) {
            Spacer(Modifier.height(12.dp))
            GlassBody(stringResource(R.string.stealth_warning_compatibility_2))
            Spacer(Modifier.height(12.dp))
            GlassBody(stringResource(R.string.stealth_warning_compatibility_3))
        }
        Spacer(Modifier.height(14.dp))
        GlassInfoCard(
            backdrop = backdrop,
            iconRes = R.drawable.ic_warning_24,
            title = stringResource(R.string.stealth_warning_detection),
        )
        Spacer(Modifier.height(14.dp))
        GlassInfoCard(
            backdrop = backdrop,
            iconRes = R.drawable.ic_help_outline_24dp,
            title = stringResource(R.string.stealth_info_reconfigure),
        ) {
            Spacer(Modifier.height(12.dp))
            GlassBody(stringResource(R.string.stealth_info_rish))
            Spacer(Modifier.height(12.dp))
            GlassBody(stringResource(R.string.stealth_info_intents))
        }
        Spacer(Modifier.height(14.dp))
        GlassInfoCard(
            backdrop = backdrop,
            iconRes = R.drawable.ic_help_outline_24dp,
            body = AnnotatedString(stringResource(R.string.stealth_info_update)),
        )
        Spacer(Modifier.height(if (showPackageName) 220.dp else 84.dp))
    }

    when (dialog) {
        StealthDialog.ChooseFolder -> GlassDialogOverlay(backdrop, onDismiss = onDismissDialog) {
            GlassDialogTitle(stringResource(R.string.choose_folder))
            GlassDialogText(stringResource(R.string.stealth_choose_folder_message))
            GlassDialogActions(
                backdrop = backdrop,
                confirmText = stringResource(R.string.choose_folder),
                onConfirm = onChooseFolderConfirm,
                dismissText = stringResource(android.R.string.cancel),
                onDismiss = onDismissDialog,
            )
        }
        StealthDialog.Uninstall -> GlassDialogOverlay(backdrop, onDismiss = onDismissDialog) {
            GlassDialogTitle(stringResource(R.string.stealth_uninstall_required))
            GlassDialogText(stringResource(R.string.stealth_uninstall_message))
            GlassDialogActions(
                backdrop = backdrop,
                confirmText = stringResource(R.string.stealth_uninstall),
                onConfirm = onUninstallConfirm,
                dismissText = stringResource(android.R.string.cancel),
                onDismiss = onDismissDialog,
            )
        }
        StealthDialog.Error -> GlassDialogOverlay(backdrop, onDismiss = onDismissDialog) {
            GlassDialogTitle(stringResource(R.string.error))
            GlassDialogText(dialogMessage.orEmpty())
            GlassDialogActions(
                backdrop = backdrop,
                confirmText = stringResource(android.R.string.ok),
                onConfirm = onDismissDialog,
            )
        }
        StealthDialog.Cancel -> GlassDialogOverlay(backdrop, onDismiss = onDismissDialog) {
            GlassDialogTitle(stringResource(android.R.string.cancel) + "?")
            GlassDialogActions(
                backdrop = backdrop,
                confirmText = "Yes",
                onConfirm = onCancelConfirm,
                dismissText = "No",
                onDismiss = onDismissDialog,
            )
        }
        StealthDialog.None -> Unit
    }
}

@Composable
private fun PackageNameCard(
    backdrop: LayerBackdrop,
    packageName: String,
    error: String?,
    onValueChange: (String) -> Unit,
) {
    val palette = LocalGlassPalette.current
    GlassCard(
        backdrop = backdrop,
        modifier = Modifier.fillMaxWidth().widthIn(max = 500.dp),
        contentPadding = 14.dp,
    ) {
        OutlinedTextField(
            value = packageName,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = error != null,
            label = { Text(stringResource(R.string.stealth_package_name)) },
            supportingText = if (error != null) {
                { Text(error) }
            } else if (packageName.isEmpty()) {
                { Text(stringResource(R.string.stealth_package_name_helper_text)) }
            } else {
                null
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = palette.accent,
                unfocusedBorderColor = palette.hairline,
                focusedLabelColor = palette.accent,
                unfocusedLabelColor = palette.textTertiary,
                cursorColor = palette.accent,
                errorBorderColor = palette.danger,
                errorLabelColor = palette.danger,
                errorTextColor = palette.danger,
                focusedSupportingTextColor = palette.textTertiary,
                unfocusedSupportingTextColor = palette.textTertiary,
                errorSupportingTextColor = palette.danger,
            ),
        )
    }
}
