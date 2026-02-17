package com.intentbridge.data.local

import androidx.room.TypeConverter
import com.intentbridge.data.model.CardCategory

/**
 * Type converters for Room database
 */
class Converters {
    
    @TypeConverter
    fun fromCardCategory(category: CardCategory): String {
        return category.name
    }
    
    @TypeConverter
    fun toCardCategory(value: String): CardCategory {
        return CardCategory.valueOf(value)
    }
}
