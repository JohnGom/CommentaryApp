package com.comentariobiblico.app.ui.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.comentariobiblico.app.domain.model.Book
import com.comentariobiblico.app.domain.model.Testament
import com.comentariobiblico.app.domain.model.VerseId
import com.comentariobiblico.app.domain.repository.CommentaryRepository
import com.comentariobiblico.app.domain.repository.SettingsRepository
import com.comentariobiblico.app.ui.common.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class ContinueReading(val verseId: VerseId, val reference: String, val preview: String)

data class BooksUiState(
    val oldTestament: List<Book>,
    val newTestament: List<Book>,
    val continueReading: ContinueReading?,
) {
    val isFiltered: Boolean get() = oldTestament.size + newTestament.size < TOTAL_BOOKS

    private companion object {
        const val TOTAL_BOOKS = 66
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BooksViewModel(
    private val commentary: CommentaryRepository,
    settings: SettingsRepository,
) : ViewModel() {

    private val _filter = MutableStateFlow("")
    val filter: StateFlow<String> = _filter.asStateFlow()

    private val books = flow { emit(commentary.books()) }

    /**
     * La tarjeta de "Continuar" ahorra tres toques a quien vuelve a la app, que
     * es el caso más frecuente. Se acompaña de un extracto para que se
     * reconozca dónde se quedó sin tener que abrirlo.
     */
    private val continueReading = settings.lastRead.mapLatest { id ->
        id?.let { verseId ->
            commentary.verseWithComment(verseId)?.let { loaded ->
                ContinueReading(
                    verseId = verseId,
                    reference = loaded.verse.reference,
                    preview = loaded.comment?.blocks?.firstOrNull()?.text?.take(PREVIEW_LENGTH)
                        ?: loaded.verse.text.take(PREVIEW_LENGTH),
                )
            }
        }
    }

    val state: StateFlow<UiState<BooksUiState>> =
        combine(books, _filter, continueReading) { all, filter, resume ->
            val visible = if (filter.isBlank()) all else commentary.findBooks(filter, limit = 66)
            UiState.Success(
                BooksUiState(
                    oldTestament = visible.filter { it.testament == Testament.OLD },
                    newTestament = visible.filter { it.testament == Testament.NEW },
                    // La tarjeta estorba mientras se está filtrando.
                    continueReading = resume.takeIf { filter.isBlank() },
                ),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    fun onFilterChange(value: String) = _filter.update { value }

    private companion object {
        const val PREVIEW_LENGTH = 140
        const val STOP_TIMEOUT = 5_000L
    }
}
