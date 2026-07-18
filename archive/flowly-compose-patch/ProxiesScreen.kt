package com.flowly.net.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowly.net.ui.components.ConfirmBar
import com.flowly.net.ui.components.NodeRow
import com.flowly.net.ui.components.TopBar
import com.flowly.net.ui.icons.LucideIcons
import com.flowly.net.ui.state.UiState
import com.flowly.net.ui.theme.LocalFlowlyExtra
import com.flowly.net.ui.theme.ScreenPadX
import com.flowly.net.ui.theme.Sp8
import com.flowly.net.ui.theme.Sp4
import com.flowly.net.ui.theme.Sp12
import com.flowly.net.ui.theme.Sp16

/**
 * Proxies screen (SPEC §3.2). Custom search bar + grouped node lists.
 * Picking a node raises the bottom ConfirmBar (pinned above the tab
 * bar via the screen's own Column); only "应用" writes it back.
 */
@Composable
fun ProxiesScreen(
    state: UiState,
    onApplyNode: (String) -> Unit,
    onTestAll: () -> Unit,
    onThemeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extra = LocalFlowlyExtra.current
    val allNodes = remember(state) { state.groups.flatMap { it.nodes } }
    val selected = remember { mutableStateOf<String?>(state.selectedNodeName) }
    val query = remember { mutableStateOf("") }

    val filtered = remember(query.value, state) {
        if (query.value.isBlank()) {
            state.groups
        } else {
            state.groups.map { g ->
                g.copy(nodes = g.nodes.filter {
                    it.name.contains(query.value, ignoreCase = true) ||
                        it.subName.contains(query.value, ignoreCase = true)
                })
            }.filter { it.nodes.isNotEmpty() }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = ScreenPadX),
    ) {
        TopBar(
            title = "代理",
            subtitle = "选择出口节点",
            themeMode = state.themeMode,
            onThemeClick = onThemeClick,
            onDesignSystemClick = { /* TODO */ },
        )

        // Custom search bar (SPEC §2.8 style: surface + 1px line + r13 + shadow-sm)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(13.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shadowElevation = 1.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Icon(
                    LucideIcons.Search,
                    "搜索",
                    tint = extra.ink3,
                    modifier = Modifier.size(16.dp),
                )
                BasicTextField(
                    value = query.value,
                    onValueChange = { query.value = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(extra.accent),
                    decorationBox = { innerTextField ->
                        Box {
                            if (query.value.isEmpty()) {
                                Text(
                                    "搜索节点",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = extra.ink3,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }
        }

        Spacer(Modifier.height(Sp12))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Sp8),
        ) {
            filtered.forEach { group ->
                // Group header with name + current node (SPEC §2.8)
                item(key = "h_${group.title}") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Sp8, horizontal = Sp4),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            group.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            group.nodes.firstOrNull()?.name ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = extra.ink3,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = Sp8),
                        )
                    }
                }
                items(group.nodes, key = { it.name }) { node ->
                    NodeRow(
                        name = node.name,
                        subName = node.subName,
                        flag = node.flag,
                        latencyMs = node.latencyMs,
                        level = node.level,
                        selected = selected.value == node.name,
                        onClick = { selected.value = node.name },
                    )
                }
            }
        }

        val picked = allNodes.find { it.name == selected.value }
        if (picked != null) {
            Spacer(Modifier.height(Sp12))
            ConfirmBar(
                nodeName = picked.name,
                latency = "${picked.latencyMs}ms",
                onApply = {
                    onApplyNode(picked.name)
                    selected.value = null
                },
                onCancel = { selected.value = null },
            )
        }
    }
}
