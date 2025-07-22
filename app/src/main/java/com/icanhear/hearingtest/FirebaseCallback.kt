package com.icanhear.hearingtest

interface FirebaseCallback {
    fun onSuccess(message: String)
    fun onFailure(error: String)
}