package com.comentariobiblico.app.domain.model

enum class SearchKind { VERSE, COMMENT }

/** Texto plano y los tramos que la búsqueda encontró, ya desmarcados. */
data class Snippet(val text: String, val highlights: List<IntRange>)

data class SearchHit(
    val verseId: VerseId,
    val kind: SearchKind,
    /** "Romanos 8:28" */
    val reference: String,
    val snippet: Snippet,
    val hasComment: Boolean,
)

data class SearchCounts(val verses: Int, val comments: Int) {
    val total: Int get() = verses + comments
}

data class SearchPage(
    val hits: List<SearchHit>,
    val counts: SearchCounts,
    val offset: Int,
    /** Cierto cuando hay más resultados de los que se pueden ordenar por relevancia. */
    val truncated: Boolean,
)

data class BookFacet(val bookId: Int, val title: String, val count: Int)
