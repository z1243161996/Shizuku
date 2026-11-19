package moe.shizuku.manager.ui.glass

import android.content.Context
import android.text.Html
import androidx.annotation.StringRes

/** The upstream strings carry markup; Compose needs the rendered text. */
@Suppress("DEPRECATION")
fun CharSequence.stripHtml(): String =
    Html.fromHtml(this.toString(), Html.FROM_HTML_MODE_LEGACY).toString().trim()

fun Context.glassString(@StringRes id: Int): String = getString(id).stripHtml()

fun Context.glassString(@StringRes id: Int, vararg args: Any): String = getString(id, *args).stripHtml()

fun Context.glassQuantity(@StringRes id: Int, quantity: Int): String =
    resources.getQuantityString(id, quantity, quantity).stripHtml()
