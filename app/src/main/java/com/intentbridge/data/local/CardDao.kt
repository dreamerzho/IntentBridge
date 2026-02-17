package com.intentbridge.data.local

import androidx.room.*
import com.intentbridge.data.model.Card
import com.intentbridge.data.model.CardCategory
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Card entity
 */
@Dao
interface CardDao {
    
    @Query("SELECT * FROM cards ORDER BY category DESC, displayOrder ASC")
    fun getAllCards(): Flow<List<Card>>
    
    @Query("SELECT * FROM cards WHERE category = :category ORDER BY displayOrder ASC")
    fun getCardsByCategory(category: CardCategory): Flow<List<Card>>
    
    @Query("SELECT * FROM cards WHERE category = 'URGENT' ORDER BY displayOrder ASC")
    fun getUrgentCards(): Flow<List<Card>>
    
    @Query("SELECT * FROM cards WHERE category = 'STANDARD' ORDER BY displayOrder ASC")
    fun getStandardCards(): Flow<List<Card>>
    
    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getCardById(id: Long): Card?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: Card): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<Card>)
    
    @Update
    suspend fun updateCard(card: Card)
    
    @Delete
    suspend fun deleteCard(card: Card)
    
    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun deleteCardById(id: Long)
    
    @Query("UPDATE cards SET clickCount = clickCount + 1, lastClickedAt = :timestamp WHERE id = :id")
    suspend fun incrementClickCount(id: Long, timestamp: Long)
    
    @Query("SELECT * FROM cards WHERE label = :label LIMIT 1")
    suspend fun getCardByLabel(label: String): Card?
    
    @Query("SELECT COUNT(*) FROM cards")
    suspend fun getCardCount(): Int
    
    @Query("SELECT COUNT(*) FROM cards WHERE category = :category")
    suspend fun getCardCountByCategory(category: CardCategory): Int
}
