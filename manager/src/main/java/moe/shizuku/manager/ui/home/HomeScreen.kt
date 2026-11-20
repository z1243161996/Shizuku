package moe.shizuku.manager.ui.home

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import moe.shizuku.manager.Helps
import moe.shizuku.manager.R
import moe.shizuku.manager.model.ServiceStatus
import moe.shizuku.manager.ui.miuix.MiuixGlassCard
import moe.shizuku.manager.ui.miuix.MiuixSnackbarHost
import moe.shizuku.manager.ui.miuix.MiuixSnackbarMessage
import moe.shizuku.manager.ui.miuix.stripHtml
import rikka.core.util.ClipboardUtils
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuApiConstants
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import moe.shizuku.manager.ui.miuix.MiuixTopBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import moe.shizuku.manager.ui.miuix.LocalMiuixBackdrop

enum class HomeAction {
    StartRoot,
    StartWirelessAdb,
    PairWirelessAdb,
    WirelessAdbHelp,
    OpenApps,
    OpenTerminal,
    OpenStealth,
    OpenLearnMore,
    OpenAdbPermissionHelp,
    OpenSettings,
    StopService,
    RegenerateToken,
    CheckForUpdates,
    Donate,
    DismissExitDialog,
    ExitApp,
}

data class HomeUiState(
    val status: ServiceStatus? = null,
    val grantedCount: Int = 0,
    val isRooted: Boolean = false,
    val isPrimaryUser: Boolean = true,
    val isTelevision: Boolean = false,
    val tlsSupported: Boolean = false,
    val adbTcpPort: Int = 0,
    val versionName: String = "",
    val adbCommand: String = "",
    val authToken: String = "",
    val aboutIcon: ImageBitmap? = null,
)

data class HomeExitDialog(val title: String, val message: String)

private enum class HomeDialog { None, About, AdbCommand, Automation, StopService }

private val CardSpacing = 16.dp

@Composable
fun HomeScreen(
    state: HomeUiState,
    snackbar: MiuixSnackbarMessage?,
    exitDialog: HomeExitDialog?,
    onAction: (HomeAction) -> Unit,
) {
    val context = LocalContext.current
    var dialog by remember { mutableStateOf(HomeDialog.None) }
    val colors = MiuixTheme.colorScheme
    val text = MiuixTheme.textStyles

    val status = state.status
    val running = status?.isRunning == true
    val adbPermission = status?.permission == true

    Scaffold(
        topBar = {
            MiuixTopBar(
                title = stringResource(R.string.app_name),
                subtitle = state.versionName,
                color = MiuixTheme.colorScheme.surface.copy(alpha = 0.55f),
                actions = {
                    IconButton(onClick = { dialog = HomeDialog.About }) {
                        Icon(painterResource(R.drawable.ic_action_about_24dp), null)
                    }
                    IconButton(onClick = { onAction(HomeAction.OpenSettings) }) {
                        Icon(painterResource(R.drawable.ic_action_settings_24dp), null)
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
            Spacer(Modifier.height(8.dp))
            StatusCard(
                status = status,
                onStop = { dialog = HomeDialog.StopService },
            )

            if (running && !adbPermission) {
                Spacer(Modifier.height(CardSpacing))
                AdbPermissionLimitedCard(onAction)
            }

            if (state.isPrimaryUser) {
                if (!running && state.isRooted) {
                    Spacer(Modifier.height(CardSpacing))
                    RootCard(onAction)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || state.isTelevision || state.adbTcpPort > 0) {
                    Spacer(Modifier.height(CardSpacing))
                    WirelessAdbCard(state, onAction)
                }
                Spacer(Modifier.height(CardSpacing))
                AdbCard(state) { dialog = HomeDialog.AdbCommand }
            }

            Spacer(Modifier.height(24.dp))
            SectionLabel(stringResource(R.string.glass_section_quick_actions))
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile(
                    iconRes = R.drawable.ic_adb_24dp,
                    title = stringResource(R.string.home_app_management_title),
                    caption = pluralStringResource(
                        R.plurals.home_app_management_authorized_apps_count,
                        state.grantedCount,
                        state.grantedCount,
                    ),
                    enabled = adbPermission,
                    modifier = Modifier.weight(1f),
                    onClick = { onAction(HomeAction.OpenApps) },
                )
                QuickTile(
                    iconRes = R.drawable.ic_terminal_24,
                    title = stringResource(R.string.home_terminal_title),
                    caption = stringResource(R.string.home_terminal_description),
                    enabled = adbPermission,
                    modifier = Modifier.weight(1f),
                    onClick = { onAction(HomeAction.OpenTerminal) },
                )
            }

            Spacer(Modifier.height(24.dp))
            SectionLabel(stringResource(R.string.glass_section_more))
            Spacer(Modifier.height(8.dp))
            MoreCard(onAction) { dialog = HomeDialog.Automation }
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.glass_footer),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                style = text.footnote1,
                color = colors.onBackground.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(12.dp))
        }

        OverlayDialog(
            show = dialog == HomeDialog.AdbCommand,
            title = stringResource(R.string.home_adb_button_view_command),
            summary = stringResource(R.string.home_adb_dialog_view_command_message, state.adbCommand).stripHtml(),
            onDismissRequest = { dialog = HomeDialog.None },
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(
                    text = stringResource(R.string.glass_action_close),
                    onClick = { dialog = HomeDialog.None },
                    modifier = Modifier.weight(1f),
                )
                Button(
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    onClick = {
                        ClipboardUtils.put(context, state.adbCommand)
                        dialog = HomeDialog.None
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.glass_action_copy), style = text.button)
                }
            }
        }

        OverlayDialog(
            show = dialog == HomeDialog.StopService,
            title = stringResource(R.string.action_stop),
            summary = stringResource(R.string.dialog_stop_message),
            onDismissRequest = { dialog = HomeDialog.None },
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(
                    text = stringResource(R.string.glass_action_close),
                    onClick = { dialog = HomeDialog.None },
                    modifier = Modifier.weight(1f),
                )
                Button(
                    colors = ButtonDefaults.buttonColors(
                        color = colors.surfaceVariant,
                        contentColor = colors.error,
                    ),
                    onClick = {
                        dialog = HomeDialog.None
                        onAction(HomeAction.StopService)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.action_stop), style = text.button)
                }
            }
        }

        OverlayDialog(
            show = dialog == HomeDialog.Automation,
            onDismissRequest = { dialog = HomeDialog.None },
        ) {
            AutomationDialog(state, onRegenerate = { onAction(HomeAction.RegenerateToken) }, onClose = { dialog = HomeDialog.None })
        }

        OverlayDialog(
            show = dialog == HomeDialog.About,
            onDismissRequest = { dialog = HomeDialog.None },
        ) {
            AboutDialog(
                state = state,
                onUpdate = { onAction(HomeAction.CheckForUpdates) },
                onDonate = { onAction(HomeAction.Donate) },
                onClose = { dialog = HomeDialog.None },
            )
        }

        exitDialog?.let {
            OverlayDialog(
                show = true,
                title = it.title,
                summary = it.message,
                onDismissRequest = { onAction(HomeAction.DismissExitDialog) },
            ) {
                Button(
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    onClick = { onAction(HomeAction.ExitApp) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.home_dialog_button_exit), style = text.button)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        modifier = Modifier.padding(start = 8.dp),
        style = MiuixTheme.textStyles.footnote1,
        color = MiuixTheme.colorScheme.onBackground,
    )
}

@Composable
private fun StatusCard(status: ServiceStatus?, onStop: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    val text = MiuixTheme.textStyles
    val running = status?.isRunning == true
    val isRoot = status?.uid == 0
    val user = if (isRoot) "root" else "adb"
    val appName = stringResource(R.string.app_name)

    MiuixGlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusOrb(running)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(
                        if (running) R.string.home_status_service_is_running
                        else R.string.home_status_service_not_running,
                        appName,
                    ).stripHtml(),
                    style = text.title2,
                    color = colors.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    when {
                        status == null || !running -> stringResource(R.string.glass_service_hint)
                        status.apiVersion != Shizuku.getLatestServiceVersion() ||
                            status.patchVersion != ShizukuApiConstants.SERVER_PATCH_VERSION ->
                            stringResource(
                                R.string.home_status_service_version_update,
                                user,
                                "${status.apiVersion}.${status.patchVersion}",
                                "${Shizuku.getLatestServiceVersion()}.${ShizukuApiConstants.SERVER_PATCH_VERSION}",
                            ).stripHtml()

                        else -> stringResource(
                            R.string.home_status_service_version,
                            user,
                            "${status.apiVersion}.${status.patchVersion}",
                        ).stripHtml()
                    },
                    style = text.body1,
                    color = colors.onSurfaceVariantSummary,
                )
            }
        }

        if (running && status != null) {
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusPill(
                    stringResource(if (isRoot) R.string.glass_status_root else R.string.glass_status_adb),
                    if (isRoot) colors.primary else colors.secondary,
                )
                StatusPill(stringResource(R.string.glass_status_uid, status.uid), null)
                StatusPill(stringResource(R.string.glass_status_api, status.apiVersion), null)
                if (status.patchVersion > 0) {
                    StatusPill(stringResource(R.string.glass_status_patch, status.patchVersion), null)
                }
            }
            status.seContext?.takeIf { it.isNotBlank() }?.let { se ->
                Spacer(Modifier.height(8.dp))
                Text(
                    se,
                    style = text.footnote2,
                    color = colors.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onStop, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(color = colors.surfaceVariant, contentColor = colors.error)) {
                Text(stringResource(R.string.glass_stop_service), style = text.button)
            }
        }
    }
}

@Composable
private fun StatusPill(label: String, dotColor: Color?) {
    val colors = MiuixTheme.colorScheme
    Row(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dotColor != null) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(label, style = MiuixTheme.textStyles.footnote2, color = colors.onSurfaceVariantSummary)
    }
}

@Composable
private fun StatusOrb(running: Boolean) {
    val color = if (running) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.error
    Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = if (running) 0.16f else 0.12f))
        )
        Box(Modifier.size(26.dp).clip(CircleShape).background(color.copy(alpha = 0.32f)))
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
    }
}

@Composable
private fun RootCard(onAction: (HomeAction) -> Unit) {
    val text = MiuixTheme.textStyles
    MiuixGlassCard(Modifier.fillMaxWidth()) {
        CardHeader(R.drawable.ic_root_24dp, stringResource(R.string.home_root_title))
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.home_root_description, Helps.SUI, Helps.SUI).stripHtml(),
            style = text.body1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            maxLines = 6,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onAction(HomeAction.StartRoot) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColorsPrimary()) {
            Text(stringResource(R.string.home_root_button_start), style = text.button)
        }
    }
}

@Composable
private fun WirelessAdbCard(state: HomeUiState, onAction: (HomeAction) -> Unit) {
    val text = MiuixTheme.textStyles
    MiuixGlassCard(Modifier.fillMaxWidth()) {
        CardHeader(R.drawable.ic_wadb_24, stringResource(R.string.home_wireless_adb_title))
        Spacer(Modifier.height(8.dp))
        Text(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                stringResource(R.string.home_wireless_adb_description).stripHtml()
            } else {
                stringResource(R.string.home_wireless_adb_description_pre_11).stripHtml()
            },
            style = text.body1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            maxLines = 6,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onAction(HomeAction.StartWirelessAdb) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColorsPrimary()) {
            Text(
                if (state.status?.isRunning == true && state.status.uid == 0) {
                    stringResource(R.string.home_root_button_restart)
                } else {
                    stringResource(R.string.home_root_button_start)
                },
                style = text.button,
            )
        }
        if (state.tlsSupported) {
            Spacer(Modifier.height(8.dp))
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    text = stringResource(R.string.adb_pairing),
                    onClick = { onAction(HomeAction.PairWirelessAdb) },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    text = stringResource(R.string.home_wireless_adb_view_guide_button),
                    onClick = { onAction(HomeAction.WirelessAdbHelp) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun AdbCard(state: HomeUiState, onViewCommand: () -> Unit) {
    val text = MiuixTheme.textStyles
    MiuixGlassCard(Modifier.fillMaxWidth()) {
        CardHeader(R.drawable.ic_adb_24dp, stringResource(R.string.home_adb_title))
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.home_adb_description, Helps.ADB).stripHtml(),
            style = text.body1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            maxLines = 8,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onViewCommand, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.home_adb_button_view_command), style = text.button)
        }
        if (state.status?.isRunning != true && state.isRooted) {
            Spacer(Modifier.height(8.dp))
            Button(onClick = onViewCommand, enabled = false, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_root_button_restart), style = text.button)
            }
        }
    }
}

@Composable
private fun AdbPermissionLimitedCard(onAction: (HomeAction) -> Unit) {
    val text = MiuixTheme.textStyles
    MiuixGlassCard(Modifier.fillMaxWidth()) {
        CardHeader(
            R.drawable.ic_warning_24,
            stringResource(R.string.home_adb_is_limited_title),
            MiuixTheme.colorScheme.error,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.home_adb_is_limited_description),
            style = text.body1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            maxLines = 10,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onAction(HomeAction.OpenAdbPermissionHelp) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.home_adb_button_view_help), style = text.button)
        }
    }
}

@Composable
private fun MoreCard(onAction: (HomeAction) -> Unit, onAutomation: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    val text = MiuixTheme.textStyles
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        insideMargin = PaddingValues(vertical = 6.dp),
    ) {
        MoreRow(
            iconRes = R.drawable.ic_code_24dp,
            title = stringResource(R.string.home_automation_title),
            summary = stringResource(R.string.home_automation_description),
            onClick = onAutomation,
        )
        MoreRow(
            iconRes = R.drawable.ic_visibility_off_outline_24,
            title = stringResource(R.string.home_stealth_title),
            summary = stringResource(R.string.home_stealth_description),
            onClick = { onAction(HomeAction.OpenStealth) },
        )
        MoreRow(
            iconRes = R.drawable.ic_learn_more_24dp,
            title = stringResource(R.string.home_learn_more_title),
            summary = stringResource(R.string.home_learn_more_description),
            onClick = { onAction(HomeAction.OpenLearnMore) },
        )
    }
}

@Composable
private fun MoreRow(iconRes: Int, title: String, summary: String, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    val text = MiuixTheme.textStyles
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(iconRes), null, Modifier.size(22.dp), tint = colors.onSurface)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = text.body1, color = colors.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(summary, style = text.footnote1, color = colors.onSurfaceVariantSummary, maxLines = 2)
        }
    }
}

@Composable
private fun QuickTile(
    iconRes: Int,
    title: String,
    caption: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val text = MiuixTheme.textStyles
    MiuixGlassCard(
        modifier = modifier.height(136.dp),
        cornerRadius = 24.dp,
        contentPadding = 14.dp,
        onClick = if (enabled) onClick else null,
    ) {
        Icon(
            painterResource(iconRes),
            null,
            Modifier.size(26.dp),
            tint = if (enabled) colors.onSurface else colors.disabledOnSurface,
        )
        Spacer(Modifier.weight(1f))
        Text(
            title,
            style = text.subtitle,
            color = if (enabled) colors.onSurface else colors.disabledOnSurface,
            maxLines = 2,
        )
        Spacer(Modifier.height(4.dp))
        Text(caption, style = text.footnote1, color = colors.onSurfaceVariantSummary, maxLines = 2)
    }
}

@Composable
private fun CardHeader(iconRes: Int, title: String, iconTint: Color = MiuixTheme.colorScheme.onSurface) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(iconRes), null, Modifier.size(22.dp), tint = iconTint)
        Spacer(Modifier.width(10.dp))
        Text(title, style = MiuixTheme.textStyles.title4, color = MiuixTheme.colorScheme.onSurface)
    }
}

@Composable
private fun AutomationDialog(state: HomeUiState, onRegenerate: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val colors = MiuixTheme.colorScheme
    val text = MiuixTheme.textStyles
    Text(stringResource(R.string.home_automation_title), style = text.title4, color = colors.onSurface)
    Spacer(Modifier.height(4.dp))
    Text(
        stringResource(R.string.home_automation_description),
        style = text.footnote1,
        color = colors.onSurfaceVariantSummary,
    )
    Spacer(Modifier.height(12.dp))
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        CopyField(stringResource(R.string.home_automation_bottom_sheet_label_action), "${context.packageName}.START", mono = true)
        Spacer(Modifier.height(8.dp))
        CopyField(stringResource(R.string.home_automation_bottom_sheet_label_package), context.packageName, mono = true)
        Spacer(Modifier.height(8.dp))
        CopyField(stringResource(R.string.home_automation_bottom_sheet_label_target), "Broadcast Receiver")
        Spacer(Modifier.height(8.dp))
        CopyField(stringResource(R.string.home_automation_bottom_sheet_label_extras), state.authToken, mono = true)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && !state.isTelevision && !state.isRooted) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.home_automation_description_device_restriction, "adb tcpip 5555"),
                style = text.footnote1,
                color = colors.onSurfaceVariantSummary,
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    Button(onClick = onRegenerate, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.glass_action_regenerate), style = text.button)
    }
    Spacer(Modifier.height(8.dp))
    TextButton(
        text = stringResource(R.string.glass_action_close),
        onClick = onClose,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CopyField(label: String, value: String, mono: Boolean = false) {
    val context = LocalContext.current
    val colors = MiuixTheme.colorScheme
    val text = MiuixTheme.textStyles
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = text.footnote1, color = colors.onSurfaceVariantSummary)
            Spacer(Modifier.height(3.dp))
            Text(
                value,
                style = if (mono) text.body1.copy(fontFamily = FontFamily.Monospace) else text.body1,
                color = colors.onSurface,
            )
        }
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = { ClipboardUtils.put(context, value) }) {
            Icon(painterResource(R.drawable.ic_copy), null, Modifier.size(20.dp), tint = colors.primary)
        }
    }
}

@Composable
private fun AboutDialog(state: HomeUiState, onUpdate: () -> Unit, onDonate: () -> Unit, onClose: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    val text = MiuixTheme.textStyles
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        state.aboutIcon?.let {
            Image(it, null, Modifier.size(44.dp).clip(CircleShape))
            Spacer(Modifier.width(12.dp))
        }
        Column {
            Text(stringResource(R.string.glass_about_title), style = text.title4, color = colors.onSurface)
            Text(state.versionName, style = text.footnote1, color = colors.onSurfaceVariantSummary)
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        stringResource(R.string.about_view_source_code, "github.com/thedjchi/Shizuku"),
        style = text.footnote1,
        color = colors.onSurfaceVariantSummary,
    )
    Spacer(Modifier.height(16.dp))
    Button(onClick = onUpdate, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColorsPrimary()) {
        Text(stringResource(R.string.glass_action_update), style = text.button)
    }
    Spacer(Modifier.height(8.dp))
    TextButton(text = stringResource(R.string.glass_action_donate), onClick = onDonate, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(4.dp))
    TextButton(text = stringResource(R.string.glass_action_close), onClick = onClose, modifier = Modifier.fillMaxWidth())
}
