package com.comentariobiblico.app.data.db

import com.comentariobiblico.app.domain.model.CommentBlock
import kotlinx.serialization.json.Json

private const val BLOCK_SEPARATOR = "\n\n"

/**
 * Cierres posibles del lema.
 *
 * No solo el punto: buena parte de los lemas son preguntas citadas del
 * versículo ("¿Dónde estás tú?", "¿Qué has hecho?"), y tratarlos como texto
 * corriente dejaba cientos de comentarios sin encabezado destacado.
 */
private val LEMMA_TERMINATORS = setOf('.', '?', '!')

private val json = Json { ignoreUnknownKeys = true }

/**
 * Convierte el comentario guardado en los bloques que pinta el lector.
 *
 * El exportador ya hizo el trabajo difícil: separó los bloques con "\n\n" y
 * anotó dónde empieza y cuánto mide el lema de cada uno. Aquí solo se
 * reconstruye, se le devuelve al lema su signo de cierre y se trasladan los
 * tramos resaltados de la búsqueda a coordenadas de cada bloque.
 */
internal fun parseCommentBlocks(marked: String, spansJson: String): List<CommentBlock> {
    // Se quitan primero las marcas de resaltado: `highlight()` las inserta
    // dentro del texto y desplazaría todos los offsets de los lemas, que se
    // calcularon sobre el cuerpo limpio.
    val (body, highlights) = parseHighlights(marked)

    val spans = runCatching { json.decodeFromString<List<List<Int>>>(spansJson) }
        .getOrDefault(emptyList())
    val lemmaLengthAt = spans.filter { it.size >= 2 }.associate { it[0] to it[1] }

    var offset = 0
    return body.split(BLOCK_SEPARATOR).map { chunk ->
        val blockStart = offset
        val declared = lemmaLengthAt[blockStart]
        offset += chunk.length + BLOCK_SEPARATOR.length

        val lemmaLength = when {
            declared == null || declared > chunk.length -> 0
            chunk.getOrNull(declared) in LEMMA_TERMINATORS -> declared + 1
            else -> declared
        }
        val text = chunk.substring(lemmaLength).trimStart()

        CommentBlock(
            lemma = if (lemmaLength == 0) null else chunk.substring(0, lemmaLength),
            text = text,
            highlights = highlights.toBlockRanges(
                blockStart = blockStart,
                blockLength = chunk.length,
                textOffset = chunk.length - text.length,
            ),
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
