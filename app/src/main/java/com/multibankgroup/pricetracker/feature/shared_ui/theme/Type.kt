package com.multibankgroup.pricetracker.feature.shared_ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The app uses the default Material 3 typography for general text,
 * and a monospace font family for price displays.
 *
 * Monospace with tabular figures ensures price digits don't shift
 * horizontally on updates — a critical detail in financial UIs.
 * We use the system monospace font to avoid adding a custom font asset.
 */
val PriceFont = FontFamily.Monospace

val Typography = Typography(
    // Feed screen: stock symbol
    titleMedium = TextStyle(
        fontFamily = PriceFont,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        letterSpacing = 0.5.sp
    ),
    // Detail screen: large price display
    headlineLarge = TextStyle(
        fontFamily = PriceFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 40.sp,
        letterSpacing = (-1).sp
    ),
    // Feed row: company name
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp
    ),
    // Price change text
    labelMedium = TextStyle(
        fontFamily = PriceFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp
    ),
    // Section headers ("Current Price", "About", etc.)
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 1.sp
    )
)