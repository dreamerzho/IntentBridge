package com.intentbridge.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intentbridge.data.model.Card
import com.intentbridge.data.model.CardCategory
import com.intentbridge.data.repository.CardRepository
import com.intentbridge.service.TTSService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for main communication screen
 */
data class MainScreenState(
    val urgentCards: List<Card> = emptyList(),
    val standardCards: List<Card> = emptyList(),
    val isLoading: Boolean = true,
    val currentSpeechText: String? = null,
    val showBubble: Boolean = false,
    val isParentMode: Boolean = false,
    val aiPrediction: String? = null,
    val coachSuggestion: String? = null
)

/**
 * ViewModel for main communication screen
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val ttsService: TTSService
) : ViewModel() {
    
    private val _state = MutableStateFlow(MainScreenState())
    val state: StateFlow<MainScreenState> = _state.asStateFlow()
    
    // Track water clicks for AI prediction
    private var lastWaterClickTime: Long = 0
    private val waterToToiletIntervalMs = 40 * 60 * 1000L // 40 minutes
    
    init {
        initializeTTS()
        loadCards()
    }
    
    /**
     * Initialize TTS engine
     */
    private fun initializeTTS() {
        ttsService.initialize()
    }
    
    /**
     * Load cards from database
     */
    private fun loadCards() {
        viewModelScope.launch {
            combine(
                cardRepository.getUrgentCards(),
                cardRepository.getStandardCards()
            ) { urgent, standard ->
                _state.update {
                    it.copy(
                        urgentCards = urgent,
                        standardCards = standard,
                        isLoading = false
                    )
                }
            }.collect()
        }
    }
    
    /**
     * Handle card click - speak the card's text
     */
    fun onCardClick(card: Card) {
        viewModelScope.launch {
            // Show bubble
            _state.update {
                it.copy(
                    currentSpeechText = card.speechText,
                    showBubble = true
                )
            }
            
            // Speak immediately with custom settings from card
            ttsService.speak(
                text = card.speechText,
                flush = true,
                speechRate = card.speechRate,
                pitch = card.speechPitch,
                voiceId = card.voiceId
            )
            
            // Record click for AI predictions
            cardRepository.recordCardClick(card.id)
            
            // Handle water click for AI prediction
            if (card.label.contains("喝水") || card.label.contains("水")) {
                lastWaterClickTime = System.currentTimeMillis()
                checkToiletPrediction()
            }
            
            // Generate coach suggestion
            generateCoachSuggestion(card)
            
            // Hide bubble after delay
            kotlinx.coroutines.delay(1500)
            _state.update { it.copy(showBubble = false) }
        }
    }
    
    /**
     * Check if toilet prediction should be shown
     */
    private fun checkToiletPrediction() {
        val currentTime = System.currentTimeMillis()
        if (lastWaterClickTime > 0 && 
            currentTime - lastWaterClickTime >= waterToToiletIntervalMs) {
            _state.update {
                it.copy(aiPrediction = "✨ AI预测：建议现在引导孩子如厕")
            }
        }
    }
    
    /**
     * Generate AI coach suggestion after card click
     */
    private fun generateCoachSuggestion(card: Card) {
        // Simple rule-based suggestions (MVP)
        // In production, this would call Gemini API
        val suggestion = when {
            card.label.contains("吃") -> "引导孩子说说是想吃什么口味"
            card.label.contains("看") -> "可以问孩子想看哪一集"
            card.label.contains("尿") -> "及时表扬孩子的表达"
            card.label.contains("帮帮") -> "鼓励孩子尝试自己完成"
            card.label.contains("抱抱") -> "多给孩子拥抱加强亲密感"
            else -> "肯定孩子的主动表达，继续鼓励"
        }
        
        _state.update {
            it.copy(coachSuggestion = suggestion)
        }
    }
    
    /**
     * Toggle parent mode
     */
    fun toggleParentMode() {
        _state.update {
            it.copy(isParentMode = !it.isParentMode)
        }
    }
    
    /**
     * Clear AI prediction
     */
    fun clearPrediction() {
        _state.update {
            it.copy(aiPrediction = null)
        }
    }
    
    /**
     * Clear coach suggestion
     */
    fun clearSuggestion() {
        _state.update {
            it.copy(coachSuggestion = null)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        ttsService.shutdown()
    }
}
