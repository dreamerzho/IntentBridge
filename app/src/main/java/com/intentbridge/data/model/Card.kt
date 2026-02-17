package com.intentbridge.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Card category for organizing cards into different zones
 */
enum class CardCategory {
    URGENT,    // Urgent zone - higher priority (toilet, water)
    STANDARD   // Standard zone - regular needs
}

/**
 * Communication card entity for the IntentBridge app
 * Represents a single card in the communication grid
 */
@Entity(tableName = "cards")
data class Card(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,                    // Display label (e.g., "尿尿", "吃面包")
    val imagePath: String,               // Local path to card image
    val speechText: String,              // Text to be spoken by TTS
    val category: CardCategory,          // Card category (URGENT or STANDARD)
    val displayOrder: Int = 0,           // Order in the grid
    val clickCount: Int = 0,             // Track usage for AI predictions
    val lastClickedAt: Long = 0,         // Timestamp of last click
    
    // Customization options
    val labelFontSize: Int = 24,         // Font size for label (default 24sp)
    val labelColor: String = "#FFFFFF",  // Label text color (hex)
    val cardColor: String? = null,       // Custom card background color (hex, null = use default)
    val speechRate: Float = 1.0f,        // Speech rate (0.5 - 2.0, default 1.0)
    val speechPitch: Float = 1.0f,       // Speech pitch (0.5 - 2.0, default 1.0)
    val voiceId: String? = null          // Custom voice ID (null = use system default)
)
