package moe.shizuku.manager.home

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Window
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import moe.shizuku.manager.ui.miuix.rememberShizukuThemeController
import moe.shizuku.manager.R
import moe.shizuku.manager.adb.AdbMdns
import moe.shizuku.manager.app.ThemeHelper
import moe.shizuku.manager.starter.StarterActivity
import moe.shizuku.manager.utils.SettingsPage
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

@RequiresApi(Build.VERSION_CODES.R)
class AdbDialogFragment : DialogFragment() {

    private lateinit var adbMdns: AdbMdns
    private val port = MutableLiveData<Int>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        adbMdns = AdbMdns(context, AdbMdns.TLS_CONNECT) {
            port.postValue(it.second)
        }

        lateinit var dialog: Dialog
        dialog = createGlassDialog {
            AdbDiscoveryDialog(
                onDeveloperSettings = {
                    SettingsPage.Developer.HighlightWirelessDebugging.launch(context)
                },
                onDismiss = { dialog.dismiss() },
            )
        }
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnShowListener {
            adbMdns.start()
            if (context.checkSelfPermission(WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED) {
                Settings.Global.putInt(context.contentResolver, "adb_wifi_enabled", 1)
            }
            port.observe(this) {
                if (it > 65535 || it < 1) return@observe
                port.removeObservers(this)
                startAndDismiss(it)
            }
        }
        return dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        adbMdns.stop()
    }

    private fun startAndDismiss(port: Int) {
        val intent = Intent(context, StarterActivity::class.java).apply {
            putExtra(StarterActivity.EXTRA_PORT, port)
        }
        requireContext().startActivity(intent)
        dismissAllowingStateLoss()
    }

    fun show(fragmentManager: FragmentManager) {
        if (fragmentManager.isStateSaved) return
        show(fragmentManager, javaClass.simpleName)
    }
}

private fun DialogFragment.createGlassDialog(content: @Composable () -> Unit): Dialog {
    val context = requireContext()
    val dialog = Dialog(context)
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    dialog.setContentView(
        ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setContent {
                val controller = rememberShizukuThemeController()
                MiuixTheme(controller = controller) {
                    content()
                }
            }
        },
    )
    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
    dialog.window?.setDimAmount(0f)
    return dialog
}

@Composable
private fun AdbDiscoveryDialog(
    onDeveloperSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    GlassDialogCard(
        title = stringResource(R.string.dialog_adb_discovery),
        onDismissRequest = onDismiss,
    ) {
        Text(
            text = stringResource(R.string.dialog_adb_discovery_message),
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.dialog_adb_discovery_message_toggle_wireless_debugging),
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.SemiBold,
            color = MiuixTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(14.dp))
        DialogButtons(
            dismissText = stringResource(android.R.string.cancel),
            onDismiss = onDismiss,
            confirmText = stringResource(R.string.development_settings),
            onConfirm = onDeveloperSettings,
        )
    }
}

@Composable
private fun GlassDialogCard(
    title: String?,
    onDismissRequest: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onDismissRequest?.invoke() },
            )
            .padding(horizontal = 32.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(colors.surface.copy(alpha = 0.96f))
                .border(1.dp, colors.dividerLine, RoundedCornerShape(24.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(18.dp),
        ) {
            if (title != null) {
                Text(text = title, style = MiuixTheme.textStyles.title2, color = colors.onSurface)
                Spacer(Modifier.height(12.dp))
            }
            content()
        }
    }
}

@Composable
private fun DialogButtons(
    dismissText: String,
    onDismiss: () -> Unit,
    confirmText: String? = null,
    onConfirm: (() -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(text = dismissText, onClick = onDismiss, modifier = Modifier.weight(1f))
        if (confirmText != null && onConfirm != null) {
            Button(
                colors = ButtonDefaults.buttonColorsPrimary(),
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
            ) {
                Text(confirmText, style = MiuixTheme.textStyles.button)
            }
        }
    }
}
