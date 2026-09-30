package com.comentariobiblico.app.data.db

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FtsQueryTest {

    @Test
    fun `varias palabras se combinan con AND`() {
        assertEquals("\"espiritu\" AND \"santo\"", toFtsQuery("espiritu santo"))
    }

    @Test
    fun `entre comillas busca la frase exacta`() {
        assertEquals("\"espiritu santo\"", toFtsQuery("\"espiritu santo\""))
    }

    @Test
    fun `el prefijo solo se aplica al ultimo termino`() {
        assertEquals("\"reino\" AND \"ciel\"*", toFtsQuery("reino ciel", prefix = true))
    }

    /**
     * Lo importante de verdad: los caracteres de sintaxis de FTS5 nunca pueden
     * llegar al MATCH, o SQLite devuelve un error en lugar de resultados.
     */
    @Test
    fun `los caracteres de sintaxis de FTS5 se neutralizan`() {
        assertEquals("\"gracia\"", toFtsQuery("gracia*"))
        assertEquals("\"gracia\"", toFtsQuery("-gracia"))
        assertEquals("\"gracia\" AND \"fe\"", toFtsQuery("gracia: fe"))
        assertEquals("\"gracia\" AND \"NEAR\" AND \"fe\"", toFtsQuery("gracia NEAR fe"))
        assertEquals("\"gracia\"", toFtsQuery("\"gracia"))
    }

    @Test
    fun `una consulta sin nada buscable devuelve null`() {
        assertNull(toFtsQuery(""))
        assertNull(toFtsQuery("   "))
        assertNull(toFtsQuery("a"))
        assertNull(toFtsQuery("*-:"))
        assertNull(toFtsQuery("\"\""))
    }

    @Test
    fun `las tildes se conservan porque las pliega el tokenizer de SQLite`() {
        assertEquals("\"Espíritu\"", toFtsQuery("Espíritu"))
    }
}

class HighlightTest {

    @Test
    fun `extrae el texto limpio y los tramos marcados`() {
        val snippet = parseHighlights("El ⟦Espíritu⟧ ⟦Santo⟧ cayó")
        assertEquals("El Espíritu Santo cayó", snippet.text)
        assertEquals(listOf(3 until 11, 12 until 17), snippet.highlights)
        assertEquals("Espíritu", snippet.text.substring(snippet.highlights[0]))
        assertEquals("Santo", snippet.text.substring(snippet.highlights[1]))
    }

    @Test
    fun `un texto sin marcas pasa intacto`() {
        val snippet = parseHighlights("En el principio creó Dios")
        assertEquals("En el principio creó Dios", snippet.text)
        assertEquals(emptyList(), snippet.highlights)
    }

    @Test
    fun `una marca vacia no genera tramo`() {
        val snippet = parseHighlights("abc⟦⟧def")
        assertEquals("abcdef", snippet.text)
        assertEquals(emptyList(), snippet.highlights)
    }
}

class FoldTest {

    @Test
    fun `pliega tildes y mayusculas como title_norm`() {
        assertEquals("genesis", foldSpanish("Génesis"))
        assertEquals("exodo", foldSpanish("Éxodo"))
        assertEquals("1 cronicas", foldSpanish("1 Crónicas"))
        assertEquals("nahum", foldSpanish("Nahúm"))
    }
}

private fun String.substring(range: IntRange) = substring(range.first, range.last + 1)
