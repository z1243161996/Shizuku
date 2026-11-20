package moe.shizuku.manager.ui.miuix

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import moe.shizuku.manager.app.ThemeHelper
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

/**
 * Seed of the app palette: a muted steel teal. TonalSpot keeps the chroma low, so surfaces stay
 * close to neutral paper and the accent is the only saturated element on screen.
 */
private val ShizukuSeed = Color(0xFF2E6E8E)

/**
 * One palette for every miuix surface, activities and dialogs alike. Dynamic color stays opt-in:
 * when it is enabled the palette follows the wallpaper, but it is still generated with the same
 * tonal style so it cannot drift into the high-chroma hues that made the glass unreadable.
 */
@Composable
fun rememberShizukuThemeController(): ThemeController = remember {
    ThemeController(
        colorSchemeMode = ColorSchemeMode.MonetSystem,
        keyColor = if (ThemeHelper.isUsingSystemColor()) null else ShizukuSeed,
        colorSpec = ThemeColorSpec.Spec2025,
        paletteStyle = ThemePaletteStyle.TonalSpot,
    )
}
