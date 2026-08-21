package com.metehanyl.borsa.data

import android.content.Context
import com.metehanyl.borsa.data.model.Holding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * "Portföyüm" pozisyonlarını cihazın dahili depolamasında düz bir JSON dosyasında
 * tutan basit kalıcı katman. Sunucuya hiçbir veri gönderilmez.
 */
class PortfolioStore(context: Context) {

    private val file = File(context.filesDir, "portfolio.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    suspend fun load(): List<Holding> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext emptyList()
        try {
            json.decodeFromString<List<Holding>>(file.readText())
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun save(holdings: List<Holding>) = withContext(Dispatchers.IO) {
        file.writeText(json.encodeToString(holdings))
    }
}
