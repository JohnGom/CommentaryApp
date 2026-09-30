package com.comentariobiblico.app.data.db

import com.comentariobiblico.app.domain.model.Snippet

/**
 * Delimitadores del resaltado. Se eligieron porque NO aparecen en el corpus,
 * así que nunca pueden confundirse con texto real del comentario.
 */
const val HIGHLIGHT_OPEN = '⟦'   // ⟦
const val HIGHLIGHT_CLOSE = '⟧'  // ⟧

private const val MIN_TOKEN_LENGTH = 2
private val SEPARATORS = Regex("[^\\p{L}\\p{Nd}]+")

/**
 * Traduce lo que escribe el usuario a sintaxis FTS5.
 *
 * Nunca se pasa el texto crudo a `MATCH`: los caracteres `"`, `*`, `-`, `:` y
 * la palabra `NEAR` son sintaxis de FTS5 y provocarían un error de SQLite.
 *
 * Entrecomillar entre `"` convierte la búsqueda en frase exacta.
 * [prefix] añade `*` al último término, para buscar mientras se teclea.
 *
 * Devuelve null cuando no queda nada buscable.
 */
fun toFtsQuery(raw: String, prefix: Boolean = false): String? {
    val trimmed = raw.trim()
    val isPhrase = trimmed.length > 2 && trimmed.startsWith('"') && trimmed.endsWith('"')

    val tokens = trimmed
        .replace('"', ' ')
        .split(SEPARATORS)
        .filter { it.length >= MIN_TOKEN_LENGTH }

    if (tokens.isEmpty()) return null

    if (isPhrase) return tokens.joinToString(" ", prefix = "\"", postfix = "\"")

    return tokens.mapIndexed { index, token ->
        val star = if (prefix && index == tokens.lastIndex) "*" else ""
        "\"$token\"$star"
    }.joinToString(" AND ")
}

/**
 * Convierte el texto que devuelve `snippet()`/`highlight()` en texto limpio más
 * los rangos a resaltar, para poder construir un `AnnotatedString` sin que los
 * delimitadores lleguen nunca a la pantalla.
 */
fun parseHighlights(marked: String): Snippet {
    if (marked.indexOf(HIGHLIGHT_OPEN) < 0) return Snippet(marked, emptyList())

    val text = StringBuilder(marked.length)
    val highlights = mutableListOf<IntRange>()
    var start = -1

    for (character in marked) {
        when (character) {
            HIGHLIGHT_OPEN -> start = text.length
            HIGHLIGHT_CLOSE -> {
                if (start >= 0 && text.length > start) highlights += start until text.length
                start = -1
            }
            else -> text.append(character)
        }
    }

    return Snippet(text.toString(), highlights)
}
