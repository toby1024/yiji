package com.bluearcyiji.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bluearcyiji.network.SkuItem

@Composable
fun MainBottomBar(
    isLoggedIn: Boolean,
    userName: String?,
    showProfileMenu: Boolean,
    homeDesc: String,
    profileDesc: String,
    profileText: String,
    accountText: String,
    logoutText: String,
    onProfileClick: () -> Unit,
    onProfileDismiss: () -> Unit,
    onAccountClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    BottomAppBar {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            IconButton(onClick = { }) {
                Icon(Icons.Default.Home, contentDescription = homeDesc)
            }

            SpacerForBar()

            Box {
                IconButton(onClick = onProfileClick) {
                    Icon(Icons.Default.Person, contentDescription = profileDesc)
                }

                DropdownMenu(
                    expanded = isLoggedIn && showProfileMenu,
                    onDismissRequest = onProfileDismiss,
                ) {
                    DropdownMenuItem(
                        text = { Text(userName ?: profileText) },
                        onClick = onProfileDismiss,
                        enabled = false,
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(accountText) },
                        onClick = onAccountClick,
                    )
                    DropdownMenuItem(
                        text = { Text(logoutText) },
                        onClick = onLogoutClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun SpacerForBar() {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(48.dp))
}

@Composable
fun CounterSummary(
    clickCount: Int,
    avgLabel: String,
    maxLabel: String,
    minLabel: String,
    durationLabel: String,
    avgValue: Double,
    maxValue: Double,
    minValue: Double,
    durationValue: Double,
) {
    Text(
        text = "$clickCount",
        fontSize = 70.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Red,
    )

    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

    StatRow(avgLabel, avgValue, maxLabel, maxValue)
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
    StatRow(minLabel, minValue, durationLabel, durationValue)
}

@Composable
fun StatRow(t1: String, v1: Double, t2: String, v2: Double) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatCard(t1, context.formatSecondsLabel(v1))
        StatCard(t2, context.formatSecondsLabel(v2))
    }
}

@Composable
fun StatCard(title: String, value: String) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(70.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, fontSize = 12.sp, color = Color.Gray)
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TapCard(
    tapText: String,
    cardColor: Color,
    enabled: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onTap),
        colors = CardDefaults.cardColors(containerColor = cardColor),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = tapText,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun SaveAndPauseActions(
    saveText: String,
    pauseText: String,
    onSaveClick: () -> Unit,
    onPauseClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            onClick = onSaveClick,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        ) {
            Text(saveText)
        }

        Button(
            onClick = onPauseClick,
            modifier = Modifier.weight(1.2f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(pauseText)
        }
    }
}

@Composable
fun PremiumOverlayDialog(
    visible: Boolean,
    loading: Boolean,
    plans: List<SkuItem>,
    selectedSkuId: String?,
    currentSkuId: String?,
    title: String,
    subtitle: String,
    loadingText: String,
    emptyText: String,
    closeText: String,
    continueText: String,
    cancelAnytimeText: String,
    onDismiss: () -> Unit,
    onSkuSelected: (SkuItem) -> Unit,
    onContinue: () -> Unit,
    priceFormatter: (Int) -> String,
) {
    if (!visible) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.size(355.dp, 480.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (subtitle.isNotBlank()) {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                    )
                }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(22.dp))

                if (loading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                        Text(loadingText)
                    }
                } else if (plans.isEmpty()) {
                    Text(emptyText)
                } else {
                    PremiumSkuList(
                        skus = plans,
                        selectedSkuId = selectedSkuId,
                        currentSkuId = currentSkuId,
                        onSkuSelected = onSkuSelected,
                        priceFormatter = priceFormatter,
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(22.dp))
                    Button(
                        onClick = onContinue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(continueText)
                    }
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = cancelAnytimeText,
                        fontSize = 12.sp,
                        color = Color.Gray,
                    )
                }

                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss) {
                    Text(closeText)
                }
            }
        }
    }
}

