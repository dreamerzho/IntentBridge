package com.intentbridge.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.intentbridge.data.model.Card
import com.intentbridge.data.model.CardCategory
import com.intentbridge.ui.theme.*

/**
 * Font size options for labels
 */
val fontSizeOptions = listOf(20, 24, 28, 32, 36)

/**
 * Config screen for parents to manage cards with full customization
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: ConfigViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<Card?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Card?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "卡片配置",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Settings button for voice settings
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "语音设置",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StandardBlue,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = StandardGreen
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加卡片",
                    tint = Color.White
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Urgent cards section
            item {
                Text(
                    text = "紧急区",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = UrgentRed
                )
            }
            
            items(state.urgentCards) { card ->
                CardItem(
                    card = card,
                    onEdit = { editingCard = card },
                    onDelete = { showDeleteConfirm = card }
                )
            }
            
            // Standard cards section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "需求区",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = StandardBlue
                )
            }
            
            items(state.standardCards) { card ->
                CardItem(
                    card = card,
                    onEdit = { editingCard = card },
                    onDelete = { showDeleteConfirm = card }
                )
            }
        }
    }
    
    // Add card dialog with full customization
    if (showAddDialog) {
        CardEditDialog(
            card = null,
            onDismiss = { showAddDialog = false },
            onSave = { label, speechText, imagePath, category, fontSize, labelColor, speechRate, speechPitch ->
                viewModel.addCard(label, speechText, imagePath, category, fontSize, labelColor, speechRate, speechPitch)
                showAddDialog = false
            }
        )
    }
    
    // Edit card dialog with full customization
    editingCard?.let { card ->
        CardEditDialog(
            card = card,
            onDismiss = { editingCard = null },
            onSave = { label, speechText, imagePath, category, fontSize, labelColor, speechRate, speechPitch ->
                viewModel.updateCard(card.copy(
                    label = label,
                    speechText = speechText,
                    imagePath = imagePath,
                    category = category,
                    labelFontSize = fontSize,
                    labelColor = labelColor,
                    speechRate = speechRate,
                    speechPitch = speechPitch
                ))
                editingCard = null
            }
        )
    }
    
    // Delete confirmation dialog
    showDeleteConfirm?.let { card ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除卡片 \"${card.label}\" 吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCard(card)
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = UrgentRed)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * Card item in the list
 */
@Composable
private fun CardItem(
    card: Card,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Card image
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(card.imagePath)
                    .crossfade(true)
                    .build(),
                contentDescription = card.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Card info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.label,
                    fontSize = card.labelFontSize.sp,
                    fontWeight = FontWeight.Bold,
                    color = try {
                        Color(android.graphics.Color.parseColor(card.labelColor))
                    } catch (e: Exception) {
                        TextOnLight
                    }
                )
                Text(
                    text = card.speechText,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    maxLines = 2
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (card.category == CardCategory.URGENT) "紧急区" else "需求区",
                        fontSize = 12.sp,
                        color = if (card.category == CardCategory.URGENT) UrgentRed else StandardBlue
                    )
                    Text(
                        text = "语速:${card.speechRate}x",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "音调:${card.speechPitch}x",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }
            
            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = UrgentRed
                )
            }
        }
    }
}

/**
 * Full-featured dialog for adding/editing cards with customization
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardEditDialog(
    card: Card?,
    onDismiss: () -> Unit,
    onSave: (
        label: String,
        speechText: String,
        imagePath: String,
        category: CardCategory,
        fontSize: Int,
        labelColor: String,
        speechRate: Float,
        speechPitch: Float
    ) -> Unit
) {
    var label by remember { mutableStateOf(card?.label ?: "") }
    var speechText by remember { mutableStateOf(card?.speechText ?: "") }
    var imagePath by remember { mutableStateOf(card?.imagePath ?: "") }
    var category by remember { mutableStateOf(card?.category ?: CardCategory.STANDARD) }
    var fontSize by remember { mutableIntStateOf(card?.labelFontSize ?: 24) }
    var labelColor by remember { mutableStateOf(card?.labelColor ?: "#FFFFFF") }
    var speechRate by remember { mutableFloatStateOf(card?.speechRate ?: 1.0f) }
    var speechPitch by remember { mutableFloatStateOf(card?.speechPitch ?: 1.0f) }
    
    var categoryExpanded by remember { mutableStateOf(false) }
    var showVoiceSettings by remember { mutableStateOf(false) }
    
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { imagePath = it.toString() }
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        // In production, save bitmap to file and use path
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (card == null) "添加卡片" else "编辑卡片",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Image selection
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedCard(
                            onClick = { imagePicker.launch("image/*") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    tint = StandardBlue
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("相册选择")
                            }
                        }
                        
                        OutlinedCard(
                            onClick = { cameraLauncher.launch(null) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = StandardOrange
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("拍照")
                            }
                        }
                    }
                }
                
                // Image preview
                if (imagePath.isNotBlank()) {
                    item {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imagePath)
                                .crossfade(true)
                                .build(),
                            contentDescription = "预览",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
                
                // Label input
                item {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("标签文字") },
                        placeholder = { Text("例如: 尿尿") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters
                        ),
                        singleLine = true
                    )
                }
                
                // Speech text input
                item {
                    OutlinedTextField(
                        value = speechText,
                        onValueChange = { speechText = it },
                        label = { Text("播报文字") },
                        placeholder = { Text("例如: 妈妈，我想尿尿！") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
                
                // Category dropdown
                item {
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = if (category == CardCategory.URGENT) "紧急区" else "需求区",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("分类") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("紧急区") },
                                onClick = {
                                    category = CardCategory.URGENT
                                    categoryExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("需求区") },
                                onClick = {
                                    category = CardCategory.STANDARD
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // Font size selection
                item {
                    Text(
                        text = "文字大小: ${fontSize}sp",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(fontSizeOptions) { size ->
                            FilterChip(
                                selected = fontSize == size,
                                onClick = { fontSize = size },
                                label = { Text("${size}sp") }
                            )
                        }
                    }
                }
                
                // Label color selection
                item {
                    Text(
                        text = "文字颜色",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(listOf("#FFFFFF" to "白", "#000000" to "黑", "#FFEB3B" to "黄", "#FF9800" to "橙")) { (color, name) ->
                            FilterChip(
                                selected = labelColor == color,
                                onClick = { labelColor = color },
                                label = { Text(name) }
                            )
                        }
                    }
                }
                
                // Voice settings toggle
                item {
                    OutlinedCard(
                        onClick = { showVoiceSettings = !showVoiceSettings },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = StandardPurple
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "语音设置",
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (showVoiceSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }
                    }
                }
                
                // Voice settings
                if (showVoiceSettings) {
                    // Speech rate
                    item {
                        Column {
                            Text(
                                text = "语速: ${String.format("%.1f", speechRate)}x",
                                fontSize = 14.sp
                            )
                            Slider(
                                value = speechRate,
                                onValueChange = { speechRate = it },
                                valueRange = 0.5f..2.0f,
                                steps = 5,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("慢", fontSize = 12.sp, color = TextSecondary)
                                Text("正常", fontSize = 12.sp, color = TextSecondary)
                                Text("快", fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                    
                    // Speech pitch
                    item {
                        Column {
                            Text(
                                text = "音调: ${String.format("%.1f", speechPitch)}x",
                                fontSize = 14.sp
                            )
                            Slider(
                                value = speechPitch,
                                onValueChange = { speechPitch = it },
                                valueRange = 0.5f..2.0f,
                                steps = 5,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("低", fontSize = 12.sp, color = TextSecondary)
                                Text("正常", fontSize = 12.sp, color = TextSecondary)
                                Text("高", fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onSave(label, speechText, imagePath, category, fontSize, labelColor, speechRate, speechPitch) 
                },
                enabled = label.isNotBlank() && speechText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = StandardGreen)
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
