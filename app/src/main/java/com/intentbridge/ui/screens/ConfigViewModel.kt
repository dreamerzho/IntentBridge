package com.intentbridge.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intentbridge.data.model.Card
import com.intentbridge.data.model.CardCategory
import com.intentbridge.data.repository.CardRepository
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
    val isLoading: Boolean = true
)

/**
 * ViewModel for card configuration screen
 */
@HiltViewModel
class ConfigViewModel @Inject constructor(
    private val cardRepository: CardRepository
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
        speechPitch: Float = 1.0f
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
                speechPitch = speechPitch
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
