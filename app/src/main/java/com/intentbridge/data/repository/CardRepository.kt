package com.intentbridge.data.repository

import com.intentbridge.data.local.CardDao
import com.intentbridge.data.model.Card
import com.intentbridge.data.model.CardCategory
import com.intentbridge.service.AudioCacheManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing communication cards
 */
@Singleton
class CardRepository @Inject constructor(
    private val cardDao: CardDao,
    private val audioCacheManager: AudioCacheManager
) {
    
    /**
     * Get all cards ordered by category and display order
     */
    fun getAllCards(): Flow<List<Card>> = cardDao.getAllCards()
    
    /**
     * Get urgent zone cards only
     */
    fun getUrgentCards(): Flow<List<Card>> = cardDao.getUrgentCards()
    
    /**
     * Get standard zone cards only
     */
    fun getStandardCards(): Flow<List<Card>> = cardDao.getStandardCards()
    
    /**
     * Get cards by category
     */
    fun getCardsByCategory(category: CardCategory): Flow<List<Card>> = 
        cardDao.getCardsByCategory(category)
    
    /**
     * Get a single card by ID
     */
    suspend fun getCardById(id: Long): Card? = cardDao.getCardById(id)
    
    /**
     * Insert or update a card and generate audio
     */
    suspend fun saveCard(card: Card): Long {
        // Generate audio for the card
        if (card.speechText.isNotBlank()) {
            val audioPath = audioCacheManager.generateAudioForCard(card)
            if (audioPath != null) {
                val cardWithAudio = card.copy(audioPath = audioPath)
                return cardDao.insertCard(cardWithAudio)
            }
        }
        return cardDao.insertCard(card)
    }
    
    /**
     * Update an existing card and regenerate audio if speech text changed
     */
    suspend fun updateCard(card: Card) {
        // Get existing card to check if speech text changed
        val existingCard = cardDao.getCardById(card.id)
        
        if (existingCard != null && existingCard.speechText != card.speechText) {
            // Speech text changed - delete old audio and generate new
            audioCacheManager.deleteAudio(existingCard)
            
            if (card.speechText.isNotBlank()) {
                val audioPath = audioCacheManager.generateAudioForCard(card)
                if (audioPath != null) {
                    cardDao.updateCard(card.copy(audioPath = audioPath))
                    return
                }
            }
        }
        
        cardDao.updateCard(card)
    }
    
    /**
     * Delete a card
     */
    suspend fun deleteCard(card: Card) {
        // Delete cached audio
        audioCacheManager.deleteAudio(card)
        cardDao.deleteCard(card)
    }
    
    /**
     * Delete a card by ID
     */
    suspend fun deleteCardById(id: Long) {
        val card = cardDao.getCardById(id)
        if (card != null) {
            audioCacheManager.deleteAudio(card)
        }
        cardDao.deleteCardById(id)
    }
    
    /**
     * Record a card click for AI prediction
     */
    suspend fun recordCardClick(cardId: Long) {
        cardDao.incrementClickCount(cardId, System.currentTimeMillis())
    }
    
    /**
     * Get total card count
     */
    suspend fun getCardCount(): Int = cardDao.getCardCount()
    
    /**
     * Get card count by category
     */
    suspend fun getCardCountByCategory(category: CardCategory): Int = 
        cardDao.getCardCountByCategory(category)
    
    // ==================== Hierarchy Support ====================
    
    /**
     * Get level-1 verb cards
     */
    fun getVerbCards(): Flow<List<Card>> = cardDao.getVerbCards()
    
    /**
     * Get level-2 child cards for a parent
     */
    fun getChildCards(parentId: Long): Flow<List<Card>> = cardDao.getChildCards(parentId)
    
    /**
     * Get level-2 child cards synchronously
     */
    suspend fun getChildCardsSync(parentId: Long): List<Card> = cardDao.getChildCardsSync(parentId)
}
