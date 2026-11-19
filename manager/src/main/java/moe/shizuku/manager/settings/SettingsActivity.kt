package moe.shizuku.manager.settings

import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import moe.shizuku.manager.BuildConfig
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.ShizukuSettings.Keys.KEY_AUTO_DISABLE_USB_DEBUGGING
import moe.shizuku.manager.ShizukuSettings.Keys.KEY_BLACK_NIGHT_THEME
import moe.shizuku.manager.ShizukuSettings.Keys.KEY_LANGUAGE
import moe.shizuku.manager.ShizukuSettings.Keys.KEY_LEGACY_PAIRING
import moe.shizuku.manager.ShizukuSettings.Keys.KEY_NIGHT_MODE
import moe.shizuku.manager.ShizukuSettings.Keys.KEY_TCP_MODE
import moe.shizuku.manager.ShizukuSettings.Keys.KEY_TCP_PORT
import moe.shizuku.manager.ShizukuSettings.Keys.KEY_UPDATE_MODE
import moe.shizuku.manager.ShizukuSettings.Keys.KEY_USE_SYSTEM_COLOR
import moe.shizuku.manager.ShizukuSettings.Keys.KEY_WATCHDOG
import moe.shizuku.manager.adb.AdbStarter
import moe.shizuku.manager.app.ThemeHelper
import moe.shizuku.manager.ktx.toHtml
import moe.shizuku.manager.receiver.NotifCancelReceiver
import moe.shizuku.manager.receiver.ShizukuReceiverStarter
import moe.shizuku.manager.ui.miuix.MiuixActivity
import moe.shizuku.manager.ui.miuix.MiuixSnackbarMessage
import moe.shizuku.manager.ui.settings.LocaleOption
import moe.shizuku.manager.ui.settings.SettingsAction
import moe.shizuku.manager.ui.settings.SettingsDialog
import moe.shizuku.manager.ui.settings.SettingsScreen
import moe.shizuku.manager.ui.settings.SettingsUiState
import moe.shizuku.manager.utils.CustomTabsHelper
import moe.shizuku.manager.utils.EnvironmentUtils
import moe.shizuku.manager.utils.SettingsHelper
import moe.shizuku.manager.utils.ShizukuStateMachine
import moe.shizuku.manager.worker.AdbStartWorker
import rikka.core.util.ResourceUtils
import rikka.material.app.LocaleDelegate
import rikka.shizuku.manager.ShizukuLocales
import java.util.Locale
import kotlin.coroutines.resume

class SettingsActivity : MiuixActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val uiState = mutableStateOf(SettingsUiState())
    private val snackbarState = mutableStateOf<MiuixSnackbarMessage?>(null)

    private var pendingApply: (() -> Unit)? = null
    private var pendingRestartSetting: String? = null
    private var batteryContinuation: CancellableContinuation<Boolean>? = null
    private var batteryActionTaken = false

    private val batteryOptimizationLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val accepted = SettingsHelper.isIgnoringBatteryOptimizations(this)
            batteryContinuation?.resume(accepted)
        }

    private val stateListener: (ShizukuStateMachine.State) -> Unit = {
        refreshRunningState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uiState.value = buildUiState()
        setMiuixContent {
            SettingsScreen(
                state = uiState.value,
                languageOptions = buildLanguageOptions(),
                snackbar = snackbarState.value,
                onAction = ::handleAction,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        ShizukuSettings.getPreferences().registerOnSharedPreferenceChangeListener(this)
        ShizukuStateMachine.addListener(stateListener)
    }

    override fun onPause() {
        ShizukuSettings.getPreferences().unregisterOnSharedPreferenceChangeListener(this)
        ShizukuStateMachine.removeListener(stateListener)
        super.onPause()
    }

    override fun onDestroy() {
        batteryContinuation?.cancel()
        super.onDestroy()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == KEY_WATCHDOG) {
            uiState.value = uiState.value.copy(watchdogRunning = ShizukuSettings.isWatchdogRunning())
        }
    }

    private fun buildUiState(): SettingsUiState {
        val prefs = ShizukuSettings.getPreferences()
        val tlsSupported = EnvironmentUtils.isTlsSupported()
        val isTelevision = EnvironmentUtils.isTelevision()
        val tcpModeVisible = tlsSupported || isTelevision
        val tcpModeChecked = if (tlsSupported) ShizukuSettings.getTcpMode() else true
        val languageTag = prefs.getString(KEY_LANGUAGE, "SYSTEM") ?: "SYSTEM"
        val nightMode = ShizukuSettings.getNightMode()
        val contributors = getString(R.string.translation_contributors).toHtml().toString()
        return SettingsUiState(
            startOnBootEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ||
                    isTelevision || EnvironmentUtils.isRooted(),
            startOnBootChecked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ||
                    isTelevision || EnvironmentUtils.isRooted()
            ) {
                ShizukuSettings.getStartOnBoot(this)
            } else {
                false
            },
            watchdogRunning = ShizukuSettings.isWatchdogRunning(),
            tcpModeVisible = tcpModeVisible,
            tcpModeChecked = tcpModeChecked,
            tcpModeEnabled = tlsSupported,
            tcpModeSummary = if (tlsSupported) getString(R.string.settings_tcp_mode_summary) else "",
            tcpPortVisible = tcpModeVisible && tcpModeChecked,
            tcpPortValue = prefs.getString(KEY_TCP_PORT, null) ?: "",
            autoDisableUsbChecked = ShizukuSettings.getAutoDisableUsbDebugging(),
            languageTag = languageTag,
            languageSummary = buildLanguageSummary(languageTag),
            translationContributors = contributors,
            nightMode = nightMode,
            blackNightVisible = nightMode != AppCompatDelegate.MODE_NIGHT_NO,
            blackNightChecked = ThemeHelper.isBlackNightTheme(this),
            useSystemColorVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
            useSystemColorChecked = ThemeHelper.isUsingSystemColor(),
            updateMode = ShizukuSettings.getUpdateMode(),
            legacyPairingVisible = !isTelevision,
            legacyPairingChecked = ShizukuSettings.getLegacyPairing(),
            running = ShizukuStateMachine.isRunning(),
            adbTcpPort = EnvironmentUtils.getAdbTcpPort(),
        )
    }

    private fun buildLanguageOptions(): List<LocaleOption> {
        val currentLocale = ShizukuSettings.getLocale()
        return ShizukuLocales.DISPLAY_LOCALES.mapIndexed { index, tag ->
            if (index == 0) {
                LocaleOption(
                    tag = ShizukuLocales.LOCALES[index],
                    nativeName = getString(R.string.follow_system),
                    localizedName = getString(R.string.follow_system),
                )
            } else {
                val locale = Locale.forLanguageTag(tag)
                val native = if (locale.script.isNotEmpty()) {
                    locale.getDisplayScript(locale)
                } else {
                    locale.getDisplayName(locale)
                }
                val localized = if (locale.script.isNotEmpty()) {
                    locale.getDisplayScript(currentLocale)
                } else {
                    locale.getDisplayName(currentLocale)
                }
                LocaleOption(
                    tag = ShizukuLocales.LOCALES[index],
                    nativeName = native,
                    localizedName = localized,
                )
            }
        }
    }

    private fun buildLanguageSummary(tag: String): String {
        if (tag.isEmpty() || tag == "SYSTEM") {
            return getString(R.string.follow_system)
        }
        val index = ShizukuLocales.LOCALES.indexOf(tag)
        if (index < 0) {
            return ""
        }
        val displayTag = ShizukuLocales.DISPLAY_LOCALES[index]
        val locale = Locale.forLanguageTag(displayTag)
        val localized = if (locale.script.isNotEmpty()) {
            locale.getDisplayScript(ShizukuSettings.getLocale())
        } else {
            locale.getDisplayName(ShizukuSettings.getLocale())
        }
        return localized.lineSequence().firstOrNull() ?: ""
    }

    private fun handleAction(action: SettingsAction) {
        when (action) {
            SettingsAction.Back -> finish()
            is SettingsAction.StartOnBootToggled -> toggleStartOnBoot(action.value)
            is SettingsAction.WatchdogToggled -> toggleWatchdog(action.value)
            is SettingsAction.TcpModeToggled -> toggleTcpMode(action.value)
            SettingsAction.OpenTcpPort -> openDialog(SettingsDialog.TcpPort)
            is SettingsAction.TcpPortSubmitted -> submitTcpPort(action.text)
            is SettingsAction.AutoDisableUsbToggled -> {
                ShizukuSettings.getPreferences().edit()
                    .putBoolean(KEY_AUTO_DISABLE_USB_DEBUGGING, action.value).apply()
                uiState.value = uiState.value.copy(autoDisableUsbChecked = action.value)
            }
            SettingsAction.OpenLanguage -> openDialog(SettingsDialog.Language)
            is SettingsAction.LanguageSelected -> selectLanguage(action.tag)
            SettingsAction.TranslationClicked ->
                CustomTabsHelper.launchUrlOrCopy(this, getString(R.string.translation_url))
            SettingsAction.OpenNightMode -> openDialog(SettingsDialog.NightMode)
            is SettingsAction.NightModeSelected -> selectNightMode(action.value)
            is SettingsAction.BlackNightThemeToggled -> toggleBlackNightTheme(action.value)
            is SettingsAction.UseSystemColorToggled -> toggleUseSystemColor(action.value)
            SettingsAction.OpenUpdateMode -> openDialog(SettingsDialog.UpdateMode)
            is SettingsAction.UpdateModeSelected -> selectUpdateMode(action.value)
            SettingsAction.HelpClicked ->
                CustomTabsHelper.launchUrlOrCopy(this, getString(R.string.help_url))
            SettingsAction.ReportBugClicked -> openDialog(SettingsDialog.BugReport)
            SettingsAction.BugReportGitHub ->
                CustomTabsHelper.launchUrlOrCopy(this, "https://github.com/thedjchi/Shizuku/issues/new")
            SettingsAction.BugReportEmail -> sendBugReportEmail()
            SettingsAction.BugReportCancel -> {
                cancelBugReportNotification()
                closeDialog()
            }
            is SettingsAction.BugReportLinkClicked ->
                CustomTabsHelper.launchUrlOrCopy(this, action.url)
            is SettingsAction.LegacyPairingToggled -> {
                ShizukuSettings.getPreferences().edit()
                    .putBoolean(KEY_LEGACY_PAIRING, action.value).apply()
                uiState.value = uiState.value.copy(legacyPairingChecked = action.value)
            }
            SettingsAction.DialogConfirmed -> confirmDialog()
            SettingsAction.DialogCancelled -> cancelDialog()
            SettingsAction.DialogDismissed -> dismissDialog()
            SettingsAction.DismissSnackbar -> snackbarState.value = null
        }
    }

    private fun toggleStartOnBoot(value: Boolean) {
        fun doToggle() {
            maybeToggleBatterySensitiveSetting(value) { accepted ->
                if (accepted) {
                    ShizukuSettings.setStartOnBoot(this, value)
                }
                uiState.value = uiState.value.copy(startOnBootChecked = ShizukuSettings.getStartOnBoot(this))
            }
        }
        if (value && !EnvironmentUtils.isTelevision() && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            openDialog(SettingsDialog.StartOnBootBug)
            pendingApply = ::doToggle
        } else {
            doToggle()
        }
    }

    private fun toggleWatchdog(value: Boolean) {
        maybeToggleBatterySensitiveSetting(value) { accepted ->
            if (accepted) {
                ShizukuSettings.setWatchdog(this, value)
            }
            uiState.value = uiState.value.copy(watchdogRunning = ShizukuSettings.isWatchdogRunning())
        }
    }

    private fun toggleTcpMode(value: Boolean) {
        fun applyChange() {
            ShizukuSettings.setTcpMode(value)
            uiState.value = uiState.value.copy(
                tcpModeChecked = value,
                tcpModeEnabled = true,
                tcpModeSummary = getString(R.string.settings_tcp_mode_summary),
                tcpPortVisible = value,
            )
        }
        if (!value && !ShizukuStateMachine.isRunning() && needsRestart(KEY_TCP_MODE, value)) {
            openDialog(SettingsDialog.StopTcp)
            pendingApply = ::applyChange
        } else {
            maybePromptRestart(KEY_TCP_MODE, value, ::applyChange)
        }
    }

    private fun submitTcpPort(text: String) {
        val port = text.toIntOrNull()
        if (port == null || port in 1..65535) {
            fun applyChange() {
                ShizukuSettings.setTcpPort(port)
                uiState.value = uiState.value.copy(tcpPortValue = port?.toString() ?: "")
            }
            closeDialog()
            maybePromptRestart(KEY_TCP_PORT, port ?: 5555, ::applyChange)
        } else {
            closeDialog()
            showSnackbar(getString(R.string.snackbar_invalid_port), null, null)
        }
    }

    private fun needsRestart(setting: String, newValue: Any?): Boolean {
        val currentPort = EnvironmentUtils.getAdbTcpPort()
        return when (setting) {
            KEY_TCP_MODE -> {
                val newMode = newValue as? Boolean ?: ShizukuSettings.getTcpMode()
                (currentPort > 0) != newMode
            }
            KEY_TCP_PORT -> {
                val newPort = newValue as? Int ?: ShizukuSettings.getTcpPort()
                currentPort > 0 && currentPort != newPort
            }
            else -> false
        }
    }

    private fun maybePromptRestart(setting: String, newValue: Any?, applyChange: () -> Unit) {
        if (!ShizukuStateMachine.isRunning() || !needsRestart(setting, newValue)) {
            applyChange()
            sendBroadcast(Intent(this, NotifCancelReceiver::class.java))
        } else {
            pendingApply = applyChange
            pendingRestartSetting = setting
            openDialog(
                if (setting == KEY_TCP_MODE) {
                    SettingsDialog.RestartTcpMode
                } else {
                    SettingsDialog.RestartTcpPort
                }
            )
        }
    }

    private fun maybeToggleBatterySensitiveSetting(newValue: Boolean, onResult: (Boolean) -> Unit) {
        if (!newValue || SettingsHelper.isIgnoringBatteryOptimizations(this) || EnvironmentUtils.isTelevision()) {
            onResult(true)
            return
        }
        lifecycleScope.launch {
            val accepted = suspendCancellableCoroutine<Boolean> { continuation ->
                batteryContinuation = continuation
                batteryActionTaken = false
                snackbarState.value = MiuixSnackbarMessage(
                    text = getString(R.string.snackbar_battery_optimization_settings),
                    actionText = getString(R.string.snackbar_action_fix),
                    onAction = {
                        batteryActionTaken = true
                        snackbarState.value = null
                        SettingsHelper.requestIgnoreBatteryOptimizations(this@SettingsActivity, batteryOptimizationLauncher)
                    },
                    onDismiss = {
                        snackbarState.value = null
                        if (!batteryActionTaken) {
                            continuation.resume(false)
                        }
                    },
                )
            }
            onResult(accepted)
        }
    }

    private fun selectLanguage(tag: String) {
        ShizukuSettings.getPreferences().edit().putString(KEY_LANGUAGE, tag).apply()
        val locale = if (tag == "SYSTEM") {
            LocaleDelegate.systemLocale
        } else {
            Locale.forLanguageTag(tag)
        }
        LocaleDelegate.defaultLocale = locale
        closeDialog()
        recreate()
    }

    private fun selectNightMode(value: Int) {
        val changed = ShizukuSettings.getNightMode() != value
        ShizukuSettings.getPreferences().edit().putInt(KEY_NIGHT_MODE, value).apply()
        closeDialog()
        if (changed) {
            AppCompatDelegate.setDefaultNightMode(value)
            recreate()
        }
    }

    private fun toggleBlackNightTheme(value: Boolean) {
        ShizukuSettings.getPreferences().edit().putBoolean(KEY_BLACK_NIGHT_THEME, value).apply()
        uiState.value = uiState.value.copy(blackNightChecked = value)
        if (ResourceUtils.isNightMode(resources.configuration)) {
            recreate()
        }
    }

    private fun toggleUseSystemColor(value: Boolean) {
        val changed = ThemeHelper.isUsingSystemColor() != value
        ShizukuSettings.getPreferences().edit().putBoolean(KEY_USE_SYSTEM_COLOR, value).apply()
        uiState.value = uiState.value.copy(useSystemColorChecked = value)
        if (changed) {
            recreate()
        }
    }

    private fun selectUpdateMode(value: Int) {
        ShizukuSettings.getPreferences().edit().putInt(KEY_UPDATE_MODE, value).apply()
        closeDialog()
        uiState.value = uiState.value.copy(updateMode = value)
    }

    private fun sendBugReportEmail() {
        val plainBody = "Please describe the bug. Include steps to reproduce if possible, as well as any relevant images/logs.\n\n" +
                "Device: " + Build.MANUFACTURER + " " + Build.MODEL + "\n" +
                "Android Version: " + Build.VERSION.RELEASE + "\n" +
                "Shizuku Version: " + BuildConfig.VERSION_NAME
        val intent = Intent(
            Intent.ACTION_SENDTO,
            Uri.parse("mailto:" + getString(R.string.support_email) +
                    "?subject=" + Uri.encode("[ISSUE TITLE]") +
                    "&body=" + Uri.encode(plainBody))
        )
        try {
            startActivity(intent)
            closeDialog()
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.toast_no_email_app, Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelBugReportNotification() {
        getSystemService(NotificationManager::class.java).cancel(AdbStartWorker.NOTIFICATION_ID)
    }

    private fun confirmDialog() {
        val dialog = uiState.value.dialog
        val apply = pendingApply
        val setting = pendingRestartSetting
        pendingApply = null
        pendingRestartSetting = null
        closeDialog()
        when (dialog) {
            SettingsDialog.StartOnBootBug -> apply?.invoke()
            SettingsDialog.StopTcp -> stopTcpThenApply(apply)
            SettingsDialog.RestartTcpMode, SettingsDialog.RestartTcpPort -> {
                apply?.invoke()
                if (setting == KEY_TCP_MODE) {
                    ShizukuReceiverStarter.start(this, true)
                }
            }
            else -> Unit
        }
    }

    private fun stopTcpThenApply(apply: (() -> Unit)?) {
        lifecycleScope.launch {
            uiState.value = uiState.value.copy(
                tcpModeEnabled = false,
                tcpModeSummary = getString(R.string.settings_tcp_mode_closing_port),
            )
            AdbStarter.stopTcp(this@SettingsActivity, EnvironmentUtils.getAdbTcpPort())
            if (EnvironmentUtils.getAdbTcpPort() <= 0) {
                apply?.invoke()
            }
        }
    }

    private fun cancelDialog() {
        pendingApply = null
        pendingRestartSetting = null
        closeDialog()
    }

    private fun dismissDialog() {
        pendingApply = null
        pendingRestartSetting = null
        closeDialog()
    }

    private fun openDialog(dialog: SettingsDialog) {
        uiState.value = uiState.value.copy(dialog = dialog)
    }

    private fun closeDialog() {
        uiState.value = uiState.value.copy(dialog = SettingsDialog.None)
    }

    private fun refreshRunningState() {
        uiState.value = uiState.value.copy(
            running = ShizukuStateMachine.isRunning(),
            adbTcpPort = EnvironmentUtils.getAdbTcpPort(),
        )
    }

    private fun showSnackbar(text: String, actionText: String?, onAction: (() -> Unit)?) {
        snackbarState.value = MiuixSnackbarMessage(
            text = text,
            actionText = actionText,
            onAction = onAction,
            onDismiss = { snackbarState.value = null },
        )
    }
}
