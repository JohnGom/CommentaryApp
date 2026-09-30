package com.comentariobiblico.app.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.comentariobiblico.app.domain.model.VerseId
import com.comentariobiblico.app.ui.common.EmptyMessage
import com.comentariobiblico.app.ui.common.EmptyReason
import com.comentariobiblico.app.ui.common.StateHost
import com.comentariobiblico.app.ui.theme.LocalReaderTypography
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: Int,
    chapter: Int,
    verse: Int,
    query: String?,
    onNavigate: (VerseId) -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = koinViewModel(),
) {
    LaunchedEffect(bookId, chapter, verse, query) {
        viewModel.load(bookId, chapter, verse, query)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    val reference = (state as? com.comentariobiblico.app.ui.common.UiState.Success)
                        ?.data?.current?.verse?.reference
                    Text(reference ?: "")
                },
                navigationIcon = {
                    IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") }
                },
                actions = {
                    IconButton(onSettings) { Icon(Icons.Default.Settings, "Ajustes de lectura") }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            val data = (state as? com.comentariobiblico.app.ui.common.UiState.Success)?.data
            if (data != null) {
                ReaderBottomBar(
                    reference = data.current.verse.reference,
                    previous = data.current.previous,
                    next = data.current.next,
                    onNavigate = onNavigate,
                )
            }
        },
    ) { padding ->
        StateHost(state, Modifier.padding(padding)) { data ->
            val verseId = data.current.verse.id

            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    VerseCard(
                        number = data.current.verse.number,
                        text = data.current.verse.text,
                        hasText = data.current.verse.hasText,
                    )
                }

                val comment = data.current.comment
                if (comment == null) {
                    item {
                        NoCommentState(
                            onPrevious = {
                                viewModel.jumpToCommented(forward = false) { it?.let(onNavigate) }
                            },
                            onNext = {
                                viewModel.jumpToCommented(forward = true) { it?.let(onNavigate) }
                            },
                        )
                    }
                } else {
                    itemsIndexed(comment.blocks) { _, block ->
                        CommentBlockText(
                            block = block,
                            context = verseId,
                            onReference = onNavigate,
                        )
                    }

                    if (comment.egwReferences.isNotBlank()) {
                        item { EgwReferences(comment.egwReferences) }
                    }
                }

                item { Box(Modifier.padding(24.dp)) }
            }
        }
    }
}

@Composable
private fun VerseCard(number: Int, text: String, hasText: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Box(
            Modifier
                .width(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary)
                .fillMaxSize(),
        )
        Column(Modifier.padding(start = 14.dp)) {
            Text(
                number.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (hasText) {
                VerseText(text)
            } else {
                // 19 versículos no traen texto en esta edición pero SÍ tienen
                // comentario: el aviso no debe ocultarlo.
                Text(
                    "Texto del versículo no disponible en esta edición",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NoCommentState(onPrevious: () -> Unit, onNext: () -> Unit) {
    EmptyMessage(EmptyReason.NO_COMMENT) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onPrevious) { Text("Anterior comentado") }
            TextButton(onNext) { Text("Siguiente comentado") }
        }
    }
}

/** Plegada por defecto: es una lista de referencias, no prosa que se lea seguida. */
@Composable
private fun EgwReferences(references: String) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Comentarios de Elena G. de White",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Ocultar" else "Mostrar",
            )
        }
        AnimatedVisibility(expanded) {
            Text(
                references,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            )
        }
    }
}

/**
 * Las flechas usan prev_id/next_id, que el exportador precalculó cruzando
 * capítulo y libro: desde Malaquías 4:6 la flecha derecha lleva a Mateo 1:1.
 */
@Composable
private fun ReaderBottomBar(
    reference: String,
    previous: VerseId?,
    next: VerseId?,
    onNavigate: (VerseId) -> Unit,
) {
    BottomAppBar {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { previous?.let(onNavigate) },
                enabled = previous != null,
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Versículo anterior")
            }

            Text(
                reference,
                style = LocalReaderTypography.current.reference,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            IconButton(
                onClick = { next?.let(onNavigate) },
                enabled = next != null,
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Versículo siguiente")
            }
        }
    }
}
