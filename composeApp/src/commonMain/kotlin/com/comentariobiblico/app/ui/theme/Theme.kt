package com.comentariobiblico.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.comentariobiblico.app.domain.model.ReaderSettings
import com.comentariobiblico.app.domain.model.ThemeMode

/**
 * Tipografías del lector, ya escaladas según los ajustes del usuario.
 *
 * Van por un CompositionLocal propio en lugar de deformar el `Typography` de
 * Material: así el texto largo crece y se separa, mientras los botones, chips y
 * barras conservan su tamaño y no se descuadra la interfaz.
 */
data class ReaderTypography(
    val verse: TextStyle,
    val body: TextStyle,
    val lemma: TextStyle,
    val reference: TextStyle,
)

val LocalReaderTypography: ProvidableCompositionLocal<ReaderTypography> =
    staticCompositionLocalOf { error("ReaderTypography no está disponible") }

val LocalReaderSettings: ProvidableCompositionLocal<ReaderSettings> =
    staticCompositionLocalOf { ReaderSettings() }

@Composable
fun ComentarioTheme(
    settings: ReaderSettings = ReaderSettings(),
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val colors = when (settings.theme) {
        ThemeMode.SYSTEM -> if (systemDark) DarkColors else LightColors
        ThemeMode.LIGHT -> LightColors
        ThemeMode.DARK -> DarkColors
        ThemeMode.SEPIA -> SepiaColors
    }

    val serif = if (settings.serif) FontFamily.Serif else FontFamily.SansSerif
    val readerTypography = remember(settings.fontScale, settings.lineHeight, settings.serif) {
        val scale = settings.fontScale
        val height = settings.lineHeight.multiplier
        ReaderTypography(
            verse = TextStyle(
                fontFamily = serif,
                fontSize = (19 * scale).sp,
                lineHeight = height.em,
                fontStyle = FontStyle.Italic,
            ),
            body = TextStyle(
                fontFamily = serif,
                fontSize = (17 * scale).sp,
                lineHeight = height.em,
                textAlign = TextAlign.Justify,
            ),
            // El lema no crece respecto al cuerpo: la negrita ya jerarquiza,
            // y agrandarlo rompería el ritmo de lectura del párrafo.
            lemma = TextStyle(
                fontFamily = serif,
                fontSize = (17 * scale).sp,
                lineHeight = height.em,
                fontWeight = FontWeight.Bold,
            ),
            reference = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = (13 * scale).sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
    ) {
        CompositionLocalProvider(
            LocalReaderTypography provides readerTypography,
            LocalReaderSettings provides settings,
            content = content,
        )
    }
}

/** Tipografía del "chrome": listas, barras, chips. No la tocan los ajustes. */
private val AppTypography = Typography().run {
    copy(
        titleMedium = titleMedium.copy(fontFamily = FontFamily.Serif),
        titleLarge = titleLarge.copy(fontFamily = FontFamily.Serif),
        headlineSmall = headlineSmall.copy(fontFamily = FontFamily.Serif),
        headlineMedium = headlineMedium.copy(fontFamily = FontFamily.Serif),
    )
}
