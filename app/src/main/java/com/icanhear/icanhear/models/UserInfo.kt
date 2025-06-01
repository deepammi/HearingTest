package com.icanhear.icanhear.models

import android.preference.PreferenceManager
import com.icanhear.icanhear.ICanHearApplication

class UserInfo {

    var name = ""
    var location = ""
    var age = ""
    var email = ""
    var phone = ""
    var medicalHistory = ""
    var gainFactor = ""

    fun save() {
        ICanHearApplication.instance.let {
            val preferences = PreferenceManager.getDefaultSharedPreferences(it)
            val editor = preferences.edit()
            editor.putString(NAME_KEY, name)
            editor.putString(LOCATION_KEY, location)
            editor.putString(AGE_KEY, age)
            editor.putString(EMAIL_KEY, email)
            editor.putString(PHONE_KEY, phone)
            editor.putString(MEDICAL_HISTORY_KEY, medicalHistory)

//            if (isGainFactorVisisble)
                editor.putString(GAIN_FACTOR, gainFactor)
//            else
//                editor.putString(GAIN_FACTOR, "")


            editor.apply()
        }
    }

    companion object {
        private const val NAME_KEY = "NAME_KEY"
        private const val LOCATION_KEY = "LOCATION_KEY"
        private const val AGE_KEY = "AGE_KEY"
        private const val EMAIL_KEY = "EMAIL_KEY"
        private const val PHONE_KEY = "PHONE_KEY"
        private const val MEDICAL_HISTORY_KEY = "MEDICAL_HISTORY_KEY"
        private const val GAIN_FACTOR = "GAIN_FACTOR"

        val instance: UserInfo
            get() {
                val userInfo = UserInfo()
                ICanHearApplication.instance.let {
                    val preferences = PreferenceManager.getDefaultSharedPreferences(it)
                    userInfo.name = preferences.getString(NAME_KEY, "") ?: ""
                    userInfo.location = preferences.getString(LOCATION_KEY, "") ?: ""
                    userInfo.age = preferences.getString(AGE_KEY, "") ?: ""
                    userInfo.email = preferences.getString(EMAIL_KEY, "") ?: ""
                    userInfo.phone = preferences.getString(PHONE_KEY, "") ?: ""
                    userInfo.medicalHistory = preferences.getString(MEDICAL_HISTORY_KEY, "") ?: ""
                    userInfo.gainFactor = preferences.getString(GAIN_FACTOR, "") ?: ""
                }
                return userInfo
            }
    }
}