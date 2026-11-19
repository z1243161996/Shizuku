package moe.shizuku.manager.home

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Process
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import moe.shizuku.manager.Helps
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.adb.AdbPairingService
import moe.shizuku.manager.management.AppsViewModel
import moe.shizuku.manager.management.ApplicationManagementActivity
import moe.shizuku.manager.settings.SettingsActivity
import moe.shizuku.manager.shell.ShellTutorialActivity
import moe.shizuku.manager.starter.Starter
import moe.shizuku.manager.starter.StarterActivity
import moe.shizuku.manager.stealth.StealthTutorialActivity
import moe.shizuku.manager.ui.glass.GlassActivity
import moe.shizuku.manager.ui.glass.GlassSnackbarMessage
import moe.shizuku.manager.ui.home.HomeAction
import moe.shizuku.manager.ui.home.HomeExitDialog
import moe.shizuku.manager.ui.home.HomeScreen
import moe.shizuku.manager.ui.home.HomeUiState
import moe.shizuku.manager.utils.AppIconCache
import moe.shizuku.manager.utils.CustomTabsHelper
import moe.shizuku.manager.utils.EnvironmentUtils
import moe.shizuku.manager.utils.SettingsHelper
import moe.shizuku.manager.utils.ShizukuStateMachine
import moe.shizuku.manager.utils.UpdateHelper
import rikka.lifecycle.Status
import rikka.shizuku.Shizuku

abstract class HomeActivity : GlassActivity() {

    private val homeModel: HomeViewModel by viewModels()
    private val appsModel: AppsViewModel by viewModels()

    private val uiState = mutableStateOf(HomeUiState())
    private val snackbarState = mutableStateOf<GlassSnackbarMessage?>(null)
    private val exitDialogState = mutableStateOf<HomeExitDialog?>(null)

    private val stateListener: (ShizukuStateMachine.State) -> Unit = {
        if (ShizukuStateMachine.isRunning()) {
            checkServerStatus()
            appsModel.load()
        } else if (ShizukuStateMachine.isDead()) {
            checkServerStatus()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        uiState.value = HomeUiState(
            isRooted = EnvironmentUtils.isRooted(),
            isPrimaryUser = Process.myUid() / 100000 == 0,
            isTelevision = EnvironmentUtils.isTelevision(),
            tlsSupported = EnvironmentUtils.isTlsSupported(),
            adbTcpPort = EnvironmentUtils.getAdbTcpPort(),
            versionName = packageManager.getPackageInfo(packageName, 0).versionName ?: "",
            adbCommand = Starter.adbCommand,
            authToken = ShizukuSettings.getAuthToken(),
            aboutIcon = AppIconCache.getOrLoadBitmap(
                this,
                applicationInfo,
                Process.myUid() / 100000,
                resources.getDimensionPixelOffset(R.dimen.default_app_icon_size),
            )?.asImageBitmap(),
        )

        setGlassContent { backdrop ->
            HomeScreen(
                backdrop = backdrop,
                state = uiState.value,
                snackbar = snackbarState.value,
                exitDialog = exitDialogState.value,
                onAction = ::handleAction,
            )
        }

        homeModel.serviceStatus.observe(this) {
            if (it.status == Status.SUCCESS) {
                val status = homeModel.serviceStatus.value?.data ?: return@observe
                ShizukuSettings.setLastLaunchMode(
                    if (status.uid == 0) ShizukuSettings.LaunchMethod.ROOT else ShizukuSettings.LaunchMethod.ADB
                )
                refreshUi()
            }
        }

        homeModel.shouldShowRebootDialog.observe(this) { shouldShow ->
            if (shouldShow) showExitDialog(
                getString(R.string.home_dialog_reboot_required_title),
                getString(R.string.home_dialog_reboot_required_message)
            )
        }

        homeModel.shouldShowUninstallDialog.observe(this) { shouldShow ->
            if (shouldShow) showExitDialog(
                getString(R.string.home_dialog_duplicate_app_detected_title),
                getString(R.string.home_dialog_duplicate_app_detected_message)
            )
        }

        homeModel.shouldShowBatteryOptimizationSnackbar.observe(this) { shouldShow ->
            if (shouldShow) {
                showSnackbar(
                    text = getString(R.string.snackbar_battery_optimization_home),
                    actionText = getString(R.string.snackbar_action_fix),
                    onAction = { SettingsHelper.requestIgnoreBatteryOptimizations(this, null) }
                )
            }
        }
        homeModel.checkBatteryOptimization()

        appsModel.grantedCount.observe(this) {
            if (it.status == Status.SUCCESS) {
                refreshUi()
            }
        }

        lifecycleScope.launch {
            if (UpdateHelper.isCheckForUpdatesEnabled() && UpdateHelper.isNewUpdateAvailable()) {
                showSnackbar(
                    text = getString(R.string.snackbar_update_available),
                    actionText = getString(R.string.snackbar_action_update),
                    onAction = { lifecycleScope.launch { UpdateHelper.update() } }
                )
                UpdateHelper.updateLastPromptedVersion()
            }
        }

        ShizukuStateMachine.addListener(stateListener)
    }

    private fun refreshUi() {
        val status = homeModel.serviceStatus.value?.data
        val granted = appsModel.grantedCount.value?.data ?: 0
        if (uiState.value.status == status && uiState.value.grantedCount == granted) {
            return
        }
        uiState.value = uiState.value.copy(status = status, grantedCount = granted)
    }

    private fun showSnackbar(text: String, actionText: String? = null, onAction: (() -> Unit)? = null) {
        snackbarState.value = GlassSnackbarMessage(
            text = text,
            actionText = actionText,
            onAction = {
                snackbarState.value = null
                onAction?.invoke()
            },
            onDismiss = { snackbarState.value = null },
        )
    }

    private fun showExitDialog(title: String, message: String) {
        exitDialogState.value = HomeExitDialog(title, message)
    }

    private fun checkServerStatus() {
        homeModel.reload()
    }

    private fun handleAction(action: HomeAction) {
        when (action) {
            HomeAction.StartRoot -> startActivity(
                Intent(this, StarterActivity::class.java).putExtra(StarterActivity.EXTRA_IS_ROOT, true)
            )

            HomeAction.StartWirelessAdb -> StartWirelessAdbViewHolder.start(this, lifecycleScope)

            HomeAction.PairWirelessAdb -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    StartWirelessAdbViewHolder.pair(this)
                }
            }

            HomeAction.WirelessAdbHelp -> CustomTabsHelper.launchUrlOrCopy(this, Helps.ADB_ANDROID11.get())

            HomeAction.OpenAdbPermissionHelp -> CustomTabsHelper.launchUrlOrCopy(this, Helps.ADB_PERMISSION.get())

            HomeAction.OpenLearnMore -> CustomTabsHelper.launchUrlOrCopy(this, Helps.HOME.get())

            HomeAction.OpenApps -> startActivity(Intent(this, ApplicationManagementActivity::class.java))

            HomeAction.OpenTerminal -> startActivity(Intent(this, ShellTutorialActivity::class.java))

            HomeAction.OpenStealth -> startActivity(Intent(this, StealthTutorialActivity::class.java))

            HomeAction.OpenSettings -> startActivity(Intent(this, SettingsActivity::class.java))

            HomeAction.StopService -> {
                if (ShizukuStateMachine.isRunning()) {
                    ShizukuStateMachine.set(ShizukuStateMachine.State.STOPPING)
                    runCatching { Shizuku.exit() }
                }
            }

            HomeAction.RegenerateToken -> {
            uiState.value = uiState.value.copy(authToken = ShizukuSettings.generateAuthToken())
            }

            HomeAction.CheckForUpdates -> lifecycleScope.launch { UpdateHelper.checkAndInstallUpdates() }

            HomeAction.Donate -> CustomTabsHelper.launchUrlOrCopy(this, "https://www.buymeacoffee.com/thedjchi")

            HomeAction.DismissExitDialog -> exitDialogState.value = null

            HomeAction.ExitApp -> finishAffinity()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val showDialog = intent.getBooleanExtra(EXTRA_SHOW_PAIRING_DIALOG, false)
        if (showDialog) showAccessibilityDialog()

        val startWadb = intent.getBooleanExtra(EXTRA_START_SERVICE_VIA_WADB, false)
        if (startWadb) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(AdbPairingService.NOTIFICATION_ID)
            StartWirelessAdbViewHolder.start(this, lifecycleScope)
        }
    }

    override fun onResume() {
        super.onResume()
        checkServerStatus()
        appsModel.load()
    }

    override fun onPause() {
        super.onPause()
        snackbarState.value = null
    }

    override fun onDestroy() {
        ShizukuStateMachine.removeListener(stateListener)
        super.onDestroy()
    }

    companion object {
        const val EXTRA_SHOW_PAIRING_DIALOG = "show_pairing_dialog"
        const val EXTRA_START_SERVICE_VIA_WADB = "start_service_via_wadb"
    }
}
