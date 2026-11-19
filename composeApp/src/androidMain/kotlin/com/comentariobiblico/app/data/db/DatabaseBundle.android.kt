package com.comentariobiblico.app.data.db

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class DatabaseBundle(private val context: Context) {

    /**
     * Copia la base de los assets a `filesDir` y devuelve su ruta.
     *
     * Son 43 MB, así que la llamada va siempre fuera del hilo principal y la
     * copia se hace a un `.part` que se renombra al terminar: si la app muere
     * a mitad, el archivo incompleto nunca ocupa el sitio del bueno.
     *
     * La decisión de recopiar no mira si el archivo existe, sino si su
     * contenido es el mismo: el build empaqueta el SHA-256 de la base junto a
     * ella, y aquí se compara con el de la copia instalada. Comprobar solo la
     * existencia dejaría a quien ya tiene la app con la base antigua para
     * siempre, porque el nombre del archivo no cambia al actualizar el texto.
     */
    actual suspend fun resolvePath(): String = withContext(Dispatchers.IO) {
        val directory = File(context.filesDir, "db").apply { mkdirs() }
        val target = File(directory, DATABASE_FILE)
        val marker = File(directory, "$DATABASE_FILE.sha256")

        val packaged = readPackagedChecksum()
        val installed = marker.takeIf { it.exists() }?.readText()?.trim()

        if (!target.exists() || packaged == null || packaged != installed) {
            val partial = File(directory, "$DATABASE_FILE.part")
            partial.delete()
            context.assets.open(DATABASE_FILE).use { input ->
                partial.outputStream().buffered(BUFFER_SIZE).use { output ->
                    input.copyTo(output, BUFFER_SIZE)
                }
            }
            check(partial.renameTo(target)) { "No se pudo instalar $DATABASE_FILE" }

            if (packaged != null) marker.writeText(packaged) else marker.delete()

            // Restos de versiones anteriores: sin esto cada actualización
            // dejaría otros 43 MB olvidados en el dispositivo.
            directory.listFiles()?.forEach { file ->
                if (file != target && file != marker) file.delete()
            }
        }

        target.absolutePath
    }

    private fun readPackagedChecksum(): String? = runCatching {
        context.assets.open("$DATABASE_FILE.sha256").use { it.readBytes().decodeToString().trim() }
    }.getOrNull()

    private companion object {
        const val BUFFER_SIZE = 1 shl 16
    }
}
