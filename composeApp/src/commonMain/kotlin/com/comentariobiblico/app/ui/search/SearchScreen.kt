package com.comentariobiblico.app.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.comentariobiblico.app.domain.model.SearchHit
import com.comentariobiblico.app.domain.model.SearchKind
import com.comentariobiblico.app.domain.model.Snippet
import com.comentariobiblico.app.domain.model.VerseId
import com.comentariobiblico.app.ui.common.StateHost
import com.comentariobiblico.app.ui.theme.searchHighlight
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onHit: (VerseId, String) -> Unit,
    onBook: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val kind by viewModel.kind.collectAsStateWithLifecycle()
    val suggestions by viewModel.bookSuggestions.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buscar") },
                navigationIcon = {
                    IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                label = { Text("Palabra o frase") },
                supportingText = { Text("Las tildes y las mayúsculas dan igual") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .focusRequester(focusRequester),
            )

            // Quien escribe "genesis" casi siempre quiere ir a Génesis, no leer
            // los comentarios que lo mencionan. Los libros van primero.
            if (suggestions.isNotEmpty()) {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(suggestions, key = { it.id }) { book ->
                        AssistChip(
                            onClick = { onBook(book.id) },
                            label = { Text(book.title) },
                        )
                    }
                }
            }

            val counts = (state as? com.comentariobiblico.app.ui.common.UiState.Success)
                ?.data?.counts
            if (counts != null) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = kind == null,
                        onClick = { viewModel.onKindChange(null) },
                        label = { Text("Todo (${counts.total})") },
                    )
                    FilterChip(
                        selected = kind == SearchKind.COMMENT,
                        onClick = { viewModel.onKindChange(SearchKind.COMMENT) },
                        label = { Text("Comentarios (${counts.comments})") },
                    )
                    FilterChip(
                        selected = kind == SearchKind.VERSE,
                        onClick = { viewModel.onKindChange(SearchKind.VERSE) },
                        label = { Text("Versículos (${counts.verses})") },
                    )
                }
            }

            StateHost(state) { page ->
                Column {
                    if (page.truncated) {
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                            Text(
                                "Mostrando los más relevantes de ${page.counts.total}. " +
                                    "Afina la búsqueda o filtra por libro.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                            )
                        }
                    }

                    LazyColumn {
                        items(page.hits, key = { it.verseId.raw to it.kind }) { hit ->
                            SearchResultRow(hit) { onHit(hit.verseId, query) }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(hit: SearchHit, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                hit.reference,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                if (hit.kind == SearchKind.COMMENT) "Comentario" else "Versículo",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HighlightedText(hit.snippet)
    }
}

@Composable
private fun HighlightedText(snippet: Snippet) {
    val highlight = MaterialTheme.colorScheme.searchHighlight
    val annotated = remember(snippet, highlight) {
        buildAnnotatedString {
            append(snippet.text)
            for (range in snippet.highlights) {
                addStyle(SpanStyle(background = highlight), range.first, range.last + 1)
            }
        }
    }
    Text(
        annotated,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
}
