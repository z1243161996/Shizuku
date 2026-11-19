package moe.shizuku.manager.ui.management

import android.content.pm.PackageInfo
import android.os.Build
import android.text.style.URLSpan
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.shizuku.manager.R
import moe.shizuku.manager.authorization.AuthorizationManager
import moe.shizuku.manager.ui.glass.GlassButton
import moe.shizuku.manager.ui.glass.GlassCard
import moe.shizuku.manager.ui.glass.liquidGlass
import moe.shizuku.manager.ui.glass.GlassDialogOverlay
import moe.shizuku.manager.ui.glass.GlassDialogTitle
import moe.shizuku.manager.ui.glass.GlassDivider
import moe.shizuku.manager.ui.glass.GlassIcon
import moe.shizuku.manager.ui.glass.GlassIconButton
import moe.shizuku.manager.ui.glass.GlassScaffold
import moe.shizuku.manager.ui.glass.GlassType
import moe.shizuku.manager.ui.glass.LiquidSwitch
import moe.shizuku.manager.ui.glass.LocalGlassPalette
import moe.shizuku.manager.utils.AppIconCache
import moe.shizuku.manager.utils.CustomTabsHelper
import moe.shizuku.manager.utils.ShizukuStateMachine
import moe.shizuku.manager.utils.ShizukuSystemApis
import moe.shizuku.manager.utils.UserHandleCompat
import rikka.html.text.HtmlCompat

data class ManagementUiState(
    val apps: List<PackageInfo> = emptyList(),
    val grantedCount: Int = 0,
    val loaded: Boolean = false,
    val grantRevision: Int = 0,
)

data class ManagementLimitedDialog(
    val title: String,
    val message: String,
)

@Composable
fun ManagementScreen(
    backdrop: LayerBackdrop,
    state: State<ManagementUiState>,
    dialog: ManagementLimitedDialog?,
    onBack: () -> Unit,
    onHelp: () -> Unit,
    onToggleApp: (PackageInfo) -> Unit,
    onToggleAll: (Boolean) -> Unit,
    onDismissDialog: () -> Unit,
) {
    GlassScaffold(
        title = stringResource(R.string.home_app_management_title),
        backdrop = backdrop,
        onBack = onBack,
        scrollable = false,
        actions = {
            GlassIconButton(
                R.drawable.ic_help_outline_24dp,
                backdrop,
                onClick = onHelp,
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 4.dp),
        ) {
            if (state.value.loaded && state.value.apps.isEmpty()) {
                item(key = "empty") {
                    GlassCard(backdrop = backdrop, modifier = Modifier.fillParentMaxSize()) {
                        EmptyState(Modifier.weight(1f))
                    }
                }
            } else if (state.value.loaded) {
                item(key = "header") {
                    val s = state.value
                    val allGranted = s.apps.isNotEmpty() && s.apps.all { pi ->
                        runCatching { AuthorizationManager.granted(pi.packageName, pi.applicationInfo!!.uid) }.getOrDefault(false)
                    }
                    HeaderRow(
                        backdrop = backdrop,
                        grantedCount = s.grantedCount,
                        allGranted = allGranted,
                        onToggleAll = onToggleAll,
                    )
                    Spacer(Modifier.height(14.dp))
                }
                itemsIndexed(state.value.apps) { index, pi ->
                    val s = state.value
                    AppRow(
                        backdrop = backdrop,
                        pi = pi,
                        grantRevision = s.grantRevision,
                        onToggle = { onToggleApp(pi) },
                    )
                    if (index < s.apps.lastIndex) {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (dialog != null) {
        LimitedAdbDialog(backdrop, dialog, onDismissDialog)
    }
}

@Composable
private fun HeaderRow(
    backdrop: LayerBackdrop,
    grantedCount: Int,
    allGranted: Boolean,
    onToggleAll: (Boolean) -> Unit,
) {
    val palette = LocalGlassPalette.current
    GlassCard(
        backdrop = backdrop,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = 16.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = pluralStringResource(
                        R.plurals.home_app_management_authorized_apps_count,
                        grantedCount,
                        grantedCount,
                    ).uppercase(),
                    style = GlassType.label,
                    color = palette.textPrimary.copy(alpha = 0.72f),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.app_management_toggle_all),
                    style = GlassType.cardTitle,
                    color = palette.textPrimary,
                )
            }
            LiquidSwitch(
                checked = allGranted,
                onCheckedChange = onToggleAll,
                backdrop = backdrop,
            )
        }
    }
}

@Composable
private fun AppRow(
    backdrop: LayerBackdrop,
    pi: PackageInfo,
    grantRevision: Int,
    onToggle: () -> Unit,
) {
    val palette = LocalGlassPalette.current
    val context = LocalContext.current
    val ai = pi.applicationInfo ?: return
    val uid = ai.uid
    val userId = UserHandleCompat.getUserId(uid)
    val checked = remember(pi.packageName, uid, grantRevision) {
        runCatching { AuthorizationManager.granted(pi.packageName, uid) }.getOrDefault(false)
    }
    val title = remember(pi.packageName, userId) {
        val label = ai.loadLabel(context.packageManager).toString()
        if (userId != UserHandleCompat.myUserId()) {
            val userInfo = ShizukuSystemApis.getUserInfo(userId)
            label + " - " + userInfo.name + " (" + userId + ")"
        } else {
            label
        }
    }
    val requiresRoot = ai.metaData != null && ai.metaData.getBoolean("moe.shizuku.client.V3_REQUIRES_ROOT")
    val enabled = ShizukuStateMachine.isRunning()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(backdrop, RoundedCornerShape(20.dp), palette.cardTint)
            .clickable(enabled = enabled, onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(pi)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = GlassType.body,
                color = if (enabled) palette.textPrimary else palette.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = ai.packageName,
                style = GlassType.caption,
                color = palette.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (requiresRoot) {
                Text(
                    text = stringResource(R.string.app_management_item_summary_requires_root),
                    style = GlassType.caption,
                    color = palette.warning,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        LiquidSwitch(
            checked = checked,
            onCheckedChange = { onToggle() },
            backdrop = backdrop,
            enabled = enabled,
        )
    }
}

@Composable
private fun AppIcon(pi: PackageInfo) {
    val context = LocalContext.current
    val ai = pi.applicationInfo!!
    val userId = UserHandleCompat.getUserId(ai.uid)
    val size = context.resources.getDimensionPixelSize(R.dimen.default_app_icon_size)
    var icon by remember(pi.packageName, userId) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(pi.packageName, userId) {
        icon = withContext(Dispatchers.IO) {
            AppIconCache.getOrLoadBitmap(context, ai, userId, size)?.asImageBitmap()
        }
    }
    val shape = RoundedCornerShape(12.dp)
    val current = icon
    when {
        current != null -> Image(
            bitmap = current,
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(shape),
        )
        else -> Image(
            painter = painterResource(R.drawable.ic_default_app_icon_foreground),
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(shape),
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    val palette = LocalGlassPalette.current
    Column(
        modifier = modifier,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GlassIcon(
            R.drawable.ic_help_outline_24dp,
            modifier = Modifier.size(48.dp),
            size = 44,
            tint = palette.textSecondary,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.home_app_management_empty),
            style = GlassType.body,
            color = palette.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@Composable
private fun LimitedAdbDialog(
    backdrop: LayerBackdrop,
    dialog: ManagementLimitedDialog,
    onDismiss: () -> Unit,
) {
    val palette = LocalGlassPalette.current
    val context = LocalContext.current
    GlassDialogOverlay(backdrop, onDismiss = onDismiss) {
        GlassDialogTitle(dialog.title)
        Spacer(Modifier.height(10.dp))
        val annotated = remember(dialog.message) {
            val spanned = HtmlCompat.fromHtml(dialog.message, HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
            buildAnnotatedString {
                val text = spanned.toString()
                append(text)
                for (span in spanned.getSpans(0, spanned.length, URLSpan::class.java)) {
                    val start = spanned.getSpanStart(span)
                    val end = spanned.getSpanEnd(span)
                    if (start >= 0 && end <= text.length && start < end) {
                        addStringAnnotation("url", span.url, start, end)
                        addStyle(SpanStyle(color = palette.accent, textDecoration = TextDecoration.Underline), start, end)
                    }
                }
            }
        }
        ClickableText(
            text = annotated,
            style = GlassType.body.copy(color = palette.textSecondary, textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth(),
            onClick = { offset ->
                annotated.getStringAnnotations("url", offset, offset).firstOrNull()?.let {
                    CustomTabsHelper.launchUrlOrCopy(context, it.item)
                }
            },
        )
        Spacer(Modifier.height(14.dp))
        GlassButton(
            text = stringResource(android.R.string.ok),
            backdrop = backdrop,
            onClick = onDismiss,
            fillWidth = true,
        )
    }
}
