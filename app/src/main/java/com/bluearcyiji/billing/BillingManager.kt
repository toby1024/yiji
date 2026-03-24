package com.bluearcyiji.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.QueryProductDetailsParams
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class BillingManager(
    context: Context,
    private val obfuscatedAccountIdProvider: (() -> String?)? = null,
) : PurchasesUpdatedListener {
    companion object {
        private const val TAG = "BillingManager"
    }

    private val eventsChannel = Channel<BillingEvent>(capacity = Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                // 订阅购买必需；同时开启一次性商品以兼容未来扩展
                .enablePrepaidPlans()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    private val productDetailsCache = LinkedHashMap<String, ProductDetails>()

    private fun billingMessage(prefix: String, result: BillingResult): String {
        val detail = when (result.responseCode) {
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE ->
                "Billing Unavailable: device/account/store does not support Google Play Billing"
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE ->
                "Service Unavailable: Google Play service is temporarily unavailable"
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED ->
                "Service Disconnected: billing service connection dropped"
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE ->
                "Item Unavailable: product is not available for this app/account/region"
            BillingClient.BillingResponseCode.DEVELOPER_ERROR ->
                "Developer Error: appId/signature/product type configuration mismatch"
            else -> result.debugMessage
        }
        return "$prefix (code=${result.responseCode}): $detail"
    }

    fun connect() {
        if (billingClient.isReady) return
        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                        eventsChannel.trySend(BillingEvent.Error(billingMessage("Billing setup failed", result)))
                        return
                    }

                    val featureResult = billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
                    if (featureResult.responseCode != BillingClient.BillingResponseCode.OK) {
                        eventsChannel.trySend(
                            BillingEvent.Error(
                                billingMessage("Subscriptions not supported on this device/account", featureResult)
                            )
                        )
                    }
                }

                override fun onBillingServiceDisconnected() {
                    // Billing 底层会自动重连；这里不主动重试，避免循环
                }
            }
        )
    }

    fun disconnect() {
        if (!billingClient.isReady) return
        billingClient.endConnection()
    }

    fun launchSubscriptionPurchase(
        activity: Activity,
        productId: String,
        obfuscatedExternalAccountId: String? = null,
        previousProductId: String? = null,
        changeMode: SubscriptionChangeMode? = null,
    ) {
        if (!billingClient.isReady) {
            eventsChannel.trySend(BillingEvent.Error("Billing not ready"))
            Log.w(TAG, "launchSubscriptionPurchase aborted: billing client not ready")
            return
        }
        querySubscriptionProductDetails(productId) { details ->
            if (details == null) {
                eventsChannel.trySend(BillingEvent.Error("Product not found: $productId"))
                Log.e(TAG, "Product details not found for productId=$productId")
                return@querySubscriptionProductDetails
            }
            val offerToken = details.subscriptionOfferDetails
                ?.firstOrNull()
                ?.offerToken
                .orEmpty()
            if (offerToken.isBlank()) {
                Log.w(
                    TAG,
                    "No offer token for productId=$productId, subscriptionOfferDetails=${details.subscriptionOfferDetails}"
                )
            }

            val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .apply {
                    if (offerToken.isNotBlank()) {
                        setOfferToken(offerToken)
                    }
                }
                .build()

            val flowParamsBuilder = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productParams))

            // Prioritize per-call value, then fallback to global provider.
            val normalizedObfuscatedAccountId = obfuscatedExternalAccountId
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: obfuscatedAccountIdProvider
                    ?.invoke()
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }

            if (normalizedObfuscatedAccountId != null) {
                flowParamsBuilder.setObfuscatedAccountId(normalizedObfuscatedAccountId)
            }

            queryActiveSubscription(previousProductId) { activePurchase ->
                if (activePurchase != null && previousProductId != productId) {
                    val replacementMode = when (changeMode) {
                        SubscriptionChangeMode.UPGRADE ->
                            BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITH_TIME_PRORATION

                        SubscriptionChangeMode.DOWNGRADE ->
                            BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.DEFERRED

                        null ->
                            BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITH_TIME_PRORATION
                    }
                    val updateParams = BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                        .setOldPurchaseToken(activePurchase.purchaseToken)
                        .setSubscriptionReplacementMode(replacementMode)
                        .build()
                    flowParamsBuilder.setSubscriptionUpdateParams(updateParams)
                }

                val flowParams = flowParamsBuilder.build()
                val result = billingClient.launchBillingFlow(activity, flowParams)
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    eventsChannel.trySend(BillingEvent.Error(result.debugMessage))
                    Log.e(
                        TAG,
                        "launchBillingFlow failed: code=${result.responseCode}, message=${result.debugMessage}"
                    )
                }
            }
        }
    }

    private fun queryActiveSubscription(previousProductId: String?, onResult: (Purchase?) -> Unit) {
        if (previousProductId.isNullOrBlank()) {
            onResult(null)
            return
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                val msg = billingMessage("queryPurchasesAsync failed", result)
                eventsChannel.trySend(BillingEvent.Error(msg))
                Log.e(TAG, "$msg, previousProductId=$previousProductId")
                onResult(null)
                return@queryPurchasesAsync
            }

            val purchased = purchases.orEmpty().filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            val matched = purchased.firstOrNull { it.products.contains(previousProductId) } ?: purchased.firstOrNull()
            onResult(matched)
        }
    }

    private fun querySubscriptionProductDetails(
        productId: String,
        onResult: (ProductDetails?) -> Unit,
    ) {
        productDetailsCache[productId]?.let {
            onResult(it)
            return
        }

        val featureResult = billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        if (featureResult.responseCode != BillingClient.BillingResponseCode.OK) {
            val msg = billingMessage("Cannot query subscription product", featureResult)
            eventsChannel.trySend(BillingEvent.Error(msg))
            Log.e(TAG, "$msg, productId=$productId")
            onResult(null)
            return
        }

        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(productId)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        billingClient.queryProductDetailsAsync(
            params,
            ProductDetailsResponseListener { result, productDetailsList ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    val msg = billingMessage("queryProductDetailsAsync failed", result)
                    eventsChannel.trySend(BillingEvent.Error(msg))
                    Log.e(
                        TAG,
                        "$msg for productId=$productId"
                    )
                    onResult(null)
                    return@ProductDetailsResponseListener
                }
                val fetchedList = productDetailsList.productDetailsList
                Log.i(
                    TAG,
                    "queryProductDetailsAsync success for productId=$productId, fetched=${fetchedList.map { it.productId }}, unfetched=${productDetailsList.unfetchedProductList}"
                )
                val details = fetchedList.firstOrNull()
                if (details != null) {
                    productDetailsCache[productId] = details
                }
                onResult(details)
            }
        )
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val list = purchases.orEmpty()
                if (list.isEmpty()) {
                    eventsChannel.trySend(BillingEvent.Error("Empty purchase result"))
                    return
                }
                list.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        acknowledgeIfNeeded(purchase)
                    }
                }
                eventsChannel.trySend(BillingEvent.PurchaseSuccess(list))
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                eventsChannel.trySend(BillingEvent.UserCancelled)
            }

            else -> {
                eventsChannel.trySend(BillingEvent.Error(result.debugMessage))
            }
        }
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                eventsChannel.trySend(BillingEvent.Error("Acknowledge failed: ${result.debugMessage}"))
            }
        }
    }
}

sealed interface BillingEvent {
    data class PurchaseSuccess(val purchases: List<Purchase>) : BillingEvent
    data object UserCancelled : BillingEvent
    data class Error(val message: String) : BillingEvent
}

enum class SubscriptionChangeMode {
    UPGRADE,
    DOWNGRADE,
}

