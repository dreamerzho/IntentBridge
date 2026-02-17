package com.intentbridge.data.repository

import com.intentbridge.data.local.CardDao
import com.intentbridge.data.model.Card
import com.intentbridge.data.model.CardCategory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing communication cards
 */
@Singleton
class CardRepository @Inject constructor(
    private val cardDao: CardDao
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
     * Insert or update a card
     */
    suspend fun saveCard(card: Card): Long = cardDao.insertCard(card)
    
    /**
     * Update an existing card
     */
    suspend fun updateCard(card: Card) = cardDao.updateCard(card)
    
    /**
     * Delete a card
     */
    suspend fun deleteCard(card: Card) = cardDao.deleteCard(card)
    
    /**
     * Delete a card by ID
     */
    suspend fun deleteCardById(id: Long) = cardDao.deleteCardById(id)
    
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
