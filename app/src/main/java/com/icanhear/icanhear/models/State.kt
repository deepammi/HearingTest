package com.icanhear.icanhear.models

import com.icanhear.icanhear.SoundGenerator

class State(
    val frequency: Float,
    val dB: Float,
    val channel: SoundGenerator.Channel
) {
    override fun toString(): String {
        return "$frequency Hz, $dB dB;"
    }
}