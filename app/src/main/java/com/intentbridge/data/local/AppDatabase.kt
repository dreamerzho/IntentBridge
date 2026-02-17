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
    version = 2,
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
         * Two-level hierarchy: Verb cards (Level 1) -> Detail cards (Level 2)
         */
        private suspend fun populateDefaultCards(cardDao: CardDao) {
            // Level 1: Verb cards (Core expressions)
            val eatCard = Card(
                label = "吃",
                imagePath = "default_eat",
                speechText = "吃",
                category = CardCategory.VERB,
                displayOrder = 1,
                speechPitch = 1.3f  // Higher pitch for child-like voice
            )
            val watchCard = Card(
                label = "看",
                imagePath = "default_watch",
                speechText = "看",
                category = CardCategory.VERB,
                displayOrder = 2,
                speechPitch = 1.3f
            )
            val wantCard = Card(
                label = "要",
                imagePath = "default_want",
                speechText = "要",
                category = CardCategory.VERB,
                displayOrder = 3,
                speechPitch = 1.3f
            )
            val dontWantCard = Card(
                label = "不要",
                imagePath = "default_dontwant",
                speechText = "不要",
                category = CardCategory.VERB,
                displayOrder = 4,
                speechPitch = 1.3f
            )
            val goCard = Card(
                label = "去",
                imagePath = "default_go",
                speechText = "去",
                category = CardCategory.VERB,
                displayOrder = 5,
                speechPitch = 1.3f
            )
            val otherCard = Card(
                label = "其他",
                imagePath = "default_other",
                speechText = "其他",
                category = CardCategory.VERB,
                displayOrder = 6,
                speechPitch = 1.3f
            )
            
            // Insert Level 1 cards first
            val eatId = cardDao.insertCard(eatCard)
            val watchId = cardDao.insertCard(watchCard)
            val wantId = cardDao.insertCard(wantCard)
            val dontWantId = cardDao.insertCard(dontWantCard)
            val goId = cardDao.insertCard(goCard)
            val otherId = cardDao.insertCard(otherCard)
            
            // Level 2: Detail cards for "吃" (Eat)
            val eatCards = listOf(
                Card(label = "面包", imagePath = "default_bread", speechText = "妈妈，我想吃面包！", category = CardCategory.STANDARD, parentId = eatId, displayOrder = 1, speechPitch = 1.3f),
                Card(label = "饼干", imagePath = "default_cookie", speechText = "妈妈，我想吃饼干！", category = CardCategory.STANDARD, parentId = eatId, displayOrder = 2, speechPitch = 1.3f),
                Card(label = "水果", imagePath = "default_fruit", speechText = "妈妈，我想吃水果！", category = CardCategory.STANDARD, parentId = eatId, displayOrder = 3, speechPitch = 1.3f),
                Card(label = "奶酪", imagePath = "default_cheese", speechText = "妈妈，我想吃奶酪！", category = CardCategory.STANDARD, parentId = eatId, displayOrder = 4, speechPitch = 1.3f),
                Card(label = "米饭", imagePath = "default_rice", speechText = "妈妈，我想吃米饭！", category = CardCategory.STANDARD, parentId = eatId, displayOrder = 5, speechPitch = 1.3f),
                Card(label = "面条", imagePath = "default_noodle", speechText = "妈妈，我想吃面条！", category = CardCategory.STANDARD, parentId = eatId, displayOrder = 6, speechPitch = 1.3f)
            )
            
            // Level 2: Detail cards for "看" (Watch)
            val watchCards = listOf(
                Card(label = "佩奇", imagePath = "default_peppa", speechText = "妈妈，我想看小猪佩奇！", category = CardCategory.STANDARD, parentId = watchId, displayOrder = 1, speechPitch = 1.3f),
                Card(label = "汪汪队", imagePath = "default_paw", speechText = "妈妈，我想看汪汪队！", category = CardCategory.STANDARD, parentId = watchId, displayOrder = 2, speechPitch = 1.3f),
                Card(label = "动画片", imagePath = "default_cartoon", speechText = "妈妈，我想看动画片！", category = CardCategory.STANDARD, parentId = watchId, displayOrder = 3, speechPitch = 1.3f),
                Card(label = "儿歌", imagePath = "default_song", speechText = "妈妈，我想听儿歌！", category = CardCategory.STANDARD, parentId = watchId, displayOrder = 4, speechPitch = 1.3f)
            )
            
            // Level 2: Detail cards for "要" (Want)
            val wantCards = listOf(
                Card(label = "玩具", imagePath = "default_toy", speechText = "妈妈，我想要玩具！", category = CardCategory.STANDARD, parentId = wantId, displayOrder = 1, speechPitch = 1.3f),
                Card(label = "书", imagePath = "default_book", speechText = "妈妈，我想要看书！", category = CardCategory.STANDARD, parentId = wantId, displayOrder = 2, speechPitch = 1.3f),
                Card(label = "气球", imagePath = "default_balloon", speechText = "妈妈，我想要气球！", category = CardCategory.STANDARD, parentId = wantId, displayOrder = 3, speechPitch = 1.3f)
            )
            
            // Level 2: Detail cards for "不要" (Don't want)
            val dontWantCards = listOf(
                Card(label = "这个", imagePath = "default_notthis", speechText = "妈妈，我不要这个！", category = CardCategory.STANDARD, parentId = dontWantId, displayOrder = 1, speechPitch = 1.3f),
                Card(label = "不吃", imagePath = "default_noeat", speechText = "妈妈，我不要吃！", category = CardCategory.STANDARD, parentId = dontWantId, displayOrder = 2, speechPitch = 1.3f),
                Card(label = "不玩", imagePath = "default_noplay", speechText = "妈妈，我不要玩！", category = CardCategory.STANDARD, parentId = dontWantId, displayOrder = 3, speechPitch = 1.3f)
            )
            
            // Level 2: Detail cards for "去" (Go)
            val goCards = listOf(
                Card(label = "外面", imagePath = "default_outside", speechText = "妈妈，我想去外面玩！", category = CardCategory.STANDARD, parentId = goId, displayOrder = 1, speechPitch = 1.3f),
                Card(label = "公园", imagePath = "default_park", speechText = "妈妈，我想去公园！", category = CardCategory.STANDARD, parentId = goId, displayOrder = 2, speechPitch = 1.3f),
                Card(label = "商场", imagePath = "default_mall", speechText = "妈妈，我想去商场！", category = CardCategory.STANDARD, parentId = goId, displayOrder = 3, speechPitch = 1.3f),
                Card(label = "学校", imagePath = "default_school", speechText = "妈妈，我想去学校！", category = CardCategory.STANDARD, parentId = goId, displayOrder = 4, speechPitch = 1.3f)
            )
            
            // Level 2: Detail cards for "其他" (Other)
            val otherCards = listOf(
                Card(label = "抱抱", imagePath = "default_hug", speechText = "妈妈抱抱！", category = CardCategory.STANDARD, parentId = otherId, displayOrder = 1, speechPitch = 1.3f),
                Card(label = "尿尿", imagePath = "default_toilet", speechText = "妈妈，我想尿尿！", category = CardCategory.URGENT, parentId = otherId, displayOrder = 2, speechPitch = 1.3f),
                Card(label = "喝水", imagePath = "default_water", speechText = "妈妈，我渴了想喝水！", category = CardCategory.URGENT, parentId = otherId, displayOrder = 3, speechPitch = 1.3f),
                Card(label = "睡觉", imagePath = "default_sleep", speechText = "妈妈，我困了想睡觉！", category = CardCategory.STANDARD, parentId = otherId, displayOrder = 4, speechPitch = 1.3f),
                Card(label = "帮帮我", imagePath = "default_help", speechText = "妈妈，帮帮我！", category = CardCategory.STANDARD, parentId = otherId, displayOrder = 5, speechPitch = 1.3f)
            )
            
            // Insert all Level 2 cards
            cardDao.insertCards(eatCards + watchCards + wantCards + dontWantCards + goCards + otherCards)
            
            // Add urgent cards at top level (not in hierarchy)
            val urgentCards = listOf(
                Card(label = "尿尿", imagePath = "default_toilet", speechText = "妈妈，我想尿尿！", category = CardCategory.URGENT, displayOrder = 1, speechPitch = 1.3f),
                Card(label = "喝水", imagePath = "default_water", speechText = "妈妈，我渴了想喝水！", category = CardCategory.URGENT, displayOrder = 2, speechPitch = 1.3f)
            )
            cardDao.insertCards(urgentCards)
        }
    }
}
