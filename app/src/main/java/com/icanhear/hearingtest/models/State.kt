package com.icanhear.hearingtest.models

import com.icanhear.hearingtest.SoundGenerator

class State(
    val frequency: Float,
    val dB: Float,
    val channel: SoundGenerator.Channel
) {
    override fun toString(): String {
        return "$frequency Hz, $dB dB;"
    }
}