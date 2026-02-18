package com.intentbridge.service

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.intentbridge.data.model.Card
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages audio cache for cards - pre-generates and stores TTS audio files locally
 */
@Singleton
class AudioCacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ttsService: AliyunTTSService
) {
    companion object {
        private const val TAG = "AudioCache"
        private const val AUDIO_DIR = "card_audio"
    }
    
    private val audioDir: File by lazy {
        File(context.filesDir, AUDIO_DIR).also { it.mkdirs() }
    }
    
    private var mediaPlayer: MediaPlayer? = null
    
    /**
     * Check if audio exists for a card
     */
    fun hasAudio(card: Card): Boolean {
        return card.audioPath.isNotBlank() && File(card.audioPath).exists()
    }
    
    /**
     * Generate and cache audio for a card
     * @return the path to the cached audio file, or null if failed
     */
    suspend fun generateAudioForCard(card: Card): String? {
        if (card.speechText.isBlank()) {
            Log.w(TAG, "No speech text for card: ${card.label}")
            return null
        }
        
        if (!ttsService.isConfigured()) {
            Log.w(TAG, "TTS not configured")
            return null
        }
        
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Generating audio for card: ${card.label}")
                
                val audioData = ttsService.generateAudio(card.speechText)
                
                if (audioData != null && audioData.isNotEmpty()) {
                    // Save to local file
                    val fileName = "card_${card.id}_${UUID.randomUUID()}.mp3"
                    val audioFile = File(audioDir, fileName)
                    
                    FileOutputStream(audioFile).use { it.write(audioData) }
                    Log.d(TAG, "Audio saved: ${audioFile.absolutePath}")
                    
                    audioFile.absolutePath
                } else {
                    Log.e(TAG, "Failed to generate audio for: ${card.label}")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating audio: ${e.message}")
                null
            }
        }
    }
    
    /**
     * Play audio for a card - first try local cache, then generate on-the-fly
     */
    suspend fun playCardAudio(card: Card, onComplete: () -> Unit = {}) {
        // Try local cache first
        if (hasAudio(card)) {
            Log.d(TAG, "Playing from cache: ${card.audioPath}")
            playFromPath(card.audioPath, onComplete)
            return
        }
        
        // Generate on-the-fly if no cache
        Log.d(TAG, "No cache, generating on-the-fly for: ${card.label}")
        val audioPath = generateAudioForCard(card)
        
        if (audioPath != null) {
            playFromPath(audioPath, onComplete)
        } else {
            // Fallback to streaming TTS
            Log.d(TAG, "Falling back to streaming TTS")
            ttsService.speak(card.speechText, onComplete)
        }
    }
    
    /**
     * Play audio from local file path
     */
    private fun playFromPath(path: String, onComplete: () -> Unit) {
        try {
            val file = File(path)
            if (!file.exists()) {
                Log.e(TAG, "Audio file not found: $path")
                onComplete()
                return
            }
            
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer()
            
            mediaPlayer?.setDataSource(path)
            mediaPlayer?.setOnCompletionListener {
                it.release()
                onComplete()
            }
            mediaPlayer?.setOnErrorListener { _, _, _ ->
                onComplete()
                true
            }
            mediaPlayer?.prepare()
            mediaPlayer?.start()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio: ${e.message}")
            onComplete()
        }
    }
    
    /**
     * Delete cached audio for a card
     */
    fun deleteAudio(card: Card) {
        if (card.audioPath.isNotBlank()) {
            try {
                File(card.audioPath).delete()
                Log.d(TAG, "Deleted audio: ${card.audioPath}")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting audio: ${e.message}")
            }
        }
    }
    
    /**
     * Delete all cached audio files
     */
    fun clearAllCache() {
        try {
            audioDir.listFiles()?.forEach { it.delete() }
            Log.d(TAG, "Cleared all audio cache")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache: ${e.message}")
        }
    }
    
    /**
     * Get total cache size in bytes
     */
    fun getCacheSize(): Long {
        return audioDir.listFiles()?.sumOf { it.length() } ?: 0L
    }
}
