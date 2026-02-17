package com.intentbridge.service

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aliyun TTS Service - Uses Aliyun DashScope WebSocket API (CosyVoice)
 */
@Singleton
class AliyunTTSService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        var API_KEY = "sk-2008310cca764a0e873906b5de8a1a04"
        
        val AVAILABLE_VOICES = listOf(
            "longanyang", "longxiaochun_v2", "xiaoxuan", "ruoxi", "aibao"
        )
        
        private const val MODEL = "cosyvoice-v3-flash"
        var DEFAULT_VOICE = "longanyang"
        private const val TAG = "AliyunTTS"
        
        private const val WS_URL = "wss://dashscope.aliyuncs.com/api-ws/v1/inference?api_key=sk-2008310cca764a0e873906b5de8a1a04"
    }
    
    private val wsClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private var mediaPlayer: MediaPlayer? = null
    
    fun isConfigured(): Boolean = API_KEY.isNotBlank()
    
    suspend fun speak(text: String, onComplete: () -> Unit = {}) {
        if (!isConfigured()) {
            Log.e(TAG, "API Key not configured")
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
        
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting WebSocket TTS: voice=$voice, text=$text")
                
                val audioDataList = mutableListOf<ByteArray>()
                var synthesisComplete = false
                var errorMessage: String? = null
                
                val request = Request.Builder()
                    .url(WS_URL)
                    .addHeader("Authorization", "Bearer $API_KEY")
                    .build()
                
                val webSocket = wsClient.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        Log.d(TAG, "WebSocket connected, response: $response")
                        // Send authentication with task request
                        val taskMsg = JSONObject().apply {
                            put("header", JSONObject().apply {
                                put("appkey", "cosyvoice")
                                put("task_id", "task_${System.currentTimeMillis()}")
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
                    
                    override fun onMessage(webSocket: WebSocket, bytes: String) {
                        // Handle binary messages if any - for now we only handle JSON text
                        Log.d(TAG, "WS Text Message received: $bytes")
                        try {
                            val json = JSONObject(bytes)
                            val header = json.optJSONObject("header")
                            val event = header?.optString("event", "")
                            
                            when (event) {
                                "task-started" -> {
                                    val receivedTaskId = header.optString("task_id", "")
                                    // Send synthesis request
                                    val taskMsg = JSONObject().apply {
                                        put("header", JSONObject().apply {
                                            put("appkey", "cosyvoice")
                                            put("task_id", receivedTaskId)
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
                                    webSocket.send(taskMsg)
                                }
                                "task-finished" -> {
                                    synthesisComplete = true
                                    webSocket.close(1000, "Done")
                                }
                                "stream-close" -> {
                                    synthesisComplete = true
                                }
                            }
                            
                            // Check for audio data in payload
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
                        errorMessage = t.message
                        synthesisComplete = true
                    }
                    
                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        Log.d(TAG, "WebSocket closed: $code $reason")
                        synthesisComplete = true
                    }
                })
                
                // Wait for synthesis to complete (with timeout)
                var waitCount = 0
                while (!synthesisComplete && waitCount < 60) {
                    Thread.sleep(100)
                    waitCount++
                }
                
                webSocket.close(1000, "Cancelled by user")
                
                // Play audio
                if (audioDataList.isNotEmpty()) {
                    val combinedAudio = audioDataList.reduce { acc, bytes ->
                        acc + bytes
                    }
                    playAudioData(combinedAudio, onComplete)
                } else {
                    Log.e(TAG, "No audio data received: $errorMessage")
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.message}")
                onComplete()
            }
        }
    }
    
    private fun playAudioData(audioData: ByteArray, onComplete: () -> Unit) {
        try {
            val tempFile = File(context.cacheDir, "aliyun_tts_output.mp3")
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
            Log.e(TAG, "Play error: ${e.message}")
            onComplete()
        }
    }
    
    fun updateCredentials(apiKey: String, voice: String = "ruoxi") {
        API_KEY = apiKey
        DEFAULT_VOICE = voice
    }
    
    suspend fun testSpeak(text: String = "你好，我是小猪佩奇！", onComplete: () -> Unit = {}) {
        speak(text, onComplete = onComplete)
    }
}
