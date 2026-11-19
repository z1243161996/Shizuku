# miuix migration guide (manager module)

The manager UI is being rebuilt on **miuix** (top.yukonga.miuix.kmp 0.9.5-beta01) instead of the old
Kyant backdrop glass. `ui/glass/**` and `ui/misc/StaticGlass.kt` are dead code and get deleted at the end;
never import them, never add features to them.

## Build / verify

```bash
cd /home/zhang/test/shizuku/repo
JAVA_HOME=/home/zhang/toolchains/jdk-21.0.13.1+2 ./gradlew :manager:compileDebugKotlin --console=plain 2>&1 | tail -20
```

A task is done only when that command ends with `BUILD SUCCESSFUL` and has no `e: ` error lines.
Do **not** run adb, emulator commands, or git. The Lead does visual verification on the emulator.

## Read these first (reference implementation, already migrated)

- `manager/src/main/java/moe/shizuku/manager/ui/miuix/MiuixActivity.kt` – Activity base class + theme + backdrop host
- `manager/src/main/java/moe/shizuku/manager/ui/miuix/MiuixGlass.kt` – `MiuixGlassCard`, `glassShape`, `isDarkColorScheme`
- `manager/src/main/java/moe/shizuku/manager/ui/miuix/MiuixSnackbar.kt` – `MiuixSnackbarMessage`, `MiuixSnackbarHost`
- `manager/src/main/java/moe/shizuku/manager/ui/miuix/MiuixText.kt` – `CharSequence.stripHtml()`
- `manager/src/main/java/moe/shizuku/manager/ui/home/HomeScreen.kt` – the screen pattern (Scaffold + cards + OverlayDialog)
- `manager/src/main/java/moe/shizuku/manager/home/HomeActivity.kt` – the Activity pattern

## Hosting a screen

```kotlin
class SomeActivity : MiuixActivity() {           // was AppBarActivity / GlassActivity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setMiuixContent { SomeScreen(...) }        // ComposeView inside the themed backdrop host
    }
}
```

The XML top bar of `AppBarActivity` is gone: every screen builds its own bar with
`Scaffold(topBar = { SmallTopAppBar(...) })`. Keep the menu actions by rendering `GlassIconButton`-style
`IconButton`s in `actions`, calling the same existing click handlers/menu ids.

## Component cheat sheet (exact signatures)

```kotlin
Scaffold(modifier, topBar, bottomBar, floatingActionButton, floatingActionButtonPosition,
         floatingToolbar, floatingToolbarPosition, snackbarHost, popupHost = { MiuixPopupHost() },
         containerColor = MiuixTheme.colorScheme.surface, contentWindowInsets,
         content: @Composable (PaddingValues) -> Unit)

SmallTopAppBar(title: String, modifier, color = MiuixTheme.colorScheme.surface, titleColor,
               subtitle: String = "", subtitleColor, navigationIcon: @Composable () -> Unit = {},
               actions: @Composable RowScope.() -> Unit = {}, scrollBehavior,
               defaultWindowInsetsPadding: Boolean = true, titlePadding, navigationIconPadding,
               actionIconPadding, bottomContent)

Card(modifier, cornerRadius, insideMargin: PaddingValues, colors, content: @Composable ColumnScope.() -> Unit)   // + onClick overload
Button(onClick, modifier, enabled, cornerRadius, minWidth, minHeight, colors = ButtonDefaults.buttonColors(), insideMargin, interactionSource, indication, content: @Composable RowScope.() -> Unit)
TextButton(text: String, onClick, modifier, enabled, cornerRadius, minWidth, minHeight, colors, insideMargin, textStyle, interactionSource, indication)
IconButton(onClick, modifier, enabled, holdDownState, backgroundColor, cornerRadius, minHeight, minWidth, content)
Switch(checked, onCheckedChange: ((Boolean) -> Unit)?, modifier, colors, enabled)
Text(text: String, modifier, color, autoSize, fontSize, fontStyle, fontWeight, fontFamily, letterSpacing, textDecoration, textAlign, lineHeight, overflow, softWrap, maxLines, minLines, onTextLayout, style = LocalTextStyles.current.main)
Icon(painter|imageVector|bitmap, contentDescription, modifier, tint = LocalContentColor.current)
Checkbox / RadioButton / Slider / TextField / HorizontalDivider / SmallTitle / Snackbar

OverlayDialog(show: Boolean, modifier, title: String? = null, titleColor, summary: String? = null, summaryColor,
              backgroundColor, enableWindowDim = true, onDismissRequest: (() -> Unit)? = null, onDismissFinished,
              outsideMargin: DpSize, insideMargin: DpSize, defaultWindowInsetsPadding = true,
              renderInRootScaffold = true, maxWidth, largeScreen, cornerRadius: Dp? = null, dimColor,
              surface: (@Composable DialogSurfaceScope.() -> Modifier)? = null, content: @Composable () -> Unit)
```

Theme access: `MiuixTheme.colorScheme` → `Colors` (`primary`, `onPrimary`, `primaryVariant`, `onSurface`,
`onSurfaceVariantSummary`, `onSurfaceVariantActions`, `surface`, `surfaceContainer`, `background`, `onBackground`,
`onBackgroundVariant`, `error`, `onError`, `outline`, `dividerLine`, `disabledOnSurface`, …; there is **no `isLight`**
and **no warning color**). Text: `MiuixTheme.textStyles.(main|paragraph|body1|body2|button|footnote1|footnote2|headline1|headline2|subtitle|title1..title4)`.

## Rules

1. **Dialogs**: no `AlertDialog`, no `DialogFragment`, no platform dialogs. Use `OverlayDialog(show = ..., title = ..., summary = ..., onDismissRequest = ...) { Row { TextButton(...); Button(...) } }`.
   For a Fragment that must stay a Fragment (hosted by the activity), keep the Fragment but render it as a Compose
   overlay helper instead — or convert the call site to a Compose state flag in the Activity, whichever needs less churn.
2. **Glass surfaces**: wrap content blocks in `MiuixGlassCard(modifier, cornerRadius = 24.dp, contentPadding = 18.dp, onClick = ...) { ... }`.
   List rows / dense items use a plain `Card` (opaque) so text stays readable.
3. **Primary actions** use `colors = ButtonDefaults.buttonColorsPrimary()`; secondary actions use `TextButton`;
   destructive confirmations may use `ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.error, contentColor = MiuixTheme.colorScheme.onError)`.
4. **Behaviour first**: every existing intent extra, preference key, permission request, thread/IO hop, toast/snackbar text,
   URL, notification and side effect must survive unchanged. Reuse the existing string resources; new strings go into
   `res/values/strings_glass_<group>.xml` (create if missing) — never into `res/values/strings.xml`.
5. **Forbidden**: `ui/glass/**`, `ui/misc/StaticGlass.kt`, `ui/home/**`, `AndroidManifest.xml`, `build.gradle*`,
   `res/xml/settings.xml`, `res/values/strings.xml`, adding dependencies, running adb/git.
6. Keep the existing package/class names and public entry points; the Activities must stay reachable by their current
   component names (the manifest is off-limits).
7. Layout rhythm: screen horizontal padding 16.dp, card gap 14.dp, card inner padding 18.dp, corner radius 24.dp.
8. Never copy code out of a tool echo/transcript into a file; always read the real file first.
