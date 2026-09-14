package com.metehanyl.borsa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.metehanyl.borsa.ui.PortfolioViewModel
import com.metehanyl.borsa.ui.favorites.FavoritesViewModel
import com.metehanyl.borsa.ui.holdings.HoldingsViewModel
import com.metehanyl.borsa.ui.navigation.BorsaNavHost
import com.metehanyl.borsa.ui.theme.KureselBorsaTheme
import com.metehanyl.borsa.ui.theme.ThemePreference
import com.metehanyl.borsa.ui.theme.ThemeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val themePreference by themeViewModel.themePreference.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themePreference) {
                ThemePreference.SYSTEM -> systemDark
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }
            KureselBorsaTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val marketViewModel: PortfolioViewModel = viewModel()
                    val holdingsViewModel: HoldingsViewModel = viewModel()
                    val favoritesViewModel: FavoritesViewModel = viewModel()
                    BorsaNavHost(
                        marketViewModel = marketViewModel,
                        holdingsViewModel = holdingsViewModel,
                        favoritesViewModel = favoritesViewModel,
                        themeViewModel = themeViewModel
                    )
                }
            }
        }
    }
}
