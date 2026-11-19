package moe.shizuku.manager.ui.tutorials

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import moe.shizuku.manager.R
import moe.shizuku.manager.stealth.Action
import moe.shizuku.manager.stealth.UiState
import moe.shizuku.manager.stealth.validatePackageName
import moe.shizuku.manager.ui.miuix.MiuixGlassCard
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

enum class StealthDialog { None, ChooseFolder, Uninstall, Error, Cancel }

@Composable
fun StealthTutorialScreen(
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
    val context = LocalContext.current
    val colors = MiuixTheme.colorScheme
    val text = MiuixTheme.textStyles
    var packageName by remember { mutableStateOf("") }
    var packageNameError by remember { mutableStateOf<String?>(null) }

    val loading = state is UiState.Loading || state is UiState.Pending
    val showPackageName = lastIdleAction == Action.HIDE
    val fabEnabled = packageNameError == null

    BackHandler(onBack = onBack)

    TutorialScaffold(
        title = stringResource(R.string.home_stealth_title),
        onBack = onBack,
        dialogs = {
            when (dialog) {
                StealthDialog.ChooseFolder -> OverlayDialog(
                    show = true,
                    title = stringResource(R.string.choose_folder),
                    summary = stringResource(R.string.stealth_choose_folder_message),
                    onDismissRequest = onDismissDialog,
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(
                            text = stringResource(android.R.string.cancel),
                            onClick = onDismissDialog,
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            colors = ButtonDefaults.buttonColorsPrimary(),
                            onClick = onChooseFolderConfirm,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.choose_folder), style = text.button)
                        }
                    }
                }

                StealthDialog.Uninstall -> OverlayDialog(
                    show = true,
                    title = stringResource(R.string.stealth_uninstall_required),
                    summary = stringResource(R.string.stealth_uninstall_message),
                    onDismissRequest = onDismissDialog,
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(
                            text = stringResource(android.R.string.cancel),
                            onClick = onDismissDialog,
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            colors = ButtonDefaults.buttonColors(
                                color = colors.error,
                                contentColor = colors.onError,
                            ),
                            onClick = onUninstallConfirm,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.stealth_uninstall), style = text.button)
                        }
                    }
                }

                StealthDialog.Error -> OverlayDialog(
                    show = true,
                    title = stringResource(R.string.error),
                    summary = dialogMessage.orEmpty(),
                    onDismissRequest = onDismissDialog,
                ) {
                    Button(
                        colors = ButtonDefaults.buttonColorsPrimary(),
                        onClick = onDismissDialog,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(android.R.string.ok), style = text.button)
                    }
                }

                StealthDialog.Cancel -> OverlayDialog(
                    show = true,
                    title = stringResource(android.R.string.cancel) + "?",
                    onDismissRequest = onDismissDialog,
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(
                            text = "No",
                            onClick = onDismissDialog,
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            colors = ButtonDefaults.buttonColorsPrimary(),
                            onClick = onCancelConfirm,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Yes", style = text.button)
                        }
                    }
                }

                StealthDialog.None -> Unit
            }
        },
    ) {
        Spacer(Modifier.height(6.dp))
        TutorialInfoCard(
            iconRes = R.drawable.ic_warning_24,
            title = stringResource(R.string.stealth_warning_compatibility),
            onClick = onCopyCompatibility,
        ) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.stealth_warning_compatibility_2),
                style = text.body2,
                color = colors.onSurfaceVariantSummary,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.stealth_warning_compatibility_3),
                style = text.body2,
                color = colors.onSurfaceVariantSummary,
            )
        }
        Spacer(Modifier.height(CardSpacing))
        TutorialInfoCard(
            iconRes = R.drawable.ic_warning_24,
            title = stringResource(R.string.stealth_warning_detection),
        )
        Spacer(Modifier.height(CardSpacing))
        TutorialInfoCard(
            iconRes = R.drawable.ic_help_outline_24dp,
            title = stringResource(R.string.stealth_info_reconfigure),
        ) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.stealth_info_rish),
                style = text.body2,
                color = colors.onSurfaceVariantSummary,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.stealth_info_intents),
                style = text.body2,
                color = colors.onSurfaceVariantSummary,
            )
        }
        Spacer(Modifier.height(CardSpacing))
        TutorialInfoCard(
            iconRes = R.drawable.ic_help_outline_24dp,
            body = AnnotatedString(stringResource(R.string.stealth_info_update)),
        )
        Spacer(Modifier.height(CardSpacing))
        if (showPackageName) {
            PackageNameCard(
                packageName = packageName,
                error = packageNameError,
                onValueChange = { input ->
                    packageName = input
                    packageNameError = input.validatePackageName()?.let { context.getString(it) }
                },
            )
            Spacer(Modifier.height(CardSpacing))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.padding(4.dp))
            } else {
                Button(
                    onClick = { onAction(lastIdleAction, packageName.ifEmpty { null }) },
                    enabled = fabEnabled,
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        painterResource(
                            if (lastIdleAction == Action.UNHIDE) {
                                R.drawable.ic_visibility_on_filled_24
                            } else {
                                R.drawable.ic_visibility_off_filled_24
                            }
                        ),
                        null,
                        Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(
                            if (lastIdleAction == Action.UNHIDE) R.string.stealth_unhide else R.string.stealth_hide
                        ),
                        style = text.button,
                    )
                }
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun PackageNameCard(
    packageName: String,
    error: String?,
    onValueChange: (String) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val text = MiuixTheme.textStyles
    MiuixGlassCard(modifier = Modifier.fillMaxWidth()) {
        TextField(
            value = packageName,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.stealth_package_name),
            useLabelAsPlaceholder = true,
            singleLine = true,
        )
        if (error != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = error,
                style = text.footnote1,
                color = colors.error,
            )
        } else if (packageName.isEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.stealth_package_name_helper_text),
                style = text.footnote1,
                color = colors.onSurfaceVariantSummary,
            )
        }
    }
}
