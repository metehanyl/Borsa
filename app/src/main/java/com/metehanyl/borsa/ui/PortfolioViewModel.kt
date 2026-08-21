package com.metehanyl.borsa.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metehanyl.borsa.analysis.AnalysisEngine
import com.metehanyl.borsa.data.QuoteResult
import com.metehanyl.borsa.data.StockRepository
import com.metehanyl.borsa.data.model.Market
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SortOrder { SCORE_ASCENDING, SCORE_DESCENDING }

data class PortfolioUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val entries: List<StockEntry> = emptyList(),
    val failedSymbols: List<String> = emptyList(),
    val selectedMarket: Market? = null,
    val sortOrder: SortOrder = SortOrder.SCORE_ASCENDING,
    val searchQuery: String = "",
    val lastUpdatedAtMillis: Long? = null,
    val errorMessage: String? = null
) {
    val visibleEntries: List<StockEntry>
        get() {
            val marketFiltered = if (selectedMarket == null) entries else entries.filter { it.quote.info.market == selectedMarket }
            val searched = if (searchQuery.isBlank()) {
                marketFiltered
            } else {
                val q = searchQuery.trim().lowercase()
                marketFiltered.filter {
                    it.quote.info.symbol.lowercase().contains(q) || it.quote.info.name.lowercase().contains(q)
                }
            }
            val sorted = searched.sortedBy { it.analysis?.score ?: Int.MIN_VALUE }
            return if (sortOrder == SortOrder.SCORE_ASCENDING) sorted else sorted.asReversed()
        }
}

class PortfolioViewModel : ViewModel() {

    private val repository = StockRepository()
    private val _uiState = MutableStateFlow(PortfolioUiState())
    val uiState: StateFlow<PortfolioUiState> = _uiState.asStateFlow()

    init {
        load(forceRefresh = false)
    }

    fun load(forceRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = _uiState.value.entries.isEmpty(),
                isRefreshing = _uiState.value.entries.isNotEmpty(),
                errorMessage = null
            )
            val results = repository.fetchAll(forceRefresh)
            val successes = results.filterIsInstance<QuoteResult.Success>().map { it.quote }
            val failures = results.filterIsInstance<QuoteResult.Failure>()

            val entries = successes.map { quote -> StockEntry(quote, AnalysisEngine.analyze(quote)) }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isRefreshing = false,
                entries = entries,
                failedSymbols = failures.map { it.info.symbol },
                lastUpdatedAtMillis = System.currentTimeMillis(),
                errorMessage = if (entries.isEmpty() && failures.isNotEmpty()) {
                    "Piyasa verisi alınamadı. İnternet bağlantınızı kontrol edip tekrar deneyin."
                } else null
            )
        }
    }

    fun selectMarket(market: Market?) {
        _uiState.value = _uiState.value.copy(selectedMarket = market)
    }

    fun toggleSortOrder() {
        val next = if (_uiState.value.sortOrder == SortOrder.SCORE_ASCENDING) SortOrder.SCORE_DESCENDING else SortOrder.SCORE_ASCENDING
        _uiState.value = _uiState.value.copy(sortOrder = next)
    }

    fun updateSearch(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun entryFor(symbol: String): StockEntry? = _uiState.value.entries.firstOrNull { it.quote.info.symbol == symbol }
}
