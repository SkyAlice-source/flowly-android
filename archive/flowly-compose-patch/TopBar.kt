package com.flowly.net.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.flowly.net.ui.icons.LucideIcons
import com.flowly.net.ui.theme.AppBarPadBottom
import com.flowly.net.ui.theme.AppBarPadY
import com.flowly.net.ui.theme.FlowlyDisplay
import com.flowly.net.ui.theme.LocalFlowlyExtra
import com.flowly.net.ui.theme.LogoSize
import com.flowly.net.ui.theme.Sp10
import com.flowly.net.ui.theme.Sp8

/**
 * Shared app bar (SPEC §2.2): gradient logo box + title/subtitle
 * + theme-toggle icon. Padding: 12dp top, 2dp horizontal, 14dp bottom.
 * `themeMode`: 0 = light, 1 = dark, 2 = system.
 */
@Composable
fun TopBar(
    title: String,
    subtitle: String,
    themeMode: Int,
    onThemeClick: () -> Unit,
    onDesignSystemClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extra = LocalFlowlyExtra.current
    val themeIcon = when (themeMode) {
        0 -> LucideIcons.Sun
        1 -> LucideIcons.Moon
        else -> LucideIcons.Monitor
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = AppBarPadY, bottom = AppBarPadBottom),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        // Brand logo (SPEC §2.2: 34x34, r10, gradient accent→accent-press)
        Box(
            modifier = Modifier
                .size(LogoSize)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.horizontalGradient(listOf(extra.accent, extra.accentPress))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = LucideIcons.WaveLogo,
                contentDescription = "logo",
                tint = Color.White,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.width(Sp10))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FlowlyDisplay),
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = extra.ink3,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Sp8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDesignSystemClick) {
                Icon(
                    imageVector = LucideIcons.DesignSystem,
                    contentDescription = "设计系统",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = onThemeClick) {
                Icon(
                    imageVector = themeIcon,
                    contentDescription = "切换主题",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
