package com.bluearcyiji

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.bluearcyiji.auth.AuthManager
import com.bluearcyiji.network.ApiHttpException
import com.bluearcyiji.network.RecordDetail
import com.bluearcyiji.network.RecordRequest
import com.bluearcyiji.network.ServerApiRepository
import com.bluearcyiji.network.SkuItem
import com.bluearcyiji.ui.theme.YIJITheme
import com.bluearcyiji.ui.AppTextKey
import com.bluearcyiji.ui.PremiumSkuList
import com.bluearcyiji.ui.MessageTone
import com.bluearcyiji.ui.TopMessageHost
import com.bluearcyiji.ui.appText
import com.bluearcyiji.ui.formatSecondsLabel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
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

@Preview(
    showBackground = true,
    showSystemUi = true
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MainScreen() {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var messageTone by remember { mutableStateOf(MessageTone.Info) }
    val serverApiRepository = remember { ServerApiRepository() }
    val appContext = context.applicationContext
    LaunchedEffect(appContext) {
        AuthManager.init(appContext)
    }
    var loggedInUserName by remember { mutableStateOf<String?>(null) }
    var serverToken by remember { mutableStateOf(AuthManager.getToken()) }
    var showProfileMenu by remember { mutableStateOf(false) }
    var loginInProgress by remember { mutableStateOf(false) }
    val tapDetails = remember { mutableStateListOf<RecordDetail>() }
    var showPremiumDialog by remember { mutableStateOf(false) }
    var premiumLoading by remember { mutableStateOf(false) }
    var premiumPlans by remember { mutableStateOf<List<SkuItem>>(emptyList()) }
    var selectedPremiumSkuId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(serverToken) {
        if (serverToken.isNullOrBlank()) {
            AuthManager.clearToken()
        } else {
            AuthManager.saveToken(serverToken.orEmpty())
        }
    }

    var clickCount by remember { mutableStateOf(0) }
    var startTime by remember { mutableStateOf(0L) }
    var lastClickTime by remember { mutableStateOf(0L) }

    var totalInterval by remember { mutableStateOf(0L) }
    var maxInterval by remember { mutableStateOf(0L) }
    var minInterval by remember { mutableStateOf(Long.MAX_VALUE) }

    var duration by remember { mutableStateOf(0L) }
    var isPaused by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

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
    val isLoggedIn = loggedInUserName != null || !serverToken.isNullOrBlank()
    val timeFormatter = remember { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US) }
    val textKeys = remember {
        mapOf(
            "desc_home" to AppTextKey.DescHome,
            "desc_profile" to AppTextKey.DescProfile,
            "profile" to AppTextKey.Profile,
            "account" to AppTextKey.Account,
            "logout" to AppTextKey.Logout,
            "tap" to AppTextKey.Tap,
            "reset_and_save" to AppTextKey.ResetAndSave,
            "pause" to AppTextKey.Pause,
            "resume" to AppTextKey.Resume,
            "stat_avg" to AppTextKey.StatAvg,
            "stat_max" to AppTextKey.StatMax,
            "stat_min" to AppTextKey.StatMin,
            "stat_duration" to AppTextKey.StatDuration,
            "action_sign_in" to AppTextKey.ActionSignIn,
            "action_save" to AppTextKey.ActionSave,
            "error_server_unavailable" to AppTextKey.ErrorServerUnavailable,
            "error_request_failed" to AppTextKey.ErrorRequestFailed,
            "error_network" to AppTextKey.ErrorNetwork,
            "error_try_again" to AppTextKey.ErrorTryAgain,
            "error_action_failed" to AppTextKey.ErrorActionFailed,
            "msg_sign_in_unavailable" to AppTextKey.MsgSignInUnavailable,
            "msg_signed_in_success" to AppTextKey.MsgSignedInSuccess,
            "msg_unsupported_sign_in_credential" to AppTextKey.MsgUnsupportedSignInCredential,
            "msg_session_expired_sign_in_again" to AppTextKey.MsgSessionExpiredSignInAgain,
            "msg_account_page_coming_soon" to AppTextKey.MsgAccountPageComingSoon,
            "msg_signed_out_success" to AppTextKey.MsgSignedOutSuccess,
            "msg_no_records_to_save" to AppTextKey.MsgNoRecordsToSave,
            "msg_records_saved_successfully" to AppTextKey.MsgRecordsSavedSuccessfully,
            "msg_premium_required" to AppTextKey.MsgPremiumRequired,
            "premium_dialog_title" to AppTextKey.PremiumDialogTitle,
            "premium_dialog_loading" to AppTextKey.PremiumDialogLoading,
            "premium_dialog_no_plans" to AppTextKey.PremiumDialogNoPlans,
            "premium_dialog_close" to AppTextKey.PremiumDialogClose,
            "action_load_plans" to AppTextKey.ActionLoadPlans,
        )
    }

    fun shouldRelogin(error: Throwable): Boolean {
        return (error as? ApiHttpException)?.statusCode == 403
    }

    fun shouldShowPremium(error: Throwable): Boolean {
        return (error as? ApiHttpException)?.statusCode == 402
    }

    fun t(key: String, vararg args: Any): String {
        val textKey = textKeys[key] ?: return key
        return context.appText(textKey, *args)
    }

    fun appErrorMessage(actionKey: String, error: Throwable? = null): String {
        val action = t(actionKey)
        val detail = when {
            error is ApiHttpException && error.statusCode in 500..599 -> t("error_server_unavailable")
            error is ApiHttpException -> t("error_request_failed")
            error != null -> t("error_network")
            else -> t("error_try_again")
        }
        return t("error_action_failed", action, detail)
    }

    suspend fun showTopMessage(message: String, tone: MessageTone) {
        messageTone = tone
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(message)
    }

    fun formatPriceInYuan(cents: Int): String {
        return String.format(Locale.US, "¥%.2f", cents / 100.0)
    }

    suspend fun startLoginFlow(): Boolean {
        if (loginInProgress) return false
        loginInProgress = true
        var loginSuccess = false

        val webClientId: String = runCatching {
            Class.forName("com.bluearcyiji.BuildConfig")
                .getField("GOOGLE_WEB_CLIENT_ID")
                .get(null) as? String
        }.getOrNull() ?: ""
        if (webClientId.isBlank()) {
            showTopMessage(t("msg_sign_in_unavailable"), MessageTone.Error)
            loginInProgress = false
            return false
        }

        val credentialManager = CredentialManager.create(context)
        val googleIdOption =
            GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .setServerClientId(webClientId)
                .build()
        val request =
            GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

        try {
            val result = credentialManager.getCredential(
                context = context,
                request = request
            )
            val credential = result.credential
            if (
                credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential =
                    GoogleIdTokenCredential.createFrom(credential.data)
                val loginResult = serverApiRepository
                    .loginWithGoogleIdToken(
                        idToken = googleCredential.idToken,
                        email = googleCredential.id,
                    )
                loginResult
                    .onSuccess { token ->
                        AuthManager.saveToken(token)
                        serverToken = token
                        loggedInUserName =
                            googleCredential.displayName ?: googleCredential.id
                        showProfileMenu = false
                        loginSuccess = true
                        showTopMessage(t("msg_signed_in_success"), MessageTone.Success)
                    }
                    .onFailure { error ->
                        showTopMessage(appErrorMessage("action_sign_in", error), MessageTone.Error)
                    }
            } else {
                showTopMessage(t("msg_unsupported_sign_in_credential"), MessageTone.Error)
            }
        } catch (_: GetCredentialCancellationException) {
            // User dismissed the account chooser.
        } catch (e: GetCredentialException) {
            showTopMessage(appErrorMessage("action_sign_in", e), MessageTone.Error)
        } finally {
            loginInProgress = false
        }
        return loginSuccess
    }

    suspend fun handleForbiddenAndRelogin(): Boolean {
        AuthManager.clearToken()
        serverToken = null
        loggedInUserName = null
        showProfileMenu = false

        if (loginInProgress) return false

        showTopMessage(t("msg_session_expired_sign_in_again"), MessageTone.Info)
        return startLoginFlow()
    }

    fun clearTapData() {
        tapDetails.clear()
        clickCount = 0
        startTime = 0
        lastClickTime = 0
        totalInterval = 0
        maxInterval = 0
        minInterval = Long.MAX_VALUE
        duration = 0
        isPaused = false
    }

    suspend fun loadPremiumPlansAndShowDialog() {
        fun premiumRank(plan: SkuItem): Int {
            val key = "${plan.skuId} ${plan.skuName}".lowercase(Locale.US)
            return when {
                "weekly" in key -> 0
                "monthly" in key -> 1
                "yearly" in key || "annual" in key -> 2
                else -> 99
            }
        }

        showPremiumDialog = true
        premiumLoading = true
        premiumPlans = emptyList()
        selectedPremiumSkuId = null

        serverApiRepository.getSkuList()
            .onSuccess { skuResponse ->
                val orderedPlans = skuResponse.skuList.subscription.sortedBy(::premiumRank)
                premiumPlans = orderedPlans
                selectedPremiumSkuId =
                    orderedPlans.firstOrNull {
                        val key = "${it.skuId} ${it.skuName}".lowercase(Locale.US)
                        "monthly" in key
                    }?.skuId ?: orderedPlans.firstOrNull()?.skuId
            }
            .onFailure { error ->
                premiumPlans = emptyList()
                selectedPremiumSkuId = null
                snackbarHostState.showSnackbar(appErrorMessage("action_load_plans", error))
            }

        premiumLoading = false
    }

    LaunchedEffect(startTime, isPaused) {
        while (startTime > 0 && !isPaused) {
            duration = System.currentTimeMillis() - startTime
            delay(100)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        snackbarHost = {},
        topBar = {
            if (isSaving) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 8.dp)
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },

        bottomBar = {
            BottomAppBar {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {

                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Home, contentDescription = t("desc_home"))
                    }

                    Spacer(modifier = Modifier.width(48.dp))

                    Box {
                        IconButton(
                            onClick = {
                                if (isLoggedIn) {
                                    showProfileMenu = true
                                } else {
                                    scope.launch {
                                        startLoginFlow()
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Person, contentDescription = t("desc_profile"))
                        }

                        DropdownMenu(
                            expanded = showProfileMenu,
                            onDismissRequest = { showProfileMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(loggedInUserName ?: t("profile")) },
                                onClick = { showProfileMenu = false },
                                enabled = false
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(t("account")) },
                                onClick = {
                                    showProfileMenu = false
                                    scope.launch {
                                        showTopMessage(t("msg_account_page_coming_soon"), MessageTone.Info)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(t("logout")) },
                                onClick = {
                                    AuthManager.clearToken()
                                    serverToken = null
                                    loggedInUserName = null
                                    showProfileMenu = false
                                    scope.launch {
                                        showTopMessage(t("msg_signed_out_success"), MessageTone.Success)
                                    }
                                }
                            )
                        }
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
                text = "$clickCount",
                fontSize = 70.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Red
            )

            Spacer(modifier = Modifier.height(16.dp))

            StatRow(t("stat_avg"), avgIntervalSec, t("stat_max"), maxIntervalSec)
            Spacer(modifier = Modifier.height(8.dp))
            StatRow(t("stat_min"), minIntervalSec, t("stat_duration"), durationSec)

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable {
                        if (isPaused) {
                            return@clickable
                        }

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
                        val sequence = clickCount + 1
                        clickCount = sequence
                        tapDetails.add(
                            RecordDetail(
                                sequence = sequence,
                                clickTime = timeFormatter.format(Date()),
                            )
                        )
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
                        t("tap"),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (startTime > 0L) {
                                isPaused = true
                            }
                            scope.launch {
                                if (tapDetails.isEmpty()) {
                                    showTopMessage(t("msg_no_records_to_save"), MessageTone.Info)
                                    return@launch
                                }

                                if (!isLoggedIn) {
                                    val loginSuccess = startLoginFlow()
                                    if (!loginSuccess) {
                                        return@launch
                                    }
                                }

                                val request = RecordRequest(
                                    avgTime = avgIntervalSec.toFloat(),
                                    maxTime = maxIntervalSec.toFloat(),
                                    minTime = minIntervalSec.toFloat(),
                                    durationTime = durationSec.toFloat(),
                                    totalClick = clickCount,
                                    details = tapDetails.toList(),
                                )

                                isSaving = true
                                try {
                                    val saveResult = serverApiRepository.saveRecords(request)
                                    if (saveResult.isSuccess) {
                                        clearTapData()
                                        showTopMessage(t("msg_records_saved_successfully"), MessageTone.Success)
                                        return@launch
                                    }

                                    val saveError = saveResult.exceptionOrNull()
                                    if (saveError != null && shouldRelogin(saveError)) {
                                        val reloginSuccess = handleForbiddenAndRelogin()
                                        if (reloginSuccess) {
                                            val retryResult = serverApiRepository.saveRecords(request)
                                            if (retryResult.isSuccess) {
                                                clearTapData()
                                                showTopMessage(t("msg_records_saved_successfully"), MessageTone.Success)
                                            } else {
                                                val retryError = retryResult.exceptionOrNull()
                                                if (retryError != null && shouldShowPremium(retryError)) {
                                                    loadPremiumPlansAndShowDialog()
                                                } else {
                                                    showTopMessage(
                                                        appErrorMessage("action_save", retryError),
                                                        MessageTone.Error
                                                    )
                                                }
                                            }
                                        }
                                    } else if (saveError != null && shouldShowPremium(saveError)) {
                                        loadPremiumPlansAndShowDialog()
                                    } else {
                                        showTopMessage(appErrorMessage("action_save", saveError), MessageTone.Error)
                                    }
                                } finally {
                                    isSaving = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(t("reset_and_save"))
                    }

                    Button(
                        onClick = {
                            if (startTime == 0L) {
                                return@Button
                            }

                            if (isPaused) {
                                startTime = System.currentTimeMillis() - duration
                                isPaused = false
                            } else {
                                isPaused = true
                            }
                        },
                        modifier = Modifier.weight(1.2f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(if (isPaused) t("resume") else t("pause"))
                    }
                }

            }
        }

        if (showPremiumDialog) {
            val dialogWidth = 355.dp
            val dialogHeight = 480.dp

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showPremiumDialog = false }
                    )
            )

            Dialog(
                onDismissRequest = { showPremiumDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .size(dialogWidth, dialogHeight)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = t("premium_dialog_title"),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = t("msg_premium_required"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        if (premiumLoading) {
                            CircularProgressIndicator()
                        } else if (premiumPlans.isEmpty()) {
                            Text(t("premium_dialog_no_plans"))
                        } else {
                            PremiumSkuList(
                                skus = premiumPlans,
                                selectedSkuId = selectedPremiumSkuId,
                                onSkuSelected = { sku -> selectedPremiumSkuId = sku.skuId },
                                priceFormatter = ::formatPriceInYuan,
                            )

                            Spacer(modifier = Modifier.height(22.dp))

                            Button(
                                onClick = { /* TODO purchase */ },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Continue")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                "Cancel anytime",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        TextButton(
                            onClick = { showPremiumDialog = false }
                        ) {
                            Text(t("premium_dialog_close"))
                        }
                    }
                }
            }
        }
        }

        TopMessageHost(
            hostState = snackbarHostState,
            tone = messageTone,
            topSpacing = 10.dp,
        )
    }
}

@Composable
fun StatRow(t1: String, v1: Double, t2: String, v2: Double) {
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
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