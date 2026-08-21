package com.metehanyl.borsa.ui.holdings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.metehanyl.borsa.data.PortfolioStore
import com.metehanyl.borsa.data.model.Holding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class HoldingsViewModel(application: Application) : AndroidViewModel(application) {

    private val store = PortfolioStore(application)

    private val _holdings = MutableStateFlow<List<Holding>>(emptyList())
    val holdings: StateFlow<List<Holding>> = _holdings.asStateFlow()

    init {
        viewModelScope.launch {
            _holdings.value = store.load()
        }
    }

    fun addHolding(symbol: String, quantity: Double, averageCost: Double, purchaseDateLabel: String, note: String) {
        val holding = Holding(
            id = UUID.randomUUID().toString(),
            symbol = symbol,
            quantity = quantity,
            averageCost = averageCost,
            purchaseDateLabel = purchaseDateLabel,
            note = note
        )
        val updated = _holdings.value + holding
        _holdings.value = updated
        viewModelScope.launch { store.save(updated) }
    }

    fun removeHolding(id: String) {
        val updated = _holdings.value.filterNot { it.id == id }
        _holdings.value = updated
        viewModelScope.launch { store.save(updated) }
    }

    fun holdingForSymbol(symbol: String): Holding? = _holdings.value.firstOrNull { it.symbol == symbol }
}
