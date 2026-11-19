package moe.shizuku.manager.shell

import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContracts
import moe.shizuku.manager.Helps
import moe.shizuku.manager.ui.miuix.MiuixActivity
import moe.shizuku.manager.ui.tutorials.ShellTutorialScreen
import moe.shizuku.manager.utils.CustomTabsHelper
import rikka.compatibility.DeviceCompatibility

class ShellTutorialActivity : MiuixActivity() {

    companion object {
        private const val SH_NAME = "rish"
        private const val DEX_NAME = "rish_shizuku.dex"
    }

    private val openDocumentsTree =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree: Uri? ->
            if (tree == null) return@registerForActivityResult

            val cr = contentResolver
            val doc = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            val child = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))

            cr.query(
                child,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use {
                while (it.moveToNext()) {
                    val id = it.getString(0)
                    val name = it.getString(1)
                    if (name == SH_NAME || name == DEX_NAME) {
                        DocumentsContract.deleteDocument(cr, DocumentsContract.buildDocumentUriUsingTree(tree, id))
                    }
                }
            }

            fun writeToDocument(name: String) {
                DocumentsContract.createDocument(contentResolver, doc, "application/octet-stream", name)?.runCatching {
                    cr.openOutputStream(this)?.let { output ->
                        assets.open(name).use { input ->
                            if (name == SH_NAME) {
                                input.bufferedReader().use {
                                    val text = it.readText()
                                        .replace("MANAGER_PKG", applicationContext.packageName)
                                    output.write(text.toByteArray())
                                }
                            } else {
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }

            writeToDocument(SH_NAME)
            writeToDocument(DEX_NAME)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = this

        setMiuixContent {
            ShellTutorialScreen(
                shName = SH_NAME,
                dexName = DEX_NAME,
                isMiui = DeviceCompatibility.isMiui(),
                onBack = { finish() },
                onExport = { openDocumentsTree.launch(null) },
                onOpenRishHelp = { CustomTabsHelper.launchUrlOrCopy(context, Helps.RISH.get()) },
            )
        }
    }
}
