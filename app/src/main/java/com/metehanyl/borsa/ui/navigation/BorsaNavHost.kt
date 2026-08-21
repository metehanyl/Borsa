package com.metehanyl.borsa.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.metehanyl.borsa.ui.PortfolioViewModel
import com.metehanyl.borsa.ui.screens.MarketsScreen
import com.metehanyl.borsa.ui.screens.StockDetailScreen

private const val ROUTE_MARKETS = "markets"
private const val ROUTE_DETAIL = "detail/{symbol}"

@Composable
fun BorsaNavHost(viewModel: PortfolioViewModel, navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = ROUTE_MARKETS) {
        composable(ROUTE_MARKETS) {
            MarketsScreen(
                viewModel = viewModel,
                onStockClick = { symbol -> navController.navigate("detail/$symbol") }
            )
        }
        composable(ROUTE_DETAIL) { backStackEntry ->
            val symbol = backStackEntry.arguments?.getString("symbol") ?: ""
            StockDetailScreen(
                viewModel = viewModel,
                symbol = symbol,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
