package com.bluearcyiji.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bluearcyiji.main.RecordsUiState
import com.bluearcyiji.network.RecordDetail
import com.bluearcyiji.network.RecordHistoryItem
import com.bluearcyiji.ui.theme.YIJITheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun RecordsScreen(
    title: String,
    records: RecordsUiState,
    isPremium: Boolean,
    onBack: () -> Unit,
    onLoadMore: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    // During drag: update position directly without launching coroutines
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    // During spring-back: use Animatable for smooth animation
    val animOffsetX = remember { Animatable(0f) }
    val expandedIndices = remember { mutableStateListOf<Int>() }
    val listState = rememberLazyListState()

    // ── Pagination: detect "near bottom" using only snapshot state ───────────
    val isNearBottom by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 2
        }
    }
    // Re-evaluate after every scroll event AND after each load completes
    LaunchedEffect(isNearBottom, records.loadingMore, records.isLastPage) {
        if (isNearBottom && !records.loading && !records.loadingMore && !records.isLastPage && isPremium) {
            onLoadMore()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .offset {
                IntOffset(
                    x = if (isDragging) dragOffsetX.roundToInt()
                        else animOffsetX.value.roundToInt(),
                    y = 0,
                )
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        // Directly update state — zero coroutine overhead per event
                        isDragging = true
                        dragOffsetX = (dragOffsetX + dragAmount).coerceAtLeast(0f)
                    },
                    onDragEnd = {
                        val endOffset = dragOffsetX
                        coroutineScope.launch {
                            // Sync animOffsetX to current drag position BEFORE switching
                            animOffsetX.snapTo(endOffset)
                            isDragging = false
                            if (endOffset > size.width * 0.3f) {
                                animOffsetX.animateTo(size.width.toFloat(), tween(200))
                                onBack()
                                animOffsetX.snapTo(0f)
                            } else {
                                animOffsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium,
                                    ),
                                )
                            }
                            dragOffsetX = 0f
                        }
                    },
                    onDragCancel = {
                        val endOffset = dragOffsetX
                        coroutineScope.launch {
                            animOffsetX.snapTo(endOffset)
                            isDragging = false
                            animOffsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                ),
                            )
                            dragOffsetX = 0f
                        }
                    },
                )
            },
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // ── Top bar ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }

            // ── Content ───────────────────────────────────────────────────
            when {
                records.loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                records.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = records.error,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp),
                        )
                    }
                }

                records.items.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No records yet",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 16.dp,
                            vertical = 8.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        itemsIndexed(
                            items = records.items,
                            key = { index, item ->
                                item.details.firstOrNull()?.clickTime ?: index.toString()
                            },
                        ) { index, item ->
                            RecordCard(
                                item = item,
                                expanded = expandedIndices.contains(index),
                                onToggle = {
                                    if (expandedIndices.contains(index)) expandedIndices.remove(index)
                                    else expandedIndices.add(index)
                                },
                            )
                        }

                        // Loading more indicator / end-of-list footer
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                when {
                                    records.loadingMore -> CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                    )
                                    !isPremium && !records.isLastPage -> PremiumGateCard(
                                        onClick = onLoadMore,
                                    )
                                    records.isLastPage && records.items.isNotEmpty() -> Text(
                                        text = "— no more records —",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Record card ───────────────────────────────────────────────────────────────
@Composable
private fun RecordCard(
    item: RecordHistoryItem,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val dateStr = item.details.firstOrNull()?.clickTime?.let { formatClickDate(it) } ?: "—"
    val timeStr = item.details.firstOrNull()?.clickTime?.let { formatClickTime(it) } ?: ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header row: date · taps count · expand icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dateStr,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    )
                    if (timeStr.isNotBlank()) {
                        Text(
                            text = timeStr,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        )
                    }
                }
                Text(
                    text = "${item.totalCnt} taps",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                                  else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }

            Spacer(Modifier.height(10.dp))

            // Stats chips row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatChip(label = "Duration", value = "${item.duration.fmtSec()}s")
                StatChip(label = "Avg", value = "${item.avg.fmtSec()}s")
                StatChip(label = "Min", value = "${item.min.fmtSec()}s")
                StatChip(label = "Max", value = "${item.max.fmtSec()}s")
            }

            // Expandable details
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    item.details.sortedBy { it.sequence }.forEach { detail ->
                        DetailRow(detail)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun PremiumGateCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "Unlock Full History",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = "Subscribe to Premium to view all records",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun DetailRow(detail: RecordDetail) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = detail.sequence.toString(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Text(
            text = formatDetailTime(detail.clickTime),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatDuration(detail.duration),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
    }
}

// ── Time helpers ──────────────────────────────────────────────────────────────
private val dateFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
private val displayDateFmt = SimpleDateFormat("MMM d, yyyy", Locale.US)
private val displayTimeFmt = SimpleDateFormat("HH:mm:ss", Locale.US)

private fun formatClickDate(raw: String): String = try {
    displayDateFmt.format(dateFmt.parse(raw)!!)
} catch (_: Exception) { raw }

private fun formatClickTime(raw: String): String = try {
    displayTimeFmt.format(dateFmt.parse(raw)!!)
} catch (_: Exception) { "" }

private fun formatDetailTime(raw: String): String = try {
    displayTimeFmt.format(dateFmt.parse(raw)!!)
} catch (_: Exception) { raw }

// ── Duration formatters ───────────────────────────────────────────────────────
/** Core formatter — input in milliseconds. */
private fun fmtDurationMs(ms: Long): String = when {
    ms <= 0L        -> "—"
    ms < 1_000L     -> "${ms}ms"
    ms < 60_000L    -> "${"%.1f".format(ms / 1000.0)}s"
    ms < 3_600_000L -> {
        val m = ms / 60_000L
        val s = (ms % 60_000L) / 1_000L
        if (s == 0L) "${m}m" else "${m}m ${s}s"
    }
    else -> {
        val h = ms / 3_600_000L
        val m = (ms % 3_600_000L) / 60_000L
        if (m == 0L) "${h}h" else "${h}h ${m}m"
    }
}

/** StatChip values are in seconds (Float from API). */
private fun Float.fmtSec(): String = fmtDurationMs((this * 1000).toLong())

/** DetailRow duration is in milliseconds (Long from API). */
private fun formatDuration(ms: Long): String = fmtDurationMs(ms)

// ── Previews ──────────────────────────────────────────────────────────────────
private val previewDetails1 = listOf(
    RecordDetail(sequence = 1, clickTime = "2026-03-24T21:29:46", duration = 0),
    RecordDetail(sequence = 2, clickTime = "2026-03-24T21:29:47", duration = 1000),
)
private val previewDetails2 = listOf(
    RecordDetail(sequence = 1, clickTime = "2026-03-25T12:50:15", duration = 0),
    RecordDetail(sequence = 2, clickTime = "2026-03-25T13:05:19", duration = 904000),
    RecordDetail(sequence = 3, clickTime = "2026-03-25T13:05:20", duration = 1000),
)
private val previewItems = listOf(
    RecordHistoryItem(
        totalCnt = 2, min = 1.127f, max = 1.127f, avg = 1.127f,
        duration = 1.886f, details = previewDetails1,
    ),
    RecordHistoryItem(
        totalCnt = 3, min = 0.471f, max = 903.603f, avg = 226.275f,
        duration = 909.095f, details = previewDetails2,
    ),
)

@Preview(showBackground = true, showSystemUi = true, name = "Records - list")
@Composable
private fun RecordsScreenListPreview() {
    YIJITheme {
        RecordsScreen(
            title = "Records",
            isPremium = false,
            records = RecordsUiState(
                loading = false,
                items = previewItems,
                currentPage = 0,
                totalPages = 3,
            ),
            onBack = {},
            onLoadMore = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Records - loading")
@Composable
private fun RecordsScreenLoadingPreview() {
    YIJITheme {
        RecordsScreen(
            title = "Records",
            isPremium = false,
            records = RecordsUiState(loading = true),
            onBack = {},
            onLoadMore = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Records - empty")
@Composable
private fun RecordsScreenEmptyPreview() {
    YIJITheme {
        RecordsScreen(
            title = "Records",
            isPremium = false,
            records = RecordsUiState(loading = false),
            onBack = {},
            onLoadMore = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Records - error")
@Composable
private fun RecordsScreenErrorPreview() {
    YIJITheme {
        RecordsScreen(
            title = "Records",
            isPremium = false,
            records = RecordsUiState(error = "Failed to load records"),
            onBack = {},
            onLoadMore = {},
        )
    }
}
