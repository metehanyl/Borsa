package com.metehanyl.borsa.ui.holdings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metehanyl.borsa.analysis.Recommendation
import com.metehanyl.borsa.analysis.computeSellGuidance
import com.metehanyl.borsa.data.StockCatalog
import com.metehanyl.borsa.data.model.Holding
import com.metehanyl.borsa.data.model.Market
import com.metehanyl.borsa.data.model.StockInfo
import com.metehanyl.borsa.ui.PortfolioViewModel
import com.metehanyl.borsa.ui.StockEntry
import com.metehanyl.borsa.ui.components.ScoreBadge
import com.metehanyl.borsa.ui.components.SellGuidanceCard
import com.metehanyl.borsa.ui.components.formatPercent
import com.metehanyl.borsa.ui.components.formatPrice
import com.metehanyl.borsa.ui.theme.BuyGreen
import com.metehanyl.borsa.ui.theme.SellRed
import java.text.SimpleDateFormat
import java.util.Locale

/** Kataloğun dışında elle eklenen kağıtlar için kullanılan sektör etiketi. */
private const val CUSTOM_SECTOR = "Kullanıcı Eklentisi"

private fun isCustomHolding(symbol: String): Boolean = StockCatalog.all.none { it.symbol == symbol }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoldingsScreen(
    marketViewModel: PortfolioViewModel,
    holdingsViewModel: HoldingsViewModel,
    onStockClick: (String) -> Unit
) {
    val marketState by marketViewModel.uiState.collectAsState()
    val holdings by holdingsViewModel.holdings.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    // Uygulama yeniden açıldığında, kataloğun dışında elle eklenmiş kağıtların
    // fiyatlarının da tekrar çekilmeye başlanmasını sağlar.
    LaunchedEffect(holdings) {
        holdings.filter { isCustomHolding(it.symbol) }.forEach { h ->
            marketViewModel.trackSymbol(StockInfo(h.symbol, h.name, h.market, CUSTOM_SECTOR))
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Portföyüm", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Pozisyon ekle")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (holdings.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "Henüz pozisyon eklemediniz",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Banka/aracı kurum uygulamanızdan aldığınız hisse veya fonu sağ alttaki + " +
                        "butonuyla ekleyin; güncel durumunu ve size özel al/sat değerlendirmesini burada görün.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { PortfolioSummary(holdings, marketState.entries) }
                items(holdings, key = { it.id }) { holding ->
                    val entry = marketState.entries.firstOrNull { it.quote.info.symbol == holding.symbol }
                    HoldingCard(
                        holding = holding,
                        entry = entry,
                        onClick = { onStockClick(holding.symbol) },
                        onDelete = { holdingsViewModel.removeHolding(holding.id) }
                    )
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    if (showAddDialog) {
        AddHoldingDialog(
            marketViewModel = marketViewModel,
            onDismiss = { showAddDialog = false },
            onConfirm = { info, quantity, investedAmount, cost, date, note ->
                holdingsViewModel.addHolding(info, quantity, cost, date, note, investedAmount)
                if (isCustomHolding(info.symbol)) marketViewModel.trackSymbol(info)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun PortfolioSummary(holdings: List<Holding>, entries: List<StockEntry>) {
    data class CurrencyValue(val currency: String, val cost: Double, val value: Double)

    val rows = holdings
        .mapNotNull { h ->
            val entry = entries.firstOrNull { it.quote.info.symbol == h.symbol } ?: return@mapNotNull null
            val qty = h.effectiveQuantity
            CurrencyValue(entry.quote.currency, h.averageCost * qty, entry.quote.price * qty)
        }
        .groupBy { it.currency }
        .map { (currency, group) -> Triple(currency, group.sumOf { it.cost }, group.sumOf { it.value }) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text("Portföy Özeti", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        if (rows.isEmpty()) {
            Text(
                "Fiyatlar yükleniyor…",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 6.dp)
            )
        } else {
            rows.forEach { (currency, cost, value) ->
                val pnl = value - cost
                val pnlPct = if (cost != 0.0) (pnl / cost) * 100.0 else 0.0
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Maliyet ($currency)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text(formatPrice(cost, currency), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Güncel Değer", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text(formatPrice(value, currency), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Kâr/Zarar", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text(
                            "${formatPrice(pnl, currency)} (${formatPercent(pnlPct)})",
                            fontSize = 14.sp,
                            color = if (pnl >= 0) BuyGreen else SellRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HoldingCard(holding: Holding, entry: StockEntry?, onClick: () -> Unit, onDelete: () -> Unit) {
    val quote = entry?.quote
    val qty = holding.effectiveQuantity
    val currentValue = quote?.price?.times(qty)
    val costValue = holding.averageCost * qty
    val pnl = currentValue?.minus(costValue)
    val pnlPct = if (currentValue != null && costValue != 0.0) (pnl!! / costValue) * 100.0 else null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(quote?.info?.market?.countryFlag ?: "🌐", fontSize = 20.sp)
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                Text(holding.symbol, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                val quantityLabel = if (holding.isAmountBased) {
                    "≈ ${"%.4f".format(qty)} adet (${formatPrice(holding.investedAmount ?: 0.0, quote?.currency ?: "")} tutar bazlı)"
                } else {
                    "${holding.quantity.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() }} adet"
                }
                Text(
                    "$quantityLabel · Maliyet ${formatPrice(holding.averageCost, quote?.currency ?: "")}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Sil", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }

        if (quote == null) {
            Text("Fiyat yükleniyor…", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.padding(top = 8.dp))
        } else {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Güncel Değer", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text(formatPrice(currentValue ?: 0.0, quote.currency), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Kâr/Zarar", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text(
                        text = if (pnl != null && pnlPct != null) "${formatPrice(pnl, quote.currency)} (${formatPercent(pnlPct)})" else "-",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = if ((pnl ?: 0.0) >= 0) BuyGreen else SellRed
                    )
                }
                entry?.analysis?.let { analysis ->
                    ScoreBadge(recommendation = analysis.recommendation, score = analysis.score)
                }
            }

            entry?.analysis?.let { analysis ->
                Text(
                    text = holdingVerdictText(pnlPct, analysis.recommendation),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    modifier = Modifier.padding(top = 8.dp)
                )
                SellGuidanceCard(
                    guidance = computeSellGuidance(quote, analysis),
                    currency = quote.currency,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}

private fun holdingVerdictText(pnlPct: Double?, recommendation: Recommendation): String {
    val inProfit = (pnlPct ?: 0.0) >= 0
    val bearish = recommendation == Recommendation.SELL || recommendation == Recommendation.STRONG_SELL
    val bullish = recommendation == Recommendation.BUY || recommendation == Recommendation.STRONG_BUY

    return when {
        inProfit && bearish -> "Karda bir pozisyon ama teknik görünüm zayıflıyor — kısmi kâr realizasyonunu değerlendirebilirsiniz."
        !inProfit && bullish -> "Zararda ama teknik görünüm toparlanma sinyali veriyor — panik satışından kaçının."
        !inProfit && bearish -> "Zararda ve teknik görünüm de zayıf — pozisyonu yakından izleyin."
        inProfit && bullish -> "Karda ve teknik görünüm hâlâ güçlü — tutmaya devam etmek mantıklı olabilir."
        else -> "Nötr görünüm — mevcut durumu izlemeye devam edebilirsiniz."
    }
}

/** Kullanıcının pozisyonu adet mi yoksa yatırım tutarı olarak mı girdiği. */
private enum class EntryMode { QUANTITY, AMOUNT }

/** "gg.aa.yyyy" biçimindeki serbest metni ayrıştırır; uymuyorsa null döner. */
private fun parseDateLabelToMillis(text: String): Long? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null
    return try {
        val format = SimpleDateFormat("dd.MM.yyyy", Locale("tr"))
        format.isLenient = false
        format.parse(trimmed)?.time
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddHoldingDialog(
    marketViewModel: PortfolioViewModel,
    /** Verilirse (ör. Piyasalar sekmesindeki "Satın Al" butonundan açıldıysa) arama adımı atlanır, bu kağıtla başlanır. */
    preselectedStock: StockInfo? = null,
    onDismiss: () -> Unit,
    onConfirm: (info: StockInfo, quantity: Double, investedAmount: Double?, cost: Double, dateLabel: String, note: String) -> Unit
) {
    var selectedSymbol by remember(preselectedStock) { mutableStateOf(preselectedStock) }
    var searchQuery by remember { mutableStateOf("") }
    var manualEntryMode by remember { mutableStateOf(false) }
    var manualSymbol by remember { mutableStateOf("") }
    var manualName by remember { mutableStateOf("") }
    var manualMarket by remember { mutableStateOf(Market.US) }
    var selectedMode by remember { mutableStateOf<EntryMode?>(null) }
    var numberText by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("") }
    var manualCostOverride by remember { mutableStateOf(false) }
    var dateText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }

    var fetchedPrice by remember { mutableStateOf<Double?>(null) }
    var isFetchingPrice by remember { mutableStateOf(false) }
    var fetchFailed by remember { mutableStateOf(false) }

    val matches = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList() else StockCatalog.all.filter {
            it.symbol.contains(searchQuery, ignoreCase = true) || it.name.contains(searchQuery, ignoreCase = true)
        }.take(8)
    }

    val current: StockInfo? = selectedSymbol ?: if (manualEntryMode && manualSymbol.isNotBlank()) {
        StockInfo(manualSymbol.trim().uppercase(), manualName.ifBlank { manualSymbol.trim().uppercase() }, manualMarket, CUSTOM_SECTOR)
    } else null

    val parsedDateMillis = remember(dateText) { parseDateLabelToMillis(dateText) }

    // Kağıt ve geçerli bir tarih seçildiğinde, o tarihteki (veya öncesindeki son
    // işlem günündeki) kapanış fiyatını otomatik çeker — kullanıcı elle maliyet
    // girmek zorunda kalmasın diye.
    LaunchedEffect(current?.symbol, parsedDateMillis) {
        val symbol = current?.symbol
        val dateMillis = parsedDateMillis
        if (symbol != null && dateMillis != null) {
            isFetchingPrice = true
            fetchFailed = false
            fetchedPrice = null
            val price = marketViewModel.fetchHistoricalClose(symbol, dateMillis)
            fetchedPrice = price
            fetchFailed = price == null
            isFetchingPrice = false
            manualCostOverride = false
        } else {
            fetchedPrice = null
            fetchFailed = false
            isFetchingPrice = false
        }
    }

    val number = numberText.replace(",", ".").toDoubleOrNull()
    val hasValidNumber = number != null && number > 0
    val manualCost = costText.replace(",", ".").toDoubleOrNull()
    val useManualCost = manualCostOverride || fetchedPrice == null
    val cost = if (useManualCost) manualCost else fetchedPrice
    val canConfirm = current != null && selectedMode != null && hasValidNumber && cost != null && cost > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pozisyon Ekle") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (selectedSymbol == null && !manualEntryMode) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Hisse ara (sembol veya şirket adı)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    matches.forEach { info ->
                        Text(
                            text = "${info.market.countryFlag} ${info.symbol} — ${info.name}",
                            fontSize = 13.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSymbol = info
                                    searchQuery = ""
                                }
                                .padding(vertical = 10.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = { manualEntryMode = true }) {
                        Text("Aradığınız kağıt listede yok mu? Sembolü elle girin")
                    }
                } else if (selectedSymbol == null && manualEntryMode) {
                    Text(
                        "Kağıdı Yahoo Finance sembolüyle elle ekleyin (ör. ARM, UMC, TSM, 2330.TW). " +
                            "Sembolün doğru olduğundan siz emin olmalısınız.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = manualSymbol,
                        onValueChange = { manualSymbol = it },
                        label = { Text("Sembol (ör. UMC)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = manualName,
                        onValueChange = { manualName = it },
                        label = { Text("Şirket adı (opsiyonel)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Piyasa", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                        items(Market.entries) { market ->
                            FilterChip(
                                selected = manualMarket == market,
                                onClick = { manualMarket = market },
                                label = { Text("${market.countryFlag} ${market.currencySymbol}") }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { manualEntryMode = false; manualSymbol = "" }) {
                        Text("Aramaya dön")
                    }
                    Spacer(Modifier.height(4.dp))
                }

                val resolved = current
                if (resolved != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "${resolved.market.countryFlag} ${resolved.symbol} — ${resolved.name}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            selectedSymbol = null
                            manualEntryMode = false
                            manualSymbol = ""
                            manualName = ""
                        }) { Text("Değiştir") }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        label = { Text("Alış tarihi (gg.aa.yyyy, ör. 16.03.2026)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Bu formatta girerseniz o tarihteki kapanış fiyatını otomatik bulup " +
                            "ortalama maliyet olarak kullanırım.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )

                    Text("Nasıl aldınız?", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)) {
                        FilterChip(
                            selected = selectedMode == EntryMode.QUANTITY,
                            onClick = { selectedMode = EntryMode.QUANTITY; numberText = "" },
                            label = { Text("Adet ile aldım") }
                        )
                        FilterChip(
                            selected = selectedMode == EntryMode.AMOUNT,
                            onClick = { selectedMode = EntryMode.AMOUNT; numberText = "" },
                            label = { Text("Tutar ile aldım") }
                        )
                    }

                    if (selectedMode != null) {
                        OutlinedTextField(
                            value = numberText,
                            onValueChange = { numberText = it },
                            label = {
                                Text(
                                    if (selectedMode == EntryMode.QUANTITY) "Adet"
                                    else "Yatırım Tutarı (${resolved.market.currencySymbol})"
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))

                        when {
                            parsedDateMillis == null -> {
                                Text(
                                    "Fiyatı otomatik getirmek için tarihi gg.aa.yyyy formatında girin, " +
                                        "ya da aşağıya elle girin.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                OutlinedTextField(
                                    value = costText,
                                    onValueChange = { costText = it },
                                    label = { Text("Ortalama alış fiyatı (${resolved.market.currencySymbol})") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            isFetchingPrice -> {
                                Text(
                                    "O tarihteki fiyat aranıyor…",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            fetchedPrice != null && !manualCostOverride -> {
                                Text(
                                    "$dateText tarihli kapanış fiyatı: ${formatPrice(fetchedPrice!!, resolved.market.currencySymbol)} " +
                                        "— ortalama maliyet olarak kullanılacak.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                )
                                TextButton(onClick = {
                                    manualCostOverride = true
                                    costText = "%.4f".format(fetchedPrice!!)
                                }) { Text("Fiyatı elle değiştir") }
                            }
                            else -> {
                                if (fetchFailed) {
                                    Text(
                                        "Bu tarih için otomatik fiyat bulunamadı, elle girin.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }
                                OutlinedTextField(
                                    value = costText,
                                    onValueChange = { costText = it },
                                    label = { Text("Ortalama alış fiyatı (${resolved.market.currencySymbol})") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (manualCostOverride && fetchedPrice != null) {
                                    TextButton(onClick = { manualCostOverride = false }) {
                                        Text("Otomatik bulunan fiyata dön")
                                    }
                                }
                            }
                        }

                        if (selectedMode == EntryMode.AMOUNT && hasValidNumber && cost != null && cost > 0) {
                            Text(
                                "≈ ${"%.4f".format(number!! / cost)} adete karşılık gelir (maliyetten türetilir).",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Not (opsiyonel)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canConfirm,
                onClick = {
                    val info = current ?: return@TextButton
                    val c = cost ?: return@TextButton
                    val n = number ?: return@TextButton
                    val q = if (selectedMode == EntryMode.QUANTITY) n else 0.0
                    val a = if (selectedMode == EntryMode.AMOUNT) n else null
                    onConfirm(info, q, a, c, dateText.ifBlank { "-" }, noteText)
                }
            ) { Text("Ekle") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç") }
        }
    )
}
