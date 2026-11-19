package moe.shizuku.manager.stealth

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import moe.shizuku.manager.R
import moe.shizuku.manager.ui.miuix.MiuixActivity
import moe.shizuku.manager.ui.tutorials.StealthDialog
import moe.shizuku.manager.ui.tutorials.StealthTutorialScreen
import moe.shizuku.manager.utils.ApkUtils.*
import rikka.core.util.ClipboardUtils

class StealthTutorialActivity : MiuixActivity() {
    private val viewModel: StealthTutorialViewModel by viewModels()

    private lateinit var outDir: Uri

    private var uiState by mutableStateOf<UiState>(UiState.Idle(Action.HIDE))
    private var lastIdleAction by mutableStateOf(Action.HIDE)
    private var dialog by mutableStateOf(StealthDialog.None)
    private var dialogMessage by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.uiState.observe(this) { state ->
            uiState = state
            when (state) {
                is UiState.Idle -> lastIdleAction = state.action

                is UiState.Loading -> Unit

                is UiState.Pending -> {
                    try {
                        val apk = state.apk
                        when (state.apkType) {
                            ApkType.CLONE -> {
                                export(apk)
                                viewModel.refresh()
                                dialog = StealthDialog.Uninstall
                            }

                            ApkType.STUB -> installPackage(apk) { isSuccess, msg ->
                                handleInstallerResult(isSuccess, msg)
                            }
                        }
                    } catch (e: Exception) {
                        showErrorDialog(e)
                    }
                }

                is UiState.Error -> showErrorDialog(state.error)
            }
        }

        setMiuixContent {
            StealthTutorialScreen(
                state = uiState,
                lastIdleAction = lastIdleAction,
                dialog = dialog,
                dialogMessage = dialogMessage,
                onBack = { handleBack() },
                onAction = { action, packageName -> onAction(action, packageName) },
                onCopyCompatibility = { copyCompatibility() },
                onDismissDialog = { dialog = StealthDialog.None },
                onChooseFolderConfirm = {
                    dialog = StealthDialog.None
                    pickFolderLauncher.launch(null)
                },
                onUninstallConfirm = {
                    dialog = StealthDialog.None
                    uninstallPackage(packageName)
                },
                onCancelConfirm = { finish() },
            )
        }
    }

    override fun onDestroy() {
        runCatching {
            unregisterReceiver(installerReceiver)
        }
        super.onDestroy()
    }

    private fun onAction(action: Action, packageName: String?) {
        when (action) {
            Action.HIDE -> {
                viewModel.setPackageName(packageName)
                dialog = StealthDialog.ChooseFolder
            }

            Action.UNHIDE -> viewModel.createApk(ApkType.STUB)

            Action.REHIDE -> uninstallPackage(ORIGINAL_PACKAGE_NAME) { isSuccess, msg ->
                handleInstallerResult(isSuccess, msg)
            }
        }
    }

    private fun copyCompatibility() {
        if (
            ClipboardUtils.put(this, codeSnippet) &&
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2
        ) {
            Toast
                .makeText(
                    this,
                    getString(R.string.toast_copied_to_clipboard),
                    Toast.LENGTH_SHORT,
                ).show()
        }
    }

    private fun handleBack() {
        val state = viewModel.uiState.value
        if (state is UiState.Idle || state is UiState.Error) {
            finish()
            return
        }
        dialog = StealthDialog.Cancel
    }

    private val pickFolderLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { folder ->
            if (folder == null) return@registerForActivityResult
            outDir = folder

            viewModel.createApk(ApkType.CLONE)
        }

    private fun export(apk: File) {
        val docUri =
            DocumentsContract.buildDocumentUriUsingTree(
                outDir,
                DocumentsContract.getTreeDocumentId(outDir),
            )

        val doc =
            DocumentsContract.createDocument(
                contentResolver,
                docUri,
                "application/vnd.android.package-archive",
                buildApkFilename()
            )

        if (doc == null) throw Exception("Could not create file in selected folder")

        contentResolver.openOutputStream(doc)?.use { output ->
            apk.inputStream().use { input ->
                input.copyTo(output)
            }
        }
    }

    private fun showErrorDialog(error: Exception) {
        dialog = StealthDialog.Error
        dialogMessage = error.message
    }

    private fun handleInstallerResult(isSuccess: Boolean, msg: String?) {
        viewModel.refresh()
        if (isSuccess) {
            Toast.makeText(
                this,
                getString(R.string.success),
                Toast.LENGTH_SHORT,
            ).show()
        } else showErrorDialog(Exception(msg ?: "Unknown error"))
    }
}

private const val codeSnippet = """
import android.content.Context
import rikka.shizuku.ShizukuProvider

private fun Context.shizukuPermission() =
    runCatching {
        packageManager.getPermissionInfo(ShizukuProvider.PERMISSION, 0)
    }.getOrNull()

fun Context.isShizukuInstalled() =
    shizukuPermission() != null

fun Context.getShizukuPackageName() =
    shizukuPermission()?.packageName
"""
