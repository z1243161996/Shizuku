package moe.shizuku.manager.authorization

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import moe.shizuku.manager.Helps
import moe.shizuku.manager.R
import moe.shizuku.manager.ui.miuix.MiuixActivity
import moe.shizuku.manager.ui.miuix.MiuixGlassCard
import moe.shizuku.manager.ui.miuix.MiuixTopBar
import moe.shizuku.manager.utils.Logger.LOGGER
import moe.shizuku.manager.utils.ShizukuStateMachine
import rikka.core.res.resolveColor
import rikka.html.text.HtmlCompat
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_ALLOWED
import rikka.shizuku.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_IS_ONETIME
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

class RequestPermissionActivity : MiuixActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!waitForBinder()) {
            finish()
            return
        }

        val uid = intent.getIntExtra("uid", -1)
        val pid = intent.getIntExtra("pid", -1)
        val requestCode = intent.getIntExtra("requestCode", -1)
        val ai = intent.getParcelableExtra<ApplicationInfo>("applicationInfo")
        if (uid == -1 || pid == -1 || ai == null) {
            finish()
            return
        }

        val limited: Boolean
        val label: CharSequence
        if (!hasGrantRuntimePermission()) {
            setResult(uid, pid, requestCode, allowed = false, onetime = true)
            limited = true
            label = ""
        } else {
            limited = false
            label = try {
                ai.loadLabel(packageManager)
            } catch (e: Exception) {
                ai.packageName
            }
        }

        setMiuixContent {
            PermissionScreen(
                uid = uid,
                pid = pid,
                requestCode = requestCode,
                label = label,
                limited = limited,
                onAllow = {
                    setResult(uid, pid, requestCode, allowed = true, onetime = false)
                    finish()
                },
                onDeny = {
                    setResult(uid, pid, requestCode, allowed = false, onetime = true)
                    finish()
                },
                onClose = { finish() },
            )
        }
    }

    private fun waitForBinder(): Boolean {
        return try {
            runBlocking {
                withTimeout(5000) {
                    ShizukuStateMachine.asFlow().first { it == ShizukuStateMachine.State.RUNNING }
                }
            }
            true
        } catch (e: TimeoutCancellationException) {
            LOGGER.e("Binder not received in 5s")
            false
        }
    }

    private fun hasGrantRuntimePermission(): Boolean {
        return Shizuku.checkRemotePermission("android.permission.GRANT_RUNTIME_PERMISSIONS") ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun setResult(requestUid: Int, requestPid: Int, requestCode: Int, allowed: Boolean, onetime: Boolean) {
        val result = Bundle().apply {
            putBoolean(REQUEST_PERMISSION_REPLY_ALLOWED, allowed)
            putBoolean(REQUEST_PERMISSION_REPLY_IS_ONETIME, onetime)
        }
        try {
            Shizuku.dispatchPermissionConfirmationResult(requestUid, requestPid, requestCode, result)
        } catch (e: Throwable) {
            LOGGER.e(e, "dispatchPermissionConfirmationResult")
        }
        setResult(if (allowed) RESULT_OK else RESULT_CANCELED)
    }
}

@Composable
private fun PermissionScreen(
    uid: Int,
    pid: Int,
    requestCode: Int,
    label: CharSequence,
    limited: Boolean,
    onAllow: () -> Unit,
    onDeny: () -> Unit,
    onClose: () -> Unit,
) {
    if (!limited) {
        BackHandler { }
    }

    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current

    Scaffold(
        topBar = {
            MiuixTopBar(
                title = stringResource(R.string.app_name),
                color = colors.surface.copy(alpha = 0.55f),
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))
            MiuixGlassCard(Modifier.fillMaxWidth()) {
                Icon(
                    painter = painterResource(R.drawable.ic_system_icon),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = colors.primary,
                )
                Spacer(Modifier.height(10.dp))
                if (limited) {
                    Text(
                        text = "Shizuku: " + context.getString(R.string.app_management_dialog_adb_is_limited_title),
                        style = MiuixTheme.textStyles.title2,
                        color = colors.onSurface,
                    )
                    Spacer(Modifier.height(10.dp))
                    HtmlText(
                        html = HtmlCompat.fromHtml(
                            context.getString(
                                R.string.app_management_dialog_adb_is_limited_message,
                                Helps.ADB.get(),
                            ),
                        ),
                        color = colors.onSurfaceVariantSummary,
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        colors = ButtonDefaults.buttonColorsPrimary(),
                        onClick = onClose,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(android.R.string.ok), style = MiuixTheme.textStyles.button)
                    }
                } else {
                    HtmlText(
                        html = HtmlCompat.fromHtml(
                            context.getString(
                                R.string.permission_warning_template,
                                label,
                                context.getString(R.string.permission_group_description),
                            ),
                        ),
                        color = colors.onSurface,
                        titleStyle = true,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(
                            text = stringResource(R.string.grant_dialog_button_deny),
                            onClick = onDeny,
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            colors = ButtonDefaults.buttonColorsPrimary(),
                            onClick = onAllow,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.grant_dialog_button_allow_always), style = MiuixTheme.textStyles.button)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HtmlText(html: CharSequence, color: Color, titleStyle: Boolean = false) {
    AndroidView(
        factory = { TextView(it) },
        update = { textView ->
            textView.text = html
            textView.movementMethod = LinkMovementMethod.getInstance()
            textView.setTextColor(color.toArgb())
            if (titleStyle) {
                textView.setTextAppearance(android.R.style.TextAppearance_Material_Title)
            } else {
                textView.setTextAppearance(android.R.style.TextAppearance_Material_Body2)
            }
        },
    )
}
