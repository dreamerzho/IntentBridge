package com.intentbridge.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intentbridge.data.model.Card
import com.intentbridge.data.model.CardCategory
import com.intentbridge.data.repository.CardRepository
import com.intentbridge.service.AliyunTTSService
import com.intentbridge.service.AudioCacheManager
import com.intentbridge.service.TTSService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for main communication screen
 */
data class MainScreenState(
    val urgentCards: List<Card> = emptyList(),       // Top-level urgent cards
    val verbCards: List<Card> = emptyList(),          // Level 1: Verb cards (吃, 看, 要, 不要, 去, 其他)
    val selectedVerbCard: Card? = null,              // Currently selected verb
    val detailCards: List<Card> = emptyList(),        // Level 2: Detail cards for selected verb
    val isLoading: Boolean = true,
    val currentSpeechText: String? = null,
    val showBubble: Boolean = false,
    val isParentMode: Boolean = false,
    val aiPrediction: String? = null,
    val coachSuggestion: String? = null,
    val peppaPigMode: Boolean = true,               // Child-like voice mode
    val useAliyunTTS: Boolean = true               // Use Aliyun TTS API (voice cloning)
)

/**
 * ViewModel for main communication screen with two-level hierarchy
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val ttsService: TTSService,
    private val aliyunTTSService: AliyunTTSService,
    private val audioCacheManager: AudioCacheManager
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
        ttsService.setPeppaPigMode(true)
    }
    
    /**
     * Load cards from database
     */
    private fun loadCards() {
        viewModelScope.launch {
            // Load urgent cards (top level)
            combine(
                cardRepository.getUrgentCards(),
                cardRepository.getVerbCards()
            ) { urgent, verbs ->
                _state.update {
                    it.copy(
                        urgentCards = urgent,
                        verbCards = verbs,
                        isLoading = false,
                        peppaPigMode = ttsService.isPeppaPigMode()
                    )
                }
            }.collect()
        }
    }
    
    /**
     * Handle urgent card click - speak immediately
     */
    fun onUrgentCardClick(card: Card) {
        viewModelScope.launch {
            // 始终使用阿里云TTS
            speakWithAliyun(card)
            
            // Record click for AI predictions
            cardRepository.recordCardClick(card.id)
            
            // Handle water click for AI prediction
            if (card.label.contains("喝水") || card.label.contains("水")) {
                lastWaterClickTime = System.currentTimeMillis()
                checkToiletPrediction()
            }
            
            // Generate coach suggestion
            generateCoachSuggestion(card)
        }
    }
    
    /**
     * Handle verb card click - navigate to level 2 (detail cards)
     */
    fun onVerbCardClick(card: Card) {
        viewModelScope.launch {
            // Immediately load and navigate to detail view
            val detailCards = cardRepository.getChildCardsSync(card.id)
            _state.update {
                it.copy(
                    selectedVerbCard = card,
                    detailCards = detailCards,
                    currentSpeechText = card.speechText,
                    showBubble = true
                )
            }
            
            // Record click
            cardRepository.recordCardClick(card.id)
            
            // Speak using Aliyun TTS
            speakWithAliyun(card)
            
            // Hide bubble after short delay
            kotlinx.coroutines.delay(800)
            _state.update { it.copy(showBubble = false) }
        }
    }
    
    /**
     * Handle detail card click - speak the complete phrase
     */
    fun onDetailCardClick(card: Card) {
        viewModelScope.launch {
            val speechText = card.speechText
            
            // Show bubble and speak
            _state.update { it.copy(currentSpeechText = speechText, showBubble = true) }
            
            // 始终使用阿里云TTS
            speakWithAliyunText(speechText)
            
            // Record click
            cardRepository.recordCardClick(card.id)
            
            // Generate coach suggestion
            generateCoachSuggestion(card)
            
            // Hide bubble after delay
            kotlinx.coroutines.delay(1500)
            _state.update { it.copy(showBubble = false) }
        }
    }
    
    /**
     * Speak using Aliyun TTS service - try cache first, then streaming
     */
    private suspend fun speakWithAliyun(card: Card) {
        if (aliyunTTSService.isConfigured()) {
            // Try to play from local cache first for instant response
            audioCacheManager.playCardAudio(card) {}
        }
    }
    
    /**
     * Speak text using Aliyun TTS service - try cache first, then streaming
     */
    private suspend fun speakWithAliyunText(text: String) {
        if (aliyunTTSService.isConfigured()) {
            // Try to play from cache by searching for matching card
            // For now, just use streaming TTS for dynamic text
            aliyunTTSService.speak(text = text) {}
        }
    }
    
    /**
     * Go back from detail view to verb list
     */
    fun onBackToVerbs() {
        _state.update {
            it.copy(
                selectedVerbCard = null,
                detailCards = emptyList()
            )
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
                it.copy(aiPrediction = "AI预测：建议现在引导孩子如厕")
            }
        }
    }
    
    /**
     * Generate AI coach suggestion after card click
     */
    private fun generateCoachSuggestion(card: Card) {
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
     * Toggle Peppa Pig mode (child-like voice)
     */
    fun togglePeppaPigMode() {
        val newMode = !_state.value.peppaPigMode
        ttsService.setPeppaPigMode(newMode)
        _state.update { it.copy(peppaPigMode = newMode) }
    }
    
    /**
     * Clear AI prediction
     */
    fun clearPrediction() {
        _state.update { it.copy(aiPrediction = null) }
    }
    
    /**
     * Clear coach suggestion
     */
    fun clearSuggestion() {
        _state.update { it.copy(coachSuggestion = null) }
    }
    
    override fun onCleared() {
        super.onCleared()
        ttsService.shutdown()
    }
}
