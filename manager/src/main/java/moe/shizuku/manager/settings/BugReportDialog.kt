package moe.shizuku.manager.settings

import android.app.Dialog
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.DialogFragment
import moe.shizuku.manager.ui.miuix.rememberShizukuThemeController
import moe.shizuku.manager.BuildConfig
import moe.shizuku.manager.R
import moe.shizuku.manager.app.ThemeHelper
import moe.shizuku.manager.ktx.asLink
import moe.shizuku.manager.utils.CustomTabsHelper
import moe.shizuku.manager.worker.AdbStartWorker
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

class BugReportDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()

        val updateLink = getString(R.string.bug_report_dialog_link_update)
            .asLink("https://github.com/thedjchi/Shizuku/releases/latest")
        val wikiLink = getString(R.string.bug_report_dialog_link_wiki)
            .asLink("https://github.com/thedjchi/Shizuku/releases/wiki#troubleshooting")
        val issuesLink = getString(R.string.bug_report_dialog_link_issues)
            .asLink("https://github.com/thedjchi/Shizuku/releases/issues")

        val updateText = TextUtils.expandTemplate(getString(R.string.bug_report_dialog_update), updateLink)
        val wikiText = TextUtils.expandTemplate(getString(R.string.bug_report_dialog_wiki), wikiLink)
        val issuesText = TextUtils.expandTemplate(getString(R.string.bug_report_dialog_issues), issuesLink)
        val methodText = TextUtils.expandTemplate(getString(R.string.bug_report_dialog_method), "GitHub")
        val methodText2 = getString(R.string.bug_report_dialog_method_2)

        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
                setContent {
                    val controller = rememberShizukuThemeController()
                    MiuixTheme(controller = controller) {
                        GlassDialogCard(title = stringResource(R.string.settings_report_bug), onDismissRequest = { dialog.cancel() }) {
                            val textColor = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            listOf(updateText, wikiText, issuesText, methodText, methodText2).forEach { text ->
                                AndroidView(
                                    factory = { TextView(it) },
                                    update = { textView ->
                                        textView.text = text
                                        textView.movementMethod = LinkMovementMethod.getInstance()
                                        textView.setTextColor(textColor.toArgb())
                                        textView.setTextAppearance(android.R.style.TextAppearance_Material_Body2)
                                    },
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                TextButton(
                                    text = stringResource(android.R.string.cancel),
                                    onClick = { dialog.cancel() },
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(
                                    text = stringResource(R.string.bug_report_dialog_button_email),
                                    onClick = { if (sendEmail(context)) dismissAllowingStateLoss() },
                                    modifier = Modifier.weight(1f),
                                )
                                Button(
                                    colors = ButtonDefaults.buttonColorsPrimary(),
                                    onClick = {
                                        CustomTabsHelper.launchUrlOrCopy(context, "https://github.com/thedjchi/Shizuku/issues/new")
                                        dismissAllowingStateLoss()
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("GitHub", style = MiuixTheme.textStyles.button)
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
        dialog.setCanceledOnTouchOutside(true)
        dialog.setOnDismissListener { _ ->
            if (activity is BugReportDialogActivity) activity?.finish()
        }
        dialog.setOnCancelListener { _ ->
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(AdbStartWorker.NOTIFICATION_ID)
        }
        return dialog
    }

    private fun sendEmail(context: Context): Boolean {
        val plainBody = """
            Please describe the bug. Include steps to reproduce if possible, as well as any relevant images/logs.

            Device: ${Build.MANUFACTURER} ${Build.MODEL}
            Android Version: ${Build.VERSION.RELEASE}
            Shizuku Version: ${BuildConfig.VERSION_NAME}
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(
            "mailto:" + context.getString(R.string.support_email) +
                "?subject=" + Uri.encode("[ISSUE TITLE]") +
                "&body=" + Uri.encode(plainBody),
        ))
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, context.getString(R.string.toast_no_email_app), Toast.LENGTH_SHORT).show()
            false
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        val nm = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(AdbStartWorker.NOTIFICATION_ID)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (activity is BugReportDialogActivity) activity?.finish()
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
