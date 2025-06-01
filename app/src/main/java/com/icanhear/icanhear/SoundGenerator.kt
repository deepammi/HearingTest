package com.icanhear.icanhear

import android.media.AudioFormat.CHANNEL_OUT_STEREO
import android.media.AudioFormat.ENCODING_PCM_16BIT
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.sin

class SoundGenerator(
    private val sampleRate: Int = 30000
) {
    private fun generateTone(freqHz: Float, durationMs: Int, channel: Channel): AudioTrack {
        val count = (sampleRate * 2.0 * (durationMs / 1000.0)).toInt() and 1.inv()
        val samples = ShortArray(count)
        var i = 0
        while (i < count) {
            val sample =
                (sin(1 * Math.PI * i / (sampleRate / freqHz)) * sampleRate).toInt().toShort()
            when (channel) {
                Channel.LEFT -> {
                    samples[i + 0] = sample
                    samples[i + 1] = 0
                }
                Channel.RIGHT -> {
                    samples[i + 0] = 0
                    samples[i + 1] = sample
                }
            }
            i += 2
        }

        val track = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            CHANNEL_OUT_STEREO, ENCODING_PCM_16BIT,
            count * 2,
            AudioTrack.MODE_STATIC
        )

        track.write(samples, 0, count)

        return track
    }


    fun playSoundWithGainFactor(
        freqHz: Float,
        db: Float,
        channel: Channel,
        gainFactor: Int
    ): AudioTrack {
        val tone = generateTone(freqHz, durationMs, channel)
        val volume = gainFactor * 0.5f / 90.0f * db
        tone.setVolume(volume)

        return tone
    }

    fun playSoundWithoutGainFactor(freqHz: Float, db: Float, channel: Channel): AudioTrack {
        val tone = generateTone(freqHz, durationMs, channel)
        val volume = 0.5f / 90.0f * db
        tone.setVolume(volume)

        return tone
    }

    companion object {
        const val durationMs = 250
    }

    enum class Channel {
        LEFT, RIGHT
    }
}