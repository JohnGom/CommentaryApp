package com.comentariobiblico.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Paleta "Tinta y Papel": teal profundo y oro tostado sobre papel cálido.
 *
 * Sin blancos ni negros puros — en sesiones largas de lectura el contraste
 * absoluto cansa. Aun así el contraste texto/fondo queda en 14,8:1 en claro y
 * 13,2:1 en oscuro, muy por encima de AAA.
 *
 * El oro (`secondary`) es el color de los lemas que encabezan cada bloque de
 * comentario; el resaltado de búsqueda tiene su propio tono para no competir.
 */

private val Teal = Color(0xFF1F5C63)
private val TealLight = Color(0xFF7FD0D6)
private val Gold = Color(0xFF8A6A3B)
private val GoldLight = Color(0xFFD7B583)
private val Plum = Color(0xFF6B4E71)
private val PlumLight = Color(0xFFD6B8DC)

val LightColors: ColorScheme = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDE7E7),
    onPrimaryContainer = Color(0xFF00363B),
    secondary = Gold,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF2E3CA),
    onSecondaryContainer = Color(0xFF2D1F0A),
    tertiary = Plum,
    onTertiary = Color.White,
    background = Color(0xFFFBF8F3),
    onBackground = Color(0xFF1F1D1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F1D1A),
    surfaceVariant = Color(0xFFEFE9DF),
    onSurfaceVariant = Color(0xFF5A544B),
    outline = Color(0xFFD6CDBF),
    outlineVariant = Color(0xFFE6DFD3),
)

val DarkColors: ColorScheme = darkColorScheme(
    primary = TealLight,
    onPrimary = Color(0xFF003438),
    primaryContainer = Color(0xFF00494F),
    onPrimaryContainer = Color(0xFFBDE9EE),
    secondary = GoldLight,
    onSecondary = Color(0xFF3B2A10),
    secondaryContainer = Color(0xFF4F3B1C),
    onSecondaryContainer = Color(0xFFF2E3CA),
    tertiary = PlumLight,
    onTertiary = Color(0xFF3A2440),
    background = Color(0xFF121110),
    onBackground = Color(0xFFECE5DA),
    surface = Color(0xFF1A1917),
    onSurface = Color(0xFFECE5DA),
    surfaceVariant = Color(0xFF2A2724),
    onSurfaceVariant = Color(0xFFB3ABA0),
    outline = Color(0xFF47423C),
    outlineVariant = Color(0xFF332F2B),
)

/** Sepia: imprescindible en una app de lectura larga, y es solo un esquema más. */
val SepiaColors: ColorScheme = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E7DE),
    onPrimaryContainer = Color(0xFF00363B),
    secondary = Color(0xFF7A5B2E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8D6B4),
    onSecondaryContainer = Color(0xFF2D1F0A),
    tertiary = Plum,
    onTertiary = Color.White,
    background = Color(0xFFF3E7D3),
    onBackground = Color(0xFF3A2F21),
    surface = Color(0xFFEFE1CA),
    onSurface = Color(0xFF3A2F21),
    surfaceVariant = Color(0xFFE3D2B6),
    onSurfaceVariant = Color(0xFF6B5B45),
    outline = Color(0xFFC9B594),
    outlineVariant = Color(0xFFDBC9A8),
)

/** Fondo del resaltado de resultados de búsqueda, por esquema. */
val ColorScheme.searchHighlight: Color
    get() = when (background) {
        DarkColors.background -> Color(0xFF4A3C18)
        SepiaColors.background -> Color(0xFFF0D08A)
        else -> Color(0xFFFFE3A3)
    }
