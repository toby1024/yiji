package com.bluearcyiji.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bluearcyiji.network.ServerApiRepository
import com.bluearcyiji.network.SkuItem
import com.bluearcyiji.ui.theme.YIJITheme
import java.util.Locale

@Composable
fun PremiumSkuCard(
    sku: SkuItem,
    selected: Boolean,
    onClick: () -> Unit,
    priceText: String,
    modifier: Modifier = Modifier,
    showPopularBadge: Boolean = sku.isPopular,
    cornerRadius: Dp = 14.dp,
    borderWidth: Dp = 3.dp,
    selectedBorderColor: Color = Color(0xFFFF9332),
    unselectedBorderColor: Color = Color(0xFFFBDABA),
) {
    Box(modifier = modifier) {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius),
            border = BorderStroke(
                width = borderWidth,
                color = if (selected) selectedBorderColor else unselectedBorderColor,
            ),
            colors = CardDefaults.outlinedCardColors(
                containerColor = if (selected) Color(0xFFFFF3E8) else MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = sku.skuName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = priceText,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) selectedBorderColor else LocalContentColor.current,
                )
            }
        }

        if (showPopularBadge) {
            val badgeBackgroundColor = if (selected) selectedBorderColor else unselectedBorderColor
            val badgeTextColor = Color(0xFFFFFFFF)

            Text(
                text = "MOST POPULAR",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 0.dp, y = 0.dp)
                    .background(badgeBackgroundColor, androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                fontSize = 10.sp,
                color = badgeTextColor,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun PremiumSkuList(
    skus: List<SkuItem>,
    selectedSkuId: String?,
    onSkuSelected: (SkuItem) -> Unit,
    priceFormatter: (Int) -> String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        skus.forEach { sku ->
            PremiumSkuCard(
                sku = sku,
                selected = sku.skuId == selectedSkuId,
                onClick = { onSkuSelected(sku) },
                priceText = priceFormatter(sku.skuPrice),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun rememberPreviewSkus(): List<SkuItem> {
    var skus by remember { mutableStateOf<List<SkuItem>>(emptyList()) }
    val repository = remember { ServerApiRepository() }

    LaunchedEffect(Unit) {
        repository.getSkuList()
            .onSuccess { response ->
                skus = response.skuList.subscription
            }
            .onFailure {
                skus = emptyList()
            }
    }
    return skus
}

@Preview(showBackground = true)
@Composable
private fun PremiumSkuCardPreview() {
    YIJITheme {
        val previewSkus = rememberPreviewSkus()
        if (previewSkus.isNotEmpty()) {
            val sku = previewSkus.first()
            PremiumSkuCard(
                sku = sku,
                selected = sku.isPopular,
                onClick = {},
                priceText = String.format(Locale.US, "¥%.2f", sku.skuPrice / 100.0),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Text("No preview SKU data")
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun PremiumSkuListPreview() {
    YIJITheme {
        val previewSkus = rememberPreviewSkus()
        PremiumSkuList(
            skus = previewSkus,
            selectedSkuId = previewSkus.firstOrNull()?.skuId,
            onSkuSelected = {},
            priceFormatter = { cents -> String.format(Locale.US, "¥%.2f", cents / 100.0) },
            modifier = Modifier.padding(12.dp),
        )
    }
}

