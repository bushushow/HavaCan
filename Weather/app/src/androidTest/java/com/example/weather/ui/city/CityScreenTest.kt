package com.example.weather.ui.city

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.weather.data.sampleCities
import com.example.weather.ui.theme.WeatherTheme
import org.junit.Rule
import org.junit.Test

class CityScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val searchField = hasSetTextAction()
    private val isSelected = SemanticsMatcher.expectValue(SemanticsProperties.Selected, true)

    private fun setScreen(restorer: StateRestorationTester? = null) {
        val content = @androidx.compose.runtime.Composable {
            WeatherTheme { CityScreen(cities = sampleCities, onCityClick = {}) }
        }
        if (restorer != null) restorer.setContent(content) else composeRule.setContent(content)
    }

    @Test
    fun typingFiltersTheList() {
        setScreen()
        composeRule.onNode(searchField).performTextInput("trab")
        composeRule.onNodeWithText("Trabzon").assertIsDisplayed()
        composeRule.onNodeWithText("Ankara").assertDoesNotExist()
        composeRule.onNodeWithText("1 şehir").assertIsDisplayed()
    }

    @Test
    fun noMatchShowsCityNotFound() {
        setScreen()
        composeRule.onNode(searchField).performTextInput("xyz")
        composeRule.onNodeWithText("Şehir bulunamadı").assertIsDisplayed()
    }

    @Test
    fun clearButtonResetsTheSearch() {
        setScreen()
        composeRule.onNode(searchField).performTextInput("xyz")
        composeRule.onNodeWithContentDescription("Aramayı temizle").performClick()
        composeRule.onNodeWithText("Ankara").assertIsDisplayed()
        composeRule.onNodeWithText("${sampleCities.size} şehir").assertIsDisplayed()
    }

    @Test
    fun queryAndSelectionSurviveRecreation() {
        // Simulates rotation / process death: saved state is restored into a fresh composition
        val restorer = StateRestorationTester(composeRule)
        setScreen(restorer)
        composeRule.onNode(searchField).performTextInput("an")
        composeRule.onNodeWithText("Ankara").performClick()

        restorer.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("an").assertIsDisplayed()
        composeRule.onNodeWithText("Trabzon").assertDoesNotExist()
        composeRule.onNode(isSelected).assertExists()
    }
}
