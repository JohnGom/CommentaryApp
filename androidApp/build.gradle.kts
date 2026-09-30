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

// La base son 43 MB generados: no vive en git, se regenera con tools/export-db.
// Aquí se comprueba que existe y que coincide con el .sha256 que sí está
// versionado, y se instala en los assets.
//
// Va en androidApp y no en composeApp porque el plugin KMP library no expone
// una carpeta de assets propia. No es un problema: el código compartido llega
// al archivo a través de DatabaseBundle, que ya es específico de cada
// plataforma (en iOS el .db irá dentro del bundle de iosApp).
val installDatabaseAsset = tasks.register("installDatabaseAsset") {
    val source = rootProject.layout.projectDirectory.file("data/comentarios_v1.db")
    val checksumFile = rootProject.layout.projectDirectory.file("data/comentarios_v1.db.sha256")
    val target = layout.projectDirectory.file("src/main/assets/comentarios_v1.db")

    inputs.file(source).withPropertyName("database")
    inputs.file(checksumFile).withPropertyName("checksum")
    outputs.file(target).withPropertyName("asset")

    doLast {
        val db = source.asFile
        if (!db.exists()) {
            throw GradleException(
                "Falta data/comentarios_v1.db. Genérala con:\n" +
                    "  cd tools/export-db && ./.venv/bin/python export_db.py --out ../../data/comentarios_v1.db",
            )
        }

        val expected = checksumFile.asFile.readText().trim().substringBefore(' ')
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
                "data/comentarios_v1.db no coincide con su .sha256 " +
                    "(esperado $expected, encontrado $actual). Regenérala con tools/export-db.",
            )
        }

        val out = target.asFile
        out.parentFile.mkdirs()
        db.copyTo(out, overwrite = true)
    }
}

tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }.configureEach {
    dependsOn(installDatabaseAsset)
}
