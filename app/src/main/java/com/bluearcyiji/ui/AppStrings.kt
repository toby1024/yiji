package com.bluearcyiji.ui

import android.content.Context

enum class AppTextKey {
    DescHome,
    DescProfile,
    Profile,
    Account,
    Logout,
    Tap,
    ResetAndSave,
    Pause,
    Resume,
    StatAvg,
    StatMax,
    StatMin,
    StatDuration,
    ActionSignIn,
    ActionSave,
    ErrorServerUnavailable,
    ErrorRequestFailed,
    ErrorNetwork,
    ErrorTryAgain,
    ErrorActionFailed,
    MsgSignInUnavailable,
    MsgSignedInSuccess,
    MsgUnsupportedSignInCredential,
    MsgSessionExpiredSignInAgain,
    MsgAccountPageComingSoon,
    MsgSignedOutSuccess,
    MsgNoRecordsToSave,
    MsgRecordsSavedSuccessfully,
    MsgRestoringSession,
    MsgPremiumRequired,
    PremiumDialogTitle,
    PremiumDialogLoading,
    PremiumDialogNoPlans,
    PremiumDialogClose,
    ActionLoadPlans,
    ActionLoadUserInfo,
    SubscriptionStatusTitle,
    SubscriptionPlanLabel,
    SubscriptionExpireLabel,
    SubscriptionFreePlan,
    SubscriptionManage,
    HelpCenter,
    MsgHelpCenterComingSoon,
    Records,
}

private val appTextCatalog = mapOf(
    AppTextKey.DescHome to "Home",
    AppTextKey.DescProfile to "Profile",
    AppTextKey.Profile to "Profile",
    AppTextKey.Account to "Account",
    AppTextKey.Logout to "Logout",
    AppTextKey.Tap to "TAP",
    AppTextKey.ResetAndSave to "Reset And Save",
    AppTextKey.Pause to "Pause",
    AppTextKey.Resume to "Resume",
    AppTextKey.StatAvg to "Avg",
    AppTextKey.StatMax to "Max",
    AppTextKey.StatMin to "Min",
    AppTextKey.StatDuration to "Duration",
    AppTextKey.ActionSignIn to "Sign-in",
    AppTextKey.ActionSave to "Save",
    AppTextKey.ErrorServerUnavailable to "The server is unavailable right now.",
    AppTextKey.ErrorRequestFailed to "The request could not be completed.",
    AppTextKey.ErrorNetwork to "Please check your network and try again.",
    AppTextKey.ErrorTryAgain to "Please try again.",
    AppTextKey.ErrorActionFailed to "%1\$s failed. %2\$s",
    AppTextKey.MsgSignInUnavailable to "Sign-in is currently unavailable. Please try again later.",
    AppTextKey.MsgSignedInSuccess to "Signed in successfully.",
    AppTextKey.MsgUnsupportedSignInCredential to "Unsupported sign-in credential. Please try again.",
    AppTextKey.MsgSessionExpiredSignInAgain to "Your session has expired. Please sign in again.",
    AppTextKey.MsgAccountPageComingSoon to "Account page is coming soon.",
    AppTextKey.MsgSignedOutSuccess to "Signed out successfully.",
    AppTextKey.MsgNoRecordsToSave to "No records available to save.",
    AppTextKey.MsgRecordsSavedSuccessfully to "Records saved successfully.",
    AppTextKey.MsgRestoringSession to "Restoring your sign-in session. Please wait a moment.",
    AppTextKey.MsgPremiumRequired to "Premium subscription is required for this action.",
    AppTextKey.PremiumDialogTitle to "Premium Plans",
    AppTextKey.PremiumDialogLoading to "Loading plans...",
    AppTextKey.PremiumDialogNoPlans to "No subscription plans available right now.",
    AppTextKey.PremiumDialogClose to "Close",
    AppTextKey.ActionLoadPlans to "Load plans",
    AppTextKey.ActionLoadUserInfo to "Load subscription",
    AppTextKey.SubscriptionStatusTitle to "Subscription",
    AppTextKey.SubscriptionPlanLabel to "Plan: %1\$s",
    AppTextKey.SubscriptionExpireLabel to "Expires: %1\$s",
    AppTextKey.SubscriptionFreePlan to "Free",
    AppTextKey.SubscriptionManage to "Manage",
    AppTextKey.HelpCenter to "Help Center",
    AppTextKey.MsgHelpCenterComingSoon to "Help Center is coming soon.",
    AppTextKey.Records to "Records",
)

fun Context.appText(key: AppTextKey, vararg args: Any): String {
    val template = appTextCatalog.getValue(key)
    return if (args.isNotEmpty()) {
        String.format(template, *args)
    } else {
        template
    }
}

fun Context.formatSecondsLabel(value: Double): String {
    return String.format("%.1f s", value)
}
