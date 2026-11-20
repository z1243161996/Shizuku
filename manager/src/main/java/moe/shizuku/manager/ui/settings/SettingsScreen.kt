package moe.shizuku.manager.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import moe.shizuku.manager.R
import moe.shizuku.manager.ui.miuix.MiuixGlassCard
import moe.shizuku.manager.ui.miuix.MiuixSnackbarHost
import moe.shizuku.manager.ui.miuix.MiuixSnackbarMessage
import moe.shizuku.manager.ui.miuix.stripHtml
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import moe.shizuku.manager.ui.miuix.MiuixTopBar
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
    state: SettingsUiState,
    languageOptions: List<LocaleOption>,
    snackbar: MiuixSnackbarMessage?,
    onAction: (SettingsAction) -> Unit,
) {
    val colors = MiuixTheme.colorScheme

    Scaffold(
        topBar = {
            MiuixTopBar(
                title = stringResource(R.string.settings_title),
                color = colors.surface.copy(alpha = 0.55f),
                navigationIcon = {
                    IconButton(onClick = { onAction(SettingsAction.Back) }) {
                        Icon(
                            painterResource(R.drawable.ic_outline_arrow_upward_24),
                            null,
                            Modifier.rotate(-90f),
                            tint = colors.onSurface,
                        )
                    }
                },
            )
        },
        snackbarHost = { MiuixSnackbarHost(snackbar, Modifier.fillMaxWidth()) },
        containerColor = Color.Transparent,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(6.dp))
            SectionLabel(stringResource(R.string.settings_behavior))
            Spacer(Modifier.height(10.dp))
            BehaviorCard(state, onAction)

            Spacer(Modifier.height(22.dp))
            SectionLabel(stringResource(R.string.settings_language))
            Spacer(Modifier.height(10.dp))
            LanguageCard(state, onAction)

            Spacer(Modifier.height(22.dp))
            SectionLabel(stringResource(R.string.settings_user_interface))
            Spacer(Modifier.height(10.dp))
            AppearanceCard(state, onAction)

            Spacer(Modifier.height(22.dp))
            SectionLabel(stringResource(R.string.settings_support))
            Spacer(Modifier.height(10.dp))
            SupportCard(state, onAction)

            if (state.legacyPairingVisible) {
                Spacer(Modifier.height(22.dp))
                SectionLabel(stringResource(R.string.settings_advanced))
                Spacer(Modifier.height(10.dp))
                AdvancedCard(state, onAction)
            }

            Spacer(Modifier.height(18.dp))
        }

        when (state.dialog) {
            SettingsDialog.None -> Unit
            SettingsDialog.StartOnBootBug -> ConfirmDialog(
                message = stringResource(R.string.settings_start_on_boot_bug).stripHtml(),
                onConfirm = { onAction(SettingsAction.DialogConfirmed) },
                onCancel = { onAction(SettingsAction.DialogCancelled) },
            )
            SettingsDialog.StopTcp -> ConfirmDialog(
                message = stringResource(R.string.settings_tcp_mode_dialog_close_port).stripHtml(),
                onConfirm = { onAction(SettingsAction.DialogConfirmed) },
                onCancel = { onAction(SettingsAction.DialogCancelled) },
            )
            SettingsDialog.RestartTcpMode -> ConfirmDialog(
                title = stringResource(R.string.settings_restart_dialog_title),
                message = (stringResource(R.string.settings_restart_dialog_message) +
                        stringResource(R.string.settings_restart_dialog_message_wifi_required)).stripHtml(),
                onConfirm = { onAction(SettingsAction.DialogConfirmed) },
                onCancel = { onAction(SettingsAction.DialogCancelled) },
            )
            SettingsDialog.RestartTcpPort -> ConfirmDialog(
                title = stringResource(R.string.settings_restart_dialog_title),
                message = stringResource(R.string.settings_restart_dialog_message).stripHtml(),
                onConfirm = { onAction(SettingsAction.DialogConfirmed) },
                onCancel = { onAction(SettingsAction.DialogCancelled) },
            )
            SettingsDialog.TcpPort -> TcpPortDialog(
                initial = state.tcpPortValue,
                onConfirm = { onAction(SettingsAction.TcpPortSubmitted(it)) },
                onCancel = { onAction(SettingsAction.DialogCancelled) },
            )
            SettingsDialog.Language -> ListDialog(
                title = stringResource(R.string.settings_language),
                options = languageOptions.map { DialogOption(it.tag, it.nativeName, it.localizedName) },
                selectedTag = state.languageTag,
                onSelect = { onAction(SettingsAction.LanguageSelected(it)) },
                onDismiss = { onAction(SettingsAction.DialogDismissed) },
            )
            SettingsDialog.NightMode -> ListDialog(
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
            SettingsDialog.BugReport -> BugReportDialog(onAction)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        modifier = Modifier.padding(start = 8.dp),
        style = MiuixTheme.textStyles.footnote1,
        color = MiuixTheme.colorScheme.onBackground,
    )
}

@Composable
private fun SettingDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 4.dp),
        color = MiuixTheme.colorScheme.dividerLine,
    )
}

@Composable
private fun RestartChip() {
    val colors = MiuixTheme.colorScheme
    Text(
        text = stringResource(R.string.glass_settings_restart_required),
        style = MiuixTheme.textStyles.footnote2,
        color = colors.onSurfaceVariantSummary,
    )
}

@Composable
private fun BehaviorCard(
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
) {
    val tcpModeRestart = (state.adbTcpPort > 0) != state.tcpModeChecked
    val currentPort = state.tcpPortValue.toIntOrNull() ?: 5555
    val tcpPortRestart = state.adbTcpPort > 0 && state.adbTcpPort != currentPort

    MiuixGlassCard(Modifier.fillMaxWidth(), contentPadding = 14.dp) {
        SettingToggleRow(
            title = stringResource(R.string.settings_start_on_boot),
            summary = if (state.startOnBootEnabled) null else stringResource(R.string.settings_start_on_boot_summary),
            checked = state.startOnBootChecked,
            enabled = state.startOnBootEnabled,
            onToggle = { onAction(SettingsAction.StartOnBootToggled(it)) },
        )
        SettingDivider()
        SettingToggleRow(
            title = stringResource(R.string.settings_watchdog),
            summary = stringResource(R.string.settings_watchdog_summary),
            checked = state.watchdogRunning,
            enabled = true,
            onToggle = { onAction(SettingsAction.WatchdogToggled(it)) },
        )
        if (state.tcpModeVisible) {
            SettingDivider()
            SettingToggleRow(
                title = stringResource(R.string.settings_tcp_mode),
                summary = state.tcpModeSummary,
                checked = state.tcpModeChecked,
                enabled = state.tcpModeEnabled,
                restartRequired = tcpModeRestart,
                onToggle = { onAction(SettingsAction.TcpModeToggled(it)) },
            )
        }
        if (state.tcpPortVisible) {
            SettingDivider()
            SettingValueRow(
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
        SettingDivider()
        SettingToggleRow(
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
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
) {
    MiuixGlassCard(Modifier.fillMaxWidth(), contentPadding = 14.dp) {
        SettingValueRow(
            title = stringResource(R.string.settings_language),
            summary = state.languageSummary,
            onClick = { onAction(SettingsAction.OpenLanguage) },
        )
        if (state.translationContributors.isNotBlank()) {
            SettingDivider()
            InfoRow(
                title = stringResource(R.string.settings_translation_contributors),
                summary = state.translationContributors,
            )
        }
        SettingDivider()
        SettingValueRow(
            title = stringResource(R.string.settings_translation),
            summary = stringResource(R.string.settings_translation_summary, stringResource(R.string.app_name)),
            onClick = { onAction(SettingsAction.TranslationClicked) },
        )
    }
}

@Composable
private fun AppearanceCard(
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
) {
    val nightSummary = when (state.nightMode) {
        1 -> stringResource(R.string.dark_theme_off)
        2 -> stringResource(R.string.dark_theme_on)
        else -> stringResource(R.string.follow_system)
    }
    MiuixGlassCard(Modifier.fillMaxWidth(), contentPadding = 14.dp) {
        SettingValueRow(
            title = stringResource(R.string.dark_theme),
            summary = nightSummary,
            onClick = { onAction(SettingsAction.OpenNightMode) },
        )
        if (state.blackNightVisible) {
            SettingDivider()
            SettingToggleRow(
                title = stringResource(R.string.settings_black_night_theme),
                summary = stringResource(R.string.settings_black_night_theme_summary),
                checked = state.blackNightChecked,
                enabled = true,
                onToggle = { onAction(SettingsAction.BlackNightThemeToggled(it)) },
            )
        }
        if (state.useSystemColorVisible) {
            SettingDivider()
            SettingToggleRow(
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
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
) {
    val updateSummary = when (state.updateMode) {
        0 -> stringResource(R.string.off)
        1 -> stringResource(R.string.settings_update_stable)
        else -> stringResource(R.string.settings_update_beta)
    }
    MiuixGlassCard(Modifier.fillMaxWidth(), contentPadding = 14.dp) {
        SettingValueRow(
            title = stringResource(R.string.check_for_updates),
            summary = updateSummary,
            onClick = { onAction(SettingsAction.OpenUpdateMode) },
        )
        SettingDivider()
        SettingValueRow(
            title = stringResource(R.string.settings_help),
            summary = null,
            onClick = { onAction(SettingsAction.HelpClicked) },
        )
        SettingDivider()
        SettingValueRow(
            title = stringResource(R.string.settings_report_bug),
            summary = null,
            onClick = { onAction(SettingsAction.ReportBugClicked) },
        )
    }
}

@Composable
private fun AdvancedCard(
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
) {
    MiuixGlassCard(Modifier.fillMaxWidth(), contentPadding = 14.dp) {
        SettingToggleRow(
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
    title: String,
    summary: String?,
    checked: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    restartRequired: Boolean = false,
) {
    val colors = MiuixTheme.colorScheme
    val text = MiuixTheme.textStyles
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .then(if (enabled) Modifier.clickable { onToggle(!checked) } else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = text.body1,
                color = if (enabled) colors.onSurface else colors.disabledOnSurface,
            )
            if (summary != null) {
                Spacer(Modifier.height(2.dp))
                Text(summary, style = text.footnote1, color = colors.onSurfaceVariantSummary, maxLines = 2)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (restartRequired) {
                RestartChip()
                Spacer(Modifier.width(8.dp))
            }
            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun SettingValueRow(
    title: String,
    summary: String?,
    onClick: () -> Unit,
    restartRequired: Boolean = false,
) {
    val colors = MiuixTheme.colorScheme
    val text = MiuixTheme.textStyles
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = text.body1, color = colors.onSurface)
            if (summary != null) {
                Spacer(Modifier.height(2.dp))
                Text(summary, style = text.footnote1, color = colors.onSurfaceVariantSummary, maxLines = 2)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (restartRequired) {
                RestartChip()
                Spacer(Modifier.width(8.dp))
            }
            Icon(
                painterResource(R.drawable.ic_outline_arrow_upward_24),
                null,
                Modifier
                    .size(20.dp)
                    .rotate(90f),
                tint = colors.onSurfaceVariantActions,
            )
        }
    }
}

@Composable
private fun InfoRow(
    title: String,
    summary: String,
) {
    val colors = MiuixTheme.colorScheme
    val text = MiuixTheme.textStyles
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = text.body1, color = colors.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(summary, style = text.footnote1, color = colors.onSurfaceVariantSummary)
        }
    }
}

@Composable
private fun ConfirmDialog(
    message: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    title: String = stringResource(android.R.string.dialog_alert_title),
) {
    val text = MiuixTheme.textStyles
    OverlayDialog(
        show = true,
        title = title,
        summary = message,
        onDismissRequest = onCancel,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(
                text = stringResource(android.R.string.cancel),
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            )
            Button(
                colors = ButtonDefaults.buttonColorsPrimary(),
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(android.R.string.ok), style = text.button)
            }
        }
    }
}

@Composable
private fun TcpPortDialog(
    initial: String,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val text = MiuixTheme.textStyles
    var value by remember { mutableStateOf(initial) }
    OverlayDialog(
        show = true,
        onDismissRequest = onCancel,
    ) {
        Text(stringResource(R.string.settings_tcp_port), style = text.title4, color = colors.onSurface)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.settings_tcp_port_hint),
            style = text.footnote1,
            color = colors.onSurfaceVariantSummary,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(
                text = stringResource(android.R.string.cancel),
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            )
            Button(
                colors = ButtonDefaults.buttonColorsPrimary(),
                onClick = { onConfirm(value) },
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(android.R.string.ok), style = text.button)
            }
        }
    }
}

@Composable
private fun ListDialog(
    title: String,
    options: List<DialogOption>,
    selectedTag: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val text = MiuixTheme.textStyles
    OverlayDialog(
        show = true,
        onDismissRequest = onDismiss,
    ) {
        Text(title, style = text.title4, color = colors.onSurface)
        Spacer(Modifier.height(10.dp))
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
                            option.title,
                            style = text.body1,
                            color = if (selected) colors.primary else colors.onSurface,
                        )
                        if (option.subtitle != null) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                option.subtitle,
                                style = text.footnote1,
                                color = colors.onSurfaceVariantSummary,
                            )
                        }
                    }
                    if (selected) {
                        Text("✓", style = text.body1, color = colors.primary)
                    }
                }
                if (index != options.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        color = colors.dividerLine,
                    )
                }
            }
        }
    }
}

@Composable
private fun BugReportDialog(
    onAction: (SettingsAction) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val text = MiuixTheme.textStyles
    val issuesUrl = "https://github.com/thedjchi/Shizuku/issues/new"
    OverlayDialog(
        show = true,
        onDismissRequest = { onAction(SettingsAction.BugReportCancel) },
    ) {
        Text(stringResource(R.string.settings_report_bug), style = text.title4, color = colors.onSurface)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.bug_report_dialog_before_reporting),
            style = text.footnote1,
            color = colors.onSurfaceVariantSummary,
        )
        Spacer(Modifier.height(8.dp))
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
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(
                text = stringResource(R.string.bug_report_dialog_button_email),
                onClick = { onAction(SettingsAction.BugReportEmail) },
                modifier = Modifier.weight(1f),
            )
            Button(
                colors = ButtonDefaults.buttonColorsPrimary(),
                onClick = { onAction(SettingsAction.BugReportGitHub) },
                modifier = Modifier.weight(1f),
            ) {
                Text("GitHub", style = text.button)
            }
        }
        Spacer(Modifier.height(4.dp))
        TextButton(
            text = stringResource(android.R.string.cancel),
            onClick = { onAction(SettingsAction.BugReportCancel) },
            modifier = Modifier.fillMaxWidth(),
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
    val colors = MiuixTheme.colorScheme
    val text = MiuixTheme.textStyles
    val annotated = remember(template, label, url) {
        buildAnnotatedString {
            val index = template.indexOf("^1")
            if (index >= 0) {
                append(template.substring(0, index))
                withStyle(SpanStyle(color = colors.primary)) {
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
            .padding(vertical = 8.dp),
        style = text.footnote1.copy(color = colors.onSurfaceVariantSummary),
        onClick = { offset ->
            annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()
                ?.let { onAction(SettingsAction.BugReportLinkClicked(it.item)) }
        },
    )
}
