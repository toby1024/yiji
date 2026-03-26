package com.bluearcyiji.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bluearcyiji.ui.theme.YIJITheme

@Composable
fun AccountScreen(
    userName: String?,
    helpCenterText: String,
    onBack: () -> Unit,
    onHelpCenterClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // ── Top bar with back button ──────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

            // ── Avatar + Username ─────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AvatarInitial(name = userName)

                Text(
                    text = (userName ?: "").uppercase(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            // ── Menu card ─────────────────────────────────────────────────
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface,
            ) {
                AccountMenuItem(
                    icon = Icons.AutoMirrored.Filled.Help,
                    label = helpCenterText,
                    onClick = onHelpCenterClick,
                )
            }
        }
    }
}

// ── Avatar circle that renders the first initial ──────────────────────────────
@Composable
private fun AvatarInitial(
    name: String?,
    modifier: Modifier = Modifier,
) {
    val initial = name?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFE39A44), Color(0xFFC96A1A)),
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

// ── Single menu row ───────────────────────────────────────────────────────────
@Composable
private fun AccountMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Color.Gray,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Account - with user")
@Composable
private fun AccountScreenPreview() {
    YIJITheme {
        AccountScreen(
            userName = "Ann Vargas",
            helpCenterText = "Help Center",
            onBack = {},
            onHelpCenterClick = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Account - no user")
@Composable
private fun AccountScreenEmptyPreview() {
    YIJITheme {
        AccountScreen(
            userName = null,
            helpCenterText = "Help Center",
            onBack = {},
            onHelpCenterClick = {},
        )
    }
}
