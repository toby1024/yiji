package com.bluearcyiji.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bluearcyiji.ui.theme.YIJITheme
import kotlinx.coroutines.launch

private const val SUPPORT_EMAIL = "toby.cheung3305@gmail.com"

@Composable
fun AccountScreen(
    userName: String?,
    subscriptionInfoText: String,
    helpCenterText: String,
    onBack: () -> Unit,
    showDeleteAccountDialog: Boolean = false,
    isDeletingAccount: Boolean = false,
    onDeleteAccountClick: () -> Unit = {},
    onDeleteAccountConfirm: () -> Unit = {},
    onDeleteAccountDismiss: () -> Unit = {},
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }

    // Build the contact annotated string once, capturing context/clipboard via lambda
    val emailLinkStyle = TextLinkStyles(
        style = SpanStyle(
            textDecoration = TextDecoration.Underline,
        ),
    )
    val contactAnnotated = buildAnnotatedString {
        append("Contact us via ")
        withLink(
            LinkAnnotation.Clickable(
                tag = "EMAIL",
                styles = emailLinkStyle,
                linkInteractionListener = {
                    clipboardManager.setText(AnnotatedString(SUPPORT_EMAIL))
                    android.widget.Toast
                        .makeText(context, "Email copied", android.widget.Toast.LENGTH_SHORT)
                        .show()
                },
            )
        ) {
            append(SUPPORT_EMAIL)
        }
        append(" for more support, we will reply in 48 hours.")
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        // Only follow rightward drags (positive direction = standard back gesture)
                        coroutineScope.launch {
                            val next = (offsetX.value + dragAmount).coerceAtLeast(0f)
                            offsetX.snapTo(next)
                        }
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            if (offsetX.value > size.width * 0.3f) {
                                // Past 30 % threshold → slide off screen to the right then dismiss
                                offsetX.animateTo(
                                    targetValue = size.width.toFloat(),
                                    animationSpec = tween<Float>(durationMillis = 200),
                                )
                                onBack()
                                offsetX.snapTo(0f)
                            } else {
                                // Not far enough → spring back
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring<Float>(stiffness = Spring.StiffnessMediumLow),
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            offsetX.animateTo(0f, spring<Float>(stiffness = Spring.StiffnessMediumLow))
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
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

            // ── Avatar + Username + Subscription ─────────────────────────
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

                if (subscriptionInfoText.isNotBlank()) {
                    Text(
                        text = subscriptionInfoText,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                }
            }

            // ── Help Center section ───────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = helpCenterText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = contactAnnotated,
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ── Delete Account ────────────────────────────────────────────
            Spacer(modifier = Modifier.weight(1f))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onDeleteAccountClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFD32F2F),
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD32F2F).copy(alpha = 0.6f)),
                    enabled = !isDeletingAccount,
                ) {
                    if (isDeletingAccount) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFD32F2F),
                        )
                    } else {
                        Text(
                            text = "Delete Account",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                Text(
                    text = "Permanently removes your account and all data.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.38f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    // ── Confirmation dialog ───────────────────────────────────────────────────
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = onDeleteAccountDismiss,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "Delete Account?",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F),
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "This action is permanent and cannot be undone.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Once deleted:\n• All your tap session records will be erased\n• Your subscription will not be automatically cancelled — please cancel it separately in Google Play\n• Your account login will be removed immediately\n• Account data cannot be recovered",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = onDeleteAccountConfirm,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD32F2F)),
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDeleteAccountDismiss) {
                    Text("Cancel")
                }
            },
        )
    }
}

// ── Avatar circle ─────────────────────────────────────────────────────────────
@Composable
private fun AvatarInitial(name: String?, modifier: Modifier = Modifier) {
    val initial = name?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFFE39A44), Color(0xFFC96A1A)))),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = initial, fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────
@Preview(showBackground = true, showSystemUi = true, name = "Account - paid")
@Composable
private fun AccountScreenPaidPreview() {
    YIJITheme {
        AccountScreen(
            userName = "Ann Vargas",
            subscriptionInfoText = "Monthly · Expires Jan 1, 2027",
            helpCenterText = "Help Center",
            onBack = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Account - delete dialog")
@Composable
private fun AccountScreenDeleteDialogPreview() {
    YIJITheme {
        AccountScreen(
            userName = "Ann Vargas",
            subscriptionInfoText = "Monthly · Expires Jan 1, 2027",
            helpCenterText = "Help Center",
            onBack = {},
            showDeleteAccountDialog = true,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Account - free")
@Composable
private fun AccountScreenFreePreview() {
    YIJITheme {
        AccountScreen(
            userName = "Ann Vargas",
            subscriptionInfoText = "Free Plan",
            helpCenterText = "Help Center",
            onBack = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Account - no user")
@Composable
private fun AccountScreenEmptyPreview() {
    YIJITheme {
        AccountScreen(
            userName = null,
            subscriptionInfoText = "Free Plan",
            helpCenterText = "Help Center",
            onBack = {},
        )
    }
}
