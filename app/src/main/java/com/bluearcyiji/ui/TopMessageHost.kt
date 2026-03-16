package com.bluearcyiji.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class MessageTone {
    Success,
    Error,
    Info,
}

@Composable
fun TopMessageHost(
    hostState: SnackbarHostState,
    tone: MessageTone,
    modifier: Modifier = Modifier,
    topSpacing: Dp = 20.dp,
    horizontalPadding: Dp = 16.dp,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(start = horizontalPadding, end = horizontalPadding, top = topSpacing),
        contentAlignment = Alignment.TopCenter,
    ) {
        SnackbarHost(
            hostState = hostState,
            modifier = Modifier.fillMaxWidth(),
            snackbar = { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(14.dp),
                    containerColor = when (tone) {
                        MessageTone.Success -> Color(0xFFE8F5E9)
                        MessageTone.Error -> Color(0xFFFFEBEE)
                        MessageTone.Info -> MaterialTheme.colorScheme.primaryContainer
                    },
                    contentColor = when (tone) {
                        MessageTone.Success -> Color(0xFF1B5E20)
                        MessageTone.Error -> Color(0xFFB00020)
                        MessageTone.Info -> MaterialTheme.colorScheme.onPrimaryContainer
                    },
                    actionColor = MaterialTheme.colorScheme.primary,
                )
            },
        )
    }
}

