package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.TakoFlowAppContainer
import com.example.ui.components.BottomNavBar
import com.example.ui.components.BrandLaunchOverlay
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EnableKeyboardStepperScreen
import com.example.ui.screens.GeneralSettingsScreen
import com.example.ui.screens.KeyboardSwitchGuideScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SetupScreen
import com.example.ui.screens.VoiceDictationScreen
import com.example.ui.screens.VoiceProfilesScreen
import com.example.ui.screens.VoiceSettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.takoFlowViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = TakoFlowAppContainer.get(applicationContext)

        setContent {
            MyApplicationTheme {
                val appViewModel: AppViewModel = takoFlowViewModel {
                    AppViewModel(container.settings)
                }
                val setupCompleted by appViewModel.setupCompleted.collectAsStateWithLifecycle()
                val navController = rememberNavController()
                val currentBackStack by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStack?.destination?.route
                var launchOverlayVisible by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(650)
                    launchOverlayVisible = false
                }

                val mainTabs = setOf("dashboard", "voice_profiles", "general_settings")
                val startDestination = if (setupCompleted) "dashboard" else "onboarding"

                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            if (currentRoute in mainTabs) {
                                BottomNavBar(
                                    currentRoute = currentRoute.orEmpty(),
                                    onNavigate = { route -> navController.navigateMainTab(route) }
                                )
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = startDestination,
                            modifier = Modifier.padding(innerPadding),
                            enterTransition = {
                                fadeIn(tween(180)) + slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Left,
                                    tween(240)
                                )
                            },
                            exitTransition = { fadeOut(tween(120)) },
                            popEnterTransition = {
                                fadeIn(tween(180)) + slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Right,
                                    tween(240)
                                )
                            },
                            popExitTransition = { fadeOut(tween(120)) }
                        ) {
                            composable("onboarding") {
                                OnboardingScreen(
                                    onFinishOnboarding = {
                                        navController.navigate("setup") {
                                            popUpTo("onboarding") { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable("setup") {
                                SetupScreen(
                                    onNavigateToEnable = { navController.navigate("enable_keyboard") }
                                )
                            }
                            composable("enable_keyboard") {
                                EnableKeyboardStepperScreen(
                                    container = container,
                                    onBack = { navController.popBackStack() },
                                    onCompleteSetup = {
                                        navController.navigate("switching_guide_setup") {
                                            popUpTo("enable_keyboard") { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                            composable("switching_guide_setup") {
                                KeyboardSwitchGuideScreen(
                                    onContinue = {
                                        appViewModel.setSetupCompleted(true)
                                        navController.navigate("dashboard") {
                                            popUpTo(navController.graph.id) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }

                            composable("dashboard") {
                                DashboardScreen(
                                    container = container,
                                    onNavigateToEnable = { navController.navigate("enable_keyboard") },
                                    onNavigateToVoiceSettings = { navController.navigate("voice_settings") },
                                    onNavigateToProfiles = { navController.navigateMainTab("voice_profiles") },
                                    onNavigateToGeneralSettings = {
                                        navController.navigateMainTab("general_settings")
                                    },
                                    onNavigateToSwitchingGuide = {
                                        navController.navigate("switching_guide")
                                    },
                                    onNavigateToDictation = { navController.navigate("dictation") }
                                )
                            }
                            composable("voice_settings") {
                                VoiceSettingsScreen(
                                    container = container,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("general_settings") {
                                GeneralSettingsScreen(
                                    container = container,
                                    onBack = { navController.navigateMainTab("dashboard") },
                                    onNavigateToVoiceSettings = {
                                        navController.navigate("voice_settings")
                                    },
                                    onNavigateToSwitchingGuide = {
                                        navController.navigate("switching_guide")
                                    },
                                    onRunSetupAgain = {
                                        navController.navigate("setup") {
                                            popUpTo(navController.graph.id) { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable("switching_guide") {
                                KeyboardSwitchGuideScreen(
                                    onBack = { navController.popBackStack() },
                                    onContinue = { navController.popBackStack() }
                                )
                            }
                            composable("voice_profiles") {
                                VoiceProfilesScreen(
                                    container = container,
                                    onBack = { navController.navigateMainTab("dashboard") }
                                )
                            }
                            composable("dictation") {
                                VoiceDictationScreen(
                                    container = container,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }

                    BrandLaunchOverlay(
                        visible = launchOverlayVisible,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

private fun NavHostController.navigateMainTab(route: String) {
    navigate(route) {
        popUpTo("dashboard") { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
