package com.bluearcyiji.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluearcyiji.auth.AuthManager
import com.bluearcyiji.network.ApiHttpException
import com.bluearcyiji.network.RecordDetail
import com.bluearcyiji.network.RecordRequest
import com.bluearcyiji.network.ServerApiRepository
import com.bluearcyiji.ui.MessageTone
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class MainViewModel(
    private val repository: ServerApiRepository = ServerApiRepository(),
    private val timeFormatter: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {

    private val _state = MutableStateFlow(MainUiState(serverToken = AuthManager.getToken()))
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    private val _effects = Channel<MainUiEffect>(capacity = Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    @Volatile
    private var pendingSaveRequest: RecordRequest? = null

    init {
        viewModelScope.launch {
            while (true) {
                val s = _state.value
                if (s.startTimeMillis > 0L && !s.isPaused) {
                    _state.update { it.copy(durationMillis = nowMillis() - it.startTimeMillis) }
                }
                delay(100)
            }
        }
    }

    fun onProfileClick() {
        val s = _state.value
        if (s.isLoggedIn) {
            _state.update { it.copy(showProfileMenu = true) }
        } else {
            requestGoogleSignIn()
        }
    }

    fun onProfileDismiss() {
        _state.update { it.copy(showProfileMenu = false) }
    }

    fun onLogoutClick() {
        AuthManager.clearToken()
        _state.update {
            it.copy(
                serverToken = null,
                loggedInUserName = null,
                showProfileMenu = false,
            )
        }
        emitMessage("msg_signed_out_success", MessageTone.Success)
    }

    fun onAccountClick() {
        _state.update { it.copy(showProfileMenu = false) }
        emitMessage("msg_account_page_coming_soon", MessageTone.Info)
    }

    fun showInfoMessage(message: String) {
        emitMessage(message, MessageTone.Info)
    }

    fun showSuccessMessage(message: String) {
        emitMessage(message, MessageTone.Success)
    }

    fun showErrorMessage(message: String) {
        _state.update { it.copy(messageTone = MessageTone.Error) }
        viewModelScope.launch { _effects.send(MainUiEffect.ShowTopMessage(message, MessageTone.Error)) }
    }

    fun onTap() {
        val now = nowMillis()
        _state.update { s ->
            if (s.isPaused) return@update s

            var startTime = s.startTimeMillis
            if (s.clickCount == 0) startTime = now

            val lastClick = s.lastClickTimeMillis
            val (totalInterval, maxInterval, minInterval) = if (lastClick != 0L) {
                val interval = now - lastClick
                Triple(
                    s.totalIntervalMillis + interval,
                    max(s.maxIntervalMillis, interval),
                    min(s.minIntervalMillis, interval),
                )
            } else {
                Triple(s.totalIntervalMillis, s.maxIntervalMillis, s.minIntervalMillis)
            }

            val sequence = s.clickCount + 1
            val detail = RecordDetail(sequence = sequence, clickTime = timeFormatter.format(Date()))
            s.copy(
                clickCount = sequence,
                startTimeMillis = startTime,
                lastClickTimeMillis = now,
                totalIntervalMillis = totalInterval,
                maxIntervalMillis = maxInterval,
                minIntervalMillis = minInterval,
                tapDetails = s.tapDetails + detail,
            )
        }
    }

    fun onPauseToggle() {
        _state.update { s ->
            if (s.startTimeMillis == 0L) return@update s
            if (s.isPaused) {
                s.copy(startTimeMillis = nowMillis() - s.durationMillis, isPaused = false)
            } else {
                s.copy(isPaused = true)
            }
        }
    }

    fun onSaveClick() {
        val s = _state.value
        if (s.startTimeMillis > 0L) {
            _state.update { it.copy(isPaused = true) }
        }
        viewModelScope.launch { saveRecordsFlow() }
    }

    fun requestGoogleSignIn() {
        val s = _state.value
        if (s.loginInProgress) return
        _state.update { it.copy(loginInProgress = true) }
        viewModelScope.launch { _effects.send(MainUiEffect.StartGoogleSignIn) }
    }

    fun onGoogleSignInCancelled() {
        _state.update { it.copy(loginInProgress = false) }
    }

    fun onGoogleSignInFailed(message: String) {
        _state.update { it.copy(loginInProgress = false, messageTone = MessageTone.Error) }
        viewModelScope.launch {
            _effects.send(MainUiEffect.ShowTopMessage(message, MessageTone.Error))
        }
    }

    fun onGoogleSignInSuccess(result: GoogleSignInResult) {
        viewModelScope.launch {
            repository.loginWithGoogleIdToken(idToken = result.idToken, email = result.email)
                .onSuccess { session ->
                    AuthManager.saveSession(
                        token = session.token,
                        refreshToken = session.refreshToken,
                        expiresAtEpochSeconds = session.expiresAtEpochSeconds,
                    )
                    _state.update {
                        it.copy(
                            serverToken = session.token,
                            loggedInUserName = result.displayName ?: result.email,
                            showProfileMenu = false,
                            loginInProgress = false,
                            messageTone = MessageTone.Success,
                        )
                    }
                    _effects.send(MainUiEffect.ShowTopMessage("msg_signed_in_success", MessageTone.Success))

                    val pending = pendingSaveRequest
                    if (pending != null) {
                        pendingSaveRequest = null
                        saveRecordsAfterLogin(pending)
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(loginInProgress = false, messageTone = MessageTone.Error) }
                    _effects.send(MainUiEffect.ShowTopMessage(mapError("action_sign_in", error), MessageTone.Error))
                }
        }
    }

    fun loadPremiumPlansAndShowDialog() {
        viewModelScope.launch { loadPremiumPlans() }
    }

    fun onPremiumDismiss() {
        _state.update { it.copy(showPremiumDialog = false) }
    }

    fun onPremiumSkuSelected(skuId: String) {
        _state.update { it.copy(selectedPremiumSkuId = skuId) }
    }

    private suspend fun loadPremiumPlans() {
        fun premiumRank(planName: String, skuId: String): Int {
            val key = "$skuId $planName".lowercase(Locale.US)
            return when {
                "weekly" in key -> 0
                "monthly" in key -> 1
                "yearly" in key || "annual" in key -> 2
                else -> 99
            }
        }

        _state.update {
            it.copy(
                showPremiumDialog = true,
                premiumLoading = true,
                premiumPlans = emptyList(),
                selectedPremiumSkuId = null,
            )
        }

        repository.getSkuList()
            .onSuccess { skuResponse ->
                val ordered = skuResponse.skuList.subscription.sortedBy { premiumRank(it.skuName, it.skuId) }
                val selected = ordered.firstOrNull {
                    val key = "${it.skuId} ${it.skuName}".lowercase(Locale.US)
                    "monthly" in key
                }?.skuId ?: ordered.firstOrNull()?.skuId

                _state.update {
                    it.copy(
                        premiumPlans = ordered,
                        selectedPremiumSkuId = selected,
                    )
                }
            }
            .onFailure { error ->
                _state.update { it.copy(messageTone = MessageTone.Error) }
                _effects.send(MainUiEffect.ShowTopMessage(mapError("action_load_plans", error), MessageTone.Error))
                _state.update { it.copy(premiumPlans = emptyList(), selectedPremiumSkuId = null) }
            }

        _state.update { it.copy(premiumLoading = false) }
    }

    private suspend fun saveRecordsFlow() {
        val s = _state.value
        if (s.tapDetails.isEmpty()) {
            _state.update { it.copy(messageTone = MessageTone.Info) }
            _effects.send(MainUiEffect.ShowTopMessage("msg_no_records_to_save", MessageTone.Info))
            return
        }

        if (!s.isLoggedIn) {
            requestGoogleSignIn()
            return
        }

        val avgInterval = if (s.clickCount > 1) s.totalIntervalMillis / (s.clickCount - 1) else 0L
        val minInterval = if (s.minIntervalMillis == Long.MAX_VALUE) 0L else s.minIntervalMillis

        val request = RecordRequest(
            avgTime = (avgInterval / 1000.0).toFloat(),
            maxTime = (s.maxIntervalMillis / 1000.0).toFloat(),
            minTime = (minInterval / 1000.0).toFloat(),
            durationTime = (s.durationMillis / 1000.0).toFloat(),
            totalClick = s.clickCount,
            details = s.tapDetails,
        )

        _state.update { it.copy(isSaving = true) }
        try {
            val result = repository.saveRecords(request)
            if (result.isSuccess) {
                onRecordsSaved()
                return
            }

            val error = result.exceptionOrNull()
            if (error != null && shouldRelogin(error)) {
                pendingSaveRequest = request
                handleForbiddenAndRelogin()
                return
            }

            if (error != null && shouldShowPremium(error)) {
                loadPremiumPlans()
                return
            }

            _state.update { it.copy(messageTone = MessageTone.Error) }
            _effects.send(MainUiEffect.ShowTopMessage(mapError("action_save", error), MessageTone.Error))
        } finally {
            _state.update { it.copy(isSaving = false) }
        }
    }

    private suspend fun saveRecordsAfterLogin(request: RecordRequest) {
        val result = repository.saveRecords(request)
        if (result.isSuccess) {
            onRecordsSaved()
            return
        }

        val error = result.exceptionOrNull()
        if (error != null && shouldShowPremium(error)) {
            loadPremiumPlans()
            return
        }

        _state.update { it.copy(messageTone = MessageTone.Error) }
        _effects.send(MainUiEffect.ShowTopMessage(mapError("action_save", error), MessageTone.Error))
    }

    private suspend fun onRecordsSaved() {
        clearTapData()
        _state.update { it.copy(messageTone = MessageTone.Success) }
        _effects.send(MainUiEffect.ShowTopMessage("msg_records_saved_successfully", MessageTone.Success))
    }

    private fun clearTapData() {
        _state.update {
            it.copy(
                tapDetails = emptyList(),
                clickCount = 0,
                startTimeMillis = 0L,
                lastClickTimeMillis = 0L,
                totalIntervalMillis = 0L,
                maxIntervalMillis = 0L,
                minIntervalMillis = Long.MAX_VALUE,
                durationMillis = 0L,
                isPaused = false,
            )
        }
    }

    private suspend fun handleForbiddenAndRelogin(): Boolean {
        AuthManager.clearToken()
        _state.update {
            it.copy(
                serverToken = null,
                loggedInUserName = null,
                showProfileMenu = false,
                messageTone = MessageTone.Info,
            )
        }
        _effects.send(MainUiEffect.ShowTopMessage("msg_session_expired_sign_in_again", MessageTone.Info))
        requestGoogleSignIn()
        return false
    }

    private fun shouldRelogin(error: Throwable): Boolean {
        return (error as? ApiHttpException)?.statusCode == 403
    }

    private fun shouldShowPremium(error: Throwable): Boolean {
        return (error as? ApiHttpException)?.statusCode == 402
    }

    private fun mapError(actionKey: String, error: Throwable? = null): String {
        val detail = when {
            error is ApiHttpException && error.statusCode in 500..599 -> "error_server_unavailable"
            error is ApiHttpException -> "error_request_failed"
            error != null -> "error_network"
            else -> "error_try_again"
        }
        return "error_action_failed|$actionKey|$detail"
    }

    private fun emitMessage(key: String, tone: MessageTone) {
        _state.update { it.copy(messageTone = tone) }
        viewModelScope.launch { _effects.send(MainUiEffect.ShowTopMessage(key, tone)) }
    }
}

