package com.flowly.net.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flowly.net.ui.components.FlowlySwitch
import com.flowly.net.ui.components.SegmentedControl
import com.flowly.net.ui.components.TopBar
import com.flowly.net.ui.icons.LucideIcons
import com.flowly.net.ui.state.UiState
import com.flowly.net.ui.theme.AppBarPadBottom
import com.flowly.net.ui.theme.LocalFlowlyExtra
import com.flowly.net.ui.theme.ScreenPadX
import com.flowly.net.ui.theme.Sp4
import com.flowly.net.ui.theme.Sp8
import com.flowly.net.ui.theme.Sp12
import com.flowly.net.ui.theme.Sp16
import com.flowly.net.ui.theme.Sp24
import com.flowly.net.ui.theme.Sp6

/**
 * Settings screen (SPEC §3.4): route mode + network + general.
 * Uses divider-line style (no card borders) per SPEC §2.10.
 */
@Composable
fun SettingsScreen(
    state: UiState,
    onRouteMode: (Int) -> Unit,
    onTun: (Boolean) -> Unit,
    onSystemProxy: (Boolean) -> Unit,
    onDns: (Boolean) -> Unit,
    onThemeCycle: () -> Unit,
    onThemeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = ScreenPadX),
    ) {
        TopBar(
            title = "设置",
            subtitle = "路由与网络",
            themeMode = state.themeMode,
            onThemeClick = onThemeClick,
            onDesignSystemClick = { /* TODO */ },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(Sp8))

            SectionHeader("路由模式")
            Spacer(Modifier.height(Sp8))
            SegmentedControl(
                options = listOf("全局", "规则", "直连"),
                selectedIndex = state.routeModeIndex,
                onSelected = onRouteMode,
            )
            Spacer(Modifier.height(Sp24))

            SectionHeader("网络")
            Spacer(Modifier.height(Sp8))
            SetRow(
                icon = LucideIcons.Network,
                title = "TUN 模式",
                sub = "接管系统全局流量",
                hasSwitch = true,
                checked = state.tunOn,
                onToggle = onTun,
                isLast = false,
            )
            SetRow(
                icon = LucideIcons.Server,
                title = "系统代理",
                sub = "为应用设置 HTTP 代理",
                hasSwitch = true,
                checked = state.systemProxyOn,
                onToggle = onSystemProxy,
                isLast = false,
            )
            SetRow(
                icon = LucideIcons.Globe,
                title = "DNS 劫持",
                sub = "增强域名解析",
                hasSwitch = true,
                checked = state.dnsHijackOn,
                onToggle = onDns,
                isLast = true,
            )
            Spacer(Modifier.height(Sp24))

            SectionHeader("通用")
            Spacer(Modifier.height(Sp8))
            ThemeRow(state.themeMode, onThemeCycle)
            SetRow(
                icon = LucideIcons.Type,
                title = "语言",
                sub = "简体中文",
                hasSwitch = false,
                checked = true,
                onToggle = {},
                isLast = false,
                showChevron = true,
            )
            SetRow(
                icon = LucideIcons.Info,
                title = "关于",
                sub = "Flowly v1.0.0",
                hasSwitch = false,
                checked = true,
                onToggle = {},
                isLast = true,
                showChevron = true,
            )
            Spacer(Modifier.height(Sp24))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun SetRow(
    icon: ImageVector,
    title: String,
    sub: String,
    hasSwitch: Boolean,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    isLast: Boolean,
    showChevron: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val extra = LocalFlowlyExtra.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Sp12, horizontal = Sp4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Icon box with accent-soft bg (SPEC §2.10)
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(extra.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = extra.accent,
                    modifier = Modifier.size(16.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    sub,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (hasSwitch) {
                FlowlySwitch(checked, onToggle)
            }
            if (showChevron) {
                Icon(
                    imageVector = LucideIcons.ChevronRight,
                    contentDescription = title,
                    tint = extra.ink3,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        // Bottom separator line (SPEC §2.10: line-soft, omitted for last row)
        if (!isLast) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 46.dp), // indent under icon
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

@Composable
private fun ThemeRow(themeMode: Int, onCycle: () -> Unit) {
    val extra = LocalFlowlyExtra.current
    val (icon, label) = when (themeMode) {
        0 -> LucideIcons.Sun to "浅色"
        1 -> LucideIcons.Moon to "深色"
        else -> LucideIcons.Monitor to "跟随系统"
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onCycle)
                .padding(vertical = Sp12, horizontal = Sp4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(extra.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "主题",
                    tint = extra.accent,
                    modifier = Modifier.size(16.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "主题",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Cycle button with arrow
            Icon(
                imageVector = LucideIcons.ChevronRight,
                contentDescription = "切换主题",
                tint = extra.accent,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
