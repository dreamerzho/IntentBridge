package com.intentbridge.service

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import com.alibaba.idst.nui.INativeStreamInputTtsCallback
import com.alibaba.idst.nui.NativeNui
import com.alibaba.idst.nui.INativeStreamInputTtsCallback.StreamInputTtsEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aliyun TTS Service - Uses Aliyun NUI SDK (Official)
 */
@Singleton
class AliyunTTSService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        var RUNTIME_API_KEY: String = ""
        
        // Use cloned voice only
        val AVAILABLE_VOICES = listOf(
            "cosyvoice-v3-plus-bailian-2f6cbcf58fa54420b4a95b2bf6510cae"
        )
        
        private const val MODEL = "cosyvoice-v3-plus"
        var DEFAULT_VOICE = "cosyvoice-v3-plus-bailian-2f6cbcf58fa54420b4a95b2bf6510cae"
        private const val TAG = "AliyunTTS"
    }
    
    private val nativeNui = NativeNui()
    private var mediaPlayer: MediaPlayer? = null
    private val isSpeaking = AtomicBoolean(false)
    
    fun isConfigured(): Boolean = RUNTIME_API_KEY.isNotBlank()
    
    suspend fun speak(text: String, onComplete: () -> Unit = {}) {
        if (!isConfigured()) {
            Log.e(TAG, "API Key not configured")
            onComplete()
            return
        }
        
        if (isSpeaking.get()) {
            Log.w(TAG, "Already speaking, ignoring request")
            onComplete()
            return
        }
        
        speakWithVoice(text, DEFAULT_VOICE, onComplete)
    }
    
    suspend fun speakWithVoice(text: String, voice: String, onComplete: () -> Unit = {}) {
        if (!isConfigured()) {
            onComplete()
            return
        }
        
        if (!isSpeaking.compareAndSet(false, true)) {
            onComplete()
            return
        }
        
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting NUI TTS: voice=$voice, text=$text")
                
                // Build ticket (connection parameters)
                val ticket = """
                {
                    "url": "wss://dashscope.aliyuncs.com/api-ws/v1/inference",
                    "apikey": "$RUNTIME_API_KEY",
                    "device_id": "intentbridge_${UUID.randomUUID()}"
                }
                """.trimIndent()
                
                // Build parameters (TTS options)
                val parameters = """
                {
                    "model": "$MODEL",
                    "voice": "$voice",
                    "format": "mp3",
                    "sample_rate": 16000,
                    "volume": 50,
                    "rate": 1.0,
                    "pitch": 1.0
                }
                """.trimIndent()
                
                // Audio data collector
                val audioDataList = mutableListOf<ByteArray>()
                val latch = CountDownLatch(1)
                var resultAudio: ByteArray? = null
                
                // Create callback
                val callback = object : INativeStreamInputTtsCallback {
                    override fun onStreamInputTtsEventCallback(
                        event: StreamInputTtsEvent?,
                        taskId: String?,
                        sessionId: String?,
                        retCode: Int,
                        errorMsg: String?,
                        timestamp: String?,
                        allResponse: String?
                    ) {
                        Log.d(TAG, "TTS Event: $event, retCode=$retCode, error=$errorMsg")
                        
                        if (event == StreamInputTtsEvent.STREAM_INPUT_TTS_EVENT_TASK_FAILED) {
                            Log.e(TAG, "TTS failed: $errorMsg")
                            isSpeaking.set(false)
                            latch.countDown()
                        } else if (event == StreamInputTtsEvent.STREAM_INPUT_TTS_EVENT_SYNTHESIS_COMPLETE) {
                            Log.d(TAG, "TTS synthesis complete")
                            resultAudio = if (audioDataList.isNotEmpty()) {
                                audioDataList.reduce { acc, bytes -> acc + bytes }
                            } else {
                                null
                            }
                            isSpeaking.set(false)
                            latch.countDown()
                        }
                    }
                    
                    override fun onStreamInputTtsDataCallback(data: ByteArray?) {
                        if (data != null && data.isNotEmpty()) {
                            audioDataList.add(data)
                            Log.d(TAG, "Audio data: ${data.size} bytes")
                        }
                    }
                }
                
                // Start TTS
                val startResult = nativeNui.startStreamInputTts(
                    callback,
                    ticket,
                    parameters,
                    "",
                    3,  // log_level: INFO
                    false // save_log
                )
                
                Log.d(TAG, "Start result: $startResult")
                
                if (startResult != 0) {
                    Log.e(TAG, "Failed to start TTS, error code: $startResult")
                    isSpeaking.set(false)
                    onComplete()
                    return@withContext
                }
                
                // Send text
                val sendResult = nativeNui.sendStreamInputTts(text)
                Log.d(TAG, "Send text result: $sendResult")
                
                if (sendResult != 0) {
                    Log.e(TAG, "Failed to send text, error code: $sendResult")
                    nativeNui.cancelStreamInputTts()
                    isSpeaking.set(false)
                    onComplete()
                    return@withContext
                }
                
                // IMPORTANT: Signal that we're done sending text
                // This is required or server will timeout waiting for more text
                nativeNui.asyncStopStreamInputTts()
                Log.d(TAG, "Called asyncStopStreamInputTts")
                
                // Wait for completion (max 30 seconds)
                val completed = latch.await(30, TimeUnit.SECONDS)
                
                if (!completed) {
                    Log.e(TAG, "TTS timeout")
                    nativeNui.cancelStreamInputTts()
                    isSpeaking.set(false)
                    onComplete()
                    return@withContext
                }
                
                // Stop TTS
                nativeNui.stopStreamInputTts()
                
                if (resultAudio != null && resultAudio!!.isNotEmpty()) {
                    Log.d(TAG, "Playing audio: ${resultAudio!!.size} bytes")
                    playAudioData(resultAudio!!, onComplete)
                } else {
                    Log.e(TAG, "No audio data received")
                    isSpeaking.set(false)
                    onComplete()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.message}")
                e.printStackTrace()
                isSpeaking.set(false)
                onComplete()
            }
        }
    }
    
    private fun playAudioData(audioData: ByteArray, onComplete: () -> Unit) {
        try {
            val tempFile = File(context.cacheDir, "tts_${UUID.randomUUID()}.mp3")
            FileOutputStream(tempFile).use { it.write(audioData) }
            
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaPlayer?.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
            }
            
            mediaPlayer?.setDataSource(tempFile.absolutePath)
            mediaPlayer?.setOnCompletionListener {
                it.release()
                tempFile.delete()
                onComplete()
            }
            mediaPlayer?.setOnErrorListener { _, _, _ ->
                tempFile.delete()
                onComplete()
                true
            }
            mediaPlayer?.prepare()
            mediaPlayer?.start()
        } catch (e: Exception) {
            Log.e(TAG, "Play: ${e.message}")
            onComplete()
        }
    }
    
    fun updateCredentials(apiKey: String, voice: String = DEFAULT_VOICE) {
        RUNTIME_API_KEY = apiKey
        DEFAULT_VOICE = voice
    }
    
    /**
     * Generate audio for given text (without playing) - used for caching
     * @return audio data as ByteArray, or null if failed
     */
    suspend fun generateAudio(text: String): ByteArray? {
        if (!isConfigured()) {
            Log.e(TAG, "API Key not configured")
            return null
        }
        
        if (!isSpeaking.compareAndSet(false, true)) {
            Log.w(TAG, "Already speaking, cannot generate")
            return null
        }
        
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Generating audio for: $text")
                
                val ticket = """
                {
                    "url": "wss://dashscope.aliyuncs.com/api-ws/v1/inference",
                    "apikey": "$RUNTIME_API_KEY",
                    "device_id": "intentbridge_cache_${UUID.randomUUID()}"
                }
                """.trimIndent()
                
                val parameters = """
                {
                    "model": "$MODEL",
                    "voice": "$DEFAULT_VOICE",
                    "format": "mp3",
                    "sample_rate": 16000,
                    "volume": 50,
                    "rate": 1.0,
                    "pitch": 1.0
                }
                """.trimIndent()
                
                val audioDataList = mutableListOf<ByteArray>()
                val latch = CountDownLatch(1)
                var resultAudio: ByteArray? = null
                
                val callback = object : INativeStreamInputTtsCallback {
                    override fun onStreamInputTtsEventCallback(
                        event: StreamInputTtsEvent?,
                        taskId: String?,
                        sessionId: String?,
                        retCode: Int,
                        errorMsg: String?,
                        timestamp: String?,
                        allResponse: String?
                    ) {
                        if (event == StreamInputTtsEvent.STREAM_INPUT_TTS_EVENT_TASK_FAILED) {
                            Log.e(TAG, "TTS failed: $errorMsg")
                            isSpeaking.set(false)
                            latch.countDown()
                        } else if (event == StreamInputTtsEvent.STREAM_INPUT_TTS_EVENT_SYNTHESIS_COMPLETE) {
                            resultAudio = if (audioDataList.isNotEmpty()) {
                                audioDataList.reduce { acc, bytes -> acc + bytes }
                            } else null
                            isSpeaking.set(false)
                            latch.countDown()
                        }
                    }
                    
                    override fun onStreamInputTtsDataCallback(data: ByteArray?) {
                        if (data != null && data.isNotEmpty()) {
                            audioDataList.add(data)
                        }
                    }
                }
                
                val startResult = nativeNui.startStreamInputTts(
                    callback, ticket, parameters, "", 3, false
                )
                
                if (startResult != 0) {
                    Log.e(TAG, "Failed to start TTS: $startResult")
                    isSpeaking.set(false)
                    return@withContext null
                }
                
                val sendResult = nativeNui.sendStreamInputTts(text)
                if (sendResult != 0) {
                    Log.e(TAG, "Failed to send text: $sendResult")
                    nativeNui.cancelStreamInputTts()
                    isSpeaking.set(false)
                    return@withContext null
                }
                
                nativeNui.asyncStopStreamInputTts()
                
                val completed = latch.await(30, TimeUnit.SECONDS)
                if (!completed) {
                    nativeNui.cancelStreamInputTts()
                    isSpeaking.set(false)
                    return@withContext null
                }
                
                nativeNui.stopStreamInputTts()
                resultAudio
            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.message}")
                isSpeaking.set(false)
                null
            }
        }
    }
    
    suspend fun testSpeak(text: String = "你好", onComplete: () -> Unit = {}) {
        speak(text, onComplete = onComplete)
    }
}
