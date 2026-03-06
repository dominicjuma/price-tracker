package com.multibankgroup.pricetracker.feature.shared_ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.multibankgroup.pricetracker.domain.model.PriceDirection
import com.multibankgroup.pricetracker.feature.shared_ui.theme.LocalSpacing
import com.multibankgroup.pricetracker.feature.shared_ui.theme.LocalStockColors
import com.multibankgroup.pricetracker.feature.shared_ui.theme.PriceTrackerIcons
import com.multibankgroup.pricetracker.feature.shared_ui.theme.PriceTrackerTheme
import java.util.Locale

/** Direction chip with icon + change amount. Early returns for NONE before any layout work. */
@Composable
fun PriceChangeIndicator(
    currentPrice: Double,
    previousPrice: Double,
    direction: PriceDirection,
    modifier: Modifier = Modifier
) {
    if (direction == PriceDirection.NONE) return

    val stockColors = LocalStockColors.current
    val spacing = LocalSpacing.current

    val (textColor, bgColor, icon) = remember(direction, stockColors) {
        when (direction) {
            PriceDirection.UP -> Triple(
                stockColors.up,
                stockColors.upContainer,
                PriceTrackerIcons.ArrowUp
            )
            PriceDirection.DOWN -> Triple(
                stockColors.down,
                stockColors.downContainer,
                PriceTrackerIcons.ArrowDown
            )
            PriceDirection.NONE -> Triple(
                stockColors.up, stockColors.upContainer, PriceTrackerIcons.ArrowUp
            ) // unreachable
        }
    }

    val changeText = remember(currentPrice, previousPrice) {
        val change = currentPrice - previousPrice
        val pct = if (previousPrice != 0.0) {
            (change / previousPrice) * 100.0
        } else {
            0.0
        }
        val sign = if (change >= 0) "+" else ""
        String.format(Locale.US, "%s%.2f (%.2f%%)", sign, change, pct)
    }

    Row(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(spacing.small))
            .padding(horizontal = spacing.small, vertical = spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = textColor
        )
        Text(
            text = changeText,
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}


@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = false, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PriceChangeIndicatorUpPreview() {
    PriceTrackerTheme {
        PriceChangeIndicator(
            currentPrice = 178.50,
            previousPrice = 175.00,
            direction = PriceDirection.UP
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = false, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PriceChangeIndicatorDownPreview() {
    PriceTrackerTheme {
        PriceChangeIndicator(
            currentPrice = 170.00,
            previousPrice = 178.50,
            direction = PriceDirection.DOWN
        )
    }
}