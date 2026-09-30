package com.comentariobiblico.app.data.db

/**
 * Minúsculas sin diacríticos, igual que la columna `book.title_norm` que genera
 * el exportador. Se usa para el filtro instantáneo de libros, donde FTS5 sería
 * exagerado: son 66 filas.
 *
 * Basta con el repertorio del español porque solo se aplica a nombres de libros.
 * La búsqueda de texto no pasa por aquí: de eso se encarga el tokenizer
 * `unicode61 remove_diacritics 2` dentro de SQLite.
 */
fun foldSpanish(text: String): String = buildString(text.length) {
    for (character in text.lowercase()) {
        append(
            when (character) {
                'á', 'à', 'ä', 'â' -> 'a'
                'é', 'è', 'ë', 'ê' -> 'e'
                'í', 'ì', 'ï', 'î' -> 'i'
                'ó', 'ò', 'ö', 'ô' -> 'o'
                'ú', 'ù', 'ü', 'û' -> 'u'
                'ñ' -> 'n'
                'ç' -> 'c'
                else -> character
            },
        )
    }
}
