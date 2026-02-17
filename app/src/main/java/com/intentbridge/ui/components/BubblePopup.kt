package com.intentbridge.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intentbridge.ui.theme.BubbleBackground
import com.intentbridge.ui.theme.BubbleText
import kotlinx.coroutines.delay

/**
 * Visual feedback bubble that appears above the card when clicked
 * Shows the speech text that will be spoken
 */
@Composable
fun BubblePopup(
    text: String,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    // Animation for the bubble
    val alphaAnimation = animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "bubble_alpha"
    )
    
    // Scale animation for bounce effect
    val scaleAnimation = animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bubble_scale"
    )
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(150)) + 
                slideInVertically(
                    animationSpec = tween(150),
                    initialOffsetY = { it / 2 }
                ),
        exit = fadeOut(animationSpec = tween(100)) + 
               slideOutVertically(
                   animationSpec = tween(100),
                   targetOffsetY = { it / 2 }
               ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(BubbleBackground)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = text,
                    color = BubbleText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
            }
        }
    }
}

/**
 * Auto-hide wrapper for BubblePopup
 * Shows the bubble for a short duration then hides it
 */
@Composable
fun AutoHideBubble(
    text: String,
    showTrigger: Boolean,
    durationMs: Long = 1500,
    modifier: Modifier = Modifier
) {
    var showBubble by remember { mutableStateOf(false) }
    
    LaunchedEffect(showTrigger) {
        if (showTrigger) {
            showBubble = true
            delay(durationMs)
            showBubble = false
        }
    }
    
    BubblePopup(
        text = text,
        visible = showBubble,
        modifier = modifier
    )
}
