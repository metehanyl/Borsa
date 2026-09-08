package com.metehanyl.borsa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.metehanyl.borsa.ui.PortfolioViewModel
import com.metehanyl.borsa.ui.favorites.FavoritesViewModel
import com.metehanyl.borsa.ui.holdings.HoldingsViewModel
import com.metehanyl.borsa.ui.navigation.BorsaNavHost
import com.metehanyl.borsa.ui.theme.KureselBorsaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KureselBorsaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val marketViewModel: PortfolioViewModel = viewModel()
                    val holdingsViewModel: HoldingsViewModel = viewModel()
                    val favoritesViewModel: FavoritesViewModel = viewModel()
                    BorsaNavHost(
                        marketViewModel = marketViewModel,
                        holdingsViewModel = holdingsViewModel,
                        favoritesViewModel = favoritesViewModel
                    )
                }
            }
        }
    }
}
