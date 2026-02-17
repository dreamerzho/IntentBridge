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
 * Settings screen for voice settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showAliyunConfigDialog by remember { mutableStateOf(false) }
    
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
            // Aliyun TTS Settings
            item {
                Text(
                    text = "阿里云语音合成",
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "使用阿里云百炼平台语音合成，支持多种音色",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        
                        // API Key display with edit button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = state.aliyunApiKey,
                                onValueChange = { },
                                label = { Text("API Key") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                readOnly = true
                            )
                            IconButton(onClick = { showAliyunConfigDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "修改API Key",
                                    tint = StandardPurple
                                )
                            }
                        }
                        
                        // Voice selection dropdown
                        var voiceExpanded by remember { mutableStateOf(false) }
                        val voiceNames = mapOf(
                            "xiaoxuan" to "小轩 (青年女声)",
                            "xiaoyun" to "小云 (青年女声)",
                            "xiaogang" to "小刚 (青年男声)",
                            "ruoxi" to "若熙 (少女声)",
                            "aibao" to "爱宝 (童年男声)",
                            "xiaomei" to "小美 (成熟女声)",
                            "yujie" to "雨洁 (温柔女声)"
                        )
                        
                        ExposedDropdownMenuBox(
                            expanded = voiceExpanded,
                            onExpandedChange = { voiceExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = voiceNames[state.selectedAliyunVoice] ?: state.selectedAliyunVoice,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("选择音色") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = voiceExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = voiceExpanded,
                                onDismissRequest = { voiceExpanded = false }
                            ) {
                                voiceNames.forEach { (key, name) ->
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = {
                                            viewModel.updateAliyunConfig(state.aliyunApiKey, key)
                                            voiceExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        // Status indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (state.isAliyunConfigured) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (state.isAliyunConfigured) StandardGreen else StandardOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (state.isAliyunConfigured) "已配置 (使用阿里云语音)" else "未配置",
                                fontSize = 14.sp,
                                color = if (state.isAliyunConfigured) StandardGreen else StandardOrange
                            )
                        }
                        
                        Divider()
                        
                        // Test button
                        Button(
                            onClick = { viewModel.testAliyunVoice() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = state.isAliyunConfigured && !state.isTesting,
                            colors = ButtonDefaults.buttonColors(containerColor = StandardPurple)
                        ) {
                            if (state.isTesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("测试语音")
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
    
    // Aliyun Config Dialog
    if (showAliyunConfigDialog) {
        var apiKeyInput by remember { mutableStateOf(state.aliyunApiKey) }
        
        AlertDialog(
            onDismissRequest = { showAliyunConfigDialog = false },
            title = { Text("配置阿里云语音", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "在阿里云百炼控制台获取API Key",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("API Key") },
                        placeholder = { Text("请输入百炼API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateAliyunConfig(apiKeyInput, state.selectedAliyunVoice)
                        showAliyunConfigDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StandardPurple)
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAliyunConfigDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
