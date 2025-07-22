package com.hearingtest.hearingtest

import android.app.Application
import com.google.firebase.FirebaseApp


//import com.hearingtest.hearingtest.paypalconfig.Config.Companion.config
//import com.paypal.android.sdk.payments.PayPalConfiguration
//import com.paypal.android.sdk.payments.PayPalService

open class ICanHearApplication : Application() {

    companion object {
        @get:Synchronized
        lateinit var instance: ICanHearApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        FirebaseApp.initializeApp(this)




        //start paypal service
        payPal();

    }


    fun payPal(){

//        val config = PayPalConfiguration()
//            .environment(PayPalConfiguration.ENVIRONMENT_SANDBOX)
//            .clientId(Config.PAYPAL_CLIENT_ID)

        //start paypal service
//        val intent = Intent(this, PayPalService::class.java)
//        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config)
//        startService(intent)
    }
}