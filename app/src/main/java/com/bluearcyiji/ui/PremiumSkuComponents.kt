package com.bluearcyiji.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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

// ── Flag shape drawn behind the text via drawBehind ──────────────────────────
// direction: "right" = tail points right (left-anchored), "left" = tail points left (right-anchored)
// cornerRadius: rounded corner radius for the "closed" end of the flag (bottom-right for "left" direction)
private fun Modifier.flagBackground(color: Color, direction: String = "right", cornerRadius: Dp = 0.dp): Modifier = this.drawBehind {
    val notch = size.height / 2f
    val r = cornerRadius.toPx().coerceAtMost(size.height / 2f)
    val path = Path().apply {
        if (direction == "left") {
            moveTo(size.width, 0f)
            lineTo(notch, 0f)
            lineTo(0f, notch)              // left-pointing notch → flag tail
            lineTo(notch, size.height)
            if (r > 0f) {
                // bottom edge, stop before the rounded corner
                lineTo(size.width - r, size.height)
                // quarter-circle arc at bottom-right corner (90° → 0°, counterclockwise)
                arcTo(
                    rect = Rect(size.width - 2 * r, size.height - 2 * r, size.width, size.height),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = -90f,
                    forceMoveTo = false,
                )
                // right edge up to top-right
                lineTo(size.width, 0f)
            } else {
                lineTo(size.width, size.height)
            }
        } else {
            moveTo(0f, 0f)
            lineTo(size.width - notch, 0f)
            lineTo(size.width, notch)      // right-pointing notch → flag tail
            lineTo(size.width - notch, size.height)
            lineTo(0f, size.height)
        }
        close()
    }
    drawPath(path, color = color)
}

@Composable
private fun CurrentFlag(modifier: Modifier = Modifier, cornerRadius: Dp = 14.dp) {
    Box(
        modifier = modifier
            .wrapContentWidth()
            .height(20.dp)
            .flagBackground(Color(0xFFE07B39), direction = "left", cornerRadius = cornerRadius)
            .padding(start = 16.dp, end = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "CURRENT",
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
fun PremiumSkuCard(
    sku: SkuItem,
    selected: Boolean,
    onClick: () -> Unit,
    priceText: String,
    modifier: Modifier = Modifier,
    isCurrent: Boolean = false,
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

        // Current flag — bottom-right, tight against the card border (not overlapping)
        if (isCurrent) {
            CurrentFlag(
                cornerRadius = cornerRadius,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = -borderWidth, y = -borderWidth),
            )
        }

        // Most-popular badge — only shown when not the current plan
        if (showPopularBadge) {
            val badgeColor = if (selected) selectedBorderColor else unselectedBorderColor
            Text(
                text = "MOST POPULAR",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(badgeColor, androidx.compose.foundation.shape.RoundedCornerShape(0.dp, 14.dp, 0.dp, 14.dp ))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                fontSize = 10.sp,
                color = Color.White,
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
    /** Base plan ID of the selected plan; disambiguates plans that share the same skuId. */
    selectedBasePlanId: String? = null,
    currentSkuId: String?,
    /** Base plan ID of the active subscription. */
    currentBasePlanId: String? = null,
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
                selected = sku.skuId == selectedSkuId &&
                    (selectedBasePlanId.isNullOrBlank() || sku.basePlanId == selectedBasePlanId),
                isCurrent = sku.skuId == currentSkuId &&
                    (currentBasePlanId.isNullOrBlank() || sku.basePlanId == currentBasePlanId),
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
            currentSkuId = null,
            onSkuSelected = {},
            priceFormatter = { cents -> String.format(Locale.US, "¥%.2f", cents / 100.0) },
            modifier = Modifier.padding(12.dp),
        )
    }
}
