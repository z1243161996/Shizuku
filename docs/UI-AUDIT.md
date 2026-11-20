# Shizuku Manager · miuix 版 UI 审计 v2

**对象**：moe.shizuku.privileged.api 13.7.0-thedjchi（miuix 重写版）
**设备**：Xiaomi M261DB · Android 17 (SDK 37) · 1120×2436 @480dpi（3.0 px/dp）
**范围**：Home / Settings / Application management / 关于·停止·ADB 指令·自动化对话框 / 配对教程；浅色 + 深色（应用内主题切换）
**手段**：真机抓屏 + ImageMagick WCAG 对比度（亮度 2/50/98 分位）+ uiautomator bounds + miuix 0.9.5-beta01 源码核对

上一版（6.3/10）基于模拟器浅色截图、摄于 Quick Wins 之前，仅作趋势参考。

## 总分 6.8 / 10 · 良好

| 维度 | 权重 | 得分 | 结论 |
|---|---|---|---|
| 概念 | 15% | 7.0 | miuix 玻璃语言统一、信息架构沿用上游；但玻璃折射的是合成壁纸 |
| 哲学一致性 | 10% | 7.0 | 单一强调色 + error 红；深浅共用一棵组件树 |
| 视觉层级 | 20% | 6.5 | 一屏一个主按钮；分区标签对比度已修；状态 pill 与卡片层级仍平 |
| 细节执行 | 25% | 6.8 | 8pt 栅格、48dp 命中盒、对比度达标；中英混排与行高不均 |
| 功能性 | 20% | 8.0 | 状态/停止/根/无线/ADB/自动化/管理/设置/教程全链路真机点通 |
| 创新性 | 10% | 5.0 | MIUI 既有语言的衍生 |

## Keep
- 玻璃在真机 GPU 生效（runtime shader）：卡内像素 (203,211,237) vs 壁纸 (156,169,255)，有真实模糊/折射。
- 一屏一个主色填充按钮；Stop 用 error 红，其余降为灰色次级。
- 触控：按钮 48dp、IconButton 48dp 命中盒、Switch 56×48dp。
- 栅格：页面 16dp / 卡内 18dp / 卡间 16dp / 圆角 24dp。
- 功能零回退：停止服务后状态翻转；管理页授权 3 → 2；对话框全部可开合。

## 本轮修复
分区标题「行为」在彩色壁纸上仅 **2.82:1**（背景 #909ae5）。根因：标题用 `onBackgroundVariant`，真机 Monet 主色把壁纸推向高饱和。

- `ui/settings/SettingsScreen.kt` 与 `ui/home/HomeScreen.kt` 的 SectionLabel 改用 `onBackground`；页脚用 `onBackground.copy(alpha = 0.8f)`。
- `ui/miuix/MiuixBackdrop.kt` 亮色 wash 饱和度 0.38/0.12/0.30 → 0.24/0.09/0.20，blob 饱和度与 alpha（0.75/0.55/0.45 → 0.55/0.40/0.34）同步下调。
- 复验：「行为」6.98:1、「界面外观」11.31:1（原 5.36）、行标题 10.45:1、深色分区 12.76:1、Home 分区与磁贴 11.3/11.6:1。

## 待办
1. ⚡ 自绘 `MiuixTopBar` 是技术债：miuix 0.9.5-beta01 的 `SmallTopAppBar` 不 place actions（`TopAppBar.kt` 1171 行测量、1227/1230 只用于算宽度；大号 1017 行正常），升级后需回归并断言 actions 有非零 bounds。
2. ⚡ 玻璃折射的是主题派生壁纸，不是系统壁纸；深色下背景亮度 15%→27%，玻璃近似深灰半透明。若追求 iOS 级效果需 WallpaperManager + 预模糊底层。
3. ⚡ 中英混排 6 条：Auto-disable USB debugging / Disable USB debugging when stopping Shizuku / Check for updates / Buy me a coffee / Close。
4. ⚡ 状态 pill 五个同级标签过载，建议折叠 SELinux context。
5. ⚡ 管理页空态缺「如何授权」引导。
6. 💡 设置页 TCP 模式说明两行导致行高不均（maxLines=2 + 行最小高 72dp）。
7. 💡 QuickTile 136dp 只有图标 + 标题，可补 13sp 副标题。
8. 💡 深色主按钮 / error 按钮对比度未测。

## Quick Wins
1. 补 6 条中文翻译。
2. 管理页空态加「查看如何授权」TextButton。
3. SELinux context 移入「详情」，状态行只留 ADB / UID / API / patch。

## 未验证
RequestPermissionActivity、LegacyIsNotSupportedActivity（需第三方 Shizuku 客户端）；Starter 控制台（需 root 或完成无线配对）；平板/电视布局；RTL 与长语言；深色主按钮对比度。

## v3 · 配色重做 + 审计建议落地（真机 789d8e52 复验）

用户反馈「配色不顺眼」。根因：`ThemeHelper.isUsingSystemColor()` 在 SDK ≥ 31 默认 true，设备没有写入过偏好，于是整套 UI 走 Monet 动态取色，颜色完全由系统壁纸决定（实测被推成饱和紫 #909ae5）。

- 新增 `ui/miuix/MiuixTheme.kt`：`ShizukuSeed = Color(0xFF2E6E8E)`（低饱和钢青），`rememberShizukuThemeController()` 用 `ThemeController(ColorSchemeMode.MonetSystem, keyColor = if (ThemeHelper.isUsingSystemColor()) null else ShizukuSeed, colorSpec = ThemeColorSpec.Spec2025, paletteStyle = ThemePaletteStyle.TonalSpot)`。关闭「使用系统颜色」时得到设计配色，打开时仍跟随系统（功能保留）。
- `app/ThemeHelper.java`：`KEY_USE_SYSTEM_COLOR` 默认值 true → false，设计配色成为默认外观。
- `ui/miuix/MiuixActivity.kt` 与 6 个对话框宿主（AdbDialogFragment / WadbNotEnabledDialogFragment / AdbPairDialogFragment / WadbEnableUsbDebuggingDialogFragment / AccessibilityDialogHelper / BugReportDialog）统一用同一个 controller，消除三处硬编码的 #3F51B5。
- `ui/miuix/MiuixBackdrop.kt` 改为调色板内插值（不再用 HSV 色相派生）：top = lerp(background, primary, 0.20/0.30)、bottom = lerp(background, secondary, 0.16/0.25)、左上/右下两个径向 blob 用 primary/secondary（alpha 亮 0.55/0.40、暗 0.45/0.35）。壁纸颜色随主题走，不再出现与调色板打架的撞色。
- Stop 按钮从 error 实心（vision 判「砖红突兀」）改为 `buttonColors(color = surfaceVariant, contentColor = error)` 灰底红字，整屏只保留一个主色填充按钮（启动）。
- 中文化：`values-zh-rCN`/`values-zh-rTW` 的 `strings.xml` 补齐 64/65 条，新增 `strings_glass.xml` 各 21 条；`comm` 校验只剩 `app_name`（base 中 translatable=false）。

审计建议落地：

| 建议 | 处理 |
| --- | --- |
| 3. 中英混排 | 已补 zh-rCN / zh-rTW 翻译（设置页、关于对话框、自动化面板全部中文） |
| 4. 状态 pill 过载 | SELinux context 移出 pill 行，改为下方 footnote2 单行（超长省略） |
| 5. 空态缺引导 | 管理页空态加「阅读帮助」TextButton → 打开 ADB 权限帮助 |
| 6. TCP 说明行高不均 | 设置页 Toggle/Value 行 `heightIn(min = 72.dp)` |
| 7. QuickTile 太单薄 | 已核对：磁贴本身带 caption 副标题，无需改动 |
| 8. 深色按钮对比度 | 已实测（见下），全部 ≥ 4.5:1 |

对比度复测（真机，WCAG，`/tmp/contrast.sh IMAGE X Y W H LABEL`）：

- 浅色：Stop 标签 5.07、分区标题「行为」8.95、行标题「自启动」10.78、正文 5.50、主按钮 5.94、状态标题 10.88。
- 深色：Stop 5.45、状态标题 11.99、主按钮 6.17、分区标题 8.26、行标题 11.99、正文 6.97。
- 说明：v2 中的对比度数值是用错误的坐标参数顺序测的，已作废，以本节为准。

顺带修掉一个上游服务端缺陷（真机暴露）：管理页授权一个应用后，该应用从列表消失、计数归零。

- 根因：`server/.../ShizukuService.java:471` 的过滤 `entry.packages != null && !entry.packages.contains(pkg)`。`updateFlagsForUid` 通过 `configManager.update(uid, null, …)` 建立的条目 `packages` 是空列表而非 null，于是空列表把该 uid 的所有包都过滤掉了。
- 修复：过滤条件补 `!entry.packages.isEmpty()`。修复后授权计数 1 → 0 → 1 往返正常，行不再消失（截图 fin-mgmt-light / fin-mgmt-dark）。

截图（真机，/home/zhang/test/shizuku/artifacts/shots/）：fin-home-light、fin-home-light-lower、fin-set-light、fin-mgmt-light、fin-dialog-about-light、fin-home-dark、fin-set-dark、fin-mgmt-dark、fin-dialog-about-dark。
