package com.multibankgroup.pricetracker.feature.shared_ui.components

import android.content.res.Configuration
import androidx.compose.animation.VectorConverter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.multibankgroup.pricetracker.R
import com.multibankgroup.pricetracker.domain.model.PriceDirection
import com.multibankgroup.pricetracker.feature.shared_ui.model.StockDisplayItem
import com.multibankgroup.pricetracker.feature.shared_ui.theme.LocalSpacing
import com.multibankgroup.pricetracker.feature.shared_ui.theme.LocalStockColors
import com.multibankgroup.pricetracker.feature.shared_ui.theme.PriceTrackerIcons
import com.multibankgroup.pricetracker.feature.shared_ui.theme.PriceTrackerTheme
import java.util.Locale

private const val FLASH_DURATION_MS = 800

/** Feed row with flash animation. Flash uses drawBehind (draw phase only, skips recomposition). */
@Composable
fun StockRow(
    item: StockDisplayItem,
    onClick: () -> Unit,
    onClickLabel: String? = null,
    modifier: Modifier = Modifier
) {
    val stockColors = LocalStockColors.current
    val spacing = LocalSpacing.current
    // rememberUpdatedState: theme change mid-animation gets latest colors without restarting effect
    val currentStockColors by rememberUpdatedState(stockColors)

    val flashColor = remember {
        Animatable(Color.Transparent, Color.VectorConverter(Color.Transparent.colorSpace))
    }

    LaunchedEffect(item.lastUpdatedTimestamp) {
        val targetColor = when (item.priceDirection) {
            PriceDirection.UP -> currentStockColors.upFlash
            PriceDirection.DOWN -> currentStockColors.downFlash
            PriceDirection.NONE -> return@LaunchedEffect
        }
        flashColor.snapTo(targetColor)
        flashColor.animateTo(
            targetValue = Color.Transparent,
            animationSpec = tween(durationMillis = FLASH_DURATION_MS)
        )
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onClick,
                    onClickLabel = onClickLabel
                )
                .drawBehind { drawRect(flashColor.value) }
                .padding(horizontal = spacing.large, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.extraSmall / 2)
            ) {
                Text(
                    text = item.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.companyName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(spacing.extraSmall / 2)
            ) {
                PriceText(price = item.currentPrice)
                PriceChangeText(
                    currentPrice = item.currentPrice,
                    previousPrice = item.previousPrice,
                    direction = item.priceDirection
                )
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 0.5.dp
        )
    }
}

/** Formatted price text with monospace tabular figures. */
@Composable
private fun PriceText(
    price: Double,
    modifier: Modifier = Modifier
) {
    val priceFormat = stringResource(R.string.price_format)
    val formatted = remember(price, priceFormat) {
        String.format(Locale.US, priceFormat, price)
    }
    Text(
        text = formatted,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}

/** Compact price change with Material Icon arrow. 12.dp icon for feed row density. */
@Composable
private fun PriceChangeText(
    currentPrice: Double,
    previousPrice: Double,
    direction: PriceDirection,
    modifier: Modifier = Modifier
) {
    val stockColors = LocalStockColors.current

    val (color, icon) = remember(direction, stockColors) {
        when (direction) {
            PriceDirection.UP -> stockColors.up to PriceTrackerIcons.ArrowUp
            PriceDirection.DOWN -> stockColors.down to PriceTrackerIcons.ArrowDown
            PriceDirection.NONE -> Color.Unspecified to PriceTrackerIcons.ArrowUp
        }
    }

    val text = remember(currentPrice, previousPrice) {
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
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (direction != PriceDirection.NONE) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = color
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (direction == PriceDirection.NONE) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                color
            }
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = false, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StockRowPreview() {
    PriceTrackerTheme {
        StockRow(
            item = StockDisplayItem(
                symbol = "AAPL",
                companyName = "Apple Inc.",
                currentPrice = 178.50,
                previousPrice = 175.00,
                priceDirection = PriceDirection.UP,
                lastUpdatedTimestamp = 1L
            ),
            onClick = {}
        )
    }
}