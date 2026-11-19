# Shizuku Liquid Glass · UI 审计（6.3 / 10）

**对象**：moe.shizuku.privileged.api v13.7.0-thedjchi（miuix 重写版）
**范围**：Home · Application management · Settings · 对话框/浮层 · 设计系统
**手段**：1080×2400 模拟器实拍（Android 15 / API 35 / density 420）+ ImageMagick 像素取证 + miuix 0.9.5-beta01 源码比对
**未覆盖**：深色主题（模拟器切换即崩溃）、教程/权限/遗留对话框页（迁移中）、RTL

**落地状态**：Quick Wins 已实施（commit 8cdb88e）—— Stop 改 error 红、次级动作降为灰色按钮、正文 14→16sp、间距对齐 8pt；本文截图摄于实施前。

完整版：[shizuku-ui-audit.html](./shizuku-ui-audit.html)（12 屏幻灯片，← → 翻页）

---

## 总分 6.3 / 10 · 良好（偏下）

| 维度 | 权重 | 得分 | 结论 |
|---|---|---|---|
| 0 概念 | 15% | 6.5 | 「色相派生壁纸 + 磨砂玻璃卡」成立，但属厂商风格移植 |
| 1 哲学一致性 | 10% | 6.5 | 语言整齐，但卡片玻璃 / 对话框实面两套材质并存 |
| 2 视觉层级 | 20% | 5.5 | 标题/正文 1.71×，同屏 3 个同权主色按钮 |
| 3 细节执行 | 25% | 6.0 | 对比度全过线，但间距非 8 倍数、字号偏小 |
| 4 功能性 | 20% | 7.5 | 修掉 2 个致命缺陷后主流程可用（修复前应记 3） |
| 5 创新性 | 10% | 5.5 | 稳、克制、无记忆点 |

---

## 审计中修复的两个致命缺陷

1. **顶栏 actions 丢失** —— miuix `TopAppBar.kt:1081` 的 `SmallTopAppBarLayout` 在 `1171` 测量 `actionIconsPlaceable`、`1227/1230` 只用于算宽度，**从未 place**（大号 TopAppBar 在 `1017` 是正常的）。结果「关于」「设置」两个入口完全不可达。处置：自写 `MiuixTopBar`，替换 3 文件 6 处。
2. **所有 OverlayDialog 不显示** —— `Scaffold.kt:104-115` 只在 content 子树内提供 `LocalDialogStates`/`LocalRootDialogStates`，而 `OverlayDialog` 读的就是它们（`MiuixPopupUtils.kt:184-188`），渲染方 `MiuixPopupHost` 在 Scaffold 内（`544`）。写在 Scaffold 外 = 落进默认空列表 = 永不渲染。处置：dialog 块移进 content lambda（Home 85 行 / Settings 61 行 / Management 3 行）。

---

## Keep

- 玻璃真的在采样：卡片内 (203,211,237) vs 顶部壁纸 (156,169,255)，下方卡片 (224,240,250) vs 壁纸 (203,239,255)。
- 壁纸从 primary 色相派生（HSV 三段渐变 + 三个径向辉光），跟随系统动态取色。
- 对比度纪律：6 个取样点 5.45–11.65，两个主标题达 AAA。
- 触控目标全部 ≥48dp（按钮 48dp、图标命中区 48dp、开关 56×48dp）。

## Fix

**⚠️ 致命（已修）**：见上。另需记入回归清单：任何新增 Screen 的 OverlayDialog 必须在 Scaffold content 内。

**⚡ 重要**
1. **主次**：一屏 3 个主色按钮 → 只留 Stop 为主色，Restart / View command 降为 TextButton；破坏性动作改 error 色（浅色 #B3261E / 深色 #FF6961）。
2. **尺度**：正文 14→16sp（body1），页脚 11→13sp（footnote1）；24/16=1.5× 仍不足，状态卡标题升 32sp（title1）达 2.0×。
3. **网格**：6→8、10→8、14→16、18→16/24、22→24，全部对齐 8pt。

**💡 优化**
- 分区标签「QUICK ACTIONS」「MORE」悬空压壁纸，改卡内标题或对齐卡片内边距（18dp）。
- 材质两套：卡片是采样玻璃、对话框是实心面板；建议对话框也走 glassPanel + fallback。

## Quick Wins

1. Stop 改 error 色 + 另两个降级 TextButton（一次改动解决 60% 层级问题）。
2. 正文 14→16sp、页脚 11→13sp。
3. 间距对齐 8pt。

---

## 关键数值

- 版式：页面左右 16dp、卡内 18dp、卡间距 14dp、卡圆角 24dp、按钮高 48dp（126px）、快捷瓦片 136dp（357px）。
- 字号（miuix TextStyles 实测顺序 17/17/16/14/17/13/11/17/16/14/32/24/20/18 sp）：卡片标题 24 / 描述 14 / 分组标签 13 / 页脚 11。
- 对比度：status_title 11.65 · status_sub 8.83 · pill 5.45 · body 9.36 · btn 7.55 · section 9.36。
- 证据截图：`artifacts/shots/miuix-home-top.png`、`miuix-home-bottom.png`、`miuix-management.png`、`miuix-settings-top.png`、`miuix-dialog-about.png`、`miuix-dialog-stop.png`。
