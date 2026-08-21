package com.metehanyl.borsa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metehanyl.borsa.analysis.Recommendation
import com.metehanyl.borsa.ui.theme.BuyGreen
import com.metehanyl.borsa.ui.theme.BuyGreenDim
import com.metehanyl.borsa.ui.theme.NeutralAmber
import com.metehanyl.borsa.ui.theme.SellRed
import com.metehanyl.borsa.ui.theme.SellRedDim

fun colorFor(recommendation: Recommendation): Color = when (recommendation) {
    Recommendation.STRONG_BUY -> BuyGreen
    Recommendation.BUY -> BuyGreenDim
    Recommendation.HOLD -> NeutralAmber
    Recommendation.SELL -> SellRedDim
    Recommendation.STRONG_SELL -> SellRed
}

@Composable
fun ScoreBadge(recommendation: Recommendation, score: Int, modifier: Modifier = Modifier) {
    val color = colorFor(recommendation)
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = "${recommendation.shortLabel} · $score",
            color = color,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
