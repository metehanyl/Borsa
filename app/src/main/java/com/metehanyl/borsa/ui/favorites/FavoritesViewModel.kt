package com.metehanyl.borsa.ui.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.metehanyl.borsa.data.FavoritesStore
import com.metehanyl.borsa.data.model.StockInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {

    private val store = FavoritesStore(application)

    private val _favorites = MutableStateFlow<List<StockInfo>>(emptyList())
    val favorites: StateFlow<List<StockInfo>> = _favorites.asStateFlow()

    init {
        viewModelScope.launch {
            _favorites.value = store.load()
        }
    }

    fun isFavorite(symbol: String): Boolean = _favorites.value.any { it.symbol == symbol }

    /** Yıldızlıysa kaldırır, değilse ekler. */
    fun toggle(info: StockInfo) {
        val updated = if (isFavorite(info.symbol)) {
            _favorites.value.filterNot { it.symbol == info.symbol }
        } else {
            _favorites.value + info
        }
        _favorites.value = updated
        viewModelScope.launch { store.save(updated) }
    }

    fun remove(symbol: String) {
        val updated = _favorites.value.filterNot { it.symbol == symbol }
        _favorites.value = updated
        viewModelScope.launch { store.save(updated) }
    }
}
