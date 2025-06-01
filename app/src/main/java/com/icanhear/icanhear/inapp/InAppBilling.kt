package com.icanhear.icanhear.inapp

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*


class InAppBilling() : PurchasesUpdatedListener
{
//Done by jatinder Updated billling library to latest

    private lateinit var billingClient: BillingClient
    private  var skuDetailLst:List<SkuDetails>?=null
    private val skuList = listOf("test_product_one","test_icanhear","test_two","test_two2")



    private fun allowMultiplePurchases(purchases: MutableList<Purchase>?) {
        val purchase = purchases?.first()
        if (purchase != null)
        {

            val consumeParams =
                ConsumeParams.newBuilder()
                    .setPurchaseToken(purchase.getPurchaseToken())
                    .build()
            billingClient.consumeAsync(
                consumeParams
            ) { billingResult, s ->
                //do something..
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && s != null)
                {
                    println("AllowMultiplePurchases success, responseCode: ${billingResult.responseCode}")
                }
                else
                {
                    println("Can't allowMultiplePurchases, responseCode: ${billingResult.responseCode}")
                }
            }

        }
    }


     fun setupBillingClient(context: Context)
     {



        billingClient = BillingClient.newBuilder(context)
            .setListener(this).enablePendingPurchases()
            .build()


        billingClient.startConnection(object : BillingClientStateListener
        {


            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
//                Toast.makeText(requireContext(), "Discooneected",Toast.LENGTH_LONG).show()

            }

            override fun onBillingSetupFinished(p0: BillingResult)
            {
                Log.e("Jatin",p0.responseCode.toString())
                if (p0.responseCode == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is setup successfully
                    loadAllSKUs()
//                    GlobalScope.launch {
//                        querySkuDetails()
//                    }


                }
            }
        })

    }

    private fun loadAllSKUs() = if (billingClient.isReady) {
        val params = SkuDetailsParams
            .newBuilder()
            .setSkusList(skuList)
            .setType(BillingClient.SkuType.INAPP)
            .build()
        billingClient.querySkuDetailsAsync(params) { responseCode, skuDetailsList ->

            // Process the result.
            if (responseCode.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList!!.isNotEmpty()) {
                skuDetailLst = skuDetailsList
//                for (skuDetails in skuDetailsList) {
//                    //this will return both the SKUs from Google Play Console
//                    if (skuDetails.sku == "test_two")
//                        binding.supportUs.setOnClickListener {
//                            val billingFlowParams = BillingFlowParams
//                                .newBuilder()
//                                .setSkuDetails(skuDetails)
//                                .build()
//                            billingClient.launchBillingFlow(context, billingFlowParams)
//                        }
//                }
            }else{
//                Toast.makeText(requireContext(),"not",Toast.LENGTH_LONG).show()
            }

        }

    } else {
        println("Billing Client not ready")
    }

    fun loadBilling(context: Activity)
    {
        if (skuDetailLst != null)
        {
            for (skuDetails in skuDetailLst!!)
            {
                if (skuDetails.sku == "test_two2")
                {
                    val billingFlowParams = BillingFlowParams
                        .newBuilder()
                        .setSkuDetails(skuDetails)
                        .build()
                    billingClient.launchBillingFlow(context, billingFlowParams)
                }

            }
            }else{

        }
        }

    override fun onPurchasesUpdated(p0: BillingResult, p1: MutableList<Purchase>?) {
        if (p0.responseCode == BillingClient.BillingResponseCode.OK && p1 != null) {
//            for (purchase in purchases) {

//                acknowledgePurchase(purchase.purchaseToken)
            allowMultiplePurchases(p1)
//
//            }
        } else if (p0.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.


        } else {
            // Handle any other error codes.
        }
    }


}

