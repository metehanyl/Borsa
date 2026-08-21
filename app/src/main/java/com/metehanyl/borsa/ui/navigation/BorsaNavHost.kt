package com.metehanyl.borsa.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.metehanyl.borsa.ui.PortfolioViewModel
import com.metehanyl.borsa.ui.holdings.HoldingsScreen
import com.metehanyl.borsa.ui.holdings.HoldingsViewModel
import com.metehanyl.borsa.ui.screens.MarketsScreen
import com.metehanyl.borsa.ui.screens.StockDetailScreen

private const val ROUTE_MARKETS = "markets"
private const val ROUTE_HOLDINGS = "holdings"
private const val ROUTE_DETAIL = "detail/{symbol}"

private data class BottomDestination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomDestinations = listOf(
    BottomDestination(ROUTE_MARKETS, "Piyasalar", Icons.Filled.ShowChart),
    BottomDestination(ROUTE_HOLDINGS, "Portföyüm", Icons.Filled.AccountBalanceWallet)
)

@Composable
fun BorsaNavHost(
    marketViewModel: PortfolioViewModel,
    holdingsViewModel: HoldingsViewModel,
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = bottomDestinations.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    bottomDestinations.forEach { dest ->
                        val selected = currentRoute == dest.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) }
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_MARKETS,
            modifier = Modifier.padding(padding)
        ) {
            composable(ROUTE_MARKETS) {
                MarketsScreen(
                    viewModel = marketViewModel,
                    onStockClick = { symbol -> navController.navigate("detail/$symbol") }
                )
            }
            composable(ROUTE_HOLDINGS) {
                HoldingsScreen(
                    marketViewModel = marketViewModel,
                    holdingsViewModel = holdingsViewModel,
                    onStockClick = { symbol -> navController.navigate("detail/$symbol") }
                )
            }
            composable(ROUTE_DETAIL) { entry ->
                val symbol = entry.arguments?.getString("symbol") ?: ""
                StockDetailScreen(
                    viewModel = marketViewModel,
                    holdingsViewModel = holdingsViewModel,
                    symbol = symbol,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
