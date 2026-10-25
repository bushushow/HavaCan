package com.example.weather.ui.city

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.weather.data.sampleCities
import com.example.weather.model.City
import com.example.weather.ui.theme.WeatherTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Each of the four ViewModel states renders as expected. */
class CityScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var retries = 0
    private var clicked: City? = null
    private var lastQuery: String? = null

    private fun show(state: CityUiState, query: String = "", selectedCityId: Long? = null) {
        composeRule.setContent {
            WeatherTheme {
                CityScreen(
                    uiState = state,
                    query = query,
                    onQueryChange = { lastQuery = it },
                    selectedCityId = selectedCityId,
                    onCityClick = { clicked = it },
                    onRetry = { retries++ },
                )
            }
        }
    }

    @Test
    fun loadingShowsPlaceholder() {
        show(CityUiState.Loading, query = "ank")
        composeRule.onNodeWithContentDescription("Şehirler yükleniyor").assertIsDisplayed()
    }

    @Test
    fun successShowsCitiesAndReportsClicks() {
        show(CityUiState.Success(sampleCities.take(3)), query = "a")
        composeRule.onNodeWithText("Sonuçlar").assertIsDisplayed()
        composeRule.onNodeWithText("3 şehir").assertIsDisplayed()
        composeRule.onNodeWithText("Ankara").performClick()
        assertEquals("Ankara", clicked?.name)
    }

    @Test
    fun selectedCityIsMarkedSelected() {
        val ankara = sampleCities.first { it.name == "Ankara" }
        show(CityUiState.Success(sampleCities, isSuggestion = true), selectedCityId = ankara.id)
        composeRule.onNodeWithText("Popüler şehirler").assertIsDisplayed()
        composeRule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true)).assertExists()
    }

    @Test
    fun emptyShowsCityNotFound() {
        show(CityUiState.Empty("xyz"), query = "xyz")
        composeRule.onNodeWithText("Şehir bulunamadı").assertIsDisplayed()
    }

    @Test
    fun errorShowsRetryThatWorks() {
        show(CityUiState.Error(ErrorReason.NoConnection), query = "ank")
        composeRule.onNodeWithText("Bağlantı kurulamadı").assertIsDisplayed()
        composeRule.onNodeWithText("Tekrar dene").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun clearButtonClearsQuery() {
        show(CityUiState.Empty("xyz"), query = "xyz")
        composeRule.onNodeWithContentDescription("Aramayı temizle").performClick()
        assertEquals("", lastQuery)
    }
}
