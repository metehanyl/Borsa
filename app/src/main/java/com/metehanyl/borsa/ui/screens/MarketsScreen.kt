package com.metehanyl.borsa.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metehanyl.borsa.data.StockCatalog
import com.metehanyl.borsa.data.model.Market
import com.metehanyl.borsa.data.model.StockInfo
import com.metehanyl.borsa.ui.PortfolioViewModel
import com.metehanyl.borsa.ui.SortOrder
import com.metehanyl.borsa.ui.components.StockListItem
import com.metehanyl.borsa.ui.holdings.AddHoldingDialog
import com.metehanyl.borsa.ui.holdings.HoldingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketsScreen(
    viewModel: PortfolioViewModel,
    holdingsViewModel: HoldingsViewModel,
    onStockClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var buyTarget by remember { mutableStateOf<StockInfo?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Küresel Borsa", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.toggleSortOrder() }) {
                        Icon(Icons.Filled.SwapVert, contentDescription = "Sıralamayı değiştir")
                    }
                    IconButton(onClick = { viewModel.load(forceRefresh = true) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Yenile")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::updateSearch,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Sembol veya şirket ara (ör. AAPL, THYAO)") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true
            )

            MarketFilterRow(
                selectedMarket = state.selectedMarket,
                onSelect = viewModel::selectMarket
            )

            SortOrderLabel(state.sortOrder, state.visibleEntries.size)

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    state.errorMessage != null -> {
                        Text(
                            text = state.errorMessage ?: "",
                            modifier = Modifier.align(Alignment.Center).padding(32.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.visibleEntries, key = { it.quote.info.symbol }) { entry ->
                                StockListItem(
                                    entry = entry,
                                    onClick = { onStockClick(entry.quote.info.symbol) },
                                    onBuyClick = { buyTarget = entry.quote.info }
                                )
                            }
                            if (state.failedSymbols.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "${state.failedSymbols.size} sembol için veri alınamadı.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val target = buyTarget
    if (target != null) {
        AddHoldingDialog(
            marketViewModel = viewModel,
            preselectedStock = target,
            onDismiss = { buyTarget = null },
            onConfirm = { info, quantity, investedAmount, cost, date, note ->
                holdingsViewModel.addHolding(info, quantity, cost, date, note, investedAmount)
                if (StockCatalog.all.none { it.symbol == info.symbol }) viewModel.trackSymbol(info)
                buyTarget = null
            }
        )
    }
}

@Composable
private fun MarketFilterRow(selectedMarket: Market?, onSelect: (Market?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedMarket == null,
                onClick = { onSelect(null) },
                label = { Text("Tümü") }
            )
        }
        items(Market.entries) { market ->
            FilterChip(
                selected = selectedMarket == market,
                onClick = { onSelect(market) },
                label = { Text("${market.countryFlag} ${market.displayName}") }
            )
        }
    }
}

@Composable
private fun SortOrderLabel(sortOrder: SortOrder, count: Int) {
    val label = if (sortOrder == SortOrder.SCORE_ASCENDING) {
        "Fırsat skoruna göre: düşükten yükseğe"
    } else {
        "Fırsat skoruna göre: yüksekten düşüğe"
    }
    Text(
        text = "$label · $count sonuç",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}
