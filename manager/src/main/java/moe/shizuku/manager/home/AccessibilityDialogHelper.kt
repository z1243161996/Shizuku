package moe.shizuku.manager.home

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.TypefaceSpan
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import moe.shizuku.manager.R
import moe.shizuku.manager.adb.AdbPairingAccessibilityService
import moe.shizuku.manager.app.ThemeHelper
import moe.shizuku.manager.utils.SettingsHelper
import moe.shizuku.manager.utils.SettingsPage
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

fun Context.showAccessibilityDialog() {
    val hasWriteSecureSettings = (checkSelfPermission(WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED)

    val installer = packageManager.getInstallerPackageName(packageName)
    val isInstalledByPlayOrAdb = (installer == "com.android.vending") || (installer == null)
    val hasAccessRestrictedSettings = isInstalledByPlayOrAdb || Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    if (isAccessibilityEnabled()) {
        showNavigateDialog()
    } else if (hasWriteSecureSettings) {
        if (enableAccessibilityService()) return
        showPermissionDialog()
    } else if (!hasAccessRestrictedSettings) {
        showPermissionDialog()
    } else {
        showEnableDialog()
    }
}

private fun Context.showPermissionDialog() {
    val permissionName = "ACCESS_RESTRICTED_SETTINGS"
    val permissionCommand = "adb shell cmd appops set $packageName $permissionName allow"
    val styledPermissionCommand =
        SpannableString(permissionCommand).apply {
            setSpan(TypefaceSpan("monospace"), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

    showGlassDialog(
        title = getString(android.R.string.dialog_alert_title),
        message = TextUtils.expandTemplate(
            getString(R.string.dialog_adb_pairing_accessibility_permission),
            permissionName,
            styledPermissionCommand,
        ),
        confirmText = "Continue",
        onConfirm = { showEnableDialog() },
    )
}

private fun Context.showEnableDialog() {
    showGlassDialog(
        title = getString(R.string.dialog_adb_pairing_title),
        message = getString(R.string.dialog_adb_pairing_accessibility_enable),
        confirmText = getString(R.string.enable),
        onConfirm = { SettingsPage.Accessibility.launch(this) },
    )
}

private fun Context.showNavigateDialog() {
    showGlassDialog(
        title = getString(R.string.dialog_adb_pairing_title),
        message = getString(R.string.dialog_adb_pairing_accessibility_navigate),
        confirmText = getString(R.string.development_settings),
        onConfirm = { SettingsPage.Developer.HighlightWirelessDebugging.launch(this) },
    )
}

private fun Context.showGlassDialog(
    title: String?,
    message: CharSequence,
    confirmText: String?,
    onConfirm: (() -> Unit)?,
    cancelable: Boolean = true,
) {
    val context = this
    val dialog = Dialog(this)
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    dialog.setContentView(
        ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setContent {
                val controller = remember {
                    ThemeController(
                        colorSchemeMode = ColorSchemeMode.MonetSystem,
                        keyColor = if (ThemeHelper.isUsingSystemColor()) null else ComposeColor(0xFF3F51B5),
                    )
                }
                MiuixTheme(controller = controller) {
                    GlassDialogCard(title = title, onDismissRequest = { dialog.cancel() }) {
                        val textColor = MiuixTheme.colorScheme.onSurface
                        AndroidView(
                            factory = { TextView(it) },
                            update = { textView ->
                                textView.text = message
                                textView.movementMethod = LinkMovementMethod.getInstance()
                                textView.setTextColor(textColor.toArgb())
                            },
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TextButton(
                                text = context.getString(android.R.string.cancel),
                                onClick = { dialog.cancel() },
                                modifier = Modifier.weight(1f),
                            )
                            if (confirmText != null && onConfirm != null) {
                                Button(
                                    colors = ButtonDefaults.buttonColorsPrimary(),
                                    onClick = {
                                        dialog.dismiss()
                                        onConfirm()
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(confirmText, style = MiuixTheme.textStyles.button)
                                }
                            }
                        }
                    }
                }
            }
        },
    )
    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
    dialog.window?.setDimAmount(0f)
    dialog.setCancelable(cancelable)
    dialog.show()
}

private fun Context.getEnabledAccessibilityServices(): List<String>? {
    val enabledServices =
        Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        )
    return enabledServices?.split(":")
}

private fun Context.isAccessibilityEnabled(): Boolean {
    val accessibilityServiceName = "$packageName/${AdbPairingAccessibilityService::class.java.canonicalName}"
    return getEnabledAccessibilityServices()?.any { it.equals(accessibilityServiceName) } ?: false
}

private fun Context.enableAccessibilityService(): Boolean {
    if (isAccessibilityEnabled()) return true

    val accessibilityServiceName = "$packageName/${AdbPairingAccessibilityService::class.java.canonicalName}"
    val enabledServices = getEnabledAccessibilityServices()
    val newServices =
        if (enabledServices.isNullOrEmpty()) {
            accessibilityServiceName
        } else {
            enabledServices.joinToString(":") + ":$accessibilityServiceName"
        }

    Settings.Secure.putString(
        contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        newServices,
    )

    return isAccessibilityEnabled()
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
