package com.comentariobiblico.app.data.db

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class DatabaseBundle(private val context: Context) {

    /**
     * Copia la base de los assets a `filesDir` la primera vez. Son 43 MB, así
     * que la llamada va siempre fuera del hilo principal y la copia se hace a
     * un `.part` que se renombra al terminar: si la app muere a mitad, el
     * archivo incompleto nunca ocupa el sitio del bueno.
     */
    actual suspend fun resolvePath(): String = withContext(Dispatchers.IO) {
        val directory = File(context.filesDir, "db").apply { mkdirs() }
        val target = File(directory, DATABASE_FILE)

        if (!target.exists()) {
            val partial = File(directory, "$DATABASE_FILE.part")
            partial.delete()
            context.assets.open(DATABASE_FILE).use { input ->
                partial.outputStream().buffered(BUFFER_SIZE).use { output ->
                    input.copyTo(output, BUFFER_SIZE)
                }
            }
            check(partial.renameTo(target)) { "No se pudo instalar $DATABASE_FILE" }

            // Versiones anteriores de la base dejan de hacer falta en cuanto
            // hay una nueva: si no, cada actualización duplicaría 43 MB.
            directory.listFiles()?.forEach { file ->
                if (file != target) file.delete()
            }
        }

        target.absolutePath
    }

    private companion object {
        const val BUFFER_SIZE = 1 shl 16
    }
}
