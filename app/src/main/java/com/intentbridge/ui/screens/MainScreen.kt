package com.intentbridge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.intentbridge.data.model.Card
import com.intentbridge.ui.components.AutoHideBubble
import com.intentbridge.ui.components.CardButton
import com.intentbridge.ui.theme.*

/**
 * Main communication screen
 * Single-layer grid layout for instant access
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToConfig: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "意图桥",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Parent mode toggle
                    IconButton(onClick = { viewModel.toggleParentMode() }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置",
                            tint = if (state.isParentMode) StandardOrange else Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StandardBlue,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundLight)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = StandardBlue
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // AI Prediction Banner (only in parent mode)
                    if (state.isParentMode && state.aiPrediction != null) {
                        AIPredictionBanner(
                            message = state.aiPrediction!!,
                            onDismiss = { viewModel.clearPrediction() }
                        )
                    }
                    
                    // Urgent Zone (top)
                    if (state.urgentCards.isNotEmpty()) {
                        UrgentZone(
                            cards = state.urgentCards,
                            onCardClick = { viewModel.onCardClick(it) }
                        )
                    }
                    
                    // Divider
                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = TextSecondary.copy(alpha = 0.3f),
                        thickness = 1.dp
                    )
                    
                    // Standard Zone (grid)
                    if (state.standardCards.isNotEmpty()) {
                        StandardZone(
                            cards = state.standardCards,
                            onCardClick = { viewModel.onCardClick(it) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // Coach Suggestion (only in parent mode)
                    if (state.isParentMode && state.coachSuggestion != null) {
                        CoachSuggestionBanner(
                            suggestion = state.coachSuggestion!!,
                            onDismiss = { viewModel.clearSuggestion() }
                        )
                    }
                }
                
                // Bubble popup overlay
                if (state.currentSpeechText != null) {
                    AutoHideBubble(
                        text = state.currentSpeechText!!,
                        showTrigger = state.showBubble,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Urgent zone with larger buttons
 */
@Composable
private fun UrgentZone(
    cards: List<Card>,
    onCardClick: (Card) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "紧急区",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = UrgentRed,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(cards) { card ->
                CardButton(
                    card = card,
                    cardColor = UrgentRed,
                    onClick = { onCardClick(card) },
                    modifier = Modifier
                        .width(160.dp)
                        .height(120.dp)
                )
            }
        }
    }
}

/**
 * Standard zone with grid layout
 */
@Composable
private fun StandardZone(
    cards: List<Card>,
    onCardClick: (Card) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "需求区",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = StandardBlue,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Determine columns based on card count
        val columns = when {
            cards.size <= 2 -> 2
            cards.size <= 4 -> 2
            else -> 3
        }
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(cards) { card ->
                val colorIndex = cards.indexOf(card) % StandardCardColors.size
                CardButton(
                    card = card,
                    cardColor = StandardCardColors[colorIndex],
                    onClick = { onCardClick(card) },
                    modifier = Modifier
                        .aspectRatio(1f)
                        .fillMaxWidth()
                )
            }
        }
    }
}

/**
 * AI Prediction banner for parents
 */
@Composable
private fun AIPredictionBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        color = StandardOrange.copy(alpha = 0.15f),
        onClick = onDismiss
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "提示",
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = StandardOrange,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Coach suggestion banner for parents
 */
@Composable
private fun CoachSuggestionBanner(
    suggestion: String,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        color = StandardGreen.copy(alpha = 0.15f),
        onClick = onDismiss
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "建议",
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "教练建议: $suggestion",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = StandardGreen,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
