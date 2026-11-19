package moe.shizuku.manager.management

import android.content.pm.PackageInfo
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateOf
import moe.shizuku.manager.Helps
import moe.shizuku.manager.R
import moe.shizuku.manager.authorization.AuthorizationManager
import moe.shizuku.manager.ui.glass.GlassActivity
import moe.shizuku.manager.ui.management.ManagementLimitedDialog
import moe.shizuku.manager.ui.management.ManagementScreen
import moe.shizuku.manager.ui.management.ManagementUiState
import moe.shizuku.manager.utils.CustomTabsHelper
import moe.shizuku.manager.utils.ShizukuStateMachine
import rikka.lifecycle.Status
import rikka.shizuku.Shizuku
import java.util.Objects

class ApplicationManagementActivity : GlassActivity() {

    private val viewModel: AppsViewModel by viewModels()
    private val uiState = mutableStateOf(ManagementUiState())
    private val dialogState = mutableStateOf<ManagementLimitedDialog?>(null)

    private val stateListener: (ShizukuStateMachine.State) -> Unit = {
        if (ShizukuStateMachine.isDead() && !isFinishing)
            finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!ShizukuStateMachine.isRunning()) {
            finish()
            return
        }

        setGlassContent { backdrop ->
            ManagementScreen(
                backdrop = backdrop,
                state = uiState,
                dialog = dialogState.value,
                onBack = { finish() },
                onHelp = { CustomTabsHelper.launchUrlOrCopy(this@ApplicationManagementActivity, Helps.APPS.get()) },
                onToggleApp = { toggleApp(it) },
                onToggleAll = { toggleAll(it) },
                onDismissDialog = { dialogState.value = null },
            )
        }

        viewModel.packages.observe(this) {
            when (it.status) {
                Status.SUCCESS -> {
                    uiState.value = uiState.value.copy(apps = it.data.orEmpty(), loaded = true)
                }
                Status.ERROR -> {
                    finish()
                    val tr = it.error
                    Toast.makeText(this, Objects.toString(tr, "unknown"), Toast.LENGTH_SHORT).show()
                    tr.printStackTrace()
                }
                Status.LOADING -> {

                }
            }
        }
        viewModel.grantedCount.observe(this) {
            if (it.status == Status.SUCCESS) {
                uiState.value = uiState.value.copy(grantedCount = it.data ?: 0)
            }
        }
        viewModel.load()

        ShizukuStateMachine.addListener(stateListener)
    }

    private fun toggleApp(pi: PackageInfo) {
        val ai = pi.applicationInfo ?: return
        try {
            if (AuthorizationManager.granted(pi.packageName, ai.uid)) {
                AuthorizationManager.revoke(pi.packageName, ai.uid)
            } else {
                AuthorizationManager.grant(pi.packageName, ai.uid)
            }
        } catch (e: SecurityException) {
            val uid = try {
                Shizuku.getUid()
            } catch (ex: Throwable) {
                return
            }
            if (uid != 0) {
                dialogState.value = ManagementLimitedDialog(
                    title = getString(R.string.app_management_dialog_adb_is_limited_title),
                    message = getString(R.string.app_management_dialog_adb_is_limited_message, Helps.ADB.get()),
                )
            }
        }
        refreshGrants()
    }

    private fun toggleAll(enabled: Boolean) {
        for (pi in uiState.value.apps) {
            val ai = pi.applicationInfo ?: continue
            try {
                if (enabled) {
                    AuthorizationManager.grant(pi.packageName, ai.uid)
                } else {
                    AuthorizationManager.revoke(pi.packageName, ai.uid)
                }
            } catch (e: Exception) {
            }
        }
        refreshGrants()
    }

    private fun refreshGrants() {
        uiState.value = uiState.value.copy(grantRevision = uiState.value.grantRevision + 1)
        viewModel.load(true)
    }

    override fun onResume() {
        super.onResume()
        uiState.value = uiState.value.copy(grantRevision = uiState.value.grantRevision + 1)
        viewModel.load(true)
    }

    override fun onDestroy() {
        ShizukuStateMachine.removeListener(stateListener)
        super.onDestroy()
    }
}
