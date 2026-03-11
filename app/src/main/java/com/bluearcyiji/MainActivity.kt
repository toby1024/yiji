package com.bluearcyiji

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.bluearcyiji.ui.theme.YIJITheme
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            YIJITheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {

    var clickCount by remember { mutableStateOf(0) }
    var startTime by remember { mutableStateOf(0L) }
    var lastClickTime by remember { mutableStateOf(0L) }

    var totalInterval by remember { mutableStateOf(0L) }
    var maxInterval by remember { mutableStateOf(0L) }
    var minInterval by remember { mutableStateOf(Long.MAX_VALUE) }

    var duration by remember { mutableStateOf(0L) }

    var resetProgress by remember { mutableStateOf(0f) }
    var isResetting by remember { mutableStateOf(false) }

    val resetInteraction = remember { MutableInteractionSource() }
    val isResetPressed by resetInteraction.collectIsPressedAsState()
    var holdReached by remember { mutableStateOf(false) }

    val avgInterval = if (clickCount > 1) totalInterval / (clickCount - 1) else 0L

    val durationSec = duration / 1000.0
    val avgIntervalSec = avgInterval / 1000.0
    val maxIntervalSec = maxInterval / 1000.0
    val minIntervalSec =
        if (minInterval == Long.MAX_VALUE) 0.0 else minInterval / 1000.0

    val tapCardColors = listOf(
        Color(0xFFE3F2FD),
        Color(0xFFC8E6C9),
        Color(0xFFFFF9C4),
        Color(0xFFFFCDD2),
        Color(0xFFD1C4E9),
        Color(0xFFFFE0B2),
        Color(0xFFB2DFDB),
        Color(0xFFF8BBD0),
        Color(0xFFDCEDC8),
        Color(0xFFCFD8DC)
    )
    val tapCardColor = tapCardColors[(clickCount / 10) % tapCardColors.size]

    LaunchedEffect(startTime) {
        while (startTime > 0) {
            duration = System.currentTimeMillis() - startTime
            delay(100)
        }
    }

    LaunchedEffect(isResetPressed) {
        if (isResetPressed) {
            isResetting = true
            holdReached = false
            resetProgress = 0f

            val steps = 20
            repeat(steps) {
                delay(100)
                resetProgress = (it + 1) / steps.toFloat()
            }

            // Hold threshold reached; actual reset happens on release.
            holdReached = true
        } else {
            if (isResetting && holdReached) {
                clickCount = 0
                startTime = 0
                lastClickTime = 0
                totalInterval = 0
                maxInterval = 0
                minInterval = Long.MAX_VALUE
                duration = 0
            }

            isResetting = false
            holdReached = false
            resetProgress = 0f
        }
    }

    Scaffold(

        bottomBar = {
            BottomAppBar {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {

                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Home, contentDescription = "Home")
                    }

                    Spacer(modifier = Modifier.width(48.dp))

                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                }
            }
        }

    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),

            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                "Training Tempo Counter",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "$clickCount",
                fontSize = 70.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Red
            )

            Spacer(modifier = Modifier.height(16.dp))

            StatRow("Avg", avgIntervalSec, "Max", maxIntervalSec)
            Spacer(modifier = Modifier.height(8.dp))
            StatRow("Min", minIntervalSec, "Duration", durationSec)

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable {

                        val now = System.currentTimeMillis()

                        if (clickCount == 0) {
                            startTime = now
                        }

                        if (lastClickTime != 0L) {

                            val interval = now - lastClickTime

                            totalInterval += interval
                            maxInterval = max(maxInterval, interval)
                            minInterval = min(minInterval, interval)
                        }

                        lastClickTime = now
                        clickCount++
                    },

                colors = CardDefaults.cardColors(
                    containerColor = tapCardColor
                )
            ) {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        "TAP",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Button(
                    onClick = {},
                    interactionSource = resetInteraction
                ) {
                    Text("Hold 2s to Reset")
                }

                if (resetProgress > 0f) {

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { resetProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun StatRow(t1: String, v1: Double, t2: String, v2: Double) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {

        StatCard(t1, "${String.format(Locale.US, "%.1f", v1)} s")
        StatCard(t2, "${String.format(Locale.US, "%.1f", v2)} s")
    }
}

@Composable
fun StatCard(title: String, value: String) {

    Card(
        modifier = Modifier
            .width(140.dp)
            .height(70.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),

            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(title, fontSize = 12.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(4.dp))

            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}