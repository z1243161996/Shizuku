package moe.shizuku.manager.ui.miuix

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import moe.shizuku.manager.app.AppActivity
import moe.shizuku.manager.app.ThemeHelper
import androidx.compose.ui.graphics.Color as ComposeColor
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * Base activity for the miuix skin: one Compose tree wrapped in [MiuixTheme], edge to edge, with
 * the app background recorded as the backdrop that glass surfaces sample.
 */
abstract class MiuixActivity : AppActivity() {

    private companion object {
        /** The upstream Shizuku accent, used when system color is off. */
        val BrandColor = ComposeColor(0xFF3F51B5)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
    }

    protected fun setMiuixContent(content: @Composable () -> Unit) {
        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    val controller = remember {
                        ThemeController(
                            colorSchemeMode = ColorSchemeMode.MonetSystem,
                            keyColor = if (ThemeHelper.isUsingSystemColor()) null else BrandColor,
                        )
                    }
                    MiuixTheme(controller = controller) {
                        val colors = MiuixTheme.colorScheme
                        SideEffect {
                            WindowCompat.getInsetsController(window, window.decorView).apply {
                                val lightBars = colors.onBackground.luminance() < 0.5f
                                isAppearanceLightStatusBars = lightBars
                                isAppearanceLightNavigationBars = lightBars
                            }
                        }
                        MiuixBackdropHost {
                            content()
                        }
                    }
                }
            }
        )
    }
}
