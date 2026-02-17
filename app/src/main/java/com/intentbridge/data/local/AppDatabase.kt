package com.intentbridge.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.intentbridge.data.model.Card
import com.intentbridge.data.model.CardCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Room database for IntentBridge app
 */
@Database(
    entities = [Card::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun cardDao(): CardDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "intentbridge_database"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * Callback to prepopulate database with default cards
         */
        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDefaultCards(database.cardDao())
                    }
                }
            }
        }
        
        /**
         * Populate database with default communication cards
         */
        private suspend fun populateDefaultCards(cardDao: CardDao) {
            val defaultCards = listOf(
                // Urgent zone cards
                Card(
                    label = "尿尿",
                    imagePath = "default_toilet",
                    speechText = "妈妈，我想尿尿！",
                    category = CardCategory.URGENT,
                    displayOrder = 1
                ),
                Card(
                    label = "喝水",
                    imagePath = "default_water",
                    speechText = "妈妈，我渴了，想喝水！",
                    category = CardCategory.URGENT,
                    displayOrder = 2
                ),
                // Standard zone cards
                Card(
                    label = "吃面包",
                    imagePath = "default_bread",
                    speechText = "妈妈，我饿了，我想吃面包！",
                    category = CardCategory.STANDARD,
                    displayOrder = 1
                ),
                Card(
                    label = "看佩奇",
                    imagePath = "default_peppa",
                    speechText = "妈妈，我想看小猪佩奇！",
                    category = CardCategory.STANDARD,
                    displayOrder = 2
                ),
                Card(
                    label = "帮帮我",
                    imagePath = "default_help",
                    speechText = "妈妈，帮帮我！",
                    category = CardCategory.STANDARD,
                    displayOrder = 3
                ),
                Card(
                    label = "出去玩",
                    imagePath = "default_play",
                    speechText = "妈妈，我想出去玩！",
                    category = CardCategory.STANDARD,
                    displayOrder = 4
                ),
                Card(
                    label = "抱抱",
                    imagePath = "default_hug",
                    speechText = "妈妈抱抱！",
                    category = CardCategory.STANDARD,
                    displayOrder = 5
                ),
                Card(
                    label = "睡觉",
                    imagePath = "default_sleep",
                    speechText = "妈妈，我困了想睡觉！",
                    category = CardCategory.STANDARD,
                    displayOrder = 6
                )
            )
            cardDao.insertCards(defaultCards)
        }
    }
}
