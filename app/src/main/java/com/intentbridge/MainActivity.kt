package com.intentbridge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.intentbridge.ui.screens.ConfigScreen
import com.intentbridge.ui.screens.MainScreen
import com.intentbridge.ui.screens.SettingsScreen
import com.intentbridge.ui.theme.IntentBridgeTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity for IntentBridge app
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            IntentBridgeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Navigation
                    val navController = rememberNavController()
                    
                    NavHost(
                        navController = navController,
                        startDestination = "main"
                    ) {
                        composable("main") {
                            MainScreen(
                                onNavigateToConfig = {
                                    navController.navigate("config")
                                },
                                onNavigateToEditCard = { cardId ->
                                    navController.navigate("edit_card/$cardId")
                                },
                                onNavigateToEditPage = { isLevel2, parentCardId ->
                                    // Navigate to config with filter for current page
                                    navController.navigate("edit_page/$isLevel2/${parentCardId ?: 0}")
                                }
                            )
                        }
                        
                        composable("config") {
                            ConfigScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                }
                            )
                        }
                        
                        composable("settings") {
                            SettingsScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        // Edit single card - for editing card from main screen
                        composable(
                            route = "edit_card/{cardId}",
                            arguments = listOf(
                                navArgument("cardId") { type = NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val cardId = backStackEntry.arguments?.getLong("cardId") ?: 0L
                            ConfigScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                },
                                editCardId = cardId
                            )
                        }
                        
                        // Edit page - for editing current page cards
                        composable(
                            route = "edit_page/{isLevel2}/{parentCardId}",
                            arguments = listOf(
                                navArgument("isLevel2") { type = NavType.BoolType },
                                navArgument("parentCardId") { type = NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val isLevel2 = backStackEntry.arguments?.getBoolean("isLevel2") ?: false
                            val parentCardId = backStackEntry.arguments?.getLong("parentCardId")
                            ConfigScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                },
                                editCardId = null,  // Will be handled by isLevel2 and parentCardId
                                isLevel2Edit = isLevel2,
                                parentCardIdForEdit = if (parentCardId == 0L) null else parentCardId
                            )
                        }
                    }
                }
            }
        }
    }
}
