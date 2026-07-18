# Flowly Compose 设计补丁 — 归档说明

> **状态：已归档（不再作为 active 方案）**
> 归档日期：2026-07-14
> 归档决策：UI 路线选 **A — 留在 XML 原生路线**（用户确认）

---

## 这是什么

本目录是 flowly 项目的 **Compose 重设计精修稿**，由 reasonix 于 2026-07-12 生成，共 12 个文件：

| 文件 | 内容 |
|---|---|
| `Color.kt` | CMFA-SPEC token 精确值（accent / ok / warn / bad / 模式色 / 背景） |
| `Type.kt` | 字体规范（Space Grotesk / Manrope / Space Mono） |
| `Dimens.kt` | 间距 Sp2–Sp24、AppBarPadBottom |
| `FlowlySwitch.kt` | 自定义开关组件 46×27 |
| `BottomNav.kt` | 底部导航 padding 8/10/0 |
| `TopBar.kt` | 顶部栏 padding 12/2/14 |
| `ConfirmBar.kt` | 确认栏滑入动画 |
| `DashboardScreen.kt` | 仪表盘主页（零滚动） |
| `ProxiesScreen.kt` | 代理页自定义搜索栏 |
| `SettingsScreen.kt` | 设置页分隔线风格 |
| `LucideIcons.kt` | ChevronRight 等图标 |
| `apply.sh` | 批量应用脚本（见下方失效说明） |

## 为什么归档（从未接入的原因）

1. **目标目录已失效**：`apply.sh` 的 `FLOWLY_BASE` 指向
   `/Users/wuhaotian/WorkBuddy/2026-07-11-20-43-18/Flowly`，
   该目录**已被删除**，脚本无法运行。
2. **架构不兼容**：当前 flowly 工程是 CMFA（Clash Meta for Android）的
   **XML 原生二开**，design 模块包名为 `com.github.kr328.clash`；
   而本补丁是 **Compose 架构**（`com.flowly.net.ui.*`），二者技术栈不同，
   无法直接套用。
3. **路线决策已定**：2026-07-14 确认走 XML 路线，故 Compose 补丁归档留存、
   不再作为开发基线，避免两套 UI 各做一半累积设计债。

## 已沉淀进当前 XML 路线的资产（可复用价值）

虽然补丁未接入，但其设计决策已成为当前 XML 路线的权威基准：

- **色彩**：`Color.kt` 的 token 值（accent `#496CEF`、press `#3655CD`、
  ok `#3FC168`、warn `#EFA831`、bad `#EA3C3F`、模式色 直连 `#14B8A6` /
  全局 `#F59E0B` / 规则 `#496CEF`、背景 `#07080B` / `#F8F7F2`）已**收敛进**
  `design/src/main/res/values/colors.xml`。
- **字体**：`Type.kt` 的规范已采纳，Space Grotesk / Manrope / Space Mono
  三款 TTF 已打包嵌入 `design/src/main/res/font/`。
- **规格**：`Dimens.kt` 的 Sp2–Sp24 间距、`FlowlySwitch` 46×27、
  `BottomNav` padding 8/10/0、`DashboardScreen` 零滚动等思想已参考并入
  XML 设计（见 `flowly-design-tokens.html`）。

## 若未来要复用 Compose 方案

1. 在 reasonix / WorkBuddy 新建一个 **Compose 工程骨架** 作为新的 `FLOWLY_BASE`；
2. 修正 `apply.sh` 顶部的 `FLOWLY_BASE` 路径指向新工程；
3. 逐文件适配到新架构（包名、Activity 接入、导航宿主等）；
4. 本 archive 仅作**设计意图与数值参考**，不可直接覆盖新工程。

---

*源文件位于 `.reasonix/global-workspace/flowly-patch/`（保底保留，未删除）。*
