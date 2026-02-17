package com.intentbridge.service

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Base64
import java.util.Properties
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Aliyun TTS Service - Uses Aliyun DashScope WebSocket API (CosyVoice)
 * Optimized version with secure API key loading and coroutine-based waiting
 */
@Singleton
class AliyunTTSService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // Load API key from local.properties (not hardcoded)
        private val apiKey: String by lazy {
            try {
                val props = Properties()
                val file = File("local.properties")
                if (file.exists()) {
                    props.load(file.inputStream())
                    props.getProperty("ALIYUN_API_KEY", "").also {
                        Log.d("AliyunTTS", "API Key loaded from local.properties")
                    }
                } else {
                    Log.w("AliyunTTS", "local.properties not found")
                    ""
                }
            } catch (e: Exception) {
                Log.e("AliyunTTS", "Failed to load API key: ${e.message}")
                ""
            }
        }
        
        // Fallback for runtime configuration
        var RUNTIME_API_KEY: String = apiKey
        
        val AVAILABLE_VOICES = listOf(
            "longanyang", "longxiaochun_v2", "xiaoxuan", "ruoxi", "aibao"
        )
        
        private const val MODEL = "cosyvoice-v3-flash"
        var DEFAULT_VOICE = "longanyang"
        private const val TAG = "AliyunTTS"
        
        // Dynamic WS URL based on API key
        private fun getWsUrl(): String = 
            "wss://dashscope.aliyuncs.com/api-ws/v1/inference?api_key=$RUNTIME_API_KEY"
    }
    
    private val wsClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private var mediaPlayer: MediaPlayer? = null
    
    fun isConfigured(): Boolean = RUNTIME_API_KEY.isNotBlank()
    
    suspend fun speak(text: String, onComplete: () -> Unit = {}) {
        if (!isConfigured()) {
            Log.e(TAG, "API Key not configured")
            onComplete()
            return
        }
        speakWithVoice(text, DEFAULT_VOICE, onComplete)
    }
    
    /**
     * Optimized speak method using suspendCancellableCoroutine instead of Thread.sleep
     */
    suspend fun speakWithVoice(text: String, voice: String, onComplete: () -> Unit = {}) {
        if (!isConfigured()) {
            onComplete()
            return
        }
        
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting WebSocket TTS: voice=$voice, text=$text")
                
                // Use suspendCancellableCoroutine for efficient waiting
                val audioData = suspendCancellableCoroutine<ByteArray?> { continuation ->
                    val audioDataList = mutableListOf<ByteArray>()
                    var synthesisComplete = false
                    
                    val request = Request.Builder()
                        .url(getWsUrl())
                        .addHeader("Authorization", "Bearer $RUNTIME_API_KEY")
                        .build()
                    
                    val webSocket = wsClient.newWebSocket(request, object : WebSocketListener() {
                        override fun onOpen(webSocket: WebSocket, response: Response) {
                            Log.d(TAG, "WebSocket connected")
                            
                            // Send synthesis task
                            val taskMsg = JSONObject().apply {
                                put("header", JSONObject().apply {
                                    put("appkey", "cosyvoice")
                                    put("task_id", "task_${UUID.randomUUID()}")
                                    put("action", "StartSynthesis")
                                })
                                put("parameter", JSONObject().apply {
                                    put("model", MODEL)
                                    put("voice", voice)
                                    put("format", "mp3")
                                    put("sample_rate", 16000)
                                    put("volume", 50)
                                    put("speech_rate", 0)
                                    put("pitch_rate", 0)
                                })
                                put("payload", JSONObject().apply {
                                    put("text", text)
                                    put("text_type", "plain")
                                    put("operation", "submit")
                                })
                            }.toString()
                            Log.d(TAG, "Sending task: $taskMsg")
                            webSocket.send(taskMsg)
                        }
                        
                        override fun onMessage(webSocket: WebSocket, text: String) {
                            Log.d(TAG, "WS Message: $text")
                            try {
                                val json = JSONObject(text)
                                val header = json.optJSONObject("header")
                                val event = header?.optString("event", "")
                                
                                when (event) {
                                    "task-started" -> {
                                        // Task started, wait for audio
                                    }
                                    "task-finished" -> {
                                        synthesisComplete = true
                                        webSocket.close(1000, "Done")
                                        if (continuation.isActive) {
                                            if (audioDataList.isNotEmpty()) {
                                                val combined = audioDataList.reduce { acc, bytes -> acc + bytes }
                                                continuation.resume(combined)
                                            } else {
                                                continuation.resume(null)
                                            }
                                        }
                                    }
                                    "stream-close" -> {
                                        synthesisComplete = true
                                        if (continuation.isActive) {
                                            continuation.resume(null)
                                        }
                                    }
                                }
                                
                                // Extract audio data from payload
                                val payload = json.optJSONObject("payload")
                                if (payload != null) {
                                    val audioData = payload.optString("audio", "")
                                    if (audioData.isNotBlank()) {
                                        try {
                                            val audioBytes = Base64.getDecoder().decode(audioData)
                                            audioDataList.add(audioBytes)
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Failed to decode audio: ${e.message}")
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Parse error: ${e.message}")
                            }
                        }
                        
                        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                            Log.e(TAG, "WebSocket error: ${t.message}")
                            if (continuation.isActive) {
                                continuation.resumeWithException(t)
                            }
                        }
                        
                        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                            Log.d(TAG, "WebSocket closed: $code $reason")
                        }
                    })
                    
                    // Clean up on cancellation
                    continuation.invokeOnCancellation {
                        webSocket.close(1000, "Canceled")
                    }
                }
                
                // Play audio with unique filename
                if (audioData != null && audioData.isNotEmpty()) {
                    playAudioData(audioData, onComplete)
                } else {
                    Log.e(TAG, "No audio data received")
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.message}")
                onComplete()
            }
        }
    }
    
    /**
     * Fixed: Use unique filename to prevent file conflicts
     */
    private fun playAudioData(audioData: ByteArray, onComplete: () -> Unit) {
        try {
            // Use unique filename with UUID to avoid conflicts
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
                tempFile.delete() // Clean up after playback
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
            Log.e(TAG, "Play error: ${e.message}")
            onComplete()
        }
    }
    
    fun updateCredentials(apiKey: String, voice: String = "ruoxi") {
        RUNTIME_API_KEY = apiKey
        DEFAULT_VOICE = voice
    }
    
    suspend fun testSpeak(text: String = "你好，我是小猪佩奇！", onComplete: () -> Unit = {}) {
        speak(text, onComplete = onComplete)
    }
}
