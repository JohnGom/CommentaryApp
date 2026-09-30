package com.comentariobiblico.app.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VerseIdTest {

    @Test
    fun `el id se descompone en libro capitulo y versiculo`() {
        val id = VerseId.of(book = 1, chapter = 1, verse = 1)
        assertEquals(1_001_001, id.raw)
        assertEquals(1, id.bookId)
        assertEquals(1, id.chapter)
        assertEquals(1, id.verse)
    }

    @Test
    fun `ida y vuelta en los extremos del canon`() {
        val cases = listOf(
            Triple(1, 1, 1),          // Génesis 1:1
            Triple(19, 119, 176),     // Salmos 119:176, el capítulo más largo
            Triple(66, 22, 21),       // Apocalipsis 22:21
        )
        for ((book, chapter, verse) in cases) {
            val id = VerseId.of(book, chapter, verse)
            assertEquals(book, id.bookId)
            assertEquals(chapter, id.chapter)
            assertEquals(verse, id.verse)
        }
    }

    @Test
    fun `el id mayor posible cabe en un Int`() {
        val biggest = VerseId.of(book = 66, chapter = 150, verse = 176)
        assertTrue(biggest.raw in 1..Int.MAX_VALUE, "desbordó: ${biggest.raw}")
    }

    @Test
    fun `el orden de los ids es el orden canonico de lectura`() {
        val malaquias = VerseId.of(39, 4, 6)
        val mateo = VerseId.of(40, 1, 1)
        assertTrue(malaquias.raw < mateo.raw)
    }

    @Test
    fun `el id del capitulo se deriva del id del versiculo`() {
        val id = VerseId.of(book = 40, chapter = 5, verse = 3)
        assertEquals(ChapterId.of(40, 5), id.chapterId)
        assertEquals(40, id.chapterId.bookId)
        assertEquals(5, id.chapterId.number)
    }
}
