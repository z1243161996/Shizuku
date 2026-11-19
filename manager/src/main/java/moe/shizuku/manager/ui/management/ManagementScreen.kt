package moe.shizuku.manager.ui.management

import android.content.pm.PackageInfo
import android.text.style.URLSpan
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.shizuku.manager.R
import moe.shizuku.manager.authorization.AuthorizationManager
import moe.shizuku.manager.ui.miuix.MiuixGlassCard
import moe.shizuku.manager.utils.AppIconCache
import moe.shizuku.manager.utils.CustomTabsHelper
import moe.shizuku.manager.utils.ShizukuStateMachine
import moe.shizuku.manager.utils.ShizukuSystemApis
import moe.shizuku.manager.utils.UserHandleCompat
import rikka.html.text.HtmlCompat
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import moe.shizuku.manager.ui.miuix.MiuixTopBar
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

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

private val BackIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Back",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(20f, 11f)
            horizontalLineTo(7.83f)
            lineTo(13.42f, 5.41f)
            lineTo(12f, 4f)
            lineTo(4f, 12f)
            lineTo(12f, 20f)
            lineTo(13.41f, 18.59f)
            lineTo(7.83f, 13f)
            horizontalLineTo(20f)
            verticalLineTo(11f)
            close()
        }
    }.build()
}

@Composable
fun ManagementScreen(
    state: State<ManagementUiState>,
    dialog: ManagementLimitedDialog?,
    onBack: () -> Unit,
    onHelp: () -> Unit,
    onToggleApp: (PackageInfo) -> Unit,
    onToggleAll: (Boolean) -> Unit,
    onDismissDialog: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            MiuixTopBar(
                title = stringResource(R.string.home_app_management_title),
                color = MiuixTheme.colorScheme.surface.copy(alpha = 0.55f),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = BackIcon,
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onHelp) {
                        Icon(
                            painter = painterResource(R.drawable.ic_help_outline_24dp),
                            contentDescription = null,
                        )
                    }
                },
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.value.loaded && state.value.apps.isEmpty()) {
                    item(key = "empty") {
                        MiuixGlassCard(modifier = Modifier.fillParentMaxSize()) {
                            EmptyState(Modifier.fillMaxSize())
                        }
                    }
                } else if (state.value.loaded) {
                    item(key = "header") {
                        val s = state.value
                        val allGranted = s.apps.size > 1 && s.apps.all { pi ->
                            runCatching {
                                AuthorizationManager.granted(pi.packageName, pi.applicationInfo!!.uid)
                            }.getOrDefault(false)
                        }
                        HeaderRow(
                            grantedCount = s.grantedCount,
                            allGranted = allGranted,
                            onToggleAll = onToggleAll,
                        )
                    }
                    itemsIndexed(state.value.apps) { _, pi ->
                        AppRow(
                            pi = pi,
                            grantRevision = state.value.grantRevision,
                            onToggle = { onToggleApp(pi) },
                        )
                    }
                }
            }
        }

        if (dialog != null) {
            LimitedAdbDialog(dialog, onDismissDialog)
        }
    }
}

@Composable
private fun HeaderRow(
    grantedCount: Int,
    allGranted: Boolean,
    onToggleAll: (Boolean) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val text = MiuixTheme.textStyles
    MiuixGlassCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = pluralStringResource(
                        R.plurals.home_app_management_authorized_apps_count,
                        grantedCount,
                        grantedCount,
                    ).uppercase(),
                    style = text.footnote2,
                    color = colors.onSurfaceVariantSummary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.app_management_toggle_all),
                    style = text.body1,
                    color = colors.onSurface,
                )
            }
            Switch(
                checked = allGranted,
                onCheckedChange = onToggleAll,
            )
        }
    }
}

@Composable
private fun AppRow(
    pi: PackageInfo,
    grantRevision: Int,
    onToggle: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val text = MiuixTheme.textStyles
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        insideMargin = PaddingValues(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onToggle)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(pi)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = text.body1,
                    color = if (enabled) colors.onSurface else colors.disabledOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = ai.packageName,
                    style = text.footnote1,
                    color = if (enabled) colors.onSurfaceVariantSummary else colors.disabledOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (requiresRoot) {
                    Text(
                        text = stringResource(R.string.app_management_item_summary_requires_root),
                        style = text.footnote2,
                        color = if (enabled) colors.error else colors.disabledOnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Switch(
                checked = checked,
                onCheckedChange = { onToggle() },
                enabled = enabled,
            )
        }
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
    val colors = MiuixTheme.colorScheme
    val text = MiuixTheme.textStyles
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_help_outline_24dp),
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            tint = colors.onSurfaceVariantSummary,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.home_app_management_empty),
            style = text.body2,
            color = colors.onSurfaceVariantSummary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@Composable
private fun LimitedAdbDialog(
    dialog: ManagementLimitedDialog,
    onDismiss: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val text = MiuixTheme.textStyles
    val context = LocalContext.current
    OverlayDialog(
        show = true,
        title = dialog.title,
        onDismissRequest = onDismiss,
    ) {
        val annotated = remember(dialog.message) {
            val spanned = HtmlCompat.fromHtml(dialog.message, HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
            buildAnnotatedString {
                val plain = spanned.toString()
                append(plain)
                for (span in spanned.getSpans(0, spanned.length, URLSpan::class.java)) {
                    val start = spanned.getSpanStart(span)
                    val end = spanned.getSpanEnd(span)
                    if (start >= 0 && end <= plain.length && start < end) {
                        addStringAnnotation("url", span.url, start, end)
                        addStyle(
                            SpanStyle(color = colors.primary, textDecoration = TextDecoration.Underline),
                            start,
                            end,
                        )
                    }
                }
            }
        }
        ClickableText(
            text = annotated,
            style = text.body2.copy(color = colors.onSurfaceVariantSummary, textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth(),
            onClick = { offset ->
                annotated.getStringAnnotations("url", offset, offset).firstOrNull()?.let {
                    CustomTabsHelper.launchUrlOrCopy(context, it.item)
                }
            },
        )
        Spacer(Modifier.height(14.dp))
        Button(
            colors = ButtonDefaults.buttonColorsPrimary(),
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(android.R.string.ok), style = text.button)
        }
    }
}
