package com.comentariobiblico.app.data.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * La conexión a una base inmutable de solo lectura.
 *
 * `SQLiteConnection` no es thread-safe, así que todo acceso pasa por un mutex.
 * No es un cuello de botella real: cada consulta resuelve en menos de un
 * milisegundo porque todas las claves son rowid.
 */
class AppDatabase(private val connection: SQLiteConnection) {

    private val mutex = Mutex()

    suspend fun <T> read(block: (SQLiteConnection) -> T): T =
        withContext(Dispatchers.IO) { mutex.withLock { block(connection) } }

    fun close() = connection.close()
}

/**
 * Abre la base sin bloquear el arranque.
 *
 * El primer acceso en Android tiene que copiar 43 MB de los assets, así que la
 * apertura se expone como [Deferred]: la pantalla de bienvenida puede esperarla
 * mientras la interfaz ya está compuesta.
 */
class DatabaseFactory(
    private val bundle: DatabaseBundle,
    scope: CoroutineScope,
) {
    val database: Deferred<AppDatabase> = scope.async(Dispatchers.IO) { open() }

    private suspend fun open(): AppDatabase {
        val connection = BundledSQLiteDriver().open(bundle.resolvePath())
        connection.execSQL("PRAGMA query_only = 1")
        connection.execSQL("PRAGMA cache_size = -8000")
        connection.execSQL("PRAGMA mmap_size = 67108864")
        connection.execSQL("PRAGMA temp_store = MEMORY")

        val version = connection.queryFirst(
            "SELECT value FROM meta WHERE key = 'schema_version'",
        ) { it.getText(0) }?.toIntOrNull()

        check(version == EXPECTED_SCHEMA_VERSION) {
            "La base tiene esquema $version y esta versión de la app espera " +
                "$EXPECTED_SCHEMA_VERSION. Regenérala con tools/export-db."
        }

        return AppDatabase(connection)
    }
}

// ── utilidades de consulta ──────────────────────────────────────────────────

inline fun <T> SQLiteConnection.queryAll(
    sql: String,
    bind: (SQLiteStatement) -> Unit = {},
    map: (SQLiteStatement) -> T,
): List<T> {
    val statement = prepare(sql)
    try {
        bind(statement)
        val rows = mutableListOf<T>()
        while (statement.step()) rows += map(statement)
        return rows
    } finally {
        statement.close()
    }
}

inline fun <T> SQLiteConnection.queryFirst(
    sql: String,
    bind: (SQLiteStatement) -> Unit = {},
    map: (SQLiteStatement) -> T,
): T? {
    val statement = prepare(sql)
    try {
        bind(statement)
        return if (statement.step()) map(statement) else null
    } finally {
        statement.close()
    }
}
