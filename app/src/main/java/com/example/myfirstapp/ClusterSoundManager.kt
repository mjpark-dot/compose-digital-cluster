package com.example.myfirstapp

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.SoundPool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.div
import kotlin.math.exp
import kotlin.math.sin
import kotlin.times

class ClusterSoundManager{

    companion object {
        private const val SAMPLE_RATE = 44100
    }

    private val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

    private val audioFormat =  AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

    private fun generateRelayBuffer(frequency: Double): ShortArray {
        val durationMs = 26
        val numSamples = (SAMPLE_RATE * durationMs) / 1000
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val time = i.toDouble() / SAMPLE_RATE
            val decay = exp(-28.0 * i / numSamples)
            val angle = 2.0 * Math.PI * frequency * time
            val wave = (sin(angle) * 0.65) + ((Math.random() - 0.5) * 0.35)
            buffer[i] = (wave * decay * Short.MAX_VALUE * 0.55).toInt().toShort()
        }
        return buffer
    }


    private val tickBuffer by lazy { generateRelayBuffer(1350.0) }
    private val tockBuffer by lazy { generateRelayBuffer(920.0) }

    fun playTick() {
        playStaticBuffer(tickBuffer)
    }

    fun playTok() {
        playStaticBuffer(tockBuffer)
    }

    // ─────────────────────────────────────────────────────────────
    // [2] 3화음 상승 시동 웰컴 차임벨 (G4 392Hz -> C5 523Hz -> E5 659Hz)
    // ─────────────────────────────────────────────────────────────
    suspend fun playWelcomeChime() = withContext(Dispatchers.Default) {
        try {
            val notes = listOf(392.0, 523.25, 659.25)
            val noteDurationMs = 260
            val totalSamples = (SAMPLE_RATE * (noteDurationMs * notes.size) / 1000)
            val chimeBuffer = ShortArray(totalSamples)

            var sampleIdx = 0
            for (freq in notes) {
                val samplesPerNote = (SAMPLE_RATE * noteDurationMs) / 1000
                for (i in 0 until samplesPerNote) {
                    val time = i.toDouble() / SAMPLE_RATE
                    val envelope = Math.exp(-3.0 * i / samplesPerNote)
                    val angle = 2.0 * Math.PI * freq * time
                    val sample = (sin(angle) * envelope * Short.MAX_VALUE * 0.75).toInt().toShort()

                    if (sampleIdx < totalSamples) {
                        chimeBuffer[sampleIdx++] = sample
                    }
                }
            }

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(chimeBuffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(chimeBuffer, 0, chimeBuffer.size)
            audioTrack.play()

            // 재생 후 리소스 해제
            delay((noteDurationMs * notes.size).toLong() + 250L)
            audioTrack.stop()
            audioTrack.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playStaticBuffer(buffer: ShortArray) {
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()

            track.setNotificationMarkerPosition(buffer.size)
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(track: AudioTrack?) {
                    track?.stop()
                    track?.release()

                }

                override fun onPeriodicNotification(track: AudioTrack?) {}
            })
            } catch (e: Exception) {
                e.printStackTrace()
        }
    }

    fun release() {
        // 단발 트랙들은 마커 리스너를 통해 자동 해제됨
    }
}