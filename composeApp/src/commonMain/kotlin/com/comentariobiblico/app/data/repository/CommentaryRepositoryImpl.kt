package com.comentariobiblico.app.data.repository

import com.comentariobiblico.app.data.db.CommentaryDao
import com.comentariobiblico.app.data.db.toFtsQuery
import com.comentariobiblico.app.domain.model.Book
import com.comentariobiblico.app.domain.model.BookFacet
import com.comentariobiblico.app.domain.model.BookIntro
import com.comentariobiblico.app.domain.model.Chapter
import com.comentariobiblico.app.domain.model.ChapterId
import com.comentariobiblico.app.domain.model.SearchCounts
import com.comentariobiblico.app.domain.model.SearchKind
import com.comentariobiblico.app.domain.model.SearchPage
import com.comentariobiblico.app.domain.model.Verse
import com.comentariobiblico.app.domain.model.VerseId
import com.comentariobiblico.app.domain.model.VerseWithComment
import com.comentariobiblico.app.domain.repository.CommentaryRepository
import com.comentariobiblico.app.domain.repository.SearchRepository

class CommentaryRepositoryImpl(private val dao: CommentaryDao) : CommentaryRepository {

    override suspend fun books(): List<Book> = dao.books()

    override suspend fun book(bookId: Int): Book? = dao.book(bookId)

    override suspend fun bookIntro(bookId: Int): BookIntro? =
        dao.bookIntro(bookId)?.takeUnless { it.isEmpty }

    override suspend fun findBooks(query: String, limit: Int): List<Book> =
        dao.findBooks(query, limit)

    override suspend fun chapters(bookId: Int): List<Chapter> = dao.chapters(bookId)

    override suspend fun chapter(id: ChapterId): Chapter? = dao.chapter(id)

    override suspend fun verses(chapterId: ChapterId, onlyWithComment: Boolean): List<Verse> =
        dao.verses(chapterId, onlyWithComment)

    override suspend fun verseWithComment(id: VerseId, highlight: String?): VerseWithComment? =
        dao.verseWithComment(id, highlight?.let { toFtsQuery(it) })

    override suspend fun neighbourWithComment(id: VerseId, forward: Boolean): VerseId? =
        dao.neighbourWithComment(id, forward)
}

class SearchRepositoryImpl(private val dao: CommentaryDao) : SearchRepository {

    override suspend fun search(
        query: String,
        kind: SearchKind?,
        bookId: Int?,
        offset: Int,
        limit: Int,
    ): SearchPage {
        val fts = toFtsQuery(query) ?: return EMPTY

        val counts = dao.searchCounts(fts)
        val hits = dao.search(fts, kind, bookId, offset, limit)

        return SearchPage(
            hits = hits,
            counts = counts,
            offset = offset,
            // Cada rama de la búsqueda se acota para poder ordenarla por
            // relevancia; si se llega a ese tope hay que decirlo en vez de
            // fingir que esos son todos los resultados.
            truncated = counts.comments > SEARCH_POOL || counts.verses > SEARCH_POOL,
        )
    }

    override suspend fun facets(query: String, limit: Int): List<BookFacet> {
        val fts = toFtsQuery(query) ?: return emptyList()
        return dao.facets(fts, limit)
    }

    private companion object {
        const val SEARCH_POOL = 400
        val EMPTY = SearchPage(emptyList(), SearchCounts(0, 0), 0, truncated = false)
    }
}
