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
    // Dialogs are platform AlertDialog windows: they cannot blur the wallpaper, so
    // they paint a near-opaque panel and lift the ink/label colours with it.
    return if (dark) StaticGlassPalette(
        dark = true,
        card = 0xF01A2030.toInt(),
        hairline = 0x33FFFFFF.toInt(),
        primary = 0xFFF7F9FC.toInt(),
        secondary = 0xFFE3EAF4.toInt(),
        tertiary = 0xFFB4C0D2.toInt(),
        accent = 0xFF30D158.toInt(),
        danger = 0xFFFF6961.toInt(),
    ) else StaticGlassPalette(
        dark = false,
        card = 0xF2FBFDFF.toInt(),
        hairline = 0x1F0B1B33.toInt(),
        primary = 0xFF0B1B2B.toInt(),
        secondary = 0xFF22344A.toInt(),
        tertiary = 0xFF3D5169.toInt(),
        accent = 0xFF0B7A3B.toInt(),
        danger = 0xFFB3261E.toInt(),
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
    val palette = context.staticGlassPalette()
    window?.setBackgroundDrawable(palette.dialogBackground(context))
    window?.setDimAmount(if (palette.dark) 0.6f else 0.42f)
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