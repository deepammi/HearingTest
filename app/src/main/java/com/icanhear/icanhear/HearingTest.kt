package com.icanhear.icanhear

import android.content.Context
import android.os.Environment
import android.text.TextUtils
import android.widget.Toast
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.icanhear.icanhear.models.State
import com.icanhear.icanhear.models.UserInfo
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

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
            stateChangeListener.onChanged(it, frequencies.size * 2, progress, approxStop, confirmation)
        }
    }

    fun saveResult(context: Context): String {



// Create a reference to 'images/mountains.jpg'

        val userInfo = UserInfo.instance

        val date = SimpleDateFormat("hh:mm:ss dd.MM.yyyy", Locale.getDefault()).format(Date())

        val pathToExternalStorage = Environment.getExternalStorageDirectory()
        val appDirectory = File(pathToExternalStorage.absolutePath + "/documents/HearingTest")
        appDirectory.mkdirs()

        val fileName = "HearingTest $date.txt"



        File(appDirectory, fileName).bufferedWriter().use { out ->
            if (!TextUtils.isEmpty(userInfo.name)) {
                out.write("${context.getString(R.string.name)}: ${userInfo.name}")
                out.newLine()
            }

            if (!TextUtils.isEmpty(userInfo.location)) {
                out.write("${context.getString(R.string.location)}: ${userInfo.location}")
                out.newLine()
            }

            if (!TextUtils.isEmpty(userInfo.age)) {
                out.write("${context.getString(R.string.age)}: ${userInfo.age}")
                out.newLine()
            }

            if (!TextUtils.isEmpty(userInfo.email)) {
                out.write("${context.getString(R.string.email)}: ${userInfo.email}")
                out.newLine()
            }

            if (!TextUtils.isEmpty(userInfo.phone)) {
                out.write("${context.getString(R.string.phone)}: ${userInfo.phone}")
                out.newLine()
            }

            if (!TextUtils.isEmpty(userInfo.medicalHistory)) {
                out.write("${context.getString(R.string.medical_history)}: ${userInfo.medicalHistory}")
                out.newLine()
            }

            if (!TextUtils.isEmpty(userInfo.gainFactor)){

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
        return fileName
    }
}