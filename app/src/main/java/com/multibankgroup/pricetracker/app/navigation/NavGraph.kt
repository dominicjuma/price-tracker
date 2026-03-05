package com.multibankgroup.pricetracker.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.multibankgroup.pricetracker.feature.detail.DetailScreen
import com.multibankgroup.pricetracker.feature.feed.FeedScreen

private const val DEEP_LINK_BASE_PATH = "stocks://symbol"

/**
 * Type-safe NavGraph (Navigation 2.8+). Deep link: stocks://symbol/{symbol}
 * Test with: adb shell am start -a android.intent.action.VIEW -d "stocks://symbol/AAPL"
 */
@Composable
fun PriceTrackerNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Feed,
        modifier = modifier
    ) {
        composable<Feed> {
            FeedScreen(
                onNavigateToDetail = { symbol ->
                    navController.navigate(Detail(symbol = symbol))
                }
            )
        }

        composable<Detail>(
            deepLinks = listOf(
                navDeepLink<Detail>(basePath = DEEP_LINK_BASE_PATH)
            )
        ) {
            DetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}