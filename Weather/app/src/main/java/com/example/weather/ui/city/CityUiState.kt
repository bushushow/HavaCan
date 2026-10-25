package com.example.weather.ui.city

import com.example.weather.model.City

sealed interface CityUiState {
    data object Loading : CityUiState

    /** [isSuggestion] is true for the built-in popular cities shown before the user searches. */
    data class Success(val cities: List<City>, val isSuggestion: Boolean = false) : CityUiState

    data class Empty(val query: String) : CityUiState

    data class Error(val reason: ErrorReason) : CityUiState
}

enum class ErrorReason { NoConnection, Server, Unknown }
