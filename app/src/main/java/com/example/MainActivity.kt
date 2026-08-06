package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.TakoFlowPreferences
import com.example.speech.FormattingProfileStore
import com.example.speech.SpeechModels
import com.example.ui.components.BottomNavBar
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EnableKeyboardStepperScreen
import com.example.ui.screens.GeneralSettingsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SetupScreen
import com.example.ui.screens.VoiceDictationScreen
import com.example.ui.screens.VoiceProfilesScreen
import com.example.ui.screens.VoiceSettingsScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preferences = TakoFlowPreferences(applicationContext)
        FormattingProfileStore.get(applicationContext)

        setContent {
            MyApplicationTheme {
                val setupCompleted by preferences.setupCompleted.collectAsState(initial = false)
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()
                val currentBackStack by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStack?.destination?.route
                val isSetupRoute = currentRoute in listOf("onboarding", "setup", "enable_keyboard")
                val startDestination = if (setupCompleted) "dashboard" else "onboarding"

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (!isSetupRoute && currentRoute != null) {
                            BottomNavBar(
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    navController.navigate(route) {
                                        popUpTo("dashboard") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding)
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
                            SetupScreen(onNavigateToEnable = { navController.navigate("enable_keyboard") })
                        }
                        composable("enable_keyboard") {
                            EnableKeyboardStepperScreen(
                                onBack = { navController.popBackStack() },
                                onCompleteSetup = {
                                    scope.launch {
                                        preferences.setSetupCompleted(true)
                                        preferences.setInferenceModel(SpeechModels.VOSK)
                                        navController.navigate("dashboard") {
                                            popUpTo(navController.graph.id) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            )
                        }

                        composable("dashboard") {
                            DashboardScreen(
                                preferences = preferences,
                                onNavigateToEnable = { navController.navigate("enable_keyboard") },
                                onNavigateToVoiceSettings = { navController.navigate("voice_settings") },
                                onNavigateToProfiles = { navController.navigate("voice_profiles") },
                                onNavigateToGeneralSettings = { navController.navigate("general_settings") },
                                onNavigateToDictation = { navController.navigate("dictation") }
                            )
                        }
                        composable("voice_settings") {
                            VoiceSettingsScreen(
                                preferences = preferences,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("general_settings") {
                            GeneralSettingsScreen(
                                preferences = preferences,
                                onBack = { navController.popBackStack() },
                                onNavigateToVoiceSettings = { navController.navigate("voice_settings") }
                            )
                        }
                        composable("voice_profiles") {
                            VoiceProfilesScreen(
                                preferences = preferences,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("dictation") {
                            VoiceDictationScreen(
                                preferences = preferences,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
