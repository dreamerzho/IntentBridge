package com.intentbridge.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Card category for organizing cards into different zones
 */
enum class CardCategory {
    URGENT,    // Urgent zone - higher priority (toilet, water)
    STANDARD,  // Standard zone - regular needs
    VERB       // Level 1: Core verbs (吃, 看, 要, 不要, 去, 其他)
}

/**
 * Communication card entity for the IntentBridge app
 * Supports two-level hierarchy: Level 1 (verbs) -> Level 2 (specific items)
 */
@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = Card::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("parentId")]
)
data class Card(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,                    // Display label (e.g., "尿尿", "吃", "面包")
    val imagePath: String = "",          // Local path to card image
    val speechText: String = "",         // Text to be spoken by TTS
    val audioPath: String = "",          // Local path to pre-generated audio file (mp3)
    val category: CardCategory = CardCategory.STANDARD, // Card category
    val parentId: Long? = null,         // Parent card ID (null for top-level cards)
    val displayOrder: Int = 0,           // Order in the grid
    val clickCount: Int = 0,            // Track usage for AI predictions
    val lastClickedAt: Long = 0,        // Timestamp of last click
    
    // Customization options
    val labelFontSize: Int = 24,         // Font size for label (default 24sp)
    val labelColor: String = "#FFFFFF", // Label text color (hex)
    val cardColor: String? = null,      // Custom card background color (hex, null = use default)
    val speechRate: Float = 1.0f,       // Speech rate (0.5 - 2.0, default 1.0)
    val speechPitch: Float = 1.0f,      // Speech pitch (0.5 - 2.0, default 1.0)
    val voiceId: String? = null         // Custom voice ID (null = use system default)
) {
    /**
     * Check if this is a level-1 (verb) card
     */
    fun isVerbCard(): Boolean = parentId == null && category == CardCategory.VERB
    
    /**
     * Check if this is a level-2 (detail) card
     */
    fun isDetailCard(): Boolean = parentId != null
    
    /**
     * Check if this is an urgent card
     */
    fun isUrgentCard(): Boolean = category == CardCategory.URGENT
}
