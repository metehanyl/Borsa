package com.metehanyl.borsa.ui

import com.metehanyl.borsa.analysis.Analysis
import com.metehanyl.borsa.data.model.Quote

/** Bir sembol için fiyat verisi + üretilen analiz birlikte. */
data class StockEntry(
    val quote: Quote,
    val analysis: Analysis?
)
