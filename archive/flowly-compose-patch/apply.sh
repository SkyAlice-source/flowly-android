#!/bin/bash
# Flowly Design Polish — apply all patches
# Run from any directory
# Usage: bash /Users/wuhaotian/.reasonix/global-workspace/flowly-patch/apply.sh

PATCH_DIR="/Users/wuhaotian/.reasonix/global-workspace/flowly-patch"
FLOWLY_BASE="/Users/wuhaotian/WorkBuddy/2026-07-11-20-43-18/Flowly"

THEME_DIR="$FLOWLY_BASE/app/src/main/java/com/flowly/net/ui/theme"
COMP_DIR="$FLOWLY_BASE/app/src/main/java/com/flowly/net/ui/components"
SCREEN_DIR="$FLOWLY_BASE/app/src/main/java/com/flowly/net/ui/screens"
ICON_DIR="$FLOWLY_BASE/app/src/main/java/com/flowly/net/ui/icons"

echo "=== Flowly 设计稿精修补丁 ==="

cp "$PATCH_DIR/Color.kt" "$THEME_DIR/Color.kt" && echo "✅  1/11 Color.kt — token 精确值"
cp "$PATCH_DIR/Type.kt" "$THEME_DIR/Type.kt" && echo "✅  2/11 Type.kt — 字体说明"
cp "$PATCH_DIR/Dimens.kt" "$THEME_DIR/Dimens.kt" && echo "✅  3/11 Dimens.kt — AppBarPadBottom"
cp "$PATCH_DIR/LucideIcons.kt" "$ICON_DIR/LucideIcons.kt" && echo "✅  4/11 LucideIcons.kt — ChevronRight 等"
cp "$PATCH_DIR/SettingsScreen.kt" "$SCREEN_DIR/SettingsScreen.kt" && echo "✅  5/11 SettingsScreen — 分隔线风格"
cp "$PATCH_DIR/ProxiesScreen.kt" "$SCREEN_DIR/ProxiesScreen.kt" && echo "✅  6/11 ProxiesScreen — 自定义搜索栏"
cp "$PATCH_DIR/TopBar.kt" "$COMP_DIR/TopBar.kt" && echo "✅  7/11 TopBar — padding 12/2/14"
cp "$PATCH_DIR/DashboardScreen.kt" "$SCREEN_DIR/DashboardScreen.kt" && echo "✅  8/11 DashboardScreen — 零滚动"
cp "$PATCH_DIR/ConfirmBar.kt" "$COMP_DIR/ConfirmBar.kt" && echo "✅  9/11 ConfirmBar — 滑入动画"
cp "$PATCH_DIR/FlowlySwitch.kt" "$COMP_DIR/FlowlySwitch.kt" && echo "✅ 10/11 FlowlySwitch — 46×27"
cp "$PATCH_DIR/BottomNav.kt" "$COMP_DIR/BottomNav.kt" && echo "✅ 11/11 BottomNav — padding 8/10/0"

echo ""
echo "=== 全部补丁已应用 ==="
echo "构建: cd $FLOWLY_BASE && ./gradlew assembleDebug"
