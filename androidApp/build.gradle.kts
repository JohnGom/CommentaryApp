import java.io.File
import java.security.MessageDigest

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "com.comentariobiblico.app.android"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.comentariobiblico.app"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    kotlin {
        jvmToolchain(17)
    }

    buildTypes {
        release {
            // Se firma con la clave de depuración solo para poder instalar y
            // medir el build de release en local. Para publicar hay que
            // sustituirla por la clave real de subida.
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.splashscreen)

    // El cascarón arranca Koin, así que necesita el grafo aunque la lógica
    // viva en composeApp.
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
}

/**
 * Instala la base de datos como asset.
 *
 * Comprueba antes que el archivo existe y que su contenido coincide con el
 * `.sha256` versionado: la base son 43 MB generados que no viven en git, y sin
 * esta verificación un checkout limpio produciría una app con datos de otra
 * época sin avisar.
 *
 * Escribe en `build/` y nunca dentro de `src/main/assets`: generar ficheros en
 * el árbol de fuentes hace que cualquier tarea que lo lea (lint, por ejemplo)
 * dependa de ésta de forma implícita, y Gradle rechaza el build.
 */
abstract class InstallDatabaseAsset : DefaultTask() {

    @get:InputFile
    abstract val database: RegularFileProperty

    @get:InputFile
    abstract val expectedChecksum: RegularFileProperty

    @get:Input
    abstract val assetName: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun install() {
        val name = assetName.get()
        val db = database.get().asFile
        if (!db.exists()) {
            throw GradleException(
                "Falta data/$name. Genérala con:\n" +
                    "  cd tools/export-db && ./.venv/bin/python export_db.py --out ../../data/$name",
            )
        }

        val expected = expectedChecksum.get().asFile.readText().trim().substringBefore(' ')
        val digest = MessageDigest.getInstance("SHA-256")
        db.inputStream().buffered().use { input ->
            val buffer = ByteArray(1 shl 20)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        val actual = digest.digest().joinToString("") { byte -> "%02x".format(byte) }
        if (actual != expected) {
            throw GradleException(
                "data/$name no coincide con su .sha256 " +
                    "(esperado $expected, encontrado $actual). Regenérala con tools/export-db.",
            )
        }

        val target = outputDir.get().asFile
        target.mkdirs()
        db.copyTo(File(target, name), overwrite = true)

        // El checksum viaja empaquetado junto a la base para que la app sepa,
        // al arrancar, si la copia que ya tiene instalada sigue siendo la
        // buena. Sin esto una actualización de contenido no llegaría nunca al
        // dispositivo: el archivo existe y se daría por bueno.
        File(target, "$name.sha256").writeText(actual)
    }
}

// Va en androidApp y no en composeApp porque el plugin KMP library no expone
// una carpeta de assets propia. No es un problema: el código compartido llega
// al archivo a través de DatabaseBundle, que ya es específico de cada
// plataforma (en iOS el .db irá dentro del bundle de iosApp).
val installDatabaseAsset = tasks.register<InstallDatabaseAsset>("installDatabaseAsset") {
    assetName.set("comentarios_v1.db") // debe coincidir con DATABASE_FILE de composeApp
    database.set(rootProject.layout.projectDirectory.file("data/comentarios_v1.db"))
    expectedChecksum.set(rootProject.layout.projectDirectory.file("data/comentarios_v1.db.sha256"))
    outputDir.set(layout.buildDirectory.dir("generated/databaseAsset"))
}

// La API de variantes conecta la dependencia sola: no hay que enumerar las
// tareas que consumen los assets ni tocar los source sets.
androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(
            installDatabaseAsset,
            InstallDatabaseAsset::outputDir,
        )
    }
}
