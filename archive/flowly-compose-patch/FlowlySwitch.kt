package com.flowly.net.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.flowly.net.ui.theme.LocalFlowlyExtra

/**
 * Custom switch (SPEC §2.10): 46×27dp, r999 pill.
 * ON → accent bg, OFF → line bg. Thumb is 21×21 white circle.
 * Material3 Switch is NOT used here because we need exact 46×27 specs.
 */
@Composable
fun FlowlySwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val extra = LocalFlowlyExtra.current
    val trackColor by animateColorAsState(
        targetValue = if (checked) extra.accent else MaterialTheme.colorScheme.outline,
        animationSpec = tween(250),
        label = "switch-track",
    )

    Box(
        modifier = modifier
            .size(width = 46.dp, height = 27.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(trackColor)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(21.dp)
                .padding(2.dp)
                .shadow(1.dp, CircleShape)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
        )
    }
}
