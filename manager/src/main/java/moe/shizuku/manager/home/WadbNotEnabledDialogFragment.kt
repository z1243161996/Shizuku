package moe.shizuku.manager.home

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import moe.shizuku.manager.R
import moe.shizuku.manager.ui.misc.applyStaticGlassAlertStyle
import moe.shizuku.manager.ui.misc.applyStaticGlassWindow

class WadbNotEnabledDialogFragment :DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        return MaterialAlertDialogBuilder(context)
                .setMessage(R.string.dialog_wireless_adb_not_enabled)
                .setPositiveButton(android.R.string.ok, null)
                .create()
                .apply {
                    applyStaticGlassWindow()
                    applyStaticGlassAlertStyle()
                }
    }

    fun show(fragmentManager: FragmentManager) {
        if (fragmentManager.isStateSaved) return
        show(fragmentManager, javaClass.simpleName)
    }
}
