package moe.shizuku.manager.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import moe.shizuku.manager.R
import moe.shizuku.manager.ui.glass.GlassButton
import moe.shizuku.manager.ui.glass.GlassButtonStyle
import moe.shizuku.manager.ui.glass.GlassCard
import moe.shizuku.manager.ui.glass.GlassChevron
import moe.shizuku.manager.ui.glass.GlassChevronDirection
import moe.shizuku.manager.ui.glass.GlassChip
import moe.shizuku.manager.ui.glass.GlassDialogActions
import moe.shizuku.manager.ui.glass.GlassDialogOverlay
import moe.shizuku.manager.ui.glass.GlassDialogText
import moe.shizuku.manager.ui.glass.GlassDialogTitle
import moe.shizuku.manager.ui.glass.GlassDivider
import moe.shizuku.manager.ui.glass.GlassRow
import moe.shizuku.manager.ui.glass.GlassScaffold
import moe.shizuku.manager.ui.glass.GlassSnackbarHost
import moe.shizuku.manager.ui.glass.GlassSnackbarMessage
import moe.shizuku.manager.ui.glass.GlassType
import moe.shizuku.manager.ui.glass.LiquidSwitch
import moe.shizuku.manager.ui.glass.LocalGlassPalette
import moe.shizuku.manager.ui.glass.liquidGlass
import moe.shizuku.manager.ui.glass.stripHtml

enum class SettingsDialog {
    None,
    StartOnBootBug,
    StopTcp,
    RestartTcpMode,
    RestartTcpPort,
    TcpPort,
    Language,
    NightMode,
    UpdateMode,
    BugReport,
}

sealed interface SettingsAction {
    object Back : SettingsAction
    data class StartOnBootToggled(val value: Boolean) : SettingsAction
    data class WatchdogToggled(val value: Boolean) : SettingsAction
    data class TcpModeToggled(val value: Boolean) : SettingsAction
    object OpenTcpPort : SettingsAction
    data class TcpPortSubmitted(val text: String) : SettingsAction
    data class AutoDisableUsbToggled(val value: Boolean) : SettingsAction
    object OpenLanguage : SettingsAction
    data class LanguageSelected(val tag: String) : SettingsAction
    object TranslationClicked : SettingsAction
    object OpenNightMode : SettingsAction
    data class NightModeSelected(val value: Int) : SettingsAction
    data class BlackNightThemeToggled(val value: Boolean) : SettingsAction
    data class UseSystemColorToggled(val value: Boolean) : SettingsAction
    object OpenUpdateMode : SettingsAction
    data class UpdateModeSelected(val value: Int) : SettingsAction
    object HelpClicked : SettingsAction
    object ReportBugClicked : SettingsAction
    object BugReportGitHub : SettingsAction
    object BugReportEmail : SettingsAction
    object BugReportCancel : SettingsAction
    data class BugReportLinkClicked(val url: String) : SettingsAction
    data class LegacyPairingToggled(val value: Boolean) : SettingsAction
    object DialogConfirmed : SettingsAction
    object DialogCancelled : SettingsAction
    object DialogDismissed : SettingsAction
    object DismissSnackbar : SettingsAction
}

data class SettingsUiState(
    val startOnBootEnabled: Boolean = true,
    val startOnBootChecked: Boolean = false,
    val watchdogRunning: Boolean = false,
    val tcpModeVisible: Boolean = true,
    val tcpModeChecked: Boolean = true,
    val tcpModeEnabled: Boolean = true,
    val tcpModeSummary: String = "",
    val tcpPortVisible: Boolean = false,
    val tcpPortValue: String = "",
    val autoDisableUsbChecked: Boolean = false,
    val languageTag: String = "SYSTEM",
    val languageSummary: String = "",
    val translationContributors: String = "",
    val nightMode: Int = -1,
    val blackNightVisible: Boolean = true,
    val blackNightChecked: Boolean = false,
    val useSystemColorVisible: Boolean = true,
    val useSystemColorChecked: Boolean = false,
    val updateMode: Int = 1,
    val legacyPairingVisible: Boolean = true,
    val legacyPairingChecked: Boolean = false,
    val running: Boolean = false,
    val adbTcpPort: Int = 0,
    val dialog: SettingsDialog = SettingsDialog.None,
)

data class LocaleOption(
    val tag: String,
    val nativeName: String,
    val localizedName: String,
)

private data class DialogOption(
    val tag: String,
    val title: String,
    val subtitle: String? = null,
)

@Composable
fun SettingsScreen(
    backdrop: LayerBackdrop,
    state: SettingsUiState,
    languageOptions: List<LocaleOption>,
    snackbar: GlassSnackbarMessage?,
    onAction: (SettingsAction) -> Unit,
) {
    GlassScaffold(
        title = stringResource(R.string.settings_title),
        backdrop = backdrop,
        onBack = { onAction(SettingsAction.Back) },
        floating = { GlassSnackbarHost(snackbar, backdrop) },
    ) {
        Spacer(Modifier.height(6.dp))
        SectionLabel(stringResource(R.string.settings_behavior))
        Spacer(Modifier.height(10.dp))
        BehaviorCard(backdrop, state, onAction)

        Spacer(Modifier.height(22.dp))
        SectionLabel(stringResource(R.string.settings_language))
        Spacer(Modifier.height(10.dp))
        LanguageCard(backdrop, state, onAction)

        Spacer(Modifier.height(22.dp))
        SectionLabel(stringResource(R.string.settings_user_interface))
        Spacer(Modifier.height(10.dp))
        AppearanceCard(backdrop, state, onAction)

        Spacer(Modifier.height(22.dp))
        SectionLabel(stringResource(R.string.settings_support))
        Spacer(Modifier.height(10.dp))
        SupportCard(backdrop, state, onAction)

        if (state.legacyPairingVisible) {
            Spacer(Modifier.height(22.dp))
            SectionLabel(stringResource(R.string.settings_advanced))
            Spacer(Modifier.height(10.dp))
            AdvancedCard(backdrop, state, onAction)
        }

        Spacer(Modifier.height(18.dp))
    }

    when (state.dialog) {
        SettingsDialog.None -> Unit
        SettingsDialog.StartOnBootBug -> ConfirmDialog(
            backdrop = backdrop,
            message = stringResource(R.string.settings_start_on_boot_bug).stripHtml(),
            onConfirm = { onAction(SettingsAction.DialogConfirmed) },
            onCancel = { onAction(SettingsAction.DialogCancelled) },
        )
        SettingsDialog.StopTcp -> ConfirmDialog(
            backdrop = backdrop,
            message = stringResource(R.string.settings_tcp_mode_dialog_close_port).stripHtml(),
            onConfirm = { onAction(SettingsAction.DialogConfirmed) },
            onCancel = { onAction(SettingsAction.DialogCancelled) },
        )
        SettingsDialog.RestartTcpMode -> ConfirmDialog(
            backdrop = backdrop,
            title = stringResource(R.string.settings_restart_dialog_title),
            message = (stringResource(R.string.settings_restart_dialog_message) +
                    stringResource(R.string.settings_restart_dialog_message_wifi_required)).stripHtml(),
            onConfirm = { onAction(SettingsAction.DialogConfirmed) },
            onCancel = { onAction(SettingsAction.DialogCancelled) },
        )
        SettingsDialog.RestartTcpPort -> ConfirmDialog(
            backdrop = backdrop,
            title = stringResource(R.string.settings_restart_dialog_title),
            message = stringResource(R.string.settings_restart_dialog_message).stripHtml(),
            onConfirm = { onAction(SettingsAction.DialogConfirmed) },
            onCancel = { onAction(SettingsAction.DialogCancelled) },
        )
        SettingsDialog.TcpPort -> TcpPortDialog(
            backdrop = backdrop,
            initial = state.tcpPortValue,
            onConfirm = { onAction(SettingsAction.TcpPortSubmitted(it)) },
            onCancel = { onAction(SettingsAction.DialogCancelled) },
        )
        SettingsDialog.Language -> ListDialog(
            backdrop = backdrop,
            title = stringResource(R.string.settings_language),
            options = languageOptions.map { DialogOption(it.tag, it.nativeName, it.localizedName) },
            selectedTag = state.languageTag,
            onSelect = { onAction(SettingsAction.LanguageSelected(it)) },
            onDismiss = { onAction(SettingsAction.DialogDismissed) },
        )
        SettingsDialog.NightMode -> ListDialog(
            backdrop = backdrop,
            title = stringResource(R.string.dark_theme),
            options = listOf(
                DialogOption("1", stringResource(R.string.dark_theme_off)),
                DialogOption("2", stringResource(R.string.dark_theme_on)),
                DialogOption("-1", stringResource(R.string.follow_system)),
            ),
            selectedTag = state.nightMode.toString(),
            onSelect = { onAction(SettingsAction.NightModeSelected(it.toInt())) },
            onDismiss = { onAction(SettingsAction.DialogDismissed) },
        )
        SettingsDialog.UpdateMode -> ListDialog(
            backdrop = backdrop,
            title = stringResource(R.string.check_for_updates),
            options = listOf(
                DialogOption("0", stringResource(R.string.off)),
                DialogOption("1", stringResource(R.string.settings_update_stable)),
                DialogOption("2", stringResource(R.string.settings_update_beta)),
            ),
            selectedTag = state.updateMode.toString(),
            onSelect = { onAction(SettingsAction.UpdateModeSelected(it.toInt())) },
            onDismiss = { onAction(SettingsAction.DialogDismissed) },
        )
        SettingsDialog.BugReport -> BugReportDialog(backdrop, onAction)
    }
}

@Composable
private fun SectionLabel(text: String) {
    val palette = LocalGlassPalette.current
    Text(
        text = text.uppercase(),
        modifier = Modifier.padding(start = 6.dp),
        style = GlassType.label,
        color = palette.textPrimary.copy(alpha = 0.75f),
    )
}

@Composable
private fun BehaviorCard(
    backdrop: LayerBackdrop,
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
) {
    val tcpModeRestart = (state.adbTcpPort > 0) != state.tcpModeChecked
    val currentPort = state.tcpPortValue.toIntOrNull() ?: 5555
    val tcpPortRestart = state.adbTcpPort > 0 && state.adbTcpPort != currentPort

    GlassCard(backdrop, Modifier.fillMaxWidth(), contentPadding = 8.dp) {
        SettingToggleRow(
            backdrop = backdrop,
            title = stringResource(R.string.settings_start_on_boot),
            summary = if (state.startOnBootEnabled) null else stringResource(R.string.settings_start_on_boot_summary),
            checked = state.startOnBootChecked,
            enabled = state.startOnBootEnabled,
            onToggle = { onAction(SettingsAction.StartOnBootToggled(it)) },
        )
        GlassDivider(Modifier.padding(horizontal = 12.dp))
        SettingToggleRow(
            backdrop = backdrop,
            title = stringResource(R.string.settings_watchdog),
            summary = stringResource(R.string.settings_watchdog_summary),
            checked = state.watchdogRunning,
            enabled = true,
            onToggle = { onAction(SettingsAction.WatchdogToggled(it)) },
        )
        if (state.tcpModeVisible) {
            GlassDivider(Modifier.padding(horizontal = 12.dp))
            SettingToggleRow(
                backdrop = backdrop,
                title = stringResource(R.string.settings_tcp_mode),
                summary = state.tcpModeSummary,
                checked = state.tcpModeChecked,
                enabled = state.tcpModeEnabled,
                restartRequired = tcpModeRestart,
                onToggle = { onAction(SettingsAction.TcpModeToggled(it)) },
            )
        }
        if (state.tcpPortVisible) {
            GlassDivider(Modifier.padding(horizontal = 12.dp))
            SettingValueRow(
                backdrop = backdrop,
                title = stringResource(R.string.settings_tcp_port),
                summary = if (state.tcpPortValue.isEmpty()) {
                    stringResource(R.string.settings_tcp_port_default)
                } else {
                    state.tcpPortValue
                },
                restartRequired = tcpPortRestart,
                onClick = { onAction(SettingsAction.OpenTcpPort) },
            )
        }
        GlassDivider(Modifier.padding(horizontal = 12.dp))
        SettingToggleRow(
            backdrop = backdrop,
            title = stringResource(R.string.settings_auto_disable_usb_debugging),
            summary = stringResource(R.string.settings_auto_disable_usb_debugging_summary),
            checked = state.autoDisableUsbChecked,
            enabled = true,
            onToggle = { onAction(SettingsAction.AutoDisableUsbToggled(it)) },
        )
    }
}

@Composable
private fun LanguageCard(
    backdrop: LayerBackdrop,
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
) {
    GlassCard(backdrop, Modifier.fillMaxWidth(), contentPadding = 8.dp) {
        SettingValueRow(
            backdrop = backdrop,
            title = stringResource(R.string.settings_language),
            summary = state.languageSummary,
            onClick = { onAction(SettingsAction.OpenLanguage) },
        )
        if (state.translationContributors.isNotBlank()) {
            GlassDivider(Modifier.padding(horizontal = 12.dp))
            GlassRow(
                backdrop = backdrop,
                title = stringResource(R.string.settings_translation_contributors),
                summary = state.translationContributors,
            )
        }
        GlassDivider(Modifier.padding(horizontal = 12.dp))
        SettingValueRow(
            backdrop = backdrop,
            title = stringResource(R.string.settings_translation),
            summary = stringResource(R.string.settings_translation_summary, stringResource(R.string.app_name)),
            onClick = { onAction(SettingsAction.TranslationClicked) },
        )
    }
}

@Composable
private fun AppearanceCard(
    backdrop: LayerBackdrop,
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
) {
    val nightSummary = when (state.nightMode) {
        1 -> stringResource(R.string.dark_theme_off)
        2 -> stringResource(R.string.dark_theme_on)
        else -> stringResource(R.string.follow_system)
    }
    GlassCard(backdrop, Modifier.fillMaxWidth(), contentPadding = 8.dp) {
        SettingValueRow(
            backdrop = backdrop,
            title = stringResource(R.string.dark_theme),
            summary = nightSummary,
            onClick = { onAction(SettingsAction.OpenNightMode) },
        )
        if (state.blackNightVisible) {
            GlassDivider(Modifier.padding(horizontal = 12.dp))
            SettingToggleRow(
                backdrop = backdrop,
                title = stringResource(R.string.settings_black_night_theme),
                summary = stringResource(R.string.settings_black_night_theme_summary),
                checked = state.blackNightChecked,
                enabled = true,
                onToggle = { onAction(SettingsAction.BlackNightThemeToggled(it)) },
            )
        }
        if (state.useSystemColorVisible) {
            GlassDivider(Modifier.padding(horizontal = 12.dp))
            SettingToggleRow(
                backdrop = backdrop,
                title = stringResource(R.string.settings_use_system_color),
                summary = null,
                checked = state.useSystemColorChecked,
                enabled = true,
                onToggle = { onAction(SettingsAction.UseSystemColorToggled(it)) },
            )
        }
    }
}

@Composable
private fun SupportCard(
    backdrop: LayerBackdrop,
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
) {
    val updateSummary = when (state.updateMode) {
        0 -> stringResource(R.string.off)
        1 -> stringResource(R.string.settings_update_stable)
        else -> stringResource(R.string.settings_update_beta)
    }
    GlassCard(backdrop, Modifier.fillMaxWidth(), contentPadding = 8.dp) {
        SettingValueRow(
            backdrop = backdrop,
            title = stringResource(R.string.check_for_updates),
            summary = updateSummary,
            onClick = { onAction(SettingsAction.OpenUpdateMode) },
        )
        GlassDivider(Modifier.padding(horizontal = 12.dp))
        SettingValueRow(
            backdrop = backdrop,
            title = stringResource(R.string.settings_help),
            summary = null,
            onClick = { onAction(SettingsAction.HelpClicked) },
        )
        GlassDivider(Modifier.padding(horizontal = 12.dp))
        SettingValueRow(
            backdrop = backdrop,
            title = stringResource(R.string.settings_report_bug),
            summary = null,
            onClick = { onAction(SettingsAction.ReportBugClicked) },
        )
    }
}

@Composable
private fun AdvancedCard(
    backdrop: LayerBackdrop,
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
) {
    GlassCard(backdrop, Modifier.fillMaxWidth(), contentPadding = 8.dp) {
        SettingToggleRow(
            backdrop = backdrop,
            title = stringResource(R.string.settings_legacy_pairing),
            summary = stringResource(R.string.settings_legacy_pairing_summary),
            checked = state.legacyPairingChecked,
            enabled = true,
            onToggle = { onAction(SettingsAction.LegacyPairingToggled(it)) },
        )
    }
}

@Composable
private fun SettingToggleRow(
    backdrop: LayerBackdrop,
    title: String,
    summary: String?,
    checked: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    restartRequired: Boolean = false,
) {
    GlassRow(
        backdrop = backdrop,
        title = title,
        summary = summary,
        enabled = enabled,
        onClick = if (enabled) {
            { onToggle(!checked) }
        } else {
            null
        },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (restartRequired) {
                    GlassChip(
                        text = stringResource(R.string.glass_settings_restart_required),
                        backdrop = backdrop,
                        dotColor = LocalGlassPalette.current.warning,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                LiquidSwitch(
                    checked = checked,
                    onCheckedChange = onToggle,
                    backdrop = backdrop,
                    enabled = enabled,
                )
            }
        },
    )
}

@Composable
private fun SettingValueRow(
    backdrop: LayerBackdrop,
    title: String,
    summary: String?,
    onClick: () -> Unit,
    restartRequired: Boolean = false,
) {
    GlassRow(
        backdrop = backdrop,
        title = title,
        summary = summary,
        onClick = onClick,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (restartRequired) {
                    GlassChip(
                        text = stringResource(R.string.glass_settings_restart_required),
                        backdrop = backdrop,
                        dotColor = LocalGlassPalette.current.warning,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                GlassChevron(GlassChevronDirection.End)
            }
        },
    )
}

@Composable
private fun ConfirmDialog(
    backdrop: LayerBackdrop,
    message: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    title: String = stringResource(android.R.string.dialog_alert_title),
) {
    GlassDialogOverlay(backdrop = backdrop, onDismiss = onCancel) {
        GlassDialogTitle(title)
        GlassDialogText(message)
        GlassDialogActions(
            backdrop = backdrop,
            confirmText = stringResource(android.R.string.ok),
            onConfirm = onConfirm,
            dismissText = stringResource(android.R.string.cancel),
            onDismiss = onCancel,
        )
    }
}

@Composable
private fun TcpPortDialog(
    backdrop: LayerBackdrop,
    initial: String,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val palette = LocalGlassPalette.current
    var text by remember { mutableStateOf(initial) }
    GlassDialogOverlay(backdrop = backdrop, onDismiss = onCancel) {
        GlassDialogTitle(stringResource(R.string.settings_tcp_port))
        Box(
            Modifier
                .fillMaxWidth()
                .liquidGlass(backdrop, RoundedCornerShape(16.dp), palette.controlTint)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (text.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_tcp_port_hint),
                    style = GlassType.body,
                    color = palette.textTertiary,
                )
            }
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = GlassType.body.copy(color = palette.textPrimary),
                cursorBrush = SolidColor(palette.accent),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
        GlassDialogActions(
            backdrop = backdrop,
            confirmText = stringResource(android.R.string.ok),
            onConfirm = { onConfirm(text) },
            dismissText = stringResource(android.R.string.cancel),
            onDismiss = onCancel,
        )
    }
}

@Composable
private fun ListDialog(
    backdrop: LayerBackdrop,
    title: String,
    options: List<DialogOption>,
    selectedTag: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalGlassPalette.current
    GlassDialogOverlay(backdrop = backdrop, onDismiss = onDismiss) {
        GlassDialogTitle(title)
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            options.forEachIndexed { index, option ->
                val selected = option.tag == selectedTag
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(option.tag) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = option.title,
                            style = GlassType.body,
                            color = if (selected) palette.accent else palette.textPrimary,
                        )
                        if (option.subtitle != null) {
                            Text(
                                text = option.subtitle,
                                style = GlassType.caption,
                                color = palette.textTertiary,
                            )
                        }
                    }
                    if (selected) {
                        Text(
                            text = "✓",
                            style = GlassType.body,
                            color = palette.accent,
                        )
                    }
                }
                if (index != options.lastIndex) {
                    GlassDivider(Modifier.padding(horizontal = 12.dp))
                }
            }
        }
    }
}

@Composable
private fun BugReportDialog(
    backdrop: LayerBackdrop,
    onAction: (SettingsAction) -> Unit,
) {
    val issuesUrl = "https://github.com/thedjchi/Shizuku/issues/new"
    GlassDialogOverlay(
        backdrop = backdrop,
        onDismiss = { onAction(SettingsAction.BugReportCancel) },
    ) {
        GlassDialogTitle(stringResource(R.string.settings_report_bug))
        GlassDialogText(stringResource(R.string.bug_report_dialog_before_reporting))
        BugReportLinkRow(
            template = stringResource(R.string.bug_report_dialog_update),
            label = stringResource(R.string.bug_report_dialog_link_update),
            url = "https://github.com/thedjchi/Shizuku/releases/latest",
            onAction = onAction,
        )
        BugReportLinkRow(
            template = stringResource(R.string.bug_report_dialog_wiki),
            label = stringResource(R.string.bug_report_dialog_link_wiki),
            url = "https://github.com/thedjchi/Shizuku/releases/wiki#troubleshooting",
            onAction = onAction,
        )
        BugReportLinkRow(
            template = stringResource(R.string.bug_report_dialog_issues),
            label = stringResource(R.string.bug_report_dialog_link_issues),
            url = "https://github.com/thedjchi/Shizuku/releases/issues",
            onAction = onAction,
        )
        BugReportLinkRow(
            template = stringResource(R.string.bug_report_dialog_method),
            label = "GitHub",
            url = issuesUrl,
            onAction = onAction,
        )
        GlassDialogActions(
            backdrop = backdrop,
            confirmText = "GitHub",
            onConfirm = { onAction(SettingsAction.BugReportGitHub) },
            dismissText = stringResource(R.string.bug_report_dialog_button_email),
            onDismiss = { onAction(SettingsAction.BugReportEmail) },
        )
        GlassButton(
            text = stringResource(android.R.string.cancel),
            backdrop = backdrop,
            onClick = { onAction(SettingsAction.BugReportCancel) },
            style = GlassButtonStyle.Glass,
            fillWidth = true,
        )
    }
}

@Composable
private fun BugReportLinkRow(
    template: String,
    label: String,
    url: String,
    onAction: (SettingsAction) -> Unit,
) {
    val palette = LocalGlassPalette.current
    val annotated = remember(template, label, url) {
        buildAnnotatedString {
            val index = template.indexOf("^1")
            if (index >= 0) {
                append(template.substring(0, index))
                withStyle(SpanStyle(color = palette.accent)) {
                    pushStringAnnotation(tag = "URL", annotation = url)
                    append(label)
                    pop()
                }
                append(template.substring(index + 2))
            } else {
                append(template)
            }
        }
    }
    ClickableText(
        text = annotated,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        style = GlassType.body.copy(color = palette.textSecondary),
        onClick = { offset ->
            annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()
                ?.let { onAction(SettingsAction.BugReportLinkClicked(it.item)) }
        },
    )
}
