package com.bluearcyiji.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.bluearcyiji.BuildConfig
import com.bluearcyiji.billing.BillingEvent
import com.bluearcyiji.billing.BillingManager
import com.bluearcyiji.billing.SubscriptionChangeMode
import com.bluearcyiji.main.GoogleSignInResult
import com.bluearcyiji.main.MainUiEffect
import com.bluearcyiji.main.MainViewModel
import com.bluearcyiji.network.SkuItem
import com.bluearcyiji.ui.theme.YIJITheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.util.Locale

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MainPreview() {
    YIJITheme {
        MainScreen(MainViewModel())
    }
}

@Composable
fun MainScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val state by vm.state.collectAsState()
    val activity = context as? ComponentActivity
    val billingManager = remember(activity) {
        if (activity == null) null else BillingManager(activity.applicationContext).apply { connect() }
    }

    val durationSec = state.durationMillis / 1000.0
    val avgInterval = if (state.clickCount > 1) state.totalIntervalMillis / (state.clickCount - 1) else 0L
    val avgIntervalSec = avgInterval / 1000.0
    val maxIntervalSec = state.maxIntervalMillis / 1000.0
    val minIntervalSec = (if (state.minIntervalMillis == Long.MAX_VALUE) 0L else state.minIntervalMillis) / 1000.0

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
        Color(0xFFCFD8DC),
    )
    val tapCardColor = tapCardColors[(state.clickCount / 10) % tapCardColors.size]

    fun t(key: AppTextKey, vararg args: Any): String = context.appText(key, *args)
    val tFn: (AppTextKey, Array<out Any>) -> String = { key, args -> context.appText(key, *args) }

    suspend fun showTopMessage(message: String) {
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(message)
    }

    fun formatPriceInYuan(cents: Int): String {
        return String.format(Locale.US, "¥%.2f", cents / 100.0)
    }

    suspend fun startLoginFlow() {
        val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()

        if (webClientId.isBlank()) {
            vm.onGoogleSignInFailed(t(AppTextKey.MsgSignInUnavailable))
            return
        }

        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .setServerClientId(webClientId)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val result = credentialManager.getCredential(context = context, request = request)
            val credential = result.credential
            if (
                credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                vm.onGoogleSignInSuccess(
                    GoogleSignInResult(
                        idToken = googleCredential.idToken,
                        email = googleCredential.id,
                        displayName = googleCredential.displayName,
                    )
                )
            } else {
                vm.onGoogleSignInFailed(t(AppTextKey.MsgUnsupportedSignInCredential))
            }
        } catch (_: GetCredentialCancellationException) {
            vm.onGoogleSignInCancelled()
        } catch (e: GetCredentialException) {
            vm.onGoogleSignInFailed(e.message ?: t(AppTextKey.ErrorNetwork))
        }
    }

    LaunchedEffect(vm) {
        vm.effects.collect { effect ->
            when (effect) {
                MainUiEffect.StartGoogleSignIn -> startLoginFlow()
                is MainUiEffect.ShowTopMessage -> {
                    val resolved = resolveMessage(effect.message, tFn)
                    showTopMessage(resolved)
                }
            }
        }
    }

    LaunchedEffect(billingManager) {
        val bm = billingManager ?: return@LaunchedEffect
        bm.events.collect { event ->
            when (event) {
                is BillingEvent.PurchaseSuccess -> {
                    vm.onSubscriptionPurchaseSucceeded(event.purchases.flatMap { purchase -> purchase.products })
                    vm.showSuccessMessage("购买成功")
                    vm.onPremiumDismiss()
                }

                BillingEvent.UserCancelled -> {
                    vm.showInfoMessage("已取消支付")
                }

                is BillingEvent.Error -> {
                    vm.showErrorMessage("支付失败：${event.message}")
                }
            }
        }
    }

    val appVersionLabel = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = {},
            topBar = {
                if (state.isSaving || state.isRestoringSession) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(top = 8.dp),
                    ) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            bottomBar = {
                MainBottomBar(
                    isLoggedIn = state.isLoggedIn,
                    userName = state.loggedInUserName,
                    showProfileMenu = state.showProfileMenu,
                    homeDesc = t(AppTextKey.DescHome),
                    profileDesc = t(AppTextKey.DescProfile),
                    profileText = t(AppTextKey.Profile),
                    accountText = t(AppTextKey.Account),
                    logoutText = t(AppTextKey.Logout),
                    onProfileClick = vm::onProfileClick,
                    onProfileDismiss = vm::onProfileDismiss,
                    onAccountClick = vm::onAccountClick,
                    onLogoutClick = vm::onLogoutClick,
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CounterSummary(
                    clickCount = state.clickCount,
                    avgLabel = t(AppTextKey.StatAvg),
                    maxLabel = t(AppTextKey.StatMax),
                    minLabel = t(AppTextKey.StatMin),
                    durationLabel = t(AppTextKey.StatDuration),
                    avgValue = avgIntervalSec,
                    maxValue = maxIntervalSec,
                    minValue = minIntervalSec,
                    durationValue = durationSec,
                )

                Spacer(modifier = Modifier.height(20.dp))

                TapCard(
                    tapText = t(AppTextKey.Tap),
                    cardColor = tapCardColor,
                    enabled = !state.isPaused,
                    modifier = Modifier.weight(1f),
                    onTap = vm::onTap,
                )

                Spacer(modifier = Modifier.height(16.dp))

                SaveAndPauseActions(
                    saveText = t(AppTextKey.ResetAndSave),
                    pauseText = if (state.isPaused) t(AppTextKey.Resume) else t(AppTextKey.Pause),
                    onSaveClick = {
                        vm.onSaveClick()
                    },
                    onPauseClick = {
                        vm.onPauseToggle()
                    },
                )

                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.Text(
                    text = appVersionLabel,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        PremiumOverlayDialog(
            visible = state.showPremiumDialog,
            loading = state.premiumLoading,
            plans = state.premiumPlans,
            selectedSkuId = state.selectedPremiumSkuId,
            title = t(AppTextKey.PremiumDialogTitle),
            subtitle = t(AppTextKey.MsgPremiumRequired),
            loadingText = t(AppTextKey.PremiumDialogLoading),
            emptyText = t(AppTextKey.PremiumDialogNoPlans),
            closeText = t(AppTextKey.PremiumDialogClose),
            continueText = "Continue",
            cancelAnytimeText = "Cancel anytime",
            onDismiss = vm::onPremiumDismiss,
            onSkuSelected = { sku -> vm.onPremiumSkuSelected(sku.skuId) },
            onContinue = {
                val skuId = state.selectedPremiumSkuId
                val bm = billingManager
                if (activity == null || bm == null) {
                    vm.showErrorMessage("无法发起支付：缺少 Activity")
                    return@PremiumOverlayDialog
                }
                if (skuId.isNullOrBlank()) {
                    vm.showErrorMessage("请选择订阅方案")
                    return@PremiumOverlayDialog
                }
                val currentSkuId = state.currentSubscriptionSkuId
                if (!currentSkuId.isNullOrBlank() && currentSkuId == skuId) {
                    vm.showInfoMessage("你已经在当前订阅方案")
                    return@PremiumOverlayDialog
                }
                bm.launchSubscriptionPurchase(
                    activity = activity,
                    productId = skuId,
                    obfuscatedExternalAccountId = state.billingAccountId,
                    previousProductId = currentSkuId,
                    changeMode = resolveSubscriptionChangeMode(
                        currentSkuId = currentSkuId,
                        targetSkuId = skuId,
                        plans = state.premiumPlans,
                    ),
                )
            },
            priceFormatter = ::formatPriceInYuan,
        )

        if (state.isLoggedIn) {
            SubscriptionTriangleBadge(
                planText = resolvePlanLabel(
                    premiumInfo = state.premiumInfo,
                    plans = state.premiumPlans,
                    currentSkuId = state.currentSubscriptionSkuId,
                    freeLabel = t(AppTextKey.SubscriptionFreePlan),
                ),
                onClick = vm::loadPremiumPlansAndShowDialog,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 4.dp, top = 2.dp),
            )
        }

        TopMessageHost(
            hostState = snackbarHostState,
            tone = state.messageTone,
            topSpacing = 24.dp,
        )
    }
}

@Composable
private fun SubscriptionTriangleBadge(
    planText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val badgeSize = 76.dp
    val normalizedPlan = planText.trim()
    val textSize = if (normalizedPlan.length >= 10) 12.sp else 14.sp
    Box(
        modifier = modifier
            .width(badgeSize)
            .height(badgeSize)
            .clickable(onClick = onClick),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val triangle = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(0f, size.height)
                close()
            }
            drawPath(
                path = triangle,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFE39A44), Color(0xFFC96A1A)),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height),
                ),
            )
            drawLine(
                color = Color.White.copy(alpha = 0.22f),
                start = Offset(size.width, 0f),
                end = Offset(0f, size.height),
                strokeWidth = 1.2.dp.toPx(),
            )
        }

        androidx.compose.material3.Text(
            text = normalizedPlan,
            color = Color.White,
            fontSize = textSize,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 2.dp, top = 12.dp)
                .width(62.dp)
                .rotate(-45f),
        )
    }
}

private fun resolvePlanLabel(
    premiumInfo: String?,
    plans: List<SkuItem>,
    currentSkuId: String?,
    freeLabel: String,
): String {
    val matched = currentSkuId?.let { skuId ->
        plans.firstOrNull { it.skuId == skuId }
    }
    if (matched != null) {
        return matched.skuName
            .substringBefore("(")
            .substringBefore("-")
            .trim()
            .ifBlank { matched.skuName }
    }

    return premiumInfo
        ?.takeIf { it.isNotBlank() }
        ?.substringBefore("(")
        ?.substringBefore("-")
        ?.trim()
        ?.ifBlank { freeLabel }
        ?: freeLabel
}

private fun premiumTierRank(skuId: String, plans: List<SkuItem>): Int {
    val sku = plans.firstOrNull { it.skuId == skuId }
    val key = "${skuId} ${sku?.skuName.orEmpty()}".lowercase(Locale.US)
    return when {
        "weekly" in key -> 1
        "monthly" in key -> 2
        "yearly" in key || "annual" in key -> 3
        else -> 0
    }
}

private fun resolveSubscriptionChangeMode(
    currentSkuId: String?,
    targetSkuId: String,
    plans: List<SkuItem>,
): SubscriptionChangeMode? {
    val current = currentSkuId ?: return null
    val currentRank = premiumTierRank(current, plans)
    val targetRank = premiumTierRank(targetSkuId, plans)
    if (currentRank <= 0 || targetRank <= 0) return null
    return if (targetRank > currentRank) SubscriptionChangeMode.UPGRADE else SubscriptionChangeMode.DOWNGRADE
}

private fun resolveMessage(
    raw: String,
    t: (AppTextKey, Array<out Any>) -> String,
): String {
    if (raw.startsWith("error_action_failed|")) {
        val parts = raw.split("|")
        if (parts.size == 3) {
            val action = keyFrom(parts[1])?.let { t(it, emptyArray()) } ?: parts[1]
            val detail = keyFrom(parts[2])?.let { t(it, emptyArray()) } ?: parts[2]
            return t(AppTextKey.ErrorActionFailed, arrayOf(action, detail))
        }
    }
    return keyFrom(raw)?.let { t(it, emptyArray()) } ?: raw
}

private fun keyFrom(raw: String): AppTextKey? = when (raw) {
    "msg_signed_out_success" -> AppTextKey.MsgSignedOutSuccess
    "msg_signed_in_success" -> AppTextKey.MsgSignedInSuccess
    "msg_no_records_to_save" -> AppTextKey.MsgNoRecordsToSave
    "msg_records_saved_successfully" -> AppTextKey.MsgRecordsSavedSuccessfully
    "msg_restoring_session" -> AppTextKey.MsgRestoringSession
    "msg_session_expired_sign_in_again" -> AppTextKey.MsgSessionExpiredSignInAgain
    "error_server_unavailable" -> AppTextKey.ErrorServerUnavailable
    "error_request_failed" -> AppTextKey.ErrorRequestFailed
    "error_network" -> AppTextKey.ErrorNetwork
    "error_try_again" -> AppTextKey.ErrorTryAgain
    "action_sign_in" -> AppTextKey.ActionSignIn
    "action_save" -> AppTextKey.ActionSave
    "action_load_plans" -> AppTextKey.ActionLoadPlans
    else -> null
}

