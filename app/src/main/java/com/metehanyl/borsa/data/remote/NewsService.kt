package com.metehanyl.borsa.data.remote

import android.util.Xml
import com.metehanyl.borsa.data.model.NewsItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Google News'in genel (anahtarsız) RSS aramasından haber başlığı çeker.
 * Herhangi bir yapay zekaya canlı çağrı yapılmaz; sadece başlık + kaynak + link
 * bilgisi ayrıştırılır. Yorum/duygu analizi ayrıca `NewsAnalyzer`'da yapılır.
 */
object NewsService {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    /** @param language "tr" Türkçe piyasa/ekonomi haberleri, "en" küresel kapsam için. */
    suspend fun search(query: String, language: String = "tr", maxItems: Int = 8): List<NewsItem> =
        withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val (hl, gl, ceid) = if (language == "tr") Triple("tr", "TR", "TR:tr") else Triple("en-US", "US", "US:en")
                val url = "https://news.google.com/rss/search?q=$encoded&hl=$hl&gl=$gl&ceid=$ceid"
                val request = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0 (Linux; Android) KureselBorsa/1.0").build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: return@withContext emptyList()
                    parseRss(body).take(maxItems)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            }
        }

    private fun parseRss(xml: String): List<NewsItem> {
        val items = mutableListOf<NewsItem>()
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(xml.reader())

        var eventType = parser.eventType
        var inItem = false
        var title: String? = null
        var link: String? = null
        var source: String? = null
        var pubDate: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "item" -> {
                            inItem = true
                            title = null; link = null; source = null; pubDate = null
                        }
                        "title" -> if (inItem) title = safeNextText(parser)
                        "link" -> if (inItem) link = safeNextText(parser)
                        "source" -> if (inItem) source = safeNextText(parser)
                        "pubDate" -> if (inItem) pubDate = safeNextText(parser)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "item") {
                        inItem = false
                        val t = title
                        if (!t.isNullOrBlank()) {
                            items += NewsItem(
                                title = t,
                                source = source ?: "",
                                link = link ?: "",
                                publishedRaw = pubDate
                            )
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return items
    }

    private fun safeNextText(parser: XmlPullParser): String? = try {
        parser.nextText()
    } catch (e: Exception) {
        null
    }
}
