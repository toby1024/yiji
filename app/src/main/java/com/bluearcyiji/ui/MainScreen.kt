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
import androidx.compose.foundation.layout.navigationBarsPadding
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
        } catch (e: Exception) {
            // Catches unexpected exceptions (e.g. GoogleIdTokenParsingException from createFrom).
            // Without this, loginInProgress stays true and subsequent login taps are silently dropped.
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
                    vm.showSuccessMessage(t(AppTextKey.MsgPurchaseSuccess))
                    vm.onPremiumDismiss()
                }

                BillingEvent.UserCancelled -> {
                    vm.showInfoMessage(t(AppTextKey.MsgPurchaseCancelled))
                }

                is BillingEvent.Error -> {
                    vm.showErrorMessage(t(AppTextKey.MsgPurchaseFailed, event.message ?: ""))
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
                    homeDesc = t(AppTextKey.DescHome),
                    profileDesc = t(AppTextKey.DescProfile),
                    onProfileClick = vm::onProfileClick,
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
            selectedBasePlanId = state.selectedPremiumBasePlanId,
            currentSkuId = state.currentSubscriptionSkuId,
            currentBasePlanId = state.currentSubscriptionBasePlanId,
            title = t(AppTextKey.PremiumDialogTitle),
            subtitle = if (state.premiumDialogShowSubtitle) t(AppTextKey.MsgPremiumRequired) else "",
            loadingText = t(AppTextKey.PremiumDialogLoading),
            emptyText = t(AppTextKey.PremiumDialogNoPlans),
            closeText = t(AppTextKey.PremiumDialogClose),
            continueText = "Continue",
            cancelAnytimeText = "Cancel anytime",
            onDismiss = vm::onPremiumDismiss,
            onSkuSelected = { sku -> vm.onPremiumSkuSelected(sku) },
            onContinue = {
                val skuId = state.selectedPremiumSkuId
                val bm = billingManager
                if (activity == null || bm == null) {
                    vm.showErrorMessage(t(AppTextKey.MsgNoActivityForPurchase))
                    return@PremiumOverlayDialog
                }
                if (skuId.isNullOrBlank()) {
                    vm.showErrorMessage(t(AppTextKey.MsgNoPlanSelected))
                    return@PremiumOverlayDialog
                }
                val currentSkuId = state.currentSubscriptionSkuId
                val currentBasePlanId = state.currentSubscriptionBasePlanId
                val selectedBasePlanId = state.selectedPremiumBasePlanId
                if (!currentSkuId.isNullOrBlank() && currentSkuId == skuId &&
                    !currentBasePlanId.isNullOrBlank() && currentBasePlanId == selectedBasePlanId) {
                    vm.showInfoMessage(t(AppTextKey.MsgAlreadyOnCurrentPlan))
                    return@PremiumOverlayDialog
                }
                bm.launchSubscriptionPurchase(
                    activity = activity,
                    productId = skuId,
                    basePlanId = selectedBasePlanId.orEmpty(),
                    obfuscatedExternalAccountId = state.billingAccountId,
                    previousProductId = currentSkuId,
                    previousBasePlanId = currentBasePlanId,
                    changeMode = resolveSubscriptionChangeMode(
                        currentSkuId = currentSkuId,
                        currentBasePlanId = currentBasePlanId,
                        targetSkuId = skuId,
                        targetBasePlanId = selectedBasePlanId.orEmpty(),
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
                    currentBasePlanId = state.currentSubscriptionBasePlanId,
                    freeLabel = t(AppTextKey.SubscriptionFreePlan),
                ),
                onClick = vm::onSubscriptionBadgeClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 4.dp, top = 2.dp),
            )
        } else {
            SubscriptionTriangleBadge(
                planText = t(AppTextKey.SubscriptionFreePlan),
                onClick = vm::requestGoogleSignIn,
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

        // Profile dropdown menu — rendered outside BottomAppBar so it can correctly
        // float above the bottom bar regardless of edge-to-edge / nav bar insets.
        if (state.isLoggedIn && state.showProfileMenu) {
            // Invisible full-screen backdrop to catch dismiss taps
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = vm::onProfileDismiss,
                    )
            )
            // Menu card anchored to the bottom-end, above the BottomAppBar
            ProfileMenuCard(
                userName = state.loggedInUserName,
                profileText = t(AppTextKey.Profile),
                accountText = t(AppTextKey.Account),
                recordsText = t(AppTextKey.Records),
                logoutText = t(AppTextKey.Logout),
                onDismiss = vm::onProfileDismiss,
                onAccountClick = vm::onAccountClick,
                onRecordsClick = vm::onRecordsClick,
                onLogoutClick = vm::onLogoutClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 12.dp, bottom = 80.dp),
            )
        }

        // Account page — full-screen overlay rendered on top of everything
        if (state.showAccountScreen) {
            val tierLabel = resolveTierLabel(state.premiumInfo)
            val subscriptionInfoText = buildString {
                if (tierLabel != null) {
                    append(tierLabel)
                    val expireSeconds = state.premiumExpireTimeEpochSeconds
                    if (expireSeconds != null && expireSeconds > 0L) {
                        val date = java.util.Date(expireSeconds * 1000L)
                        val formatted = java.text.SimpleDateFormat("MMM d, yyyy", Locale.US).format(date)
                        append(" · Expires $formatted")
                    }
                } else {
                    append(t(AppTextKey.SubscriptionFreePlan))
                }
            }
            AccountScreen(
                    userName = state.loggedInUserName,
                    subscriptionInfoText = subscriptionInfoText,
                    helpCenterText = t(AppTextKey.HelpCenter),
                    onBack = vm::onAccountBack,
                    showDeleteAccountDialog = state.showDeleteAccountDialog,
                    isDeletingAccount = state.isDeletingAccount,
                    onDeleteAccountClick = vm::onDeleteAccountClick,
                    onDeleteAccountConfirm = vm::onDeleteAccountConfirm,
                    onDeleteAccountDismiss = vm::onDeleteAccountDismiss,
                )
        }

        // Records page — full-screen overlay
        if (state.showRecordsScreen) {
            RecordsScreen(
                title = t(AppTextKey.Records),
                records = state.recordsState,
                isPremium = state.isPremium,
                onBack = vm::onRecordsBack,
                onLoadMore = vm::onLoadMoreRecords,
            )
        }
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
    /** Base plan ID of the active subscription; required to disambiguate when plans share the same skuId. */
    currentBasePlanId: String? = null,
    freeLabel: String,
): String {
    resolveTierLabel(premiumInfo)?.let { return it }

    val matched = currentSkuId?.let { skuId ->
        // Prefer the plan that matches both skuId and basePlanId (same-productId model).
        plans.firstOrNull {
            it.skuId == skuId &&
                (!currentBasePlanId.isNullOrBlank() && it.basePlanId == currentBasePlanId)
        } ?: plans.firstOrNull { it.skuId == skuId }
    }
    // Include basePlanId in the key so tier can be resolved from it when skuName lacks tier info.
    val key = "${currentSkuId.orEmpty()} ${matched?.skuName.orEmpty()} ${matched?.basePlanId.orEmpty()}"
    return resolveTierLabel(key) ?: freeLabel
}

private fun resolveTierLabel(raw: String?): String? {
    val key = raw?.trim()?.lowercase(Locale.US).orEmpty()
    if (key.isBlank()) return null
    return when {
        "weekly" in key -> "Weekly"
        "monthly" in key -> "Monthly"
        "yearly" in key || "annual" in key -> "Yearly"
        else -> null
    }
}

private fun premiumTierRank(skuId: String, basePlanId: String?, plans: List<SkuItem>): Int {
    val sku = plans.firstOrNull { it.skuId == skuId && (basePlanId.isNullOrBlank() || it.basePlanId == basePlanId) }
        ?: plans.firstOrNull { it.skuId == skuId }
    val key = "${skuId} ${sku?.skuName.orEmpty()} ${sku?.basePlanId.orEmpty()} ${basePlanId.orEmpty()}".lowercase(Locale.US)
    return when {
        "weekly" in key -> 1
        "monthly" in key -> 2
        "yearly" in key || "annual" in key -> 3
        else -> 0
    }
}

private fun resolveSubscriptionChangeMode(
    currentSkuId: String?,
    currentBasePlanId: String?,
    targetSkuId: String,
    targetBasePlanId: String,
    plans: List<SkuItem>,
): SubscriptionChangeMode? {
    val current = currentSkuId ?: return null
    val currentRank = premiumTierRank(current, currentBasePlanId, plans)
    val targetRank = premiumTierRank(targetSkuId, targetBasePlanId, plans)
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
    // 新增对 msg_records_saved_and_remaining|N 这类的解析
    if (raw.startsWith("msg_records_saved_and_remaining|")) {
        val parts = raw.split("|", limit = 2)
        val count = parts.getOrNull(1) ?: "0"
        return t(AppTextKey.MsgRecordsSavedAndRemaining, arrayOf(count))
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
    "msg_help_center_coming_soon" -> AppTextKey.MsgHelpCenterComingSoon
    "error_server_unavailable" -> AppTextKey.ErrorServerUnavailable
    "error_request_failed" -> AppTextKey.ErrorRequestFailed
    "error_network" -> AppTextKey.ErrorNetwork
    "error_try_again" -> AppTextKey.ErrorTryAgain
    "action_sign_in" -> AppTextKey.ActionSignIn
    "action_save" -> AppTextKey.ActionSave
    "action_load_plans" -> AppTextKey.ActionLoadPlans
    "action_load_user_info" -> AppTextKey.ActionLoadUserInfo
    else -> null
}

