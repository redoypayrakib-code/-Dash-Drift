package com.dashdrift.game

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.PendingPurchasesParams

class MainActivity : Activity(), PurchasesUpdatedListener {
    private lateinit var web: WebView
    private lateinit var billing: BillingClient
    private val productIds = listOf("coins_101", "coins_201", "coins_301", "coins_401", "coins_501", "coins_551")
    private val coinAmounts = mapOf("coins_101" to 100, "coins_201" to 200, "coins_301" to 300, "coins_401" to 400, "coins_501" to 500, "coins_551" to 550)
    private val productDetails = mutableMapOf<String, ProductDetails>()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.webChromeClient = WebChromeClient()
        web.webViewClient = WebViewClient()
        web.addJavascriptInterface(BillingBridge(), "AndroidBilling")
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")

        billing = BillingClient.newBuilder(this)
            .setListener(this)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection()
            .build()
        connectBilling()
    }

    private fun connectBilling() {
        billing.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    loadProducts()
                    restorePurchases()
                } else showMessage("Google Play Billing connection failed: ${result.debugMessage}")
            }
            override fun onBillingServiceDisconnected() { /* auto reconnection enabled */ }
        })
    }

    private fun loadProducts() {
        if (!::billing.isInitialized || !billing.isReady) return
        val products = productIds.map { id ->
            QueryProductDetailsParams.Product.newBuilder().setProductId(id).setProductType(BillingClient.ProductType.INAPP).build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(products).build()
        billing.queryProductDetailsAsync(params) { result, queryResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails.clear()
                queryResult.productDetailsList.forEach { productDetails[it.productId] = it }
                showMessage(if (productDetails.isEmpty()) "Coin products are not configured in Play Console yet." else "Google Play products loaded. Prices shown by Google Play at checkout.")
            } else showMessage("Could not load products: ${result.debugMessage}")
        }
    }

    private fun restorePurchases() {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        billing.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }.forEach { consumeAndGrant(it) }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases?.forEach { purchase ->
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) consumeAndGrant(purchase)
                else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) showMessage("Payment is pending. Coins will arrive after Google confirms payment.")
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> showMessage("Purchase cancelled.")
            else -> showMessage("Purchase failed: ${result.debugMessage}")
        }
    }

    private fun consumeAndGrant(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        val productId = purchase.products.firstOrNull() ?: return
        val amount = coinAmounts[productId] ?: return
        val prefs = getSharedPreferences("purchase_ledger", MODE_PRIVATE)
        val key = purchase.purchaseToken.hashCode().toString()
        if (prefs.getBoolean(key, false)) return
        val params = ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        billing.consumeAsync(params) { result, _ ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                prefs.edit().putBoolean(key, true).apply()
                runOnUiThread { web.evaluateJavascript("window.onNativeCoinsAdded && window.onNativeCoinsAdded($amount)", null) }
            } else showMessage("Payment completed but coin delivery needs retry: ${result.debugMessage}")
        }
    }

    private fun showMessage(message: String) {
        if (::web.isInitialized) runOnUiThread {
            val safe = message.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ")
            web.evaluateJavascript("window.onBillingMessage && window.onBillingMessage('$safe')", null)
        }
    }

    inner class BillingBridge {
        @JavascriptInterface fun loadProducts() { runOnUiThread { loadProducts() } }
        @JavascriptInterface fun purchase(productId: String) {
            runOnUiThread {
                val details = productDetails[productId]
                if (details == null) { showMessage("This coin package is not available yet. Configure it in Play Console."); loadProducts(); return@runOnUiThread }
                val productParams = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build()
                val flow = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(productParams)).build()
                billing.launchBillingFlow(this@MainActivity, flow)
            }
        }
    }

    override fun onDestroy() {
        if (::billing.isInitialized) billing.endConnection()
        web.destroy()
        super.onDestroy()
    }
}
