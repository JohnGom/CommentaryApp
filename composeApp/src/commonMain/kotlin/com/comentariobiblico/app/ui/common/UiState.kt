package com.comentariobiblico.app.ui.common

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Empty(val reason: EmptyReason) : UiState<Nothing>
    data class Error(val message: String) : UiState<Nothing>
}

enum class EmptyReason { NO_COMMENT, NO_RESULTS, NO_INTRO, TYPE_TO_SEARCH }

/**
 * Con una base local casi todo resuelve en menos de 10 ms. Mostrar el
 * indicador de carga antes de este umbral solo produce un parpadeo que hace
 * parecer lenta una app que no lo es.
 */
const val SPINNER_DELAY_MS = 120L
