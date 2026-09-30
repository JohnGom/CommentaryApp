package com.comentariobiblico.app.ui.common

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Dibuja el estado correcto sin que cada pantalla repita el mismo when.
 *
 * El indicador de carga solo aparece si la espera supera [SPINNER_DELAY_MS]:
 * con una base local casi todo resuelve en menos de 10 ms y un spinner
 * instantáneo produce un parpadeo que hace parecer lenta la app.
 */
@Composable
fun <T> StateHost(
    state: UiState<T>,
    modifier: Modifier = Modifier,
    empty: @Composable (EmptyReason) -> Unit = { EmptyMessage(it) },
    content: @Composable (T) -> Unit,
) {
    Crossfade(state, modifier = modifier, label = "state") { current ->
        when (current) {
            is UiState.Loading -> DelayedLoading()
            is UiState.Success -> content(current.data)
            is UiState.Empty -> empty(current.reason)
            is UiState.Error -> ErrorMessage(current.message)
        }
    }
}

@Composable
private fun DelayedLoading() {
    val visible by produceState(false) {
        delay(SPINNER_DELAY_MS)
        value = true
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (visible) CircularProgressIndicator()
    }
}

@Composable
fun EmptyMessage(
    reason: EmptyReason,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val (title, detail) = when (reason) {
        EmptyReason.NO_COMMENT -> "Este versículo no tiene comentario" to
            "La obra comenta unos cuatro de cada cinco versículos."
        EmptyReason.NO_RESULTS -> "Sin resultados" to
            "Prueba con menos palabras o busca dentro de un libro."
        EmptyReason.NO_INTRO -> "Sin introducción" to
            "Esta obra no incluye material introductorio para este libro."
        EmptyReason.TYPE_TO_SEARCH -> "Busca en toda la obra" to
            "24.892 comentarios y 31.092 versículos. Las tildes dan igual."
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        action?.invoke()
    }
}

@Composable
fun ErrorMessage(message: String, onRetry: (() -> Unit)? = null) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Algo no ha ido bien", style = MaterialTheme.typography.titleMedium)
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) Button(onRetry) { Text("Reintentar") }
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

/**
 * Celda numérica de los retículos de capítulos y versículos.
 *
 * Las cifras van con `tnum` (cifras tabulares) para que todas ocupen lo mismo
 * y las columnas no bailen entre el 9 y el 10.
 */
@Composable
fun NumberCell(
    number: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = true,
    outlined: Boolean = false,
    footer: (@Composable BoxScope.() -> Unit)? = null,
) {
    val background = if (highlighted) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .then(
                if (outlined) {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(12.dp),
                    )
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontFeatureSettings = TABULAR_FIGURES,
            ),
            color = if (highlighted) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.alpha(if (highlighted) 1f else 0.55f),
        )
        footer?.invoke(this)
    }
}

const val TABULAR_FIGURES = "tnum"

@Composable
fun Dot(color: Color, size: Int = 6) {
    Box(Modifier.size(size.dp).clip(RoundedCornerShape(50)).background(color))
}
