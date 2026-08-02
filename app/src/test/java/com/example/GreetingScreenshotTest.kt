package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.ui.screens.OnboardingScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OnboardingComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun onboardingShowsVoiceTypingMessage() {
        composeRule.setContent {
            MyApplicationTheme {
                OnboardingScreen(onFinishOnboarding = {})
            }
        }

        composeRule.onNodeWithText("Voice typing").assertIsDisplayed()
        composeRule.onNodeWithText("in any app").assertIsDisplayed()
    }
}
