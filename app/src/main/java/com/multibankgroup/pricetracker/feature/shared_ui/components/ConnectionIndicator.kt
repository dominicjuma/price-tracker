package com.multibankgroup.pricetracker.feature.shared_ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.multibankgroup.pricetracker.R
import com.multibankgroup.pricetracker.feature.shared_ui.theme.LocalStockColors

/**
 * Animated green/red dot with "Live"/"Offline" label.
 *
 * The glow effect uses [drawBehind] (lambda modifier) to avoid recomposing the entire composable
 * when only the draw phase needs to change.
 */
@Composable
fun ConnectionIndicator(
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val stockColors = LocalStockColors.current
    val dotColor by animateColorAsState(
        targetValue = if (isConnected) stockColors.up else stockColors.down,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "connection_dot_color"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(dotColor, CircleShape)
                .drawBehind {
                    // Glow effect — drawn in the draw phase only.
                    // When dotColor changes, only redraw happens, no recomposition.
                    drawCircle(
                        color = dotColor.copy(alpha = 0.4f),
                        radius = size.minDimension * 0.8f
                    )
                }
        )
        Text(
            text = stringResource(
                if (isConnected) R.string.connection_live
                else R.string.connection_offline
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}