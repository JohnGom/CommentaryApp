package com.comentariobiblico.app.ui.books

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.comentariobiblico.app.domain.model.Book
import com.comentariobiblico.app.domain.model.VerseId
import com.comentariobiblico.app.ui.common.StateHost
import com.comentariobiblico.app.ui.common.TABULAR_FIGURES
import com.comentariobiblico.app.ui.common.SectionHeader
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksScreen(
    onBook: (Int) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onContinue: (VerseId) -> Unit,
    viewModel: BooksViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Comentario Bíblico") },
                actions = {
                    IconButton(onSearch) { Icon(Icons.Default.Search, "Buscar") }
                    IconButton(onSettings) { Icon(Icons.Default.Settings, "Ajustes") }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        StateHost(state, Modifier.padding(padding)) { books ->
            LazyColumn {
                item {
                    OutlinedTextField(
                        value = filter,
                        onValueChange = viewModel::onFilterChange,
                        label = { Text("Buscar un libro") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    )
                }

                books.continueReading?.let { resume ->
                    item {
                        ContinueReadingCard(resume, onClick = { onContinue(resume.verseId) })
                    }
                }

                if (books.oldTestament.isNotEmpty()) {
                    stickyHeader { SectionHeader("Antiguo Testamento") }
                    items(books.oldTestament, key = { it.id }) { BookRow(it, onBook) }
                }

                if (books.newTestament.isNotEmpty()) {
                    stickyHeader { SectionHeader("Nuevo Testamento") }
                    items(books.newTestament, key = { it.id }) { BookRow(it, onBook) }
                }
            }
        }
    }
}

@Composable
private fun ContinueReadingCard(resume: ContinueReading, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "CONTINUAR LEYENDO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(resume.reference, style = MaterialTheme.typography.titleMedium)
            Text(
                resume.preview,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun BookRow(book: Book, onClick: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(book.id) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                book.id.toString(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFeatureSettings = TABULAR_FIGURES,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(Modifier.weight(1f)) {
            Text(book.title, style = MaterialTheme.typography.titleMedium)
            Text(
                "${book.chapterCount} capítulos · ${book.commentCount} comentarios",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
