package com.comentariobiblico.app.data.db

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CommentBlocksTest {

    @Test
    fun `separa los bloques y destaca su lema`() {
        // Mateo 5:3 encadena cuatro lemas sin separador en el original.
        val body = "Bienaventurados. Gr. makárioi.\n\nPobres. Gr. ptōjós.\n\nDe ellos. La comprensión."
        val blocks = parseCommentBlocks(body, "[[0,15],[32,6],[53,8]]")

        assertEquals(3, blocks.size)
        assertEquals("Bienaventurados.", blocks[0].lemma)
        assertEquals("Gr. makárioi.", blocks[0].text)
        assertEquals("Pobres.", blocks[1].lemma)
        assertEquals("De ellos.", blocks[2].lemma)
    }

    /**
     * El caso que dejaba cientos de comentarios sin encabezado: el lema es una
     * pregunta citada del versículo, así que cierra con "?" y no con punto.
     */
    @Test
    fun `el lema puede cerrar con interrogacion o exclamacion`() {
        val pregunta = parseCommentBlocks("¿Dónde estás tú? Adán se ocultó.", "[[0,15]]").single()
        assertEquals("¿Dónde estás tú?", pregunta.lemma)
        assertEquals("Adán se ocultó.", pregunta.text)

        val exclamacion = parseCommentBlocks("¡Ay! Lamento profético.", "[[0,3]]").single()
        assertEquals("¡Ay!", exclamacion.lemma)
        assertEquals("Lamento profético.", exclamacion.text)
    }

    @Test
    fun `un comentario sin lema se devuelve entero y sin encabezado`() {
        val block = parseCommentBlocks("Ver com. vers. 8.", "[]").single()
        assertNull(block.lemma)
        assertEquals("Ver com. vers. 8.", block.text)
    }

    @Test
    fun `un span imposible no rompe el bloque`() {
        // Si la base y el código se desincronizan, el lector debe seguir
        // mostrando el texto en vez de reventar con un índice fuera de rango.
        val block = parseCommentBlocks("Texto corto.", "[[0,999]]").single()
        assertNull(block.lemma)
        assertEquals("Texto corto.", block.text)

        val roto = parseCommentBlocks("Texto corto.", "no es json").single()
        assertNull(roto.lemma)
        assertEquals("Texto corto.", roto.text)
    }

    @Test
    fun `los tramos resaltados se recolocan dentro de su bloque`() {
        // El resaltado de FTS5 llega marcado sobre el cuerpo completo; cada
        // bloque debe recibirlo en sus propias coordenadas, ya sin las marcas.
        val marked = "Pobres. Gr. ⟦ptōjós⟧.\n\nDe ellos. El ⟦reino⟧."
        val blocks = parseCommentBlocks(marked, "[[0,6],[21,8]]")

        assertEquals("Pobres.", blocks[0].lemma)
        assertEquals("ptōjós", blocks[0].text.substring(blocks[0].highlights.single()))
        assertEquals("De ellos.", blocks[1].lemma)
        assertEquals("reino", blocks[1].text.substring(blocks[1].highlights.single()))
    }
}

private fun String.substring(range: IntRange) = substring(range.first, range.last + 1)
