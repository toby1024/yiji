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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.bluearcyiji.auth.AuthManager
import com.bluearcyiji.network.ApiHttpException
import com.bluearcyiji.network.RecordRequest
import com.bluearcyiji.network.ServerApiRepository
import com.bluearcyiji.ui.theme.YIJITheme
import com.bluearcyiji.ui.AppTextKey
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

@Composable
fun MainScreen() {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val serverApiRepository = remember { ServerApiRepository() }
    val appContext = context.applicationContext
    LaunchedEffect(appContext) {
        AuthManager.init(appContext)
    }
    var loggedInUserName by remember { mutableStateOf<String?>(null) }
    var serverToken by remember { mutableStateOf(AuthManager.getToken()) }
    var showProfileMenu by remember { mutableStateOf(false) }
    var loginInProgress by remember { mutableStateOf(false) }
    val tapRecords = remember { mutableStateListOf<RecordRequest>() }

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
        )
    }

    fun Throwable.isForbidden(): Boolean {
        return this is ApiHttpException && this.statusCode == 403
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

    suspend fun startLoginFlow() {
        if (loginInProgress) return
        loginInProgress = true

        val webClientId: String = runCatching {
            Class.forName("com.bluearcyiji.BuildConfig")
                .getField("GOOGLE_WEB_CLIENT_ID")
                .get(null) as? String
        }.getOrNull() ?: ""
        if (webClientId.isBlank()) {
            snackbarHostState.showSnackbar(t("msg_sign_in_unavailable"))
            loginInProgress = false
            return
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
                        snackbarHostState.showSnackbar(t("msg_signed_in_success"))
                    }
                    .onFailure { error ->
                        snackbarHostState.showSnackbar(appErrorMessage("action_sign_in", error))
                    }
            } else {
                snackbarHostState.showSnackbar(t("msg_unsupported_sign_in_credential"))
            }
        } catch (_: GetCredentialCancellationException) {
            // User dismissed the account chooser.
        } catch (e: GetCredentialException) {
            snackbarHostState.showSnackbar(appErrorMessage("action_sign_in", e))
        } finally {
            loginInProgress = false
        }
    }

    suspend fun handleForbiddenAndRelogin() {
        AuthManager.clearToken()
        serverToken = null
        loggedInUserName = null
        showProfileMenu = false

        if (loginInProgress) return

        snackbarHostState.showSnackbar(t("msg_session_expired_sign_in_again"))
        startLoginFlow()
    }

    LaunchedEffect(startTime, isPaused) {
        while (startTime > 0 && !isPaused) {
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
                isPaused = false
                tapRecords.clear()
            }

            isResetting = false
            holdReached = false
            resetProgress = 0f
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (resetProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { resetProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
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
                                        snackbarHostState.showSnackbar(t("msg_account_page_coming_soon"))
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
                                        snackbarHostState.showSnackbar(t("msg_signed_out_success"))
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
                        tapRecords.add(
                            RecordRequest(
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
                            scope.launch {
                                if (tapRecords.isEmpty()) {
                                    snackbarHostState.showSnackbar(t("msg_no_records_to_save"))
                                    return@launch
                                }
                                serverApiRepository.saveRecords(tapRecords.toList())
                                    .onSuccess {
                                        tapRecords.clear()
                                        clickCount = 0
                                        startTime = 0
                                        lastClickTime = 0
                                        totalInterval = 0
                                        maxInterval = 0
                                        minInterval = Long.MAX_VALUE
                                        duration = 0
                                        isPaused = false
                                        snackbarHostState.showSnackbar(t("msg_records_saved_successfully"))
                                    }
                                    .onFailure { error ->
                                        if (error.isForbidden()) {
                                            handleForbiddenAndRelogin()
                                        } else {
                                            snackbarHostState.showSnackbar(appErrorMessage("action_save", error))
                                        }
                                    }
                            }
                        },
                        interactionSource = resetInteraction,
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