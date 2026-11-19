package moe.shizuku.manager.ui.tutorials

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import moe.shizuku.manager.R
import moe.shizuku.manager.ui.glass.GlassScaffold
import moe.shizuku.manager.ui.glass.GlassType
import moe.shizuku.manager.ui.glass.LocalGlassPalette
import moe.shizuku.manager.ui.glass.stripHtml

@Composable
fun ShellTutorialScreen(
    backdrop: LayerBackdrop,
    shName: String,
    dexName: String,
    isMiui: Boolean,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onOpenRishHelp: () -> Unit,
) {
    GlassScaffold(
        title = stringResource(R.string.home_terminal_title),
        backdrop = backdrop,
        onBack = onBack,
    ) {
        Spacer(Modifier.height(6.dp))
        GlassInfoCard(
            backdrop = backdrop,
            iconRes = R.drawable.ic_help_outline_24dp,
            body = monoAnnotated(stringResource(R.string.rish_description, shName).stripHtml(), shName),
            onClick = onOpenRishHelp,
        )
        if (isMiui) {
            Spacer(Modifier.height(14.dp))
            GlassInfoCard(
                backdrop = backdrop,
                iconRes = R.drawable.ic_warning_24,
                body = AnnotatedString(stringResource(R.string.terminal_tutorial_miui).stripHtml()),
            ) {
                val palette = LocalGlassPalette.current
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.terminal_tutorial_miui_2).stripHtml(),
                    style = GlassType.body,
                    color = palette.textSecondary,
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        TutorialStep(
            iconRes = R.drawable.ic_numeric_1_circle_outline_24,
            title = stringResource(R.string.terminal_tutorial_1),
            backdrop = backdrop,
            description = stringResource(R.string.terminal_tutorial_1_description, shName, dexName).stripHtml(),
            descriptionMono = arrayOf(shName, dexName),
            buttonText = stringResource(R.string.terminal_export_files),
            onButtonClick = onExport,
        )
        Spacer(Modifier.height(22.dp))
        TutorialStep(
            iconRes = R.drawable.ic_numeric_2_circle_outline_24,
            title = stringResource(R.string.terminal_tutorial_2, shName).stripHtml(),
            titleMono = arrayOf(shName),
            backdrop = backdrop,
            command = "cp /sdcard/chosen-folder/* /data/data/terminal.package.name/files",
            description = stringResource(R.string.terminal_tutorial_2_description, shName, shName, ".bashrc").stripHtml(),
            descriptionMono = arrayOf(shName, ".bashrc"),
        )
        Spacer(Modifier.height(22.dp))
        TutorialStep(
            iconRes = R.drawable.ic_numeric_3_circle_outline_24,
            title = stringResource(R.string.terminal_tutorial_3),
            backdrop = backdrop,
            command = "sh /path/to/$shName",
        )
    }
}
