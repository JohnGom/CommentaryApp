package com.comentariobiblico.app.ui.reader

import com.comentariobiblico.app.domain.model.VerseId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScriptureReferencesTest {

    /** Mateo 5:3, el contexto desde el que se resuelven las citas relativas. */
    private val context = VerseId.of(40, 5, 3)

    private fun targets(text: String) =
        findScriptureLinks(text, context).map { it.target }

    @Test
    fun `resuelve citas completas con abreviatura`() {
        assertEquals(
            listOf(VerseId.of(58, 1, 10)),
            targets("recuerda lo dicho en Heb. 1: 10-12 sobre el tiempo"),
        )
        assertEquals(
            listOf(VerseId.of(19, 90, 2)),
            targets("véase Sal. 90: 2, 10"),
        )
        assertEquals(
            listOf(VerseId.of(1, 1, 26)),
            targets("como en Gén. 1: 26, 27"),
        )
    }

    @Test
    fun `distingue los libros numerados`() {
        assertEquals(listOf(VerseId.of(62, 3, 1)), targets("1 Juan 3: 1-3"))
        assertEquals(listOf(VerseId.of(43, 3, 16)), targets("Juan 3: 16"))
        assertEquals(listOf(VerseId.of(46, 13, 4)), targets("1 Cor. 13: 4"))
        assertEquals(listOf(VerseId.of(47, 5, 17)), targets("2 Cor. 5: 17"))
    }

    @Test
    fun `cap y vers se resuelven contra el contexto`() {
        // "cap. 21: 7" dentro de Mateo apunta a Mateo 21:7.
        assertEquals(listOf(VerseId.of(40, 21, 7)), targets("ver com. cap. 21: 7"))
        // "vers. 8" apunta al versículo 8 del mismo capítulo.
        assertEquals(listOf(VerseId.of(40, 5, 8)), targets("Ver com. vers. 8."))
    }

    /**
     * La obra cita `1 Mac.` y `2 Mac.` 45 veces, pero los macabeos no están
     * entre los 66 libros de esta base: enlazarlos llevaría a ninguna parte.
     */
    @Test
    fun `no enlaza libros que no estan en la obra`() {
        assertTrue(targets("según 1 Mac. 4: 59").isEmpty())
        assertTrue(targets("según 2 Mac. 7: 9").isEmpty())
    }

    @Test
    fun `ignora numeros que no son citas`() {
        assertTrue(targets("durante 40 años en el desierto").isEmpty())
        assertTrue(targets("en el año 1916 se publicó").isEmpty())
        assertTrue(targets("una proporción de 3: 1 en la muestra").isEmpty())
    }

    @Test
    fun `el tramo marcado cubre exactamente la cita`() {
        val text = "recuerda lo dicho en Heb. 1: 10-12 sobre el tiempo"
        val link = findScriptureLinks(text, context).single()
        assertEquals("Heb. 1: 10", text.substring(link.range.first, link.range.last + 1))
    }

    @Test
    fun `tolera la falta de punto y los acentos de la abreviatura`() {
        assertEquals(listOf(VerseId.of(30, 5, 24)), targets("Amós 5: 24"))
        assertEquals(listOf(VerseId.of(41, 12, 30)), targets("Mar 12: 30"))
    }
}
