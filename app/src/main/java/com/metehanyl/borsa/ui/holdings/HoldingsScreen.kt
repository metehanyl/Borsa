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
import com.metehanyl.borsa.data.StockCatalog
import com.metehanyl.borsa.data.model.Holding
import com.metehanyl.borsa.data.model.StockInfo
import com.metehanyl.borsa.ui.PortfolioViewModel
import com.metehanyl.borsa.ui.StockEntry
import com.metehanyl.borsa.ui.components.ScoreBadge
import com.metehanyl.borsa.ui.components.formatPercent
import com.metehanyl.borsa.ui.components.formatPrice
import com.metehanyl.borsa.ui.theme.BuyGreen
import com.metehanyl.borsa.ui.theme.SellRed

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
            onDismiss = { showAddDialog = false },
            onConfirm = { symbol, quantity, cost, date, note ->
                holdingsViewModel.addHolding(symbol, quantity, cost, date, note)
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
            CurrencyValue(entry.quote.currency, h.averageCost * h.quantity, entry.quote.price * h.quantity)
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
    val currentValue = quote?.price?.times(holding.quantity)
    val costValue = holding.averageCost * holding.quantity
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
                Text(
                    "${holding.quantity} adet · Maliyet ${formatPrice(holding.averageCost, quote?.currency ?: "")}",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddHoldingDialog(
    onDismiss: () -> Unit,
    onConfirm: (symbol: String, quantity: Double, cost: Double, dateLabel: String, note: String) -> Unit
) {
    var selectedSymbol by remember { mutableStateOf<StockInfo?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }

    val matches = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList() else StockCatalog.all.filter {
            it.symbol.contains(searchQuery, ignoreCase = true) || it.name.contains(searchQuery, ignoreCase = true)
        }.take(8)
    }

    val quantity = quantityText.replace(",", ".").toDoubleOrNull()
    val cost = costText.replace(",", ".").toDoubleOrNull()
    val canConfirm = selectedSymbol != null && quantity != null && quantity > 0 && cost != null && cost > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pozisyon Ekle") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                val current = selectedSymbol
                if (current == null) {
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
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "${current.market.countryFlag} ${current.symbol} — ${current.name}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { selectedSymbol = null }) { Text("Değiştir") }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Adet") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = costText,
                        onValueChange = { costText = it },
                        label = { Text("Ortalama alış fiyatı (${current.market.currencySymbol})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        label = { Text("Alış tarihi (ör. 16.03.2026)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
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
                    val info = selectedSymbol ?: return@TextButton
                    val q = quantity ?: return@TextButton
                    val c = cost ?: return@TextButton
                    onConfirm(info.symbol, q, c, dateText.ifBlank { "-" }, noteText)
                }
            ) { Text("Ekle") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç") }
        }
    )
}
