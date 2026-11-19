package moe.shizuku.manager.adb

import android.Manifest
import android.app.AppOpsManager
import android.app.ForegroundServiceStartNotAllowedException
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import moe.shizuku.manager.AppConstants
import moe.shizuku.manager.ui.miuix.MiuixActivity
import moe.shizuku.manager.ui.tutorials.AdbPairingTutorialScreen
import moe.shizuku.manager.utils.SettingsHelper
import moe.shizuku.manager.utils.SettingsPage
import rikka.compatibility.DeviceCompatibility

@RequiresApi(Build.VERSION_CODES.R)
class AdbPairingTutorialActivity : MiuixActivity() {

    private var notificationEnabled by mutableStateOf(false)
    private var isMiui by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = this

        isMiui = DeviceCompatibility.isMiui()
        notificationEnabled = isNotificationEnabled()

        if (notificationEnabled) {
            startPairingService()
        }

        setMiuixContent {
            AdbPairingTutorialScreen(
                notificationEnabled = notificationEnabled,
                isMiui = isMiui,
                onBack = { finish() },
                onDeveloperOptions = { SettingsHelper.launchOrHighlightWirelessDebugging(context) },
                onNotificationOptions = { SettingsPage.Notifications.NotificationSettings.launch(context) },
            )
        }
    }

    private fun isNotificationEnabled(): Boolean {
        val nm = getSystemService(NotificationManager::class.java)
        val channel = nm.getNotificationChannel(AdbPairingService.NOTIFICATION_CHANNEL)
        return nm.areNotificationsEnabled() &&
                (channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE)
    }

    override fun onResume() {
        super.onResume()

        val newNotificationEnabled = isNotificationEnabled()
        if (newNotificationEnabled != notificationEnabled) {
            notificationEnabled = newNotificationEnabled

            if (newNotificationEnabled) {
                startPairingService()
            }
        }
    }

    // Android 17 (SDK 37) gates local-network access behind ACCESS_LOCAL_NETWORK;
    // Android 16 (SDK 36) uses NEARBY_WIFI_DEVICES. Without a runtime grant the OS
    // intercepts the pairing connection with an endless "choose a device" picker.
    private fun localNetworkPermission(): String? = when {
        Build.VERSION.SDK_INT >= 37 -> "android.permission.ACCESS_LOCAL_NETWORK"
        Build.VERSION.SDK_INT >= 36 -> Manifest.permission.NEARBY_WIFI_DEVICES
        else -> null
    }

    private val localNetworkPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Start pairing whether or not the grant succeeded; a denial simply means
            // discovery/connect will fail and the service surfaces the error.
            doStartPairingService()
        }

    private fun startPairingService() {
        val permission = localNetworkPermission()
        if (permission != null && checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            localNetworkPermissionLauncher.launch(permission)
        } else {
            doStartPairingService()
        }
    }

    private fun doStartPairingService() {
        val intent = AdbPairingService.startIntent(this)
        try {
            startForegroundService(intent)
        } catch (e: Throwable) {
            Log.e(AppConstants.TAG, "startForegroundService", e)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && e is ForegroundServiceStartNotAllowedException
            ) {
                val mode = getSystemService(AppOpsManager::class.java)
                    .noteOpNoThrow("android:start_foreground", android.os.Process.myUid(), packageName, null, null)
                if (mode == AppOpsManager.MODE_ERRORED) {
                    Toast.makeText(this, "OP_START_FOREGROUND is denied. What are you doing?", Toast.LENGTH_LONG).show()
                }
                startService(intent)
            }
        }
    }
}
