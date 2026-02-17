package com.intentbridge.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.intentbridge.data.model.Card
import com.intentbridge.data.model.CardCategory
import com.intentbridge.ui.theme.*

/**
 * Communication card button with long-press support
 * Long press (0.2s) to trigger, prevents accidental touches
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardButton(
    card: Card,
    cardColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit = onClick,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Scale animation for press feedback
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "card_scale"
    )
    
    // Background gradient based on category
    val backgroundGradient = when (card.category) {
        CardCategory.URGENT -> listOf(
            UrgentRed,
            UrgentRedDark
        )
        CardCategory.STANDARD -> listOf(
            cardColor,
            cardColor.copy(alpha = 0.8f)
        )
        CardCategory.VERB -> listOf(
            StandardPurple,
            StandardPurple.copy(alpha = 0.8f)
        )
    }
    
    Card(
        modifier = modifier
            .fillMaxSize()
            .scale(scale)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = true, color = Color.White.copy(alpha = 0.3f)),
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (card.category == CardCategory.URGENT) 8.dp else 4.dp
        ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(colors = backgroundGradient)
                )
        ) {
            // Image or icon background
            if (card.imagePath.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(getImageResource(card.imagePath))
                        .crossfade(true)
                        .build(),
                    contentDescription = card.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                
                // Dark overlay for text readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.6f)
                                )
                            )
                        )
                )
            }
            
            // Label text at bottom
            val customLabelColor = try {
                Color(android.graphics.Color.parseColor(card.labelColor))
            } catch (e: Exception) {
                TextOnDark
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = card.label,
                    color = customLabelColor,
                    fontSize = card.labelFontSize.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

/**
 * Get image resource from various sources
 */
private fun getImageResource(imagePath: String): Any {
    // For default images, use drawable resources
    // For custom images, use file path
    return when {
        imagePath.startsWith("default_") -> {
            // Map to drawable resource
            when (imagePath) {
                "default_toilet" -> android.R.drawable.ic_menu_compass
                "default_water" -> android.R.drawable.ic_menu_myplaces
                "default_bread" -> android.R.drawable.ic_menu_agenda
                "default_peppa" -> android.R.drawable.ic_menu_gallery
                "default_help" -> android.R.drawable.ic_menu_help
                "default_play" -> android.R.drawable.ic_menu_view
                "default_hug" -> android.R.drawable.ic_menu_my_calendar
                "default_sleep" -> android.R.drawable.ic_menu_recent_history
                else -> android.R.drawable.ic_menu_gallery
            }
        }
        else -> imagePath // File path for custom images
    }
}
