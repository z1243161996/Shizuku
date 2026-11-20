package moe.shizuku.manager.ui.miuix

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import moe.shizuku.manager.app.AppActivity
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Base activity for the miuix skin: one Compose tree wrapped in [MiuixTheme], edge to edge, with
 * the app background recorded as the backdrop that glass surfaces sample.
 */
abstract class MiuixActivity : AppActivity() {

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
                    MiuixTheme(controller = rememberShizukuThemeController()) {
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
