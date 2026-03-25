package com.bluearcyiji.ui

import android.content.Context
import java.util.Locale

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
}

private data class AppTextEntry(
    val en: String,
    val zhCn: String,
)

private val appTextCatalog = mapOf(
    AppTextKey.DescHome to AppTextEntry("Home", "首页"),
    AppTextKey.DescProfile to AppTextEntry("Profile", "个人中心"),
    AppTextKey.Profile to AppTextEntry("Profile", "个人中心"),
    AppTextKey.Account to AppTextEntry("Account", "账户"),
    AppTextKey.Logout to AppTextEntry("Logout", "退出登录"),
    AppTextKey.Tap to AppTextEntry("TAP", "点击"),
    AppTextKey.ResetAndSave to AppTextEntry("Reset And Save", "重置并保存"),
    AppTextKey.Pause to AppTextEntry("Pause", "暂停"),
    AppTextKey.Resume to AppTextEntry("Resume", "继续"),
    AppTextKey.StatAvg to AppTextEntry("Avg", "平均"),
    AppTextKey.StatMax to AppTextEntry("Max", "最大"),
    AppTextKey.StatMin to AppTextEntry("Min", "最小"),
    AppTextKey.StatDuration to AppTextEntry("Duration", "时长"),
    AppTextKey.ActionSignIn to AppTextEntry("Sign-in", "登录"),
    AppTextKey.ActionSave to AppTextEntry("Save", "保存"),
    AppTextKey.ErrorServerUnavailable to AppTextEntry(
        "The server is unavailable right now.",
        "服务器暂时不可用。",
    ),
    AppTextKey.ErrorRequestFailed to AppTextEntry(
        "The request could not be completed.",
        "请求未能完成。",
    ),
    AppTextKey.ErrorNetwork to AppTextEntry(
        "Please check your network and try again.",
        "请检查网络后重试。",
    ),
    AppTextKey.ErrorTryAgain to AppTextEntry(
        "Please try again.",
        "请稍后重试。",
    ),
    AppTextKey.ErrorActionFailed to AppTextEntry(
        "%1\$s failed. %2\$s",
        "%1\$s失败。%2\$s",
    ),
    AppTextKey.MsgSignInUnavailable to AppTextEntry(
        "Sign-in is currently unavailable. Please try again later.",
        "暂时无法登录，请稍后再试。",
    ),
    AppTextKey.MsgSignedInSuccess to AppTextEntry(
        "Signed in successfully.",
        "登录成功。",
    ),
    AppTextKey.MsgUnsupportedSignInCredential to AppTextEntry(
        "Unsupported sign-in credential. Please try again.",
        "当前登录凭证不受支持，请重试。",
    ),
    AppTextKey.MsgSessionExpiredSignInAgain to AppTextEntry(
        "Your session has expired. Please sign in again.",
        "登录状态已过期，请重新登录。",
    ),
    AppTextKey.MsgAccountPageComingSoon to AppTextEntry(
        "Account page is coming soon.",
        "账户页面即将上线。",
    ),
    AppTextKey.MsgSignedOutSuccess to AppTextEntry(
        "Signed out successfully.",
        "已退出登录。",
    ),
    AppTextKey.MsgNoRecordsToSave to AppTextEntry(
        "No records available to save.",
        "暂无可保存的数据。",
    ),
    AppTextKey.MsgRecordsSavedSuccessfully to AppTextEntry(
        "Records saved successfully.",
        "数据保存成功。",
    ),
    AppTextKey.MsgRestoringSession to AppTextEntry(
        "Restoring your sign-in session. Please wait a moment.",
        "正在恢复登录状态，请稍候。",
    ),
    AppTextKey.MsgPremiumRequired to AppTextEntry(
        "Premium subscription is required for this action.",
        "此操作需要 Premium 订阅。",
    ),
    AppTextKey.PremiumDialogTitle to AppTextEntry(
        "Premium Plans",
        "Premium 订阅方案",
    ),
    AppTextKey.PremiumDialogLoading to AppTextEntry(
        "Loading plans...",
        "正在加载订阅方案...",
    ),
    AppTextKey.PremiumDialogNoPlans to AppTextEntry(
        "No subscription plans available right now.",
        "当前暂无可用订阅方案。",
    ),
    AppTextKey.PremiumDialogClose to AppTextEntry(
        "Close",
        "关闭",
    ),
    AppTextKey.ActionLoadPlans to AppTextEntry(
        "Load plans",
        "加载方案",
    ),
    AppTextKey.ActionLoadUserInfo to AppTextEntry(
        "Load subscription",
        "加载订阅信息",
    ),
    AppTextKey.SubscriptionStatusTitle to AppTextEntry(
        "Subscription",
        "订阅状态",
    ),
    AppTextKey.SubscriptionPlanLabel to AppTextEntry(
        "Plan: %1\$s",
        "当前方案：%1\$s",
    ),
    AppTextKey.SubscriptionExpireLabel to AppTextEntry(
        "Expires: %1\$s",
        "到期时间：%1\$s",
    ),
    AppTextKey.SubscriptionFreePlan to AppTextEntry(
        "Free",
        "免费版",
    ),
    AppTextKey.SubscriptionManage to AppTextEntry(
        "Manage",
        "管理订阅",
    ),
)

private fun Context.currentLocale(): Locale {
    val configuration = resources.configuration
    return configuration.locales[0] ?: Locale.getDefault()
}

fun Context.appText(key: AppTextKey, vararg args: Any): String {
    val locale = currentLocale()
    val entry = appTextCatalog.getValue(key)
    val template = if (locale.language.startsWith("zh")) entry.zhCn else entry.en
    return if (args.isNotEmpty()) {
        String.format(locale, template, *args)
    } else {
        template
    }
}

fun Context.formatSecondsLabel(value: Double): String {
    val locale = currentLocale()
    return if (locale.language.startsWith("zh")) {
        String.format(locale, "%.1f 秒", value)
    } else {
        String.format(locale, "%.1f s", value)
    }
}

