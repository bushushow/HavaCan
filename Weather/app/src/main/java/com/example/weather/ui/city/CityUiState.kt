package com.example.weather.ui.city

import com.example.weather.model.City

sealed interface CityUiState {
    data object Loading : CityUiState

    /**
     * [isSuggestion] is true for the built-in lists shown before the user searches.
     * [districtsOf] names the province when the list is that province's districts.
     */
    data class Success(
        val cities: List<City>,
        val isSuggestion: Boolean = false,
        val districtsOf: String? = null,
    ) : CityUiState

    data class Empty(val query: String) : CityUiState

    data class Error(val reason: ErrorReason) : CityUiState

    /** Nothing typed yet for a country without a built-in list; null country means worldwide. */
    data class Prompt(val countryName: String?) : CityUiState
}

enum class ErrorReason { NoConnection, Server, Unknown }
