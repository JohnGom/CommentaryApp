package com.comentariobiblico.app.domain.repository

import com.comentariobiblico.app.domain.model.Book
import com.comentariobiblico.app.domain.model.BookFacet
import com.comentariobiblico.app.domain.model.BookIntro
import com.comentariobiblico.app.domain.model.Chapter
import com.comentariobiblico.app.domain.model.ChapterId
import com.comentariobiblico.app.domain.model.ReaderSettings
import com.comentariobiblico.app.domain.model.SearchKind
import com.comentariobiblico.app.domain.model.SearchPage
import com.comentariobiblico.app.domain.model.Verse
import com.comentariobiblico.app.domain.model.VerseId
import com.comentariobiblico.app.domain.model.VerseWithComment
import kotlinx.coroutines.flow.Flow

/**
 * La base es inmutable y de solo lectura, así que las lecturas son `suspend` y
 * no `Flow`: un Flow que nunca vuelve a emitir solo añade ruido. Flow se
 * reserva para lo que sí cambia — ajustes y posición de lectura.
 */
interface CommentaryRepository {
    suspend fun books(): List<Book>
    suspend fun book(bookId: Int): Book?
    suspend fun bookIntro(bookId: Int): BookIntro?
    suspend fun findBooks(query: String, limit: Int = 12): List<Book>

    suspend fun chapters(bookId: Int): List<Chapter>
    suspend fun chapter(id: ChapterId): Chapter?

    /** Los números que existen de verdad: respeta los 4 huecos del canon. */
    suspend fun verses(chapterId: ChapterId, onlyWithComment: Boolean = false): List<Verse>

    suspend fun verseWithComment(id: VerseId, highlight: String? = null): VerseWithComment?

    /** Salta al versículo comentado más cercano en la dirección indicada. */
    suspend fun neighbourWithComment(id: VerseId, forward: Boolean): VerseId?
}

interface SearchRepository {
    suspend fun search(
        query: String,
        kind: SearchKind? = null,
        bookId: Int? = null,
        offset: Int = 0,
        limit: Int = PAGE_SIZE,
    ): SearchPage

    suspend fun facets(query: String, limit: Int = 12): List<BookFacet>

    companion object {
        const val PAGE_SIZE = 25
    }
}

interface SettingsRepository {
    val settings: Flow<ReaderSettings>
    suspend fun update(transform: (ReaderSettings) -> ReaderSettings)

    /** Último versículo leído, para la tarjeta de "Continuar". */
    val lastRead: Flow<VerseId?>
    suspend fun setLastRead(id: VerseId)
}
