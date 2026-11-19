package moe.shizuku.manager.ui.glass

import android.graphics.Color
import android.os.Bundle
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kyant.backdrop.backdrops.LayerBackdrop
import moe.shizuku.manager.app.AppActivity

/**
 * Base activity for the liquid glass skin. Hosts one Compose tree that owns the
 * wallpaper backdrop and the whole screen, edge to edge.
 */
abstract class GlassActivity : AppActivity() {

    var snackbar: GlassSnackbarMessage? by mutableStateOf(null)
        protected set

    protected var glassView: ComposeView? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }

    /** Replaces the content view with a full screen glass composition. */
    protected fun setGlassContent(content: @Composable BoxScope.(LayerBackdrop) -> Unit) {
        val view = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                GlassTheme {
                    LiquidBackdropHost { backdrop ->
                        content(backdrop)
                    }
                }
            }
        }
        glassView = view
        setContentView(view)
    }

    fun showSnackbar(message: GlassSnackbarMessage) {
        snackbar = message
    }

    fun dismissSnackbar() {
        snackbar = null
    }
}
