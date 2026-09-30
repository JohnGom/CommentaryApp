package com.comentariobiblico.app.data.db

/** Nombre versionado: al cambiar de contenido cambia el archivo y se reinstala. */
const val DATABASE_FILE = "comentarios_v1.db"

/** Versión de esquema que este código sabe leer. Se valida contra la tabla `meta`. */
const val EXPECTED_SCHEMA_VERSION = 1

/**
 * Deja la base en una ruta que SQLite pueda abrir y la devuelve.
 *
 * Las plataformas son asimétricas a propósito: en Android hay que copiar el
 * archivo de los assets a `filesDir` porque SQLite no puede abrir nada que esté
 * dentro del APK; en iOS el bundle ya está en disco y, como abrimos en solo
 * lectura, no hace falta copiar nada. Por eso la abstracción es "resuélveme una
 * ruta" y no "copia el archivo".
 */
expect class DatabaseBundle {
    suspend fun resolvePath(): String
}
