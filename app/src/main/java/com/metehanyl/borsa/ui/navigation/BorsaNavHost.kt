package com.metehanyl.borsa.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.metehanyl.borsa.R
import com.metehanyl.borsa.ui.PortfolioViewModel
import com.metehanyl.borsa.ui.favorites.FavoritesViewModel
import com.metehanyl.borsa.ui.holdings.HoldingsScreen
import com.metehanyl.borsa.ui.holdings.HoldingsViewModel
import com.metehanyl.borsa.ui.screens.ChartDetailScreen
import com.metehanyl.borsa.ui.screens.FavoritesScreen
import com.metehanyl.borsa.ui.screens.MarketsScreen
import com.metehanyl.borsa.ui.screens.RecommendationsScreen
import com.metehanyl.borsa.ui.screens.StockDetailScreen
import com.metehanyl.borsa.ui.theme.ThemeViewModel

private const val ROUTE_MARKETS = "markets"
private const val ROUTE_HOLDINGS = "holdings"
private const val ROUTE_RECOMMENDATIONS = "recommendations"
private const val ROUTE_FAVORITES = "favorites"
private const val ROUTE_DETAIL = "detail/{symbol}"
private const val ROUTE_CHART_DETAIL = "chart/{symbol}"

private data class BottomDestination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomDestinations = listOf(
    BottomDestination(ROUTE_MARKETS, "Piyasalar", Icons.Filled.ShowChart),
    BottomDestination(ROUTE_RECOMMENDATIONS, "Önerilerim", Icons.Filled.ThumbUp),
    BottomDestination(ROUTE_FAVORITES, "Favoriler", Icons.Filled.Star),
    BottomDestination(ROUTE_HOLDINGS, "Portföyüm", Icons.Filled.AccountBalanceWallet)
)

@Composable
fun BorsaNavHost(
    marketViewModel: PortfolioViewModel,
    holdingsViewModel: HoldingsViewModel,
    favoritesViewModel: FavoritesViewModel,
    themeViewModel: ThemeViewModel,
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = bottomDestinations.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Column {
                    DeveloperCreditFooter()
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
                    holdingsViewModel = holdingsViewModel,
                    favoritesViewModel = favoritesViewModel,
                    themeViewModel = themeViewModel,
                    onStockClick = { symbol -> navController.navigate("detail/$symbol") }
                )
            }
            composable(ROUTE_RECOMMENDATIONS) {
                RecommendationsScreen(
                    viewModel = marketViewModel,
                    holdingsViewModel = holdingsViewModel,
                    favoritesViewModel = favoritesViewModel,
                    onStockClick = { symbol -> navController.navigate("detail/$symbol") }
                )
            }
            composable(ROUTE_FAVORITES) {
                FavoritesScreen(
                    viewModel = marketViewModel,
                    holdingsViewModel = holdingsViewModel,
                    favoritesViewModel = favoritesViewModel,
                    onStockClick = { symbol -> navController.navigate("detail/$symbol") }
                )
            }
            composable(ROUTE_HOLDINGS) {
                HoldingsScreen(
                    marketViewModel = marketViewModel,
                    holdingsViewModel = holdingsViewModel,
                    favoritesViewModel = favoritesViewModel,
                    onStockClick = { symbol -> navController.navigate("detail/$symbol") }
                )
            }
            composable(ROUTE_DETAIL) { entry ->
                val symbol = entry.arguments?.getString("symbol") ?: ""
                StockDetailScreen(
                    viewModel = marketViewModel,
                    holdingsViewModel = holdingsViewModel,
                    favoritesViewModel = favoritesViewModel,
                    symbol = symbol,
                    onBack = { navController.popBackStack() },
                    onOpenChart = { sym -> navController.navigate("chart/$sym") }
                )
            }
            composable(ROUTE_CHART_DETAIL) { entry ->
                val symbol = entry.arguments?.getString("symbol") ?: ""
                ChartDetailScreen(
                    viewModel = marketViewModel,
                    symbol = symbol,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

/**
 * Alt navigasyonun hemen üstünde, tüm ana ekranlarda görünen, küçük ve göze
 * batmayan bir geliştirici imzası. Uygulamanın kendi simgesi (borsa temalı)
 * ile karışmasın diye kullanıcının kişisel logosu burada, ayrı bir yerde
 * gösteriliyor — işaret her zaman okunabilsin diye krem renkli küçük bir
 * rozet içinde (uygulama teması açık/koyu olsa da kontrastı garanti eder).
 */
@Composable
private fun DeveloperCreditFooter() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(Color(0xFFF5F3EE), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_mark),
                contentDescription = "Yıldırım logosu",
                modifier = Modifier.size(10.dp)
            )
        }
        Spacer(Modifier.width(5.dp))
        Text(
            "Yıldırım",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        )
    }
}
