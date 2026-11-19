# 可读性与易用性优化（第二轮）

针对 liquid glass 版本在实际运行中的可读性问题做的表现层修复。**行为、权限、跳转、偏好键、网络请求全部不变**，只改颜色/字号/尺寸/触摸热区。

## 一、问题清单

| # | 问题 | 证据（改前） | 严重度 |
|---|---|---|---|
| 1 | 浅色玻璃上是近白色文字，正文在亮青壁纸上几乎不可见 | 管理页行标题、设置页行摘要压在白玻璃上，白墨与亮底亮度接近（估 ≈1.3:1，远低于 4.5:1） | 阻断 |
| 2 | 玻璃卡片太透（白 14%），壁纸细节穿过正文，字迹发糊 | 管理页/设置页卡片内文字与壁纸纹理互相干扰 | 高 |
| 3 | 昏暗文字层级乱：caption/summary 用最低一档 tertiary，14sp 太小 | 设置页 "Default (5555)"、管理页包名、自动化 sheet 字段标签读不清 | 中 |
| 4 | 玻璃按钮/图标按钮/开关触摸热区不足 48dp | 顶栏图标按钮 40dp 视觉即热区；开关热区 = 轨道 52x32dp | 中（可用性） |
| 5 | 强调按钮对比不足：暗色下白字压 #30D158 约 1.9:1；浅色 danger 填充带 0.82 alpha 偏灰 | Stop 对话框、确认类按钮 | 中 |
| 6 | 对话框面板与背景层次不够：光线散射到面板外，正文与背后内容混 | Stop/View command/bug report 对话框 | 中 |
| 7 | 顶栏副标题、section 小标题字号偏小（12–13sp），弱视用户吃力 | 首页 "QUICK ACTIONS"、设置页 "BEHAVIOR" | 低 |
| 8 | 暗色 caption 也不达标 | 暗色管理页 row_caption 实测 4.31:1（< 4.5） | 中 |

## 二、落地方案

**方向：浅色玻璃配深墨、暗色玻璃配浅墨；玻璃更「实」但仍然是玻璃。** 这是本轮所有改动的统一依据——原实现沿用了「玻璃上放白字」的移动端习惯，但壁纸是亮青色，白字必然失效。

1. **调色板分档（`ui/glass/GlassTokens.kt`）**
   - 浅色：textPrimary `#0B1B2B`、textSecondary `#22344A`、textTertiary `#3D5169`；cardTint 14%→**30%** 白、barTint 40%、controlTint 36%；accent `#0B7A3B`（深绿，白字压得住）、danger `#B3261E`。
   - 暗色：textPrimary `#F7F9FC`、textSecondary `#E3EAF4`、textTertiary `#C4CEDE`；accent `#30D158`、danger `#FF6961`。
   - 新增 `dialogTint`（近不透明面板：浅 `#F2FBFDFF`／暗 `#F01A2030`）与 `scrim`（对话框背景遮罩：浅 `#3D0B1B33`／暗 `#73000000`）。
2. **排版上调**：hero 24→30、title 20→21、cardTitle 16→18、body 14→16、caption 13→14（Medium）、label 12→13、新增 button 15 SemiBold、mono 12→13。行标题/摘要的默认色由 tertiary 提到 secondary。
3. **触摸热区**：GlassIconButton 48dp 盒子（内层保留原视觉尺寸）、LiquidSwitch 56x48dp 点击盒、GlassButton 高度 44→48dp、对话框按钮 padding 加大。
4. **按钮对比**：Prominent/Destructive 去掉 0.82 alpha；文字色改为 `Glass → textPrimary；暗色 → #10151C；浅色 → 白`，避免亮绿/亮红上放白字。
5. **对话框**：面板用 dialogTint + 34dp blur + 24dp 内边距，背景改用 palette scrim；平台 AlertDialog（StaticGlass）面板改为近不透明，dim 0.6/0.42。
6. **壁纸**：浅色下取消顶部压暗（改成深墨后不再需要），vignette 0.22→0.14，保留玻璃亮面质感。

## 三、实测对比度（WCAG，PIL 百分位取样）

| 界面 | 元素 | 比值 |
|---|---|---|
| 管理页（浅） | 页面标题 / 分区标签 / 行标题 / 行说明 | 14.47 / 6.55 / 12.63 / 5.74 |
| 管理页（暗） | 同上 | 8.13 / 4.92 / 7.22 / 4.99 |
| 设置页（浅） | 标题 / 分区标签 / 行标题 / 行摘要 / Restart required 徽标 | 14.66 / 6.61 / 13.77 / 9.75 / 11.93 |
| Stop 对话框（浅） | 正文 / 玻璃按钮标签 | 8.69 / 9.92 |

（对话框标题的读数受圆角描边干扰，ASCII 灰度复核显示为清晰深墨；暗色 caption 首次实测 4.31 后已提高到 4.99。）

## 四、回归验证

- `./gradlew :manager:assembleDebug` 通过，APK 安装到 API 35 模拟器。
- 管理页开关点按（48dp 热区）：`AUTHORIZED 1 APPLICATION` → `AUTHORIZED 2 APPLICATIONS`，授权链路未受影响。
- 入口链路：首页 → 设置、管理、Shell 教程、自动化 sheet、Stop 对话框、关于对话框均可正常打开/关闭。
- 截图见 `shots/v2-*.png`（浅色/暗色双套）。

## 五、未覆盖

- RequestPermissionActivity / ShellRequestHandlerActivity / LegacyIsNotSupportedActivity 需要真实调用方或特定系统版本才能触发，模拟器上无法截图，仅保证编译与调色板一致。
- 深色主题下部分第三方应用图标为浅色素材时观感仍依赖图标本身。
