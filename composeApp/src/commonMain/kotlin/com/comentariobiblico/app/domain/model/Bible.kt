package com.comentariobiblico.app.domain.model

import kotlin.jvm.JvmInline

enum class Testament { OLD, NEW }

/**
 * Identificador aritmético: `libro*1_000_000 + capítulo*1_000 + versículo`.
 * Es el rowid de la tabla `verse`, así que cualquier consulta por id es O(1).
 */
@JvmInline
value class VerseId(val raw: Int) {
    val bookId: Int get() = raw / 1_000_000
    val chapter: Int get() = (raw / 1_000) % 1_000
    val verse: Int get() = raw % 1_000
    val chapterId: ChapterId get() = ChapterId(bookId * 1_000 + chapter)

    companion object {
        fun of(book: Int, chapter: Int, verse: Int) =
            VerseId(book * 1_000_000 + chapter * 1_000 + verse)
    }
}

@JvmInline
value class ChapterId(val raw: Int) {
    val bookId: Int get() = raw / 1_000
    val number: Int get() = raw % 1_000

    companion object {
        fun of(book: Int, chapter: Int) = ChapterId(book * 1_000 + chapter)
    }
}

data class Book(
    val id: Int,
    val testament: Testament,
    val title: String,
    val shortTitle: String,
    val chapterCount: Int,
    val verseCount: Int,
    val commentCount: Int,
    val hasIntro: Boolean,
)

/** Vacío en 2 Samuel, 2 Reyes, 2 Crónicas y Nehemías. */
data class BookIntro(
    val bookId: Int,
    val description: String,
    val author: String,
    val scenery: String,
    val theme: String,
    val outline: String,
) {
    val isEmpty: Boolean
        get() = description.isBlank() && author.isBlank() && scenery.isBlank() &&
            theme.isBlank() && outline.isBlank()
}

data class Chapter(
    val id: ChapterId,
    val bookId: Int,
    val number: Int,
    val verseCount: Int,
    /** Distinto de [verseCount] en los 4 capítulos con huecos de numeración. */
    val lastVerse: Int,
    val commentCount: Int,
    /** El "argumento" del capítulo. Vacío en 223 capítulos. */
    val summary: String,
) {
    val commentRatio: Float
        get() = if (verseCount == 0) 0f else commentCount.toFloat() / verseCount
}

data class Verse(
    val id: VerseId,
    val number: Int,
    /** "Génesis 1:1" */
    val reference: String,
    val text: String,
    /** Falso en 19 versículos que aun así tienen comentario. */
    val hasText: Boolean,
    val hasComment: Boolean,
)

/**
 * Un lema citado del versículo y la explicación que lo sigue.
 *
 * [highlights] son los tramos de [text] que coinciden con la búsqueda de la que
 * se viene, ya en coordenadas del texto limpio.
 */
data class CommentBlock(
    val lemma: String?,
    val text: String,
    val highlights: List<IntRange> = emptyList(),
)

data class Comment(
    val verseId: VerseId,
    val blocks: List<CommentBlock>,
    /** Referencias de Elena G. de White. Vacío salvo en 1.063 comentarios. */
    val egwReferences: String,
    /** Remisiones puras del tipo "Ver com. vers. 8.". */
    val isCrossReference: Boolean,
)

data class VerseWithComment(
    val verse: Verse,
    val comment: Comment?,
    val previous: VerseId?,
    val next: VerseId?,
)
