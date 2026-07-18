package com.flowly.net.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flowly.net.ui.icons.LucideIcons
import com.flowly.net.ui.theme.LocalFlowlyExtra
import com.flowly.net.ui.theme.LocalWindowScale
import com.flowly.net.ui.theme.Sp4
import com.flowly.net.ui.theme.Sp6

enum class Screen { Dashboard, Proxies, Profiles, Settings }

data class NavItem(val screen: Screen, val label: String, val icon: ImageVector)

/**
 * Bottom navigation (SPEC §2.6). 4 items, flex-equal, 22px icons.
 * Padding 8/10/0, selected item shows accent tint + 30×3px top indicator.
 * Frosted-glass effect approximated with 86% alpha surface.
 */
@Composable
fun BottomNav(
    current: Screen,
    onSelect: (Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    val extra = LocalFlowlyExtra.current
    val items = listOf(
        NavItem(Screen.Dashboard, "首页", LucideIcons.LayoutDashboard),
        NavItem(Screen.Proxies, "代理", LucideIcons.Globe),
        NavItem(Screen.Profiles, "配置", LucideIcons.FileText),
        NavItem(Screen.Settings, "设置", LucideIcons.Settings),
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(LocalWindowScale.current.bottomNavHeight)
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        // SPEC §2.6: padding 8/10/0
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp, start = 10.dp, end = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            items.forEach { item ->
                val sel = item.screen == current
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelect(item.screen) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (sel) {
                        Box(
                            modifier = Modifier
                                .height(3.dp)
                                .width(30.dp)
                                .background(extra.accent, RoundedCornerShape(3.dp)),
                        )
                    }
                    Spacer(Modifier.size(Sp6))
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (sel) extra.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.size(Sp4))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (sel) extra.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
