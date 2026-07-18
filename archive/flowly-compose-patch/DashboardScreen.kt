package com.github.kr328.clash

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.kr328.clash.core.bridge.TunnelState
import com.github.kr328.clash.design.store.UiStore
import com.github.kr328.clash.design.util.resolveThemedColor
import com.github.kr328.clash.util.withClash
import com.github.kr328.clash.util.withProfile
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select
import com.github.kr328.clash.common.util.ticker
import java.util.concurrent.TimeUnit

/**
 * Compose-based dashboard — "Instrument Panel" design.
 */
@Composable
fun DashboardScreen(
    connected: Boolean,
    mode: TunnelState.Mode?,
    profileName: String?,
    trafficForwarded: Long,
    nodeCount: Int,
    currentNode: String,
    currentLatency: Int,
    uptime: String,
    themeMode: Int,
    onToggle: () -> Unit,
    onGoProxies: () -> Unit,
    onThemeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val trafficText = if (trafficForwarded > 0) {
        "%.1f".format(trafficForwarded / 1_000_000_000.0)
    } else "0.0"

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        // Top bar
        TopBar(
            title = "Flowly",
            subtitle = when {
                connected -> "已连接 · ${mode?.name ?: "规则"}"
                else -> "未连接"
            },
            themeMode = themeMode,
            onThemeClick = onThemeClick,
        )

        Spacer(Modifier.height(12.dp))

        // Info stack
        InfoCard(label = "活动配置", value = profileName ?: "未选择")
        Spacer(Modifier.height(10.dp))
        InfoCard(label = "出口节点", value = "$currentNode · ${currentLatency}ms")

        // Hero dial
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            ConnectionDial(
                connected = connected,
                rateDown = trafficText,
                rateUp = "0.0",
            )
        }

        // 3×2 tiles
        TileGrid {
            StatTile(value = trafficText, label = "今日 GB", modifier = Modifier.weight(1f))
            StatTile(value = nodeCount.toString(), label = "节点数", modifier = Modifier.weight(1f))
            StatTile(value = uptime, label = "运行时长", modifier = Modifier.weight(1f))
            ActionTile(
                icon = Icons.Default.Zap,
                label = "测速全部",
                onClick = { /* TODO: health check */ },
                modifier = Modifier.weight(1f),
            )
            ActionTile(
                icon = Icons.Default.Shuffle,
                label = "切换节点",
                onClick = onGoProxies,
                modifier = Modifier.weight(1f),
            )
            ActionTile(
                icon = Icons.Default.Refresh,
                label = "刷新订阅",
                onClick = { /* TODO: refresh */ },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.weight(1f))

        // Connect button
        Button(
            onClick = onToggle,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (connected)
                    MaterialTheme.colorScheme.surface
                else
                    MaterialTheme.colorScheme.primary,
                contentColor = if (connected)
                    MaterialTheme.colorScheme.onSurface
                else
                    Color.White,
            ),
        ) {
            Text(
                text = if (connected) "断开连接" else "开启连接",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

// Simple implementations — will be replaced with the Flowly versions

@Composable
private fun TopBar(title: String, subtitle: String, themeMode: Int, onThemeClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.weight(1f))
        Text(
            subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onThemeClick) {
            Icon(Icons.Default.DarkMode, "切换主题")
        }
    }
}

@Composable
private fun InfoCard(label: String, value: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        }
    }
}

@Composable
private fun ConnectionDial(connected: Boolean, rateDown: String, rateUp: String) {
    Box(
        modifier = Modifier.size(176.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = androidx.compose.ui.geometry.Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f,
            )
            val rect = androidx.compose.ui.geometry.Rect(topLeft, androidx.compose.ui.geometry.Size(diameter, diameter))
            // Track
            drawArc(
                color = MaterialTheme.colorScheme.outlineVariant,
                startAngle = -90f, sweepAngle = 360f,
                useCenter = false, topLeft = topLeft,
                size = androidx.compose.ui.geometry.Size(diameter, diameter),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
            // Progress (full loop when connected)
            if (connected) {
                drawArc(
                    color = Color(0xFF3FC168),
                    startAngle = -90f, sweepAngle = 360f,
                    useCenter = false, topLeft = topLeft,
                    size = androidx.compose.ui.geometry.Size(diameter, diameter),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (connected) "● 已连接" else "未连接",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (connected) Color(0xFF3FC168) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            if (connected) {
                Text(
                    "↓ $rateDown  ↑ $rateUp MB/s",
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            } else {
                Text("轻触下方开启", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TileGrid(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier.fillMaxWidth(),
        maxItemsInEachRow = 3,
        maxLines = 2,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) { content() }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(84.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(value, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ActionTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .height(84.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        }
    }
}
