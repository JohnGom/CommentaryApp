package com.comentariobiblico.app.ui.chapters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.comentariobiblico.app.domain.model.Book
import com.comentariobiblico.app.domain.model.BookIntro
import com.comentariobiblico.app.domain.model.Chapter
import com.comentariobiblico.app.domain.repository.CommentaryRepository
import com.comentariobiblico.app.ui.common.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

data class ChaptersUiState(val book: Book, val chapters: List<Chapter>)

@OptIn(ExperimentalCoroutinesApi::class)
class ChaptersViewModel(private val commentary: CommentaryRepository) : ViewModel() {

    private val bookId = MutableStateFlow<Int?>(null)

    val state: StateFlow<UiState<ChaptersUiState>> = bookId
        .filterNotNull()
        .mapLatest { id ->
            val book = commentary.book(id)
                ?: return@mapLatest UiState.Error("No se encontró el libro $id")
            UiState.Success(ChaptersUiState(book, commentary.chapters(id)))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    fun load(book: Int) {
        bookId.value = book
    }

    private companion object {
        const val STOP_TIMEOUT = 5_000L
    }
}

data class BookIntroUiState(val book: Book, val intro: BookIntro)

@OptIn(ExperimentalCoroutinesApi::class)
class BookIntroViewModel(private val commentary: CommentaryRepository) : ViewModel() {

    private val bookId = MutableStateFlow<Int?>(null)

    val state: StateFlow<UiState<BookIntroUiState>> = bookId
        .filterNotNull()
        .mapLatest { id ->
            val book = commentary.book(id)
                ?: return@mapLatest UiState.Error("No se encontró el libro $id")
            // 2 Samuel, 2 Reyes, 2 Crónicas y Nehemías no traen metadatos.
            val intro = commentary.bookIntro(id)
                ?: return@mapLatest UiState.Empty(
                    com.comentariobiblico.app.ui.common.EmptyReason.NO_INTRO,
                )
            UiState.Success(BookIntroUiState(book, intro))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    fun load(book: Int) {
        bookId.value = book
    }

    private companion object {
        const val STOP_TIMEOUT = 5_000L
    }
}
