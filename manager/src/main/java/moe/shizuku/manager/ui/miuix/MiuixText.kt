package moe.shizuku.manager.ui.miuix

import android.text.Html

/** The upstream strings carry markup; Compose needs the rendered text. */
@Suppress("DEPRECATION")
fun CharSequence.stripHtml(): String =
    Html.fromHtml(this.toString(), Html.FROM_HTML_MODE_LEGACY).toString().trim()
