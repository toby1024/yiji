package com.bluearcyiji.billing

import android.app.Activity
import android.content.Context
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
import com.android.billingclient.api.QueryProductDetailsParams
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class BillingManager(
    context: Context,
) : PurchasesUpdatedListener {

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

    fun connect() {
        if (billingClient.isReady) return
        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                        eventsChannel.trySend(BillingEvent.Error(result.debugMessage))
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

    fun launchSubscriptionPurchase(activity: Activity, productId: String) {
        if (!billingClient.isReady) {
            eventsChannel.trySend(BillingEvent.Error("Billing not ready"))
            return
        }
        querySubscriptionProductDetails(productId) { details ->
            if (details == null) {
                eventsChannel.trySend(BillingEvent.Error("Product not found: $productId"))
                return@querySubscriptionProductDetails
            }
            val offerToken = details.subscriptionOfferDetails
                ?.firstOrNull()
                ?.offerToken
                .orEmpty()

            val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .apply {
                    if (offerToken.isNotBlank()) {
                        setOfferToken(offerToken)
                    }
                }
                .build()

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productParams))
                .build()

            val result = billingClient.launchBillingFlow(activity, flowParams)
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                eventsChannel.trySend(BillingEvent.Error(result.debugMessage))
            }
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
                    eventsChannel.trySend(BillingEvent.Error(result.debugMessage))
                    onResult(null)
                    return@ProductDetailsResponseListener
                }
                val details = if (productDetailsList.productDetailsList.isNotEmpty()) productDetailsList.productDetailsList[0] else null
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

