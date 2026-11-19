package moe.shizuku.manager.ui.home

import android.os.Build
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import moe.shizuku.manager.Helps
import moe.shizuku.manager.R
import moe.shizuku.manager.model.ServiceStatus
import moe.shizuku.manager.ui.glass.GlassBody
import moe.shizuku.manager.ui.glass.GlassButton
import moe.shizuku.manager.ui.glass.GlassButtonStyle
import moe.shizuku.manager.ui.glass.GlassCaption
import moe.shizuku.manager.ui.glass.GlassCard
import moe.shizuku.manager.ui.glass.GlassChip
import moe.shizuku.manager.ui.glass.GlassDialogActions
import moe.shizuku.manager.ui.glass.GlassDialogOverlay
import moe.shizuku.manager.ui.glass.GlassDialogText
import moe.shizuku.manager.ui.glass.GlassDialogTitle
import moe.shizuku.manager.ui.glass.GlassDivider
import moe.shizuku.manager.ui.glass.GlassIcon
import moe.shizuku.manager.ui.glass.GlassIconButton
import moe.shizuku.manager.ui.glass.GlassRow
import moe.shizuku.manager.ui.glass.GlassScaffold
import moe.shizuku.manager.ui.glass.GlassSnackbarHost
import moe.shizuku.manager.ui.glass.GlassSnackbarMessage
import moe.shizuku.manager.ui.glass.GlassType
import moe.shizuku.manager.ui.glass.LocalGlassPalette
import moe.shizuku.manager.ui.glass.stripHtml
import rikka.core.util.ClipboardUtils
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuApiConstants

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

private val CardSpacing = 14.dp

@Composable
fun HomeScreen(
    backdrop: LayerBackdrop,
    state: HomeUiState,
    snackbar: GlassSnackbarMessage?,
    exitDialog: HomeExitDialog?,
    onAction: (HomeAction) -> Unit,
) {
    val palette = LocalGlassPalette.current
    val context = LocalContext.current
    var dialog by remember { mutableStateOf(HomeDialog.None) }

    val status = state.status
    val running = status?.isRunning == true
    val adbPermission = status?.permission == true

    GlassScaffold(
        title = stringResource(R.string.app_name),
        subtitle = state.versionName,
        backdrop = backdrop,
        actions = {
            GlassIconButton(R.drawable.ic_action_about_24dp, backdrop, onClick = { dialog = HomeDialog.About })
            Spacer(Modifier.width(8.dp))
            GlassIconButton(R.drawable.ic_action_settings_24dp, backdrop, onClick = { onAction(HomeAction.OpenSettings) })
        },
        floating = { GlassSnackbarHost(snackbar, backdrop) },
    ) {
        Spacer(Modifier.height(6.dp))
        StatusCard(backdrop, state) { dialog = HomeDialog.StopService }

        if (running && !adbPermission) {
            Spacer(Modifier.height(CardSpacing))
            AdbPermissionLimitedCard(backdrop, onAction)
        }

        if (state.isPrimaryUser) {
            if (!running && state.isRooted) {
                Spacer(Modifier.height(CardSpacing))
                RootCard(backdrop, onAction)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || state.isTelevision || state.adbTcpPort > 0) {
                Spacer(Modifier.height(CardSpacing))
                WirelessAdbCard(backdrop, state, onAction)
            }
            Spacer(Modifier.height(CardSpacing))
            AdbCard(backdrop, state) { dialog = HomeDialog.AdbCommand }
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel(stringResource(R.string.glass_section_quick_actions))
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickTile(
                backdrop = backdrop,
                iconRes = R.drawable.ic_adb_24dp,
                title = stringResource(R.string.home_app_management_title),
                caption = pluralStringResource(
                    R.plurals.home_app_management_authorized_apps_count, state.grantedCount, state.grantedCount
                ),
                enabled = adbPermission,
                modifier = Modifier.weight(1f),
                onClick = { onAction(HomeAction.OpenApps) },
            )
            QuickTile(
                backdrop = backdrop,
                iconRes = R.drawable.ic_terminal_24,
                title = stringResource(R.string.home_terminal_title),
                caption = stringResource(R.string.home_terminal_description),
                enabled = adbPermission,
                modifier = Modifier.weight(1f),
                onClick = { onAction(HomeAction.OpenTerminal) },
            )
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel(stringResource(R.string.glass_section_more))
        Spacer(Modifier.height(10.dp))
        MoreCard(backdrop, onAction) { dialog = HomeDialog.Automation }
        Spacer(Modifier.height(18.dp))
        GlassCaption(
            stringResource(R.string.glass_footer),
            Modifier.fillMaxWidth(),
            palette.textTertiary.copy(alpha = 0.75f),
        )
    }

    when (dialog) {
        HomeDialog.AdbCommand -> GlassDialogOverlay(backdrop, onDismiss = { dialog = HomeDialog.None }) {
            GlassDialogTitle(stringResource(R.string.home_adb_button_view_command))
            GlassDialogText(stringResource(R.string.home_adb_dialog_view_command_message, state.adbCommand).stripHtml())
            GlassDialogActions(
                backdrop = backdrop,
                confirmText = stringResource(R.string.glass_action_copy),
                onConfirm = {
                    ClipboardUtils.put(context, state.adbCommand)
                },
                dismissText = stringResource(R.string.glass_action_close),
                onDismiss = { dialog = HomeDialog.None },
            )
        }

        HomeDialog.Automation -> AutomationDialog(
            backdrop = backdrop,
            state = state,
            onRegenerate = { onAction(HomeAction.RegenerateToken) },
            onClose = { dialog = HomeDialog.None },
        )

        HomeDialog.About -> AboutDialog(
            backdrop = backdrop,
            state = state,
            onUpdate = { onAction(HomeAction.CheckForUpdates) },
            onDonate = { onAction(HomeAction.Donate) },
            onClose = { dialog = HomeDialog.None },
        )

        HomeDialog.StopService -> GlassDialogOverlay(backdrop, onDismiss = { dialog = HomeDialog.None }) {
            GlassDialogTitle(stringResource(R.string.action_stop))
            GlassDialogText(stringResource(R.string.dialog_stop_message))
            GlassDialogActions(
                backdrop = backdrop,
                confirmText = stringResource(R.string.action_stop),
                onConfirm = {
                    dialog = HomeDialog.None
                    onAction(HomeAction.StopService)
                },
                dismissText = stringResource(R.string.glass_action_close),
                onDismiss = { dialog = HomeDialog.None },
                destructive = true,
            )
        }

        HomeDialog.None -> Unit
    }

    exitDialog?.let {
        GlassDialogOverlay(backdrop, onDismiss = { onAction(HomeAction.DismissExitDialog) }) {
            GlassDialogTitle(it.title)
            GlassDialogText(it.message)
            GlassDialogActions(
                backdrop = backdrop,
                confirmText = stringResource(R.string.home_dialog_button_exit),
                onConfirm = { onAction(HomeAction.ExitApp) },
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        modifier = Modifier.padding(start = 6.dp),
        style = GlassType.label,
        color = LocalGlassPalette.current.textPrimary.copy(alpha = 0.75f),
    )
}

@Composable
private fun StatusCard(backdrop: LayerBackdrop, state: HomeUiState, onStop: () -> Unit) {
    val palette = LocalGlassPalette.current
    val status = state.status
    val running = status?.isRunning == true
    val isRoot = status?.uid == 0
    val user = if (isRoot) "root" else "adb"
    val appName = stringResource(R.string.app_name)

    GlassCard(backdrop, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusOrb(running)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(
                        if (running) R.string.home_status_service_is_running else R.string.home_status_service_not_running,
                        appName,
                    ).stripHtml(),
                    style = GlassType.hero,
                    color = palette.textPrimary,
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
                    style = GlassType.caption,
                    color = palette.textSecondary,
                )
            }
        }

        if (running && status != null) {
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GlassChip(
                    text = stringResource(if (isRoot) R.string.glass_status_root else R.string.glass_status_adb),
                    backdrop = backdrop,
                    dotColor = if (isRoot) palette.accent else palette.warning,
                )
                GlassChip(stringResource(R.string.glass_status_uid, status.uid), backdrop)
                GlassChip(stringResource(R.string.glass_status_api, status.apiVersion), backdrop)
                if (status.patchVersion > 0) {
                    GlassChip(stringResource(R.string.glass_status_patch, status.patchVersion), backdrop)
                }
                status.seContext?.takeIf { it.isNotBlank() }?.let { GlassChip(it, backdrop) }
            }
            Spacer(Modifier.height(14.dp))
            GlassButton(
                text = stringResource(R.string.glass_stop_service),
                backdrop = backdrop,
                onClick = onStop,
                style = GlassButtonStyle.Destructive,
                fillWidth = true,
                iconRes = R.drawable.ic_close_24,
            )
        }
    }
}

@Composable
private fun StatusOrb(running: Boolean) {
    val palette = LocalGlassPalette.current
    val color = if (running) palette.accent else palette.danger
    val transition = rememberInfiniteTransition(label = "orb")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "pulse",
    )
    Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = if (running) 0.22f * pulse else 0.18f))
        )
        Box(Modifier.size(26.dp).clip(CircleShape).background(color.copy(alpha = 0.35f)))
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
    }
}

@Composable
private fun RootCard(backdrop: LayerBackdrop, onAction: (HomeAction) -> Unit) {
    GlassCard(backdrop, modifier = Modifier.fillMaxWidth()) {
        CardHeader(R.drawable.ic_root_24dp, stringResource(R.string.home_root_title))
        Spacer(Modifier.height(8.dp))
        GlassBody(
            stringResource(R.string.home_root_description, Helps.SUI, Helps.SUI).stripHtml(),
            maxLines = 6,
        )
        Spacer(Modifier.height(14.dp))
        GlassButton(
            text = stringResource(R.string.home_root_button_start),
            backdrop = backdrop,
            onClick = { onAction(HomeAction.StartRoot) },
            style = GlassButtonStyle.Prominent,
            fillWidth = true,
        )
    }
}

@Composable
private fun WirelessAdbCard(backdrop: LayerBackdrop, state: HomeUiState, onAction: (HomeAction) -> Unit) {
    GlassCard(backdrop, modifier = Modifier.fillMaxWidth()) {
        CardHeader(R.drawable.ic_wadb_24, stringResource(R.string.home_wireless_adb_title))
        Spacer(Modifier.height(8.dp))
        GlassBody(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                stringResource(R.string.home_wireless_adb_description).stripHtml()
            } else {
                stringResource(R.string.home_wireless_adb_description_pre_11).stripHtml()
            },
            maxLines = 6,
        )
        Spacer(Modifier.height(14.dp))
        GlassButton(
            text = if (state.status?.isRunning == true && state.status.uid == 0) {
                stringResource(R.string.home_root_button_restart)
            } else {
                stringResource(R.string.home_root_button_start)
            },
            backdrop = backdrop,
            onClick = { onAction(HomeAction.StartWirelessAdb) },
            style = GlassButtonStyle.Prominent,
            fillWidth = true,
        )
        if (state.tlsSupported) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton(
                    text = stringResource(R.string.adb_pairing),
                    backdrop = backdrop,
                    onClick = { onAction(HomeAction.PairWirelessAdb) },
                    modifier = Modifier.weight(1f),
                )
                GlassButton(
                    text = stringResource(R.string.home_wireless_adb_view_guide_button),
                    backdrop = backdrop,
                    onClick = { onAction(HomeAction.WirelessAdbHelp) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AdbCard(backdrop: LayerBackdrop, state: HomeUiState, onViewCommand: () -> Unit) {
    GlassCard(backdrop, modifier = Modifier.fillMaxWidth()) {
        CardHeader(R.drawable.ic_adb_24dp, stringResource(R.string.home_adb_title))
        Spacer(Modifier.height(8.dp))
        GlassBody(stringResource(R.string.home_adb_description, Helps.ADB).stripHtml(), maxLines = 8)
        Spacer(Modifier.height(14.dp))
        GlassButton(
            text = stringResource(R.string.home_adb_button_view_command),
            backdrop = backdrop,
            onClick = onViewCommand,
            fillWidth = true,
        )
        if (state.status?.isRunning != true && state.isRooted) {
            Spacer(Modifier.height(8.dp))
            GlassButton(
                text = stringResource(R.string.home_root_button_restart),
                backdrop = backdrop,
                onClick = onViewCommand,
                fillWidth = true,
                enabled = false,
            )
        }
    }
}

@Composable
private fun AdbPermissionLimitedCard(backdrop: LayerBackdrop, onAction: (HomeAction) -> Unit) {
    val palette = LocalGlassPalette.current
    GlassCard(backdrop, modifier = Modifier.fillMaxWidth()) {
        CardHeader(
            R.drawable.ic_warning_24,
            stringResource(R.string.home_adb_is_limited_title),
            palette.warning,
        )
        Spacer(Modifier.height(8.dp))
        GlassBody(stringResource(R.string.home_adb_is_limited_description), maxLines = 10)
        Spacer(Modifier.height(14.dp))
        GlassButton(
            text = stringResource(R.string.home_adb_button_view_help),
            backdrop = backdrop,
            onClick = { onAction(HomeAction.OpenAdbPermissionHelp) },
            fillWidth = true,
        )
    }
}

@Composable
private fun MoreCard(backdrop: LayerBackdrop, onAction: (HomeAction) -> Unit, onAutomation: () -> Unit) {
    GlassCard(backdrop, modifier = Modifier.fillMaxWidth(), contentPadding = 8.dp) {
        GlassRow(
            backdrop = backdrop,
            title = stringResource(R.string.home_automation_title),
            summary = stringResource(R.string.home_automation_description),
            iconRes = R.drawable.ic_code_24dp,
            onClick = onAutomation,
        )
        GlassDivider(Modifier.padding(horizontal = 12.dp))
        GlassRow(
            backdrop = backdrop,
            title = stringResource(R.string.home_stealth_title),
            summary = stringResource(R.string.home_stealth_description),
            iconRes = R.drawable.ic_visibility_off_outline_24,
            onClick = { onAction(HomeAction.OpenStealth) },
        )
        GlassDivider(Modifier.padding(horizontal = 12.dp))
        GlassRow(
            backdrop = backdrop,
            title = stringResource(R.string.home_learn_more_title),
            summary = stringResource(R.string.home_learn_more_description),
            iconRes = R.drawable.ic_learn_more_24dp,
            onClick = { onAction(HomeAction.OpenLearnMore) },
        )
    }
}

@Composable
private fun QuickTile(
    backdrop: LayerBackdrop,
    iconRes: Int,
    title: String,
    caption: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val palette = LocalGlassPalette.current
    GlassCard(
        backdrop = backdrop,
        modifier = modifier.height(136.dp),
        shape = RoundedCornerShape(24.dp),
        contentPadding = 14.dp,
        enabled = enabled,
        onClick = onClick,
    ) {
        GlassIcon(iconRes, size = 26, tint = if (enabled) palette.textPrimary else palette.textTertiary)
        Spacer(Modifier.weight(1f))
        Text(
            title,
            style = GlassType.cardTitle,
            color = if (enabled) palette.textPrimary else palette.textTertiary,
            maxLines = 2,
        )
        Spacer(Modifier.height(4.dp))
        Text(caption, style = GlassType.caption, color = palette.textSecondary, maxLines = 2)
    }
}

@Composable
private fun CardHeader(iconRes: Int, title: String, iconTint: Color = LocalGlassPalette.current.textPrimary) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        GlassIcon(iconRes, size = 22, tint = iconTint)
        Spacer(Modifier.width(10.dp))
        Text(title, style = GlassType.cardTitle, color = LocalGlassPalette.current.textPrimary)
    }
}

@Composable
private fun AutomationDialog(
    backdrop: LayerBackdrop,
    state: HomeUiState,
    onRegenerate: () -> Unit,
    onClose: () -> Unit,
) {
    val palette = LocalGlassPalette.current
    val context = LocalContext.current
    GlassDialogOverlay(backdrop, onDismiss = onClose) {
        GlassDialogTitle(stringResource(R.string.home_automation_title))
        GlassDialogText(stringResource(R.string.home_automation_description))
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            CopyField(
                backdrop = backdrop,
                context = context,
                label = stringResource(R.string.home_automation_bottom_sheet_label_action),
                value = "${context.packageName}.START",
                mono = true,
            )
            Spacer(Modifier.height(10.dp))
            CopyField(
                backdrop = backdrop,
                context = context,
                label = stringResource(R.string.home_automation_bottom_sheet_label_package),
                value = context.packageName,
                mono = true,
            )
            Spacer(Modifier.height(10.dp))
            CopyField(
                backdrop = backdrop,
                context = context,
                label = stringResource(R.string.home_automation_bottom_sheet_label_target),
                value = "Broadcast Receiver",
            )
            Spacer(Modifier.height(10.dp))
            CopyField(
                backdrop = backdrop,
                context = context,
                label = stringResource(R.string.home_automation_bottom_sheet_label_extras),
                value = state.authToken,
                mono = true,
            )
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && !state.isTelevision && !state.isRooted) {
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.home_automation_description_device_restriction, "adb tcpip 5555"),
                    style = GlassType.caption,
                    color = palette.textTertiary,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        GlassButton(
            text = stringResource(R.string.glass_action_regenerate),
            backdrop = backdrop,
            onClick = onRegenerate,
            fillWidth = true,
        )
        Spacer(Modifier.height(8.dp))
        GlassButton(
            text = stringResource(R.string.glass_action_close),
            backdrop = backdrop,
            onClick = onClose,
            fillWidth = true,
        )
    }
}

@Composable
private fun CopyField(
    backdrop: LayerBackdrop,
    context: android.content.Context,
    label: String,
    value: String,
    mono: Boolean = false,
) {
    val palette = LocalGlassPalette.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = GlassType.label, color = palette.textSecondary.copy(alpha = 0.85f))
            Spacer(Modifier.height(3.dp))
            Text(value, style = if (mono) GlassType.mono else GlassType.body, color = palette.textPrimary)
        }
        Spacer(Modifier.width(8.dp))
        GlassIconButton(
            iconRes = R.drawable.ic_copy,
            backdrop = backdrop,
            onClick = { ClipboardUtils.put(context, value) },
            size = 34.dp,
        )
    }
}

@Composable
private fun AboutDialog(
    backdrop: LayerBackdrop,
    state: HomeUiState,
    onUpdate: () -> Unit,
    onDonate: () -> Unit,
    onClose: () -> Unit,
) {
    val palette = LocalGlassPalette.current
    GlassDialogOverlay(backdrop, onDismiss = onClose) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            state.aboutIcon?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp).clip(CircleShape),
                )
                Spacer(Modifier.width(12.dp))
            }
            Column {
                GlassDialogTitle(stringResource(R.string.glass_about_title))
                Text(state.versionName, style = GlassType.caption, color = palette.textSecondary)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.about_view_source_code, "github.com/thedjchi/Shizuku"),
            style = GlassType.caption,
            color = palette.textSecondary,
        )
        Spacer(Modifier.height(14.dp))
        GlassButton(
            text = stringResource(R.string.glass_action_update),
            backdrop = backdrop,
            onClick = onUpdate,
            fillWidth = true,
        )
        Spacer(Modifier.height(8.dp))
        GlassButton(
            text = stringResource(R.string.glass_action_donate),
            backdrop = backdrop,
            onClick = onDonate,
            fillWidth = true,
        )
        Spacer(Modifier.height(8.dp))
        GlassButton(
            text = stringResource(R.string.glass_action_close),
            backdrop = backdrop,
            onClick = onClose,
            fillWidth = true,
        )
    }
}
