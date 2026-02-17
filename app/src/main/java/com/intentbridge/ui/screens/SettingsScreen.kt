package com.intentbridge.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.intentbridge.ui.theme.*

/**
 * Settings screen for global voice and app settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "语音设置",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StandardBlue,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Global voice settings
            item {
                Text(
                    text = "全局语音设置",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = StandardBlue
                )
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Default speech rate
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "默认语速",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${String.format("%.1f", state.defaultSpeechRate)}x",
                                    fontSize = 16.sp,
                                    color = StandardBlue
                                )
                            }
                            Slider(
                                value = state.defaultSpeechRate,
                                onValueChange = { viewModel.setDefaultSpeechRate(it) },
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
                        
                        Divider()
                        
                        // Default pitch
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "默认音调",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${String.format("%.1f", state.defaultPitch)}x",
                                    fontSize = 16.sp,
                                    color = StandardBlue
                                )
                            }
                            Slider(
                                value = state.defaultPitch,
                                onValueChange = { viewModel.setDefaultPitch(it) },
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
                        
                        Divider()
                        
                        // Test voice button
                        Button(
                            onClick = { viewModel.testVoice() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = StandardPurple)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("测试语音")
                        }
                    }
                }
            }
            
            // Available voices section
            item {
                Text(
                    text = "可用语音",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = StandardBlue
                )
            }
            
            item {
                if (state.availableVoices.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight)
                    ) {
                        Text(
                            text = "正在加载可用语音...",
                            modifier = Modifier.padding(16.dp),
                            color = TextSecondary
                        )
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp)
                        ) {
                            state.availableVoices.forEach { voice ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = state.selectedVoiceId == voice.id,
                                        onClick = { viewModel.setDefaultVoice(voice.id) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = voice.name,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = voice.locale.displayName,
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    if (voice.isQualityHigh) {
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = "高品质",
                                            fontSize = 12.sp,
                                            color = StandardGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // About section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "关于",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = StandardBlue
                )
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "意图桥 IntentBridge",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "版本 1.0.0",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "为ASD儿童打造的极速沟通工具",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
