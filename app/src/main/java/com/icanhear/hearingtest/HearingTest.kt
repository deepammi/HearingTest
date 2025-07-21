package com.hearingtest.hearingtest

import android.content.Context
import android.os.Build
import android.os.Environment
import android.text.TextUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.hearingtest.hearingtest.models.State
import com.hearingtest.hearingtest.models.UserInfo
import com.icanhear.hearingtest.FirebaseCallback
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HearingTest(
    private val stateChangeListener: StateChangeListener
) {

    private val frequencies = arrayListOf(
        500.0f,
        1000.0f,
        2000.0f,
        3000.0f,
        4000.0f,
        6000.0f,
        8000.0f
    )

    private val dBMin = 20.0f
    private val dBMax = 90.0f
    private val dBStep = 10.0f
    private val dBAccurateStep = 5.0f

    var currentChannel = SoundGenerator.Channel.RIGHT
    var currentDb = dBMax
    var frequencyIndex = 0

    var currentState: State? = null

    var confirmation = 0

    var approxStop = 0.0f

    val rightEarResults = ArrayList<State>()
    val leftEarResults = ArrayList<State>()

    fun start() {
        approxStop = 0.0f
        frequencyIndex = 0
        currentDb = dBMax
        invalidateState()
    }

    fun end() {
        currentState?.let {
            when (currentChannel) {
                SoundGenerator.Channel.LEFT -> {
                    leftEarResults.add(it)
                }

                SoundGenerator.Channel.RIGHT -> {
                    rightEarResults.add(it)
                }
            }
        }
        notifyStateChanged()

        if (currentChannel == SoundGenerator.Channel.RIGHT) {
            currentChannel = SoundGenerator.Channel.LEFT
            start()
        } else {
            finish()
        }

    }

    private fun finish() {
        stateChangeListener.onFinish()
    }

    fun yes() {
        if (frequencyIndex >= frequencies.size) {
            end()
        } else {
            if (approxStop == currentDb) {
                confirmation += 1
                if (confirmation == 3) {
                    if (frequencyIndex + 1 < frequencies.size) {
                        next()
                    } else {
                        end()
                    }
                    return
                }
            } else {
                confirmation = 1
                approxStop = currentDb
            }

            if (currentDb > dBMin) {
                currentDb -= if (currentDb - dBStep > dBMin) {
                    dBStep
                } else {
                    dBAccurateStep
                }
                invalidateState()
            } else {
                currentDb = dBMax
                if (frequencyIndex + 1 < frequencies.size) {
                    next()
                } else {
                    end()
                }
            }
        }
    }

    fun no() {
        if (currentDb + dBAccurateStep < dBMax) {
            currentDb += dBAccurateStep
            invalidateState()
        } else {
            currentDb = dBMax
            approxStop = 0.0f
            confirmation = 0
            if (frequencyIndex + 1 >= frequencies.size) {
                end()
            } else {
                next()
            }
        }
    }

    fun next() {
        currentState?.let {
            when (currentChannel) {
                SoundGenerator.Channel.LEFT -> {
                    leftEarResults.add(it)
                }

                SoundGenerator.Channel.RIGHT -> {
                    rightEarResults.add(it)
                }
            }
        }
        approxStop = 0.0f
        currentDb = dBMax
        confirmation = 0
        frequencyIndex += 1
        invalidateState()
    }

    private fun invalidateState() {
        currentState = State(frequencies[frequencyIndex], currentDb, currentChannel)
        notifyStateChanged()
    }

    private fun notifyStateChanged() {
        currentState?.let {
            var progress = frequencyIndex + 1
            if (currentChannel == SoundGenerator.Channel.LEFT) {
                progress += frequencies.size
            }
            stateChangeListener.onChanged(
                it,
                frequencies.size * 2,
                progress,
                approxStop,
                confirmation
            )
        }
    }

   fun saveResult(context: Context, firebaseCallback: FirebaseCallback? = null): String {
        val userInfo = UserInfo.instance
        val date = SimpleDateFormat("hh:mm:ss dd.MM.yyyy", Locale.getDefault()).format(Date())

        // Always use app-specific directory under Documents — no permissions needed
        val appDirectory = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "HearingTest")
        if (!appDirectory.exists()) appDirectory.mkdirs()

        val fileName = "HearingTest $date.txt"
        val file = File(appDirectory, fileName)

        // Write data to file
        file.bufferedWriter().use { out ->
            if (!userInfo.name.isNullOrEmpty()) {
                out.write("${context.getString(R.string.name)}: ${userInfo.name}")
                out.newLine()
            }
            if (!userInfo.location.isNullOrEmpty()) {
                out.write("${context.getString(R.string.location)}: ${userInfo.location}")
                out.newLine()
            }
            if (!userInfo.age.isNullOrEmpty()) {
                out.write("${context.getString(R.string.age)}: ${userInfo.age}")
                out.newLine()
            }
            if (!userInfo.email.isNullOrEmpty()) {
                out.write("${context.getString(R.string.email)}: ${userInfo.email}")
                out.newLine()
            }
            if (!userInfo.phone.isNullOrEmpty()) {
                out.write("${context.getString(R.string.phone)}: ${userInfo.phone}")
                out.newLine()
            }
            if (!userInfo.medicalHistory.isNullOrEmpty()) {
                out.write("${context.getString(R.string.medical_history)}: ${userInfo.medicalHistory}")
                out.newLine()
            }
            if (!userInfo.gainFactor.isNullOrEmpty()) {
                out.write("${context.getString(R.string.gain_factor)}: ${userInfo.gainFactor}")
                out.newLine()
            }

            out.newLine()

            out.write("${context.getString(R.string.right_ear)}:")
            out.newLine()
            rightEarResults.forEach { result ->
                out.write(result.toString())
                out.newLine()
            }

            out.newLine()

            out.write("${context.getString(R.string.left_ear)}:")
            out.newLine()
            leftEarResults.forEach { result ->
                out.write(result.toString())
                out.newLine()
            }
        }

        // Upload to Firebase Firestore
        val db = FirebaseFirestore.getInstance()
        val testResult = hashMapOf(
            "timestamp" to date,
            "gainFactor" to userInfo.gainFactor,
            "rightEar" to rightEarResults.map { mapOf("frequency" to it.frequency, "dB" to it.dB) },
            "leftEar" to leftEarResults.map { mapOf("frequency" to it.frequency, "dB" to it.dB) }
        )

        val userDocRef = db.collection("hearing_tests").document(userInfo.phone)
        val profileUpdates = mutableMapOf<String, Any>()
        if (!userInfo.name.isNullOrBlank()) profileUpdates["name"] = userInfo.name
        if (!userInfo.location.isNullOrBlank()) profileUpdates["location"] = userInfo.location
        if (!userInfo.age.isNullOrBlank()) profileUpdates["age"] = userInfo.age
        if (!userInfo.email.isNullOrBlank()) profileUpdates["email"] = userInfo.email
        if (!userInfo.phone.isNullOrBlank()) profileUpdates["phone"] = userInfo.phone
        if (!userInfo.medicalHistory.isNullOrBlank()) profileUpdates["medicalHistory"] = userInfo.medicalHistory

        userDocRef.set(profileUpdates, SetOptions.merge())
            .addOnSuccessListener {
                userDocRef.collection("tests")
                    .add(testResult)
                    .addOnSuccessListener {
                        firebaseCallback?.onSuccess("Hearing test result has been successfully saved")
                    }
                    .addOnFailureListener { e ->
                        firebaseCallback?.onFailure("Result failed due to server issue. Please try again.: ${e.localizedMessage}")
                    }
            }
            .addOnFailureListener { e ->
                firebaseCallback?.onFailure("Connection failed: ${e.localizedMessage}")
            }

        return fileName
    }

}