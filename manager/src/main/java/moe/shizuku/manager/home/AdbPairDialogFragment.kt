package moe.shizuku.manager.home

import android.annotation.SuppressLint
import android.app.Application
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.adb.*
import moe.shizuku.manager.app.ThemeHelper
import moe.shizuku.manager.utils.SettingsHelper
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import java.net.ConnectException

@RequiresApi(VERSION_CODES.R)
class AdbPairDialogFragment : DialogFragment() {

    private val viewModel: ViewModel by activityViewModels()

    private val discoveryMode = mutableStateOf<Boolean?>(null)
    private val portValue = mutableStateOf<Int?>(null)
    private val pairingCodeError = mutableStateOf<String?>(null)
    private val portError = mutableStateOf<String?>(null)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val inMultiScreen = requireActivity().isInMultiWindowMode ||
            (requireActivity().window?.decorView?.display?.displayId ?: -1) > 0

        lateinit var dialog: Dialog
        dialog = createGlassDialog {
            AdbPairDialogContent(
                inMultiScreen = inMultiScreen,
                discovery = discoveryMode.value,
                port = portValue.value,
                pairingCodeError = pairingCodeError.value,
                portError = portError.value,
                onPairingCodeChange = { pairingCodeError.value = null },
                onConfirm = { portText, code ->
                    val parsed = portText.toIntOrNull() ?: -1
                    if (parsed > 65535 || parsed < 1) {
                        portError.value = context.getString(R.string.dialog_adb_invalid_port)
                    } else {
                        viewModel.run(parsed, code)
                    }
                },
                onDeveloperSettings = {
                    SettingsHelper.launchOrHighlightWirelessDebugging(context)
                },
                onDismiss = { dialog.dismiss() },
            )
        }
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnShowListener {
            viewModel.port.observe(this) {
                discoveryMode.value = it == -1
                portValue.value = it
            }
        }
        return dialog
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val context = requireContext()
        viewModel.result.observe(this) {
            if (it == null) {
                dismissAllowingStateLoss()
            } else {
                when (it) {
                    is ConnectException -> {
                        portError.value = context.getString(R.string.cannot_connect_port)
                    }
                    is AdbInvalidPairingCodeException -> {
                        pairingCodeError.value = context.getString(R.string.paring_code_is_wrong)
                    }
                    is AdbKeyException -> {
                        Toast.makeText(context, context.getString(R.string.adb_error_key_store), Toast.LENGTH_LONG)
                            .apply { setGravity(Gravity.CENTER, 0, 0) }.show()
                    }
                }
            }
        }
    }

    fun show(fragmentManager: FragmentManager) {
        if (fragmentManager.isStateSaved) return
        show(fragmentManager, javaClass.simpleName)
    }

    override fun getDialog(): Dialog? {
        return super.getDialog()
    }
}

@SuppressLint("NewApi")
class ViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = getApplication<Application>().applicationContext

    private val _result = MutableLiveData<Throwable?>()
    val result = _result as LiveData<Throwable?>

    private val _port = MutableLiveData<Int>()
    val port = _port as LiveData<Int>

    @Volatile
    private var resolvedHost: String = "127.0.0.1"

    private val adbMdns: AdbMdns = AdbMdns(appContext, AdbMdns.TLS_PAIRING) {
        if (it.second > 0) resolvedHost = it.first
        _port.postValue(it.second)
    }

    init {
        adbMdns.start()
    }

    fun run(port: Int, password: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val host = resolvedHost

            val key = try {
                AdbKey(PreferenceAdbKeyStore(ShizukuSettings.getPreferences()), "shizuku")
            } catch (e: Throwable) {
                e.printStackTrace()
                _result.postValue(AdbKeyException(e))
                return@launch
            }

            AdbPairingClient(host, port, password, key).runCatching {
                start()
            }.onFailure {
                _result.postValue(it)
                it.printStackTrace()
            }.onSuccess {
                if (it) {
                    _result.postValue(null)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        adbMdns.stop()
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
                val controller = remember {
                    ThemeController(
                        colorSchemeMode = ColorSchemeMode.MonetSystem,
                        keyColor = if (ThemeHelper.isUsingSystemColor()) null else ComposeColor(0xFF3F51B5),
                    )
                }
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
private fun AdbPairDialogContent(
    inMultiScreen: Boolean,
    discovery: Boolean?,
    port: Int?,
    pairingCodeError: String?,
    portError: String?,
    onPairingCodeChange: () -> Unit,
    onConfirm: (portText: String, code: String) -> Unit,
    onDeveloperSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val disc = discovery ?: inMultiScreen
    val title = if (disc) {
        stringResource(R.string.dialog_adb_pairing_discovery)
    } else {
        stringResource(R.string.dialog_adb_pairing_title)
    }
    val pairingVisible = discovery == false
    val neutralVisible = discovery != false

    var code by remember { mutableStateOf("") }
    var portText by remember { mutableStateOf(port?.toString() ?: "") }
    LaunchedEffect(port) {
        portText = port?.toString() ?: ""
    }

    GlassDialogCard(title = title, onDismissRequest = onDismiss) {
        if (!inMultiScreen) {
            Text(
                text = stringResource(R.string.adb_pairing_requires_multi_window),
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.adb_pairing_requires_multi_window_reason),
                style = MiuixTheme.textStyles.body2,
                color = colors.onSurfaceVariantSummary,
            )
            Spacer(Modifier.height(10.dp))
        }
        if (disc) {
            Text(
                text = stringResource(R.string.dialog_adb_pairing_message),
                style = MiuixTheme.textStyles.body1,
                color = colors.onSurface,
            )
            Spacer(Modifier.height(14.dp))
        }
        if (pairingVisible) {
            OutlinedTextField(
                value = code,
                onValueChange = {
                    code = it.filter { c -> c.isDigit() }.take(6)
                    onPairingCodeChange()
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = pairingCodeError != null,
                label = { Text(stringResource(R.string.dialog_adb_pairing_paring_code)) },
                supportingText = if (pairingCodeError != null) {
                    { Text(pairingCodeError) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = miuixOutlinedTextFieldColors(),
            )
            Spacer(Modifier.height(10.dp))
        }
        if (portError != null) {
            OutlinedTextField(
                value = portText,
                onValueChange = { portText = it.filter { c -> c.isDigit() }.take(5) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = true,
                label = { Text(stringResource(R.string.dialog_adb_port)) },
                supportingText = { Text(portError) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = miuixOutlinedTextFieldColors(),
            )
            Spacer(Modifier.height(10.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(
                text = stringResource(android.R.string.cancel),
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            )
            if (neutralVisible) {
                TextButton(
                    text = stringResource(R.string.development_settings),
                    onClick = onDeveloperSettings,
                    modifier = Modifier.weight(1f),
                )
            }
            if (pairingVisible) {
                Button(
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    onClick = { onConfirm(portText, code) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(android.R.string.ok), style = MiuixTheme.textStyles.button)
                }
            }
        }
    }
}

@Composable
private fun miuixOutlinedTextFieldColors(): TextFieldColors {
    val colors = MiuixTheme.colorScheme
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colors.primary,
        unfocusedBorderColor = colors.dividerLine,
        focusedLabelColor = colors.primary,
        unfocusedLabelColor = colors.onSurfaceVariantSummary,
        cursorColor = colors.primary,
        errorBorderColor = colors.error,
        errorLabelColor = colors.error,
        errorTextColor = colors.error,
        focusedSupportingTextColor = colors.onSurfaceVariantSummary,
        unfocusedSupportingTextColor = colors.onSurfaceVariantSummary,
        errorSupportingTextColor = colors.error,
    )
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
