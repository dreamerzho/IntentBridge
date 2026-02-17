package com.intentbridge.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intentbridge.data.model.Card
import com.intentbridge.data.model.CardCategory
import com.intentbridge.data.repository.CardRepository
import com.intentbridge.service.AliyunTTSService
import com.intentbridge.service.TTSService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for config screen
 */
data class ConfigScreenState(
    val urgentCards: List<Card> = emptyList(),
    val standardCards: List<Card> = emptyList(),
    val verbCards: List<Card> = emptyList(),  // Level 1 cards (can be parents)
    val isLoading: Boolean = true,
    val isSpeaking: Boolean = false  // For voice preview state
)

/**
 * ViewModel for card configuration screen
 */
@HiltViewModel
class ConfigViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val ttsService: TTSService,
    private val aliyunTTSService: AliyunTTSService
) : ViewModel() {
    
    private val _state = MutableStateFlow(ConfigScreenState())
    val state: StateFlow<ConfigScreenState> = _state.asStateFlow()
    
    init {
        loadCards()
    }
    
    private fun loadCards() {
        viewModelScope.launch {
            combine(
                cardRepository.getUrgentCards(),
                cardRepository.getStandardCards(),
                cardRepository.getVerbCards()
            ) { urgent, standard, verbs ->
                _state.update {
                    it.copy(
                        urgentCards = urgent,
                        standardCards = standard,
                        verbCards = verbs,
                        isLoading = false
                    )
                }
            }.collect()
        }
    }
    
    /**
     * Preview voice for a card
     */
    fun previewVoice(speechText: String, speechRate: Float = 1.0f, speechPitch: Float = 1.0f) {
        if (speechText.isBlank()) return
        
        _state.update { it.copy(isSpeaking = true) }
        
        viewModelScope.launch {
            // Use Aliyun TTS if configured, otherwise use system TTS
            if (aliyunTTSService.isConfigured()) {
                aliyunTTSService.speak(text = speechText) {
                    _state.update { it.copy(isSpeaking = false) }
                }
            } else {
                ttsService.speak(
                    text = speechText,
                    flush = true,
                    speechRate = speechRate,
                    pitch = speechPitch
                )
                _state.update { it.copy(isSpeaking = false) }
            }
        }
    }
    
    /**
     * Stop current speech
     */
    fun stopSpeaking() {
        ttsService.stop()
        _state.update { it.copy(isSpeaking = false) }
    }
    
    /**
     * Add a new card with full customization
     */
    fun addCard(
        label: String,
        speechText: String,
        imagePath: String,
        category: CardCategory,
        fontSize: Int = 24,
        labelColor: String = "#FFFFFF",
        speechRate: Float = 1.0f,
        speechPitch: Float = 1.0f,
        parentId: Long? = null  // For level 2 cards
    ) {
        viewModelScope.launch {
            val displayOrder = if (category == CardCategory.URGENT) {
                _state.value.urgentCards.size + 1
            } else {
                _state.value.standardCards.size + 1
            }
            
            val card = Card(
                label = label,
                speechText = speechText,
                imagePath = imagePath,
                category = category,
                displayOrder = displayOrder,
                labelFontSize = fontSize,
                labelColor = labelColor,
                speechRate = speechRate,
                speechPitch = speechPitch,
                parentId = parentId
            )
            
            cardRepository.saveCard(card)
        }
    }
    
    /**
     * Update an existing card
     */
    fun updateCard(card: Card) {
        viewModelScope.launch {
            cardRepository.updateCard(card)
        }
    }
    
    /**
     * Delete a card
     */
    fun deleteCard(card: Card) {
        viewModelScope.launch {
            cardRepository.deleteCard(card)
        }
    }
}
