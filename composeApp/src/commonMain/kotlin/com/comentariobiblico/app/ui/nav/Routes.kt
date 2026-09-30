package com.comentariobiblico.app.ui.nav

import kotlinx.serialization.Serializable

@Serializable
data object BooksRoute

@Serializable
data class ChaptersRoute(val book: Int)

@Serializable
data class VersesRoute(val book: Int, val chapter: Int)

@Serializable
data class ReaderRoute(
    val book: Int,
    val chapter: Int,
    val verse: Int,
    /** Consulta de origen, para resaltar los términos dentro del comentario. */
    val query: String? = null,
)

@Serializable
data class BookIntroRoute(val book: Int)

@Serializable
data object SearchRoute
