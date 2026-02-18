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
 * Communication card button optimized for ASD children
 * - Large touch area
 * - Clear visual feedback
 * - Simple one-tap interaction
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
    
    // Scale animation for press feedback - gentle bounce
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
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
            cardColor.copy(alpha = 0.85f)
        )
        CardCategory.VERB -> listOf(
            StandardPurple,
            StandardPurple.copy(alpha = 0.85f)
        )
    }
    
    Card(
        modifier = modifier
            .fillMaxSize()
            .scale(scale)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = true, color = Color.White.copy(alpha = 0.4f)),
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(20.dp),  // Rounder corners for friendly feel
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (card.category == CardCategory.URGENT) 10.dp else 6.dp
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
            // Icon/Emoji at top (larger for children)
            val emoji = getCardEmoji(card.imagePath)
            if (emoji != null) {
                Text(
                    text = emoji,
                    fontSize = 48.sp,  // Large emoji
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-16).dp)
                )
            } else if (card.imagePath.isNotBlank()) {
                // Fallback to image if no emoji
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(getImageResource(card.imagePath))
                        .crossfade(true)
                        .build(),
                    contentDescription = card.label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
            
            // Label text at bottom - larger font for readability
            val customLabelColor = try {
                Color(android.graphics.Color.parseColor(card.labelColor))
            } catch (e: Exception) {
                TextOnDark
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(vertical = 12.dp),  // More padding
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = card.label,
                    color = customLabelColor,
                    fontSize = 22.sp,  // Larger font for children
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

/**
 * Get emoji for card - more intuitive for children
 */
private fun getCardEmoji(imagePath: String): String? {
    if (!imagePath.startsWith("default_")) return null
    
    return when (imagePath) {
        // Level 1: Verb cards (一级功能卡片) - 使用孩子熟悉的emoji
        "default_eat" -> "🍽️"     // 吃 - 餐具
        "default_watch" -> "👀"    // 看 - 眼睛
        "default_want" -> "🙋"     // 要 - 举手
        "default_dontwant" -> "🙅"  // 不要 - 拒绝
        "default_go" -> "🏃"       // 去 - 跑
        "default_other" -> "✨"     // 其他 - 星星
        
        // Level 2: Detail cards (二级卡片)
        "default_toilet" -> "🚽"   // 尿尿
        "default_water" -> "💧"    // 喝水
        "default_bread" -> "🍞"    // 面包
        "default_peppa" -> "🐷"    // 佩奇
        "default_help" -> "🆘"     // 帮助
        "default_play" -> "🎮"    // 玩
        "default_hug" -> "🤗"     // 抱抱
        "default_sleep" -> "😴"    // 睡觉
        
        // Food
        "default_cookie" -> "🍪"   // 饼干
        "default_fruit" -> "🍎"   // 水果
        "default_cheese" -> "🧀"  // 奶酪
        "default_rice" -> "🍚"    // 米饭
        "default_noodle" -> "🍜"   // 面条
        
        // Entertainment
        "default_paw" -> "🐕"     // 汪汪队
        "default_cartoon" -> "📺"  // 动画片
        "default_song" -> "🎵"    // 儿歌
        "default_toy" -> "🧸"     // 玩具
        "default_book" -> "📚"    // 书
        "default_balloon" -> "🎈"  // 气球
        
        // Negation
        "default_notthis" -> "❌"  // 这个不要
        "default_noeat" -> "🚫"   // 不吃
        "default_noplay" -> "⏹️"  // 不玩
        
        // Places
        "default_outside" -> "🌳"  // 外面
        "default_park" -> "🎠"    // 公园
        "default_mall" -> "🏬"    // 商场
        "default_school" -> "🏫"   // 学校
        
        else -> null
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
            when (imagePath) {
                // Level 1: Verb cards
                "default_eat" -> android.R.drawable.ic_menu_edit
                "default_watch" -> android.R.drawable.ic_menu_view
                "default_want" -> android.R.drawable.ic_input_add
                "default_dontwant" -> android.R.drawable.ic_delete
                "default_go" -> android.R.drawable.ic_menu_directions
                "default_other" -> android.R.drawable.ic_menu_more
                
                // Level 2: Detail cards
                "default_toilet" -> android.R.drawable.ic_menu_compass
                "default_water" -> android.R.drawable.ic_menu_myplaces
                "default_bread" -> android.R.drawable.ic_menu_agenda
                "default_peppa" -> android.R.drawable.ic_menu_gallery
                "default_help" -> android.R.drawable.ic_menu_help
                "default_play" -> android.R.drawable.ic_menu_view
                "default_hug" -> android.R.drawable.ic_menu_my_calendar
                "default_sleep" -> android.R.drawable.ic_menu_recent_history
                
                // Other cards
                "default_cookie" -> android.R.drawable.ic_menu_agenda
                "default_fruit" -> android.R.drawable.ic_menu_myplaces
                "default_cheese" -> android.R.drawable.ic_menu_agenda
                "default_rice" -> android.R.drawable.ic_menu_agenda
                "default_noodle" -> android.R.drawable.ic_menu_agenda
                "default_paw" -> android.R.drawable.ic_menu_gallery
                "default_cartoon" -> android.R.drawable.ic_menu_gallery
                "default_song" -> android.R.drawable.ic_media_play
                "default_toy" -> android.R.drawable.ic_menu_view
                "default_book" -> android.R.drawable.ic_menu_agenda
                "default_balloon" -> android.R.drawable.ic_menu_myplaces
                "default_notthis" -> android.R.drawable.ic_delete
                "default_noeat" -> android.R.drawable.ic_delete
                "default_noplay" -> android.R.drawable.ic_delete
                "default_outside" -> android.R.drawable.ic_menu_directions
                "default_park" -> android.R.drawable.ic_menu_directions
                "default_mall" -> android.R.drawable.ic_menu_directions
                "default_school" -> android.R.drawable.ic_menu_directions
                
                else -> android.R.drawable.ic_menu_gallery
            }
        }
        else -> imagePath
    }
}
