package com.intentbridge.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Person
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
 * Main communication screen with two-level hierarchy
 * Level 1: Verb cards (吃, 看, 要, 不要, 去, 其他)
 * Level 2: Detail cards for each verb
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToConfig: () -> Unit,
    onNavigateToEditCard: (Long) -> Unit,
    onNavigateToEditPage: (Boolean, Long?) -> Unit,  // (isLevel2, parentCardId)
    viewModel: MainViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.selectedVerbCard != null) {
                            state.selectedVerbCard!!.label
                        } else {
                            "意图桥"
                        },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    // Back button when in detail view
                    if (state.selectedVerbCard != null) {
                        IconButton(onClick = { viewModel.onBackToVerbs() }) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = Color.White
                            )
                        }
                    }
                },
                actions = {
                    // Parent mode toggle (家长模式)
                    IconButton(onClick = { viewModel.toggleParentMode() }) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "家长模式",
                            tint = if (state.isParentMode) StandardOrange else Color.White
                        )
                    }
                    // Peppa Pig mode indicator
                    if (state.peppaPigMode) {
                        Text(
                            text = "佩奇",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    // Settings button - navigate to config screen (full config) - only in parent mode
                    if (state.isParentMode) {
                        IconButton(onClick = onNavigateToConfig) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "卡片配置",
                                tint = Color.White
                            )
                        }
                        // Edit button for current page - edit current level cards - only in parent mode
                        IconButton(onClick = { 
                            // If in level 2 (detail view), pass parent card id
                            onNavigateToEditPage(state.selectedVerbCard != null, state.selectedVerbCard?.id)
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "编辑当前页面",
                                tint = Color.White
                            )
                        }
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
                AnimatedContent(
                    targetState = state.selectedVerbCard,
                    transitionSpec = {
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    },
                    label = "screen_transition"
                ) { selectedVerb ->
                    if (selectedVerb != null) {
                        // Level 2: Detail cards view
                        DetailView(
                            detailCards = state.detailCards,
                            parentVerb = selectedVerb,
                            onDetailCardClick = { viewModel.onDetailCardClick(it) },
                            onDetailCardLongClick = { onNavigateToEditCard(it.id) },
                            isParentMode = state.isParentMode,
                            aiPrediction = state.aiPrediction,
                            coachSuggestion = state.coachSuggestion,
                            onDismissPrediction = { viewModel.clearPrediction() },
                            onDismissSuggestion = { viewModel.clearSuggestion() }
                        )
                    } else {
                        // Level 1: Main view with urgent + verb cards
                        MainView(
                            urgentCards = state.urgentCards,
                            verbCards = state.verbCards,
                            onUrgentCardClick = { viewModel.onUrgentCardClick(it) },
                            onVerbCardClick = { viewModel.onVerbCardClick(it) },
                            onUrgentCardLongClick = { onNavigateToEditCard(it.id) },
                            onVerbCardLongClick = { onNavigateToEditCard(it.id) },
                            isParentMode = state.isParentMode,
                            aiPrediction = state.aiPrediction,
                            coachSuggestion = state.coachSuggestion,
                            onDismissPrediction = { viewModel.clearPrediction() },
                            onDismissSuggestion = { viewModel.clearSuggestion() }
                        )
                    }
                }
            }
            
            // Bubble popup overlay (shows on top of both levels)
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

/**
 * Level 1: Main view with urgent zone and verb grid
 */
@Composable
private fun MainView(
    urgentCards: List<Card>,
    verbCards: List<Card>,
    onUrgentCardClick: (Card) -> Unit,
    onVerbCardClick: (Card) -> Unit,
    onUrgentCardLongClick: (Card) -> Unit,
    onVerbCardLongClick: (Card) -> Unit,
    isParentMode: Boolean,
    aiPrediction: String?,
    coachSuggestion: String?,
    onDismissPrediction: () -> Unit,
    onDismissSuggestion: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // AI Prediction Banner (only in parent mode)
        if (isParentMode && aiPrediction != null) {
            AIPredictionBanner(
                message = aiPrediction,
                onDismiss = onDismissPrediction
            )
        }
        
        // Urgent Zone (top) - always visible
        if (urgentCards.isNotEmpty()) {
            UrgentZone(
                cards = urgentCards,
                onCardClick = onUrgentCardClick,
                onCardLongClick = onUrgentCardLongClick
            )
            
            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = TextSecondary.copy(alpha = 0.3f),
                thickness = 1.dp
            )
        }
        
        // Verb Zone (Level 1 grid)
        if (verbCards.isNotEmpty()) {
            VerbZone(
                cards = verbCards,
                onCardClick = onVerbCardClick,
                onCardLongClick = onVerbCardLongClick,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Coach Suggestion (only in parent mode)
        if (isParentMode && coachSuggestion != null) {
            CoachSuggestionBanner(
                suggestion = coachSuggestion,
                onDismiss = onDismissSuggestion
            )
        }
    }
}

/**
 * Level 2: Detail view for specific verb
 */
@Composable
private fun DetailView(
    detailCards: List<Card>,
    parentVerb: Card,
    onDetailCardClick: (Card) -> Unit,
    onDetailCardLongClick: (Card) -> Unit,
    isParentMode: Boolean,
    aiPrediction: String?,
    coachSuggestion: String?,
    onDismissPrediction: () -> Unit,
    onDismissSuggestion: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // AI Prediction Banner
        if (isParentMode && aiPrediction != null) {
            AIPredictionBanner(
                message = aiPrediction,
                onDismiss = onDismissPrediction
            )
        }
        
        // Detail cards grid
        if (detailCards.isNotEmpty()) {
            DetailZone(
                cards = detailCards,
                onCardClick = onDetailCardClick,
                onCardLongClick = onDetailCardLongClick,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Coach Suggestion
        if (isParentMode && coachSuggestion != null) {
            CoachSuggestionBanner(
                suggestion = coachSuggestion,
                onDismiss = onDismissSuggestion
            )
        }
    }
}

/**
 * Urgent zone with larger horizontal buttons
 */
@Composable
private fun UrgentZone(
    cards: List<Card>,
    onCardClick: (Card) -> Unit,
    onCardLongClick: ((Card) -> Unit)? = null
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
                    onLongClick = { onCardLongClick?.invoke(card) },
                    modifier = Modifier
                        .width(160.dp)
                        .height(120.dp)
                )
            }
        }
    }
}

/**
 * Verb zone - Level 1 grid
 */
@Composable
private fun VerbZone(
    cards: List<Card>,
    onCardClick: (Card) -> Unit,
    onCardLongClick: ((Card) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "需求表达",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = StandardBlue,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // 2x3 grid for verbs
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(cards) { card ->
                VerbCardButton(
                    card = card,
                    onClick = { onCardClick(card) },
                    onLongClick = { onCardLongClick?.invoke(card) }
                )
            }
        }
    }
}

/**
 * Detail zone - Level 2 grid
 */
@Composable
private fun DetailZone(
    cards: List<Card>,
    onCardClick: (Card) -> Unit,
    onCardLongClick: ((Card) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "选择具体内容",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = StandardPurple,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Dynamic columns based on card count
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
                    onLongClick = { onCardLongClick?.invoke(card) },
                    modifier = Modifier
                        .aspectRatio(1f)
                        .fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Verb card button with special styling
 */
@Composable
private fun VerbCardButton(
    card: Card,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    CardButton(
        card = card,
        cardColor = StandardPurple,
        onClick = onClick,
        onLongClick = onLongClick ?: onClick,
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth()
    )
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
