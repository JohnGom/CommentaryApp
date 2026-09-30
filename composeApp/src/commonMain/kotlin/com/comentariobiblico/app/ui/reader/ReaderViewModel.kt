package com.comentariobiblico.app.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.comentariobiblico.app.domain.model.Book
import com.comentariobiblico.app.domain.model.Verse
import com.comentariobiblico.app.domain.model.VerseId
import com.comentariobiblico.app.domain.model.VerseWithComment
import com.comentariobiblico.app.domain.repository.CommentaryRepository
import com.comentariobiblico.app.domain.repository.SettingsRepository
import com.comentariobiblico.app.ui.common.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReaderUiState(
    val book: Book,
    /** Los versículos del capítulo, para deslizar entre ellos. */
    val chapterVerses: List<Verse>,
    val index: Int,
    val current: VerseWithComment,
    val query: String?,
)

private data class Request(val id: VerseId, val query: String?)

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModel(
    private val commentary: CommentaryRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val request = MutableStateFlow<Request?>(null)

    val state: StateFlow<UiState<ReaderUiState>> = request
        .filterNotNull()
        .mapLatest { (id, query) ->
            val loaded = commentary.verseWithComment(id, query)
                ?: return@mapLatest UiState.Error("No se encontró ${id.raw}")
            val book = commentary.book(id.bookId)
                ?: return@mapLatest UiState.Error("No se encontró el libro")
            val verses = commentary.verses(id.chapterId)

            UiState.Success(
                ReaderUiState(
                    book = book,
                    chapterVerses = verses,
                    index = verses.indexOfFirst { it.id == id }.coerceAtLeast(0),
                    current = loaded,
                    query = query,
                ),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    fun load(book: Int, chapter: Int, verse: Int, query: String? = null) {
        val id = VerseId.of(book, chapter, verse)
        if (request.value?.id == id && request.value?.query == query) return
        request.value = Request(id, query)
        // Se recuerda en cuanto se abre, no al salir: si el usuario mata la app
        // leyendo, la posición ya está guardada.
        viewModelScope.launch { settings.setLastRead(id) }
    }

    /** Mueve dentro del capítulo sin recargarlo todo. */
    fun select(verse: Verse) = load(
        book = verse.id.bookId,
        chapter = verse.id.chapter,
        verse = verse.number,
        query = request.value?.query,
    )

    /**
     * Salta al versículo comentado más cercano. Es la salida del estado vacío:
     * uno de cada cinco versículos no tiene comentario, y dejar al usuario en
     * un callejón sin salida sería el peor momento de la app.
     */
    fun jumpToCommented(forward: Boolean, onResult: (VerseId?) -> Unit) {
        viewModelScope.launch {
            val current = request.value?.id
            onResult(current?.let { commentary.neighbourWithComment(it, forward) })
        }
    }

    private companion object {
        const val STOP_TIMEOUT = 5_000L
    }
}
