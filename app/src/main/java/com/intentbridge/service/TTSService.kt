package com.intentbridge.service

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Available voice information
 */
data class VoiceInfo(
    val id: String,
    val name: String,
    val locale: Locale,
    val isQualityHigh: Boolean
)

/**
 * Text-to-Speech service for instant voice feedback
 * Uses Android system TTS for fastest response with customization options
 */
@Singleton
class TTSService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()
    
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()
    
    private val _availableVoices = MutableStateFlow<List<VoiceInfo>>(emptyList())
    val availableVoices: StateFlow<List<VoiceInfo>> = _availableVoices.asStateFlow()
    
    // Default settings - Use child-friendly (Peppa Pig) voice by default
    private var defaultSpeechRate = 1.1f  // Slightly faster for children
    private var defaultPitch = 1.3f        // Higher pitch for child-like voice
    private var defaultVoiceId: String? = null
    
    // Peppa Pig mode - child-like voice settings
    private var peppaPigMode = true  // Default to child-like voice for ASD children
    
    /**
     * Initialize TTS engine
     */
    fun initialize() {
        if (isInitialized) return
        
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.SIMPLIFIED_CHINESE)
                if (result != TextToSpeech.LANG_MISSING_DATA && 
                    result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isInitialized = true
                    _isReady.value = true
                    
                    // Configure TTS parameters for natural speech
                    tts?.setSpeechRate(defaultSpeechRate)
                    tts?.setPitch(defaultPitch)
                    
                    // Load available voices
                    loadAvailableVoices()
                }
            }
            setupUtteranceListener()
        }
    }
    
    /**
     * Load available TTS voices
     */
    private fun loadAvailableVoices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val voices = tts?.voices ?: emptySet()
            val voiceInfoList = voices.mapNotNull { voice ->
                if (voice.locale?.language == "zh") {
                    VoiceInfo(
                        id = voice.name,
                        name = voice.name.substringAfter("."),
                        locale = voice.locale,
                        isQualityHigh = voice.quality >= Voice.QUALITY_HIGH
                    )
                } else null
            }.sortedByDescending { it.isQualityHigh }
            
            _availableVoices.value = voiceInfoList
        }
    }
    
    /**
     * Set up listener for speech completion
     */
    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }
            
            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
            }
            
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
            }
        })
    }
    
    /**
     * Speak the given text with custom settings
     * @param text Text to speak
     * @param flush Whether to interrupt any current speech
     * @param speechRate Speech rate (0.5 - 2.0)
     * @param pitch Speech pitch (0.5 - 2.0)
     * @param voiceId Custom voice ID (null = use default)
     */
    fun speak(
        text: String,
        flush: Boolean = true,
        speechRate: Float = defaultSpeechRate,
        pitch: Float = defaultPitch,
        voiceId: String? = defaultVoiceId
    ) {
        if (!isInitialized || text.isBlank()) return
        
        val utteranceId = UUID.randomUUID().toString()
        
        if (flush) {
            tts?.stop()
        }
        
        // Apply custom settings
        tts?.setSpeechRate(speechRate.coerceIn(0.5f, 2.0f))
        tts?.setPitch(pitch.coerceIn(0.5f, 2.0f))
        
        // Set voice if specified and available
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && voiceId != null) {
            val voice = tts?.voices?.find { it.name == voiceId }
            voice?.let { tts?.voice = it }
        }
        
        tts?.speak(
            text,
            if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
            null,
            utteranceId
        )
    }
    
    /**
     * Speak with default settings
     */
    fun speakDefault(text: String, flush: Boolean = true) {
        speak(text, flush, defaultSpeechRate, defaultPitch, defaultVoiceId)
    }
    
    /**
     * Set default speech rate for all utterances
     */
    fun setDefaultSpeechRate(rate: Float) {
        defaultSpeechRate = rate.coerceIn(0.5f, 2.0f)
    }
    
    /**
     * Set default pitch for all utterances
     */
    fun setDefaultPitch(pitch: Float) {
        defaultPitch = pitch.coerceIn(0.5f, 2.0f)
    }
    
    /**
     * Set default voice for all utterances
     */
    fun setDefaultVoice(voiceId: String?) {
        defaultVoiceId = voiceId
    }
    
    /**
     * Get current default settings
     */
    fun getDefaultSettings(): Triple<Float, Float, String?> {
        return Triple(defaultSpeechRate, defaultPitch, defaultVoiceId)
    }
    
    /**
     * Enable/disable Peppa Pig mode (child-like voice)
     * When enabled, uses higher pitch and slightly faster rate for child-friendly voice
     */
    fun setPeppaPigMode(enabled: Boolean) {
        peppaPigMode = enabled
        if (enabled) {
            // Apply child-like voice settings
            defaultPitch = 1.3f
            defaultSpeechRate = 1.1f
            tts?.setPitch(1.3f)
            tts?.setSpeechRate(1.1f)
        }
    }
    
    /**
     * Check if Peppa Pig mode is enabled
     */
    fun isPeppaPigMode(): Boolean = peppaPigMode
    
    /**
     * Stop any ongoing speech
     */
    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }
    
    /**
     * Release TTS resources
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        _isReady.value = false
    }
}
