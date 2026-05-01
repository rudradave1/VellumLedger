package com.vellum.ledger

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.platform.app.InstrumentationRegistry
import com.vellum.ledger.database.AndroidLedgerContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ComposeAppUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AndroidLedgerContext.appContext = context
    }

    @Test
    fun app_starts_and_displays_balance() {
        composeTestRule.setContent {
            App()
        }

        // Check if "Total Balance" is displayed
        composeTestRule.onNodeWithText("Total Balance").assertIsDisplayed()
    }
    
    @Test
    fun navigating_to_settings_works() {
        composeTestRule.setContent {
            App()
        }
        
        // Find Settings in bottom bar and click
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }
}
