package moe.shizuku.manager.ui.misc

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputLayout

data class StaticGlassPalette(
    val dark: Boolean,
    val card: Int,
    val hairline: Int,
    val primary: Int,
    val secondary: Int,
    val tertiary: Int,
    val accent: Int,
    val danger: Int,
)

fun Context.staticGlassPalette(): StaticGlassPalette {
    val dark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    return if (dark) StaticGlassPalette(
        dark = true,
        card = 0x33FFFFFF.toInt(),
        hairline = 0x40FFFFFF.toInt(),
        primary = 0xFFF5F6FA.toInt(),
        secondary = 0xC7E8EDF7.toInt(),
        tertiary = 0x80E8EDF7.toInt(),
        accent = 0xFF30D158.toInt(),
        danger = 0xFFFF6961.toInt(),
    ) else StaticGlassPalette(
        dark = false,
        card = 0x38FFFFFF.toInt(),
        hairline = 0x66FFFFFF.toInt(),
        primary = 0xFFFAFAFC.toInt(),
        secondary = 0xCCFFFFFF.toInt(),
        tertiary = 0x8AFFFFFF.toInt(),
        accent = 0xFF34C759.toInt(),
        danger = 0xFFFF453A.toInt(),
    )
}

fun StaticGlassPalette.dialogBackground(context: Context): GradientDrawable {
    val density = context.resources.displayMetrics.density
    return GradientDrawable().apply {
        cornerRadius = 28f * density
        setColor(card)
        setStroke((1f * density).toInt(), hairline)
    }
}

fun Dialog.applyStaticGlassWindow() {
    window?.setBackgroundDrawable(context.staticGlassPalette().dialogBackground(context))
    window?.setDimAmount(0.55f)
}

fun Dialog.applyStaticGlassAlertStyle() {
    setOnShowListener { (it as? Dialog)?.styleStaticGlassAlert() }
}

fun Dialog.styleStaticGlassAlert() {
    val dialog = this as? AlertDialog ?: return
    val palette = dialog.context.staticGlassPalette()

    val title = dialog.findViewById<TextView>(com.google.android.material.R.id.alertTitle)
        ?: dialog.findViewById<TextView>(androidx.appcompat.R.id.alertTitle)
    title?.setTextColor(palette.primary)

    dialog.findViewById<TextView>(android.R.id.message)?.setTextColor(palette.secondary)
    dialog.findViewById<ImageView>(android.R.id.icon)?.setColorFilter(palette.primary)

    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(palette.accent)
    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(palette.secondary)
    dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setTextColor(palette.secondary)
}

fun ViewGroup.tintGlassTexts(palette: StaticGlassPalette) {
    for (i in 0 until childCount) {
        when (val child = getChildAt(i)) {
            is TextView -> {
                child.setTextColor(palette.secondary)
                child.setLinkTextColor(palette.accent)
            }
            is ViewGroup -> child.tintGlassTexts(palette)
        }
    }
}

fun TextInputLayout.applyStaticGlass(palette: StaticGlassPalette) {
    hintTextColor = ColorStateList.valueOf(palette.tertiary)
    setBoxStrokeColor(palette.hairline)
    editText?.setTextColor(palette.primary)
    editText?.setHintTextColor(palette.tertiary)
}
