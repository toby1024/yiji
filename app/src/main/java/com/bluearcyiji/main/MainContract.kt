package com.bluearcyiji.main

import com.bluearcyiji.network.RecordDetail
import com.bluearcyiji.network.SkuItem
import com.bluearcyiji.ui.MessageTone

data class MainUiState(
    val loggedInUserName: String? = null,
    val billingAccountId: String? = null,
    val serverToken: String? = null,
    val showProfileMenu: Boolean = false,
    val loginInProgress: Boolean = false,
    val messageTone: MessageTone = MessageTone.Info,
    val isSaving: Boolean = false,
    val showPremiumDialog: Boolean = false,
    val premiumLoading: Boolean = false,
    val premiumPlans: List<SkuItem> = emptyList(),
    val selectedPremiumSkuId: String? = null,
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
}

public sealed interface MainUiEffect {
    data object StartGoogleSignIn : MainUiEffect
    data class ShowTopMessage(val message: String, val tone: MessageTone) : MainUiEffect
}

public final data class GoogleSignInResult(
    val idToken: String,
    val email: String,
    val displayName: String?,
)

