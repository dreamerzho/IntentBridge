package com.intentbridge.service

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MiniMax TTS Service
 * Uses MiniMax Speech API for voice synthesis with voice cloning support
 */
@Singleton
class MiniMaxTTSService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // MiniMax API credentials - user needs to configure
        var API_KEY = ""  // MiniMax API Key
        var VOICE_ID = ""  // Cloned voice ID from MiniMax console
        
        // API endpoints
        private const val BASE_URL = "https://api.minimax.io"
        private const val TTS_V2_URL = "$BASE_URL/v1/t2a_v2"
        private const val VOICE_CLONE_URL = "$BASE_URL/v1/voice_clone"
        private const val FILE_UPLOAD_URL = "$BASE_URL/v1/files/upload"
        
        // Model settings
        private const val MODEL = "speech-2.6-turbo"  // Use turbo for speed
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    /**
     * Check if service is configured
     */
    fun isConfigured(): Boolean = API_KEY.isNotBlank()
    
    /**
     * Synthesize speech and play directly
     * @param text Text to synthesize
     * @param voiceId Custom voice ID (uses default if null)
     * @param onComplete Callback when playback completes
     */
    suspend fun speak(
        text: String,
        voiceId: String? = null,
        speed: Float = 1.0f,
        onComplete: () -> Unit = {}
    ) {
        if (!isConfigured()) {
            onComplete()
            return
        }
        
        withContext(Dispatchers.IO) {
            try {
                val effectiveVoiceId = voiceId ?: VOICE_ID
                if (effectiveVoiceId.isBlank()) {
                    onComplete()
                    return@withContext
                }
                
                val requestBody = JSONObject().apply {
                    put("model", MODEL)
                    put("text", text)
                    put("voice_id", effectiveVoiceId)
                    put("speed", speed)
                    put("emotion", "happy")  // Child-friendly emotion
                }.toString().toRequestBody("application/json".toMediaType())
                
                val request = Request.Builder()
                    .url(TTS_V2_URL)
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer $API_KEY")
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val json = JSONObject(response.body?.string() ?: "")
                        val audioUrl = json.optString("audio_url", "")
                        
                        if (audioUrl.isNotBlank()) {
                            downloadAndPlayAudio(audioUrl, onComplete)
                        } else {
                            // Try base64 audio from response
                            val audioBase64 = json.optString("audio", "")
                            if (audioBase64.isNotBlank()) {
                                playBase64Audio(audioBase64, onComplete)
                            } else {
                                onComplete()
                            }
                        }
                    } else {
                        // Log error for debugging
                        println("MiniMax TTS Error: ${response.code} - ${response.body?.string()}")
                        onComplete()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete()
            }
        }
    }
    
    /**
     * Download audio from URL and play
     */
    private fun downloadAndPlayAudio(audioUrl: String, onComplete: () -> Unit) {
        try {
            val request = Request.Builder()
                .url(audioUrl)
                .build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val audioData = response.body?.bytes()
                    if (audioData != null && audioData.isNotEmpty()) {
                        playAudioData(audioData, onComplete)
                    } else {
                        onComplete()
                    }
                } else {
                    onComplete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete()
        }
    }
    
    /**
     * Play base64 encoded audio
     */
    private fun playBase64Audio(base64String: String, onComplete: () -> Unit) {
        try {
            val audioData = Base64.getDecoder().decode(base64String)
            playAudioData(audioData, onComplete)
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete()
        }
    }
    
    /**
     * Play raw audio data
     */
    private fun playAudioData(audioData: ByteArray, onComplete: () -> Unit) {
        try {
            val tempFile = File(context.cacheDir, "minimax_tts_output.mp3")
            FileOutputStream(tempFile).use { it.write(audioData) }
            
            val mediaPlayer = MediaPlayer()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaPlayer.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
            }
            
            mediaPlayer.setDataSource(tempFile.absolutePath)
            mediaPlayer.setOnCompletionListener {
                it.release()
                tempFile.delete()
                onComplete()
            }
            mediaPlayer.setOnErrorListener { _, _, _ ->
                tempFile.delete()
                onComplete()
                true
            }
            mediaPlayer.prepare()
            mediaPlayer.start()
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete()
        }
    }
    
    /**
     * Update API credentials
     */
    fun updateCredentials(apiKey: String, voiceId: String = "") {
        API_KEY = apiKey
        if (voiceId.isNotBlank()) {
            VOICE_ID = voiceId
        }
    }
    
    /**
     * Test voice synthesis
     */
    suspend fun testSpeak(text: String = "你好，我是小猪佩奇！", onComplete: () -> Unit = {}) {
        speak(text, onComplete = onComplete)
    }
}
