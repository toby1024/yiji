package com.bluearcyiji

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModelProvider
import com.bluearcyiji.billing.BillingEvent
import com.bluearcyiji.billing.BillingManager
import com.bluearcyiji.main.GoogleSignInResult
import com.bluearcyiji.main.MainUiEffect
import com.bluearcyiji.main.MainViewModel
import com.bluearcyiji.ui.AppTextKey
import com.bluearcyiji.ui.CounterSummary
import com.bluearcyiji.ui.MainBottomBar
import com.bluearcyiji.ui.MessageTone
import com.bluearcyiji.ui.PremiumOverlayDialog
import com.bluearcyiji.ui.SaveAndPauseActions
import com.bluearcyiji.ui.TapCard
import com.bluearcyiji.ui.TopMessageHost
import com.bluearcyiji.ui.appText
import com.bluearcyiji.ui.formatSecondsLabel
import com.bluearcyiji.ui.theme.YIJITheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YIJITheme {
                val vm = remember {
                    ViewModelProvider(this@MainActivity)[MainViewModel::class.java]
                }
                MainScreen(vm)
            }
        }
    }
}

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

    suspend fun showTopMessage(message: String, tone: MessageTone) {
        snackbarHostState.currentSnackbarData?.dismiss()
        // tone 由 state 驱动，UI 先更新再展示
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
            // User cancelled chooser.
            vm.onGoogleSignInCancelled()
        } catch (e: GetCredentialException) {
            vm.onGoogleSignInFailed(e.message ?: t(AppTextKey.ErrorNetwork))
        }
    }

    LaunchedEffect(Unit) {
        // 确保 token store 初始化只做一次
        com.bluearcyiji.auth.AuthManager.init(context.applicationContext)
    }

    LaunchedEffect(vm) {
        vm.effects.collect { effect ->
            when (effect) {
                MainUiEffect.StartGoogleSignIn -> startLoginFlow()
                is MainUiEffect.ShowTopMessage -> {
                    val resolved = resolveMessage(effect.message, tFn)
                    // tone 颜色来源于 TopMessageHost 参数，这里用 ViewModel 的 tone
                    showTopMessage(resolved, effect.tone)
                }
            }
        }
    }

    LaunchedEffect(billingManager) {
        val bm = billingManager ?: return@LaunchedEffect
        bm.events.collect { event ->
            when (event) {
                is BillingEvent.PurchaseSuccess -> {
                    // 这里先做本地成功提示；服务端发放权益可在后续接入你的后端校验
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
                if (state.isSaving) {
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
                bm.launchSubscriptionPurchase(activity, skuId)
            },
            priceFormatter = ::formatPriceInYuan,
        )

        TopMessageHost(
            hostState = snackbarHostState,
            tone = state.messageTone,
            topSpacing = 24.dp,
        )
    }
}

private fun resolveMessage(
    raw: String,
    t: (AppTextKey, Array<out Any>) -> String,
): String {
    // MainViewModel 可能发两种消息：
    // 1) 直接是 AppTextKey 的“逻辑 key”（如 msg_signed_out_success）
    // 2) error_action_failed|<actionKey>|<detailKey>
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

@Composable
fun StatRow(t1: String, v1: Double, t2: String, v2: Double) {
    val context = LocalContext.current
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
    ) {
        StatCard(t1, context.formatSecondsLabel(v1))
        StatCard(t2, context.formatSecondsLabel(v2))
    }
}

@Composable
fun StatCard(title: String, value: String) {
    androidx.compose.material3.Card(
        modifier = Modifier
            .width(140.dp)
            .height(70.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            androidx.compose.material3.Text(title, fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            androidx.compose.material3.Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}