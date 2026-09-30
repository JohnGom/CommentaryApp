package com.comentariobiblico.app.data.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import com.comentariobiblico.app.domain.model.Book
import com.comentariobiblico.app.domain.model.BookFacet
import com.comentariobiblico.app.domain.model.BookIntro
import com.comentariobiblico.app.domain.model.Chapter
import com.comentariobiblico.app.domain.model.ChapterId
import com.comentariobiblico.app.domain.model.Comment
import com.comentariobiblico.app.domain.model.CommentBlock
import com.comentariobiblico.app.domain.model.SearchCounts
import com.comentariobiblico.app.domain.model.SearchHit
import com.comentariobiblico.app.domain.model.SearchKind
import com.comentariobiblico.app.domain.model.Testament
import com.comentariobiblico.app.domain.model.Verse
import com.comentariobiblico.app.domain.model.VerseId
import com.comentariobiblico.app.domain.model.VerseWithComment
import kotlinx.coroutines.Deferred
import kotlinx.serialization.json.Json

/**
 * Todo el SQL de la app, en un sitio.
 *
 * Sin ORM a propósito: la base es inmutable y de solo lectura, son una docena
 * de consultas, y `androidx.sqlite` con el driver bundled garantiza el mismo
 * SQLite (y el mismo tokenizer FTS5) en Android y en iOS.
 */
class CommentaryDao(private val opening: Deferred<AppDatabase>) {

    private suspend fun <T> read(block: (SQLiteConnection) -> T): T = opening.await().read(block)

    // ── libros ──────────────────────────────────────────────────────────────

    suspend fun books(): List<Book> = read { connection ->
        connection.queryAll(
            "SELECT id, testament, title, short_title, chapter_count, verse_count, " +
                "comment_count, has_intro FROM book ORDER BY id",
            map = ::readBook,
        )
    }

    suspend fun book(bookId: Int): Book? = read { connection ->
        connection.queryFirst(
            "SELECT id, testament, title, short_title, chapter_count, verse_count, " +
                "comment_count, has_intro FROM book WHERE id = ?",
            bind = { it.bindLong(1, bookId.toLong()) },
            map = ::readBook,
        )
    }

    suspend fun bookIntro(bookId: Int): BookIntro? = read { connection ->
        connection.queryFirst(
            "SELECT description, author, scenery, theme, outline FROM book WHERE id = ?",
            bind = { it.bindLong(1, bookId.toLong()) },
        ) { row ->
            BookIntro(
                bookId = bookId,
                description = row.getText(0),
                author = row.getText(1),
                scenery = row.getText(2),
                theme = row.getText(3),
                outline = row.getText(4),
            )
        }
    }

    /** Filtro instantáneo por nombre: "gen" encuentra Génesis, "1 sa" 1 Samuel. */
    suspend fun findBooks(query: String, limit: Int = 12): List<Book> {
        val needle = foldSpanish(query).trim()
        if (needle.isEmpty()) return emptyList()
        return read { connection ->
            connection.queryAll(
                "SELECT id, testament, title, short_title, chapter_count, verse_count, " +
                    "comment_count, has_intro FROM book " +
                    "WHERE title_norm LIKE ?1 || '%' OR title_norm LIKE '% ' || ?1 || '%' " +
                    "ORDER BY (title_norm LIKE ?1 || '%') DESC, id LIMIT ?2",
                bind = {
                    it.bindText(1, needle)
                    it.bindLong(2, limit.toLong())
                },
                map = ::readBook,
            )
        }
    }

    // ── capítulos ───────────────────────────────────────────────────────────

    suspend fun chapters(bookId: Int): List<Chapter> = read { connection ->
        connection.queryAll(
            "SELECT id, book_id, number, verse_count, last_verse, comment_count, summary " +
                "FROM chapter WHERE book_id = ? ORDER BY number",
            bind = { it.bindLong(1, bookId.toLong()) },
            map = ::readChapter,
        )
    }

    suspend fun chapter(id: ChapterId): Chapter? = read { connection ->
        connection.queryFirst(
            "SELECT id, book_id, number, verse_count, last_verse, comment_count, summary " +
                "FROM chapter WHERE id = ?",
            bind = { it.bindLong(1, id.raw.toLong()) },
            map = ::readChapter,
        )
    }

    // ── versículos ──────────────────────────────────────────────────────────

    /**
     * Los números reales que existen. Nunca construyas el rango `1..verseCount`:
     * hay 4 capítulos con huecos (Génesis 40 salta el 22, y también Levítico 11,
     * Números 29 y Salmos 110).
     */
    suspend fun verses(chapterId: ChapterId, onlyWithComment: Boolean = false): List<Verse> =
        read { connection ->
            val filter = if (onlyWithComment) " AND has_comment = 1" else ""
            connection.queryAll(
                "SELECT id, number, ref, text, has_text, has_comment FROM verse " +
                    "WHERE chapter_id = ?$filter ORDER BY number",
                bind = { it.bindLong(1, chapterId.raw.toLong()) },
                map = ::readVerse,
            )
        }

    suspend fun verseWithComment(id: VerseId, highlightQuery: String? = null): VerseWithComment? =
        read { connection ->
            val verse = connection.queryFirst(
                "SELECT id, number, ref, text, has_text, has_comment, prev_id, next_id " +
                    "FROM verse WHERE id = ?",
                bind = { it.bindLong(1, id.raw.toLong()) },
            ) { row ->
                Triple(
                    readVerse(row),
                    row.takeIf { !it.isNull(6) }?.let { VerseId(it.getLong(6).toInt()) },
                    row.takeIf { !it.isNull(7) }?.let { VerseId(it.getLong(7).toInt()) },
                )
            } ?: return@read null

            VerseWithComment(
                verse = verse.first,
                comment = readComment(connection, id, highlightQuery),
                previous = verse.second,
                next = verse.third,
            )
        }

    /** Salta a los versículos que sí tienen comentario, en cualquier dirección. */
    suspend fun neighbourWithComment(id: VerseId, forward: Boolean): VerseId? =
        read { connection ->
            val comparison = if (forward) ">" else "<"
            val direction = if (forward) "ASC" else "DESC"
            connection.queryFirst(
                "SELECT id FROM verse WHERE id $comparison ? AND has_comment = 1 " +
                    "ORDER BY id $direction LIMIT 1",
                bind = { it.bindLong(1, id.raw.toLong()) },
            ) { VerseId(it.getLong(0).toInt()) }
        }

    // ── búsqueda ────────────────────────────────────────────────────────────

    suspend fun searchCounts(ftsQuery: String): SearchCounts = read { connection ->
        connection.queryFirst(
            "SELECT (SELECT count(*) FROM comment_fts WHERE comment_fts MATCH ?1), " +
                "(SELECT count(*) FROM verse_fts WHERE verse_fts MATCH ?1)",
            bind = { it.bindText(1, ftsQuery) },
        ) { SearchCounts(comments = it.getLong(0).toInt(), verses = it.getLong(1).toInt()) }
            ?: SearchCounts(0, 0)
    }

    suspend fun search(
        ftsQuery: String,
        kind: SearchKind?,
        bookId: Int?,
        offset: Int,
        limit: Int,
    ): List<SearchHit> = read { connection ->
        connection.queryAll(
            SEARCH_SQL,
            bind = { statement ->
                statement.bindText(1, ftsQuery)
                statement.bindLong(2, (bookId ?: 0).toLong())
                statement.bindLong(3, kind?.let { if (it == SearchKind.COMMENT) 1L else 0L } ?: -1L)
                statement.bindLong(4, limit.toLong())
                statement.bindLong(5, offset.toLong())
            },
        ) { row ->
            SearchHit(
                verseId = VerseId(row.getLong(0).toInt()),
                kind = if (row.getLong(1) == 1L) SearchKind.COMMENT else SearchKind.VERSE,
                reference = row.getText(3),
                snippet = parseHighlights(row.getText(2)),
                hasComment = row.getLong(4) == 1L,
            )
        }
    }

    suspend fun facets(ftsQuery: String, limit: Int = 12): List<BookFacet> =
        read { connection ->
            connection.queryAll(
                "SELECT v.book_id, b.title, count(*) FROM comment_fts " +
                    "JOIN verse v ON v.id = comment_fts.rowid " +
                    "JOIN book b ON b.id = v.book_id " +
                    "WHERE comment_fts MATCH ?1 " +
                    "GROUP BY v.book_id ORDER BY count(*) DESC, v.book_id LIMIT ?2",
                bind = {
                    it.bindText(1, ftsQuery)
                    it.bindLong(2, limit.toLong())
                },
            ) { row ->
                BookFacet(
                    bookId = row.getLong(0).toInt(),
                    title = row.getText(1),
                    count = row.getLong(2).toInt(),
                )
            }
        }

    // ── lectura de filas ────────────────────────────────────────────────────

    private fun readBook(row: SQLiteStatement) = Book(
        id = row.getLong(0).toInt(),
        testament = if (row.getLong(1) == 0L) Testament.OLD else Testament.NEW,
        title = row.getText(2),
        shortTitle = row.getText(3),
        chapterCount = row.getLong(4).toInt(),
        verseCount = row.getLong(5).toInt(),
        commentCount = row.getLong(6).toInt(),
        hasIntro = row.getLong(7) == 1L,
    )

    private fun readChapter(row: SQLiteStatement) = Chapter(
        id = ChapterId(row.getLong(0).toInt()),
        bookId = row.getLong(1).toInt(),
        number = row.getLong(2).toInt(),
        verseCount = row.getLong(3).toInt(),
        lastVerse = row.getLong(4).toInt(),
        commentCount = row.getLong(5).toInt(),
        summary = row.getText(6),
    )

    private fun readVerse(row: SQLiteStatement) = Verse(
        id = VerseId(row.getLong(0).toInt()),
        number = row.getLong(1).toInt(),
        reference = row.getText(2),
        text = row.getText(3),
        hasText = row.getLong(4) == 1L,
        hasComment = row.getLong(5) == 1L,
    )

    private fun readComment(
        connection: SQLiteConnection,
        id: VerseId,
        highlightQuery: String?,
    ): Comment? {
        // Al llegar desde un resultado de búsqueda se pide el cuerpo con los
        // términos ya marcados por FTS5, para resaltarlos dentro del comentario
        // completo y no solo en el extracto.
        val sql = if (highlightQuery == null) {
            "SELECT body, lemma_spans, egw_refs, is_crossref FROM comment WHERE verse_id = ?"
        } else {
            "SELECT highlight(comment_fts, 0, '$HIGHLIGHT_OPEN', '$HIGHLIGHT_CLOSE'), " +
                "c.lemma_spans, c.egw_refs, c.is_crossref " +
                "FROM comment_fts JOIN comment c ON c.verse_id = comment_fts.rowid " +
                "WHERE comment_fts.rowid = ? AND comment_fts MATCH ?"
        }

        return connection.queryFirst(
            sql,
            bind = { statement ->
                statement.bindLong(1, id.raw.toLong())
                if (highlightQuery != null) statement.bindText(2, highlightQuery)
            },
        ) { row ->
            Comment(
                verseId = id,
                blocks = toBlocks(row.getText(0), row.getText(1)),
                egwReferences = row.getText(2),
                isCrossReference = row.getLong(3) == 1L,
            )
        }
    }

    /**
     * El exportador ya separó el comentario en bloques (unidos por "\n\n") y
     * apuntó dónde empieza y cuánto mide el lema de cada uno.
     */
    private fun toBlocks(marked: String, spansJson: String): List<CommentBlock> {
        // Se quitan primero las marcas de resaltado: `highlight()` las inserta
        // dentro del texto y desplazaría todos los offsets de los lemas, que se
        // calcularon sobre el cuerpo limpio.
        val (body, highlights) = parseHighlights(marked)

        val spans = runCatching { json.decodeFromString<List<List<Int>>>(spansJson) }
            .getOrDefault(emptyList())
        val starts = spans.associate { it[0] to it[1] }

        var offset = 0
        return body.split(BLOCK_SEPARATOR).map { chunk ->
            val blockStart = offset
            val length = starts[blockStart]
            offset += chunk.length + BLOCK_SEPARATOR.length

            // El lema se guardó sin el punto final; se le devuelve aquí para
            // que la interfaz lo pinte tal como aparece en el libro.
            val lemmaLength = when {
                length == null || length > chunk.length -> 0
                chunk.getOrNull(length) == '.' -> length + 1
                else -> length
            }
            val text = chunk.substring(lemmaLength).trimStart()
            val bodyOffset = chunk.length - text.length

            CommentBlock(
                lemma = if (lemmaLength == 0) null else chunk.substring(0, lemmaLength),
                text = text,
                highlights = highlights.toBlockRanges(blockStart, chunk.length, bodyOffset),
            )
        }
    }

    /** Traslada los tramos resaltados del cuerpo completo a un bloque concreto. */
    private fun List<IntRange>.toBlockRanges(
        blockStart: Int,
        blockLength: Int,
        textOffset: Int,
    ): List<IntRange> {
        if (isEmpty()) return emptyList()
        val blockEnd = blockStart + blockLength
        return mapNotNull { range ->
            if (range.first < blockStart || range.last >= blockEnd) return@mapNotNull null
            val start = range.first - blockStart - textOffset
            val end = range.last - blockStart - textOffset
            if (start < 0) null else start..end
        }
    }

    private companion object {
        const val BLOCK_SEPARATOR = "\n\n"
        val json = Json { ignoreUnknownKeys = true }

        /**
         * Busca en comentarios y versículos a la vez.
         *
         * Cada rama se acota a [SEARCH_POOL] filas antes de mezclarlas: así el
         * conjunto ordenado nunca pasa de 800 y paginar con OFFSET sigue siendo
         * barato. `bm25()` devuelve valores negativos (más negativo = mejor),
         * de ahí el ORDER BY ascendente; el factor 1,35 de los versículos los
         * favorece frente a los comentarios.
         */
        const val SEARCH_POOL = 400

        val SEARCH_SQL = """
            WITH cmt AS (
              SELECT rowid AS verse_id, 1 AS kind, bm25(comment_fts) AS score,
                     snippet(comment_fts, 0, '$HIGHLIGHT_OPEN', '$HIGHLIGHT_CLOSE', '…', 14) AS snip
              FROM comment_fts WHERE comment_fts MATCH ?1
              ORDER BY bm25(comment_fts) LIMIT $SEARCH_POOL
            ),
            vrs AS (
              SELECT rowid AS verse_id, 0 AS kind, bm25(verse_fts) * 1.35 AS score,
                     snippet(verse_fts, 0, '$HIGHLIGHT_OPEN', '$HIGHLIGHT_CLOSE', '…', 14) AS snip
              FROM verse_fts WHERE verse_fts MATCH ?1
              ORDER BY bm25(verse_fts) LIMIT $SEARCH_POOL
            ),
            hits AS (SELECT * FROM cmt UNION ALL SELECT * FROM vrs)
            SELECT h.verse_id, h.kind, h.snip, v.ref, v.has_comment
            FROM hits h JOIN verse v ON v.id = h.verse_id
            WHERE (?2 = 0 OR v.book_id = ?2) AND (?3 = -1 OR h.kind = ?3)
            ORDER BY h.score ASC, h.verse_id ASC
            LIMIT ?4 OFFSET ?5
        """.trimIndent()
    }
}
