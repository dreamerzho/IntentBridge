package com.intentbridge.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intentbridge.service.TTSService
import com.intentbridge.service.VoiceInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for settings screen
 */
data class SettingsScreenState(
    val defaultSpeechRate: Float = 1.0f,
    val defaultPitch: Float = 1.0f,
    val selectedVoiceId: String? = null,
    val availableVoices: List<VoiceInfo> = emptyList(),
    val isTesting: Boolean = false
)

/**
 * ViewModel for settings screen
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val ttsService: TTSService
) : ViewModel() {
    
    private val _state = MutableStateFlow(SettingsScreenState())
    val state: StateFlow<SettingsScreenState> = _state.asStateFlow()
    
    init {
        loadSettings()
        loadAvailableVoices()
    }
    
    private fun loadSettings() {
        val (rate, pitch, voiceId) = ttsService.getDefaultSettings()
        _state.update {
            it.copy(
                defaultSpeechRate = rate,
                defaultPitch = pitch,
                selectedVoiceId = voiceId
            )
        }
    }
    
    private fun loadAvailableVoices() {
        viewModelScope.launch {
            ttsService.availableVoices.collect { voices ->
                _state.update { it.copy(availableVoices = voices) }
            }
        }
    }
    
    fun setDefaultSpeechRate(rate: Float) {
        _state.update { it.copy(defaultSpeechRate = rate) }
        ttsService.setDefaultSpeechRate(rate)
    }
    
    fun setDefaultPitch(pitch: Float) {
        _state.update { it.copy(defaultPitch = pitch) }
        ttsService.setDefaultPitch(pitch)
    }
    
    fun setDefaultVoice(voiceId: String) {
        _state.update { it.copy(selectedVoiceId = voiceId) }
        ttsService.setDefaultVoice(voiceId)
    }
    
    fun testVoice() {
        _state.update { it.copy(isTesting = true) }
        ttsService.speak(
            text = "你好，我是语音测试",
            flush = true,
            speechRate = _state.value.defaultSpeechRate,
            pitch = _state.value.defaultPitch,
            voiceId = _state.value.selectedVoiceId
        )
    }
}
