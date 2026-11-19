package moe.shizuku.manager.legacy

import android.app.Activity
import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import moe.shizuku.manager.MainActivity
import moe.shizuku.manager.R
import moe.shizuku.manager.ktx.toHtml
import moe.shizuku.manager.ui.miuix.MiuixActivity
import moe.shizuku.manager.ui.miuix.MiuixGlassCard
import moe.shizuku.manager.ui.miuix.MiuixTopBar
import rikka.html.text.HtmlCompat
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

class LegacyIsNotSupportedActivity : MiuixActivity() {

    companion object {
        private inline val RESULT_CANCELED get() = Activity.RESULT_CANCELED
        private const val RESULT_ERROR = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callingComponent = callingActivity
        if (callingComponent == null) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        val ai = try {
            packageManager.getApplicationInfo(callingComponent.packageName, PackageManager.GET_META_DATA)
        } catch (e: Throwable) {
            finish()
            return
        }

        val label = try {
            ai.loadLabel(packageManager)
        } catch (e: Exception) {
            ai.packageName
        }

        val v3Support = ai.metaData?.getBoolean("moe.shizuku.client.V3_SUPPORT") == true

        setMiuixContent {
            LegacyScreen(
                v3Support = v3Support,
                label = label,
                onOpenShizuku = {
                    startActivity(
                        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                    finishWithResult()
                },
                onClose = { finishWithResult() },
            )
        }
    }

    private fun finishWithResult() {
        setResult(RESULT_ERROR)
        finish()
    }
}

@Composable
private fun LegacyScreen(
    v3Support: Boolean,
    label: CharSequence,
    onOpenShizuku: () -> Unit,
    onClose: () -> Unit,
) {
    BackHandler { }

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
                Text(
                    text = if (v3Support) {
                        context.getString(R.string.dialog_requesting_legacy_title, label)
                    } else {
                        context.getString(R.string.dialog_legacy_not_support_title, label)
                    },
                    style = MiuixTheme.textStyles.title2,
                    color = colors.onSurface,
                )
                Spacer(Modifier.height(10.dp))
                AndroidView(
                    factory = { TextView(it) },
                    update = { textView ->
                        textView.text = if (v3Support) {
                            context.getString(R.string.dialog_requesting_legacy_message, label)
                                .toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
                        } else {
                            context.getString(R.string.dialog_legacy_not_support_message, label)
                                .toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
                        }
                        textView.movementMethod = LinkMovementMethod.getInstance()
                        textView.setTextColor(colors.onSurfaceVariantSummary.toArgb())
                        textView.setTextAppearance(android.R.style.TextAppearance_Material_Body2)
                    },
                )
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (v3Support) {
                        TextButton(
                            text = stringResource(R.string.dialog_requesting_legacy_button_open_shizuku),
                            onClick = onOpenShizuku,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Button(
                        colors = ButtonDefaults.buttonColorsPrimary(),
                        onClick = onClose,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(android.R.string.ok), style = MiuixTheme.textStyles.button)
                    }
                }
            }
        }
    }
}
