package com.bluearcyiji.main

import com.bluearcyiji.network.RecordDetail
import com.bluearcyiji.network.RecordHistoryItem
import com.bluearcyiji.network.SkuItem
import com.bluearcyiji.ui.MessageTone

data class RecordsUiState(
    val loading: Boolean = false,
    val items: List<RecordHistoryItem> = emptyList(),
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val loadingMore: Boolean = false,
    val error: String? = null,
) {
    val isLastPage: Boolean get() = totalPages == 0 || currentPage + 1 >= totalPages
}

data class MainUiState(
    val loggedInUserName: String? = null,
    val billingAccountId: String? = null,
    val serverToken: String? = null,
    val isRestoringSession: Boolean = false,
    val premiumInfo: String? = null,
    val premiumExpireTimeEpochSeconds: Long? = null,
    val currentSubscriptionSkuId: String? = null,
    /** Google Play base plan ID of the active subscription (e.g. "monthly"). */
    val currentSubscriptionBasePlanId: String? = null,
    val showProfileMenu: Boolean = false,
    val showAccountScreen: Boolean = false,
    val showDeleteAccountDialog: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val showRecordsScreen: Boolean = false,
    val recordsState: RecordsUiState = RecordsUiState(),
    val loginInProgress: Boolean = false,
    val messageTone: MessageTone = MessageTone.Info,
    val isSaving: Boolean = false,
    val showPremiumDialog: Boolean = false,
    val premiumDialogShowSubtitle: Boolean = false,
    val premiumLoading: Boolean = false,
    val premiumPlans: List<SkuItem> = emptyList(),
    val selectedPremiumSkuId: String? = null,
    /** Google Play base plan ID of the plan the user has highlighted in the dialog. */
    val selectedPremiumBasePlanId: String? = null,
    val clickCount: Int = 0,
    val startTimeMillis: Long = 0L,
    val lastClickTimeMillis: Long = 0L,
    val totalIntervalMillis: Long = 0L,
    val maxIntervalMillis: Long = 0L,
    val minIntervalMillis: Long = Long.MAX_VALUE,
    val durationMillis: Long = 0L,
    val isPaused: Boolean = false,
    val tapDetails: List<RecordDetail> = emptyList(),
) {
    val isLoggedIn: Boolean get() = loggedInUserName != null || !serverToken.isNullOrBlank()
    val isPremium: Boolean get() = !premiumInfo.isNullOrBlank() && !premiumInfo.equals("free", ignoreCase = true)
}

sealed interface MainUiEffect {
    data object StartGoogleSignIn : MainUiEffect
    data class ShowTopMessage(val message: String, val tone: MessageTone) : MainUiEffect
}

data class GoogleSignInResult(
    val idToken: String,
    val email: String,
    val displayName: String?,
)

