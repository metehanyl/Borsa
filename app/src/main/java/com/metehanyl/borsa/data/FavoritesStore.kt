package com.metehanyl.borsa.data

import android.content.Context
import com.metehanyl.borsa.data.model.StockInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * "Favoriler" listesini cihazın dahili depolamasında düz bir JSON dosyasında
 * tutan basit kalıcı katman (Portföyüm'deki PortfolioStore ile aynı desen).
 * Sunucuya hiçbir veri gönderilmez.
 */
class FavoritesStore(context: Context) {

    private val file = File(context.filesDir, "favorites.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    suspend fun load(): List<StockInfo> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext emptyList()
        try {
            json.decodeFromString<List<StockInfo>>(file.readText())
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun save(favorites: List<StockInfo>) = withContext(Dispatchers.IO) {
        file.writeText(json.encodeToString(favorites))
    }
}
