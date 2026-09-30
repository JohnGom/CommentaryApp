package com.comentariobiblico.app.ui.verses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.comentariobiblico.app.domain.model.Book
import com.comentariobiblico.app.domain.model.Chapter
import com.comentariobiblico.app.domain.model.ChapterId
import com.comentariobiblico.app.domain.model.Verse
import com.comentariobiblico.app.domain.repository.CommentaryRepository
import com.comentariobiblico.app.ui.common.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

data class VersesUiState(
    val book: Book,
    val chapter: Chapter,
    val verses: List<Verse>,
)

@OptIn(ExperimentalCoroutinesApi::class)
class VersesViewModel(private val commentary: CommentaryRepository) : ViewModel() {

    private val chapterId = MutableStateFlow<ChapterId?>(null)

    private val _onlyWithComment = MutableStateFlow(false)
    val onlyWithComment: StateFlow<Boolean> = _onlyWithComment.asStateFlow()

    val state: StateFlow<UiState<VersesUiState>> =
        combine(chapterId.filterNotNull(), _onlyWithComment) { id, filtered -> id to filtered }
            .mapLatest { (id, filtered) ->
                val chapter = commentary.chapter(id)
                    ?: return@mapLatest UiState.Error("No se encontró el capítulo")
                val book = commentary.book(chapter.bookId)
                    ?: return@mapLatest UiState.Error("No se encontró el libro")
                UiState.Success(
                    VersesUiState(
                        book = book,
                        chapter = chapter,
                        // Siempre desde la base: hay 4 capítulos con huecos de
                        // numeración y construir 1..verseCount inventaría
                        // versículos que no existen.
                        verses = commentary.verses(id, onlyWithComment = filtered),
                    ),
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    fun load(book: Int, chapter: Int) {
        chapterId.value = ChapterId.of(book, chapter)
    }

    fun toggleOnlyWithComment() {
        _onlyWithComment.value = !_onlyWithComment.value
    }

    private companion object {
        const val STOP_TIMEOUT = 5_000L
    }
}
