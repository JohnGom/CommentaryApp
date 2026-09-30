package com.comentariobiblico.app.ui.reader

import com.comentariobiblico.app.data.db.foldSpanish
import com.comentariobiblico.app.domain.model.VerseId

/**
 * Convierte las citas que aparecen dentro de los comentarios en destinos
 * navegables: `Heb. 1: 10-12`, `Sal. 90: 2, 10`, `1 Juan 3: 1-3`, `cap. 21: 7`,
 * `vers. 8`.
 *
 * Se resuelve en tiempo de ejecución y no en el exportador: cuesta menos de
 * 5 ms por comentario y mantiene la base simple.
 *
 * Las abreviaturas salen del corpus real, no de una convención supuesta (se
 * extrajeron las 202 formas distintas que aparecen en los 24.892 comentarios).
 * `1 Mac.` y `2 Mac.` aparecen 45 veces pero son libros apócrifos que esta
 * obra no incluye: al no estar en el mapa, simplemente no se enlazan.
 */
private val BOOK_ABBREVIATIONS: Map<String, Int> = mapOf(
    "gen" to 1, "genesis" to 1,
    "exo" to 2, "exod" to 2, "exodo" to 2,
    "lev" to 3, "levitico" to 3,
    "num" to 4, "numeros" to 4,
    "deut" to 5, "deuteronomio" to 5,
    "jos" to 6, "josue" to 6,
    "juec" to 7, "jueces" to 7,
    "rut" to 8,
    "1 sam" to 9, "1 samuel" to 9,
    "2 sam" to 10, "2 samuel" to 10,
    "1 rey" to 11, "1 reyes" to 11,
    "2 rey" to 12, "2 reyes" to 12,
    "1 cron" to 13, "1 cronicas" to 13,
    "2 cron" to 14, "2 cronicas" to 14,
    "esd" to 15, "esdras" to 15,
    "neh" to 16, "nehemias" to 16,
    "est" to 17, "ester" to 17,
    "job" to 18,
    "sal" to 19, "salmo" to 19, "salmos" to 19,
    "prov" to 20, "proverbios" to 20,
    "ecl" to 21, "eclesiastes" to 21,
    "cant" to 22, "cantares" to 22,
    "isa" to 23, "isaias" to 23,
    "jer" to 24, "jeremias" to 24,
    "lam" to 25, "lamentaciones" to 25,
    "eze" to 26, "ezeq" to 26, "ezequiel" to 26,
    "dan" to 27, "daniel" to 27,
    "ose" to 28, "oseas" to 28,
    "joel" to 29,
    "amos" to 30,
    "abd" to 31, "abdias" to 31,
    "jon" to 32, "jonas" to 32,
    "miq" to 33, "miqueas" to 33,
    "nah" to 34, "nahum" to 34,
    "hab" to 35, "habacuc" to 35,
    "sof" to 36, "sofonias" to 36,
    "hag" to 37, "hageo" to 37,
    "zac" to 38, "zacarias" to 38,
    "mal" to 39, "malaquias" to 39,
    "mat" to 40, "mateo" to 40,
    "mar" to 41, "marcos" to 41,
    "luc" to 42, "lucas" to 42,
    "juan" to 43,
    "hech" to 44, "hechos" to 44,
    "rom" to 45, "romanos" to 45,
    "1 cor" to 46, "1 corintios" to 46, "1 con" to 46, // "1 Con" es un error de OCR
    "2 cor" to 47, "2 corintios" to 47,
    "gal" to 48, "galatas" to 48,
    "efe" to 49, "efesios" to 49,
    "fil" to 50, "filip" to 50, "filipenses" to 50,
    "col" to 51, "colosenses" to 51,
    "1 tes" to 52, "1 tesalonicenses" to 52,
    "2 tes" to 53, "2 tesalonicenses" to 53,
    "1 tim" to 54, "1 timoteo" to 54,
    "2 tim" to 55, "2 timoteo" to 55,
    "tito" to 56,
    "flm" to 57, "filem" to 57, "filemon" to 57,
    "heb" to 58, "hebreos" to 58,
    "sant" to 59, "santiago" to 59,
    "1 ped" to 60, "1 pedro" to 60,
    "2 ped" to 61, "2 pedro" to 61,
    "1 juan" to 62,
    "2 juan" to 63,
    "3 juan" to 64,
    "jud" to 65, "judas" to 65,
    "apoc" to 66, "apocalipsis" to 66,
)

/**
 * Tres formas, en orden de prioridad:
 *  1. `vers. 8`            — mismo libro y capítulo
 *  2. `cap. 21: 7`         — mismo libro
 *  3. `1 Juan 3: 1-3`      — cita completa
 */
private val REFERENCE = Regex(
    """(?:\bvers\.?\s+(\d{1,3}))""" +
        """|(?:\bcaps?\.?\s+(\d{1,3})\s*:\s*(\d{1,3}))""" +
        """|(?:\b((?:[123]\s+)?\p{Lu}\p{L}{1,11})\.?\s+(\d{1,3})\s*:\s*(\d{1,3}))""",
)

data class ScriptureLink(val range: IntRange, val target: VerseId)

/** Encuentra las citas de [text] y las resuelve relativas a [context]. */
fun findScriptureLinks(text: String, context: VerseId): List<ScriptureLink> =
    REFERENCE.findAll(text).mapNotNull { match ->
        val (verseOnly, chapterRef, chapterVerse, bookName, bookChapter, bookVerse) =
            match.destructured

        val target = when {
            verseOnly.isNotEmpty() -> verseOnly.toIntOrNull()?.let {
                VerseId.of(context.bookId, context.chapter, it)
            }

            chapterRef.isNotEmpty() -> {
                val chapter = chapterRef.toIntOrNull()
                val verse = chapterVerse.toIntOrNull()
                if (chapter != null && verse != null) {
                    VerseId.of(context.bookId, chapter, verse)
                } else {
                    null
                }
            }

            bookName.isNotEmpty() -> {
                val book = BOOK_ABBREVIATIONS[normalizeAbbreviation(bookName)]
                val chapter = bookChapter.toIntOrNull()
                val verse = bookVerse.toIntOrNull()
                if (book != null && chapter != null && verse != null) {
                    VerseId.of(book, chapter, verse)
                } else {
                    null
                }
            }

            else -> null
        }

        target?.let { ScriptureLink(match.range, it) }
    }.toList()

private fun normalizeAbbreviation(raw: String): String =
    foldSpanish(raw).replace(Regex("\\s+"), " ").trim()
