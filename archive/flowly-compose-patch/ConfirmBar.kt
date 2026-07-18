package com.flowly.net.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flowly.net.ui.theme.ConnBarRadius
import com.flowly.net.ui.theme.FlowlyDisplay
import com.flowly.net.ui.theme.LocalFlowlyExtra
import com.flowly.net.ui.theme.Sp12

/**
 * Bottom confirm bar (SPEC §3.2) — same thumb-zone style as ConnBar.
 * Pinned above the tab bar; slides in with a translateY animation
 * after a node is picked.
 */
@Composable
fun ConfirmBar(
    nodeName: String,
    latency: String,
    onApply: () -> Unit,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val extra = LocalFlowlyExtra.current
    // Slide-up entrance animation
    val slideAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(320),
        label = "confirm-bar",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .alpha(slideAlpha),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 8.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .padding(Sp12)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "已选节点",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    nodeName,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    latency,
                    style = MaterialTheme.typography.labelSmall,
                    color = extra.ok,
                )
            }
            if (onCancel != null) {
                TextButton(onClick = onCancel) {
                    Text("取消")
                }
            }
            Button(
                onClick = onApply,
                shape = RoundedCornerShape(ConnBarRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = extra.accent,
                    contentColor = Color.White,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                Text(
                    "应用",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FlowlyDisplay),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
