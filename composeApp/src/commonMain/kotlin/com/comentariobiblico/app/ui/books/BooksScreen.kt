package com.comentariobiblico.app.ui.books

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.comentariobiblico.app.domain.model.Book
import com.comentariobiblico.app.domain.model.VerseId
import com.comentariobiblico.app.ui.common.StateHost
import com.comentariobiblico.app.ui.common.TABULAR_FIGURES
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

    // Sobreviven a la rotación y a volver desde otra pantalla: plegar una
    // sección es una decisión del usuario, no un detalle de composición.
    var oldOpen by rememberSaveable { mutableStateOf(true) }
    var newOpen by rememberSaveable { mutableStateOf(true) }

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
            // Mientras se filtra se abren las dos secciones: esconder la
            // coincidencia que el usuario acaba de pedir sería absurdo.
            val filtering = filter.isNotBlank()

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

                testamentSection(
                    title = "Antiguo Testamento",
                    books = books.oldTestament,
                    expanded = filtering || oldOpen,
                    onToggle = { oldOpen = !oldOpen },
                    onBook = onBook,
                )

                testamentSection(
                    title = "Nuevo Testamento",
                    books = books.newTestament,
                    expanded = filtering || newOpen,
                    onToggle = { newOpen = !newOpen },
                    onBook = onBook,
                )
            }
        }
    }
}

/**
 * Una sección plegable del listado.
 *
 * Los libros se añaden como elementos sueltos de la LazyColumn y no dentro de
 * un contenedor: así siguen componiéndose de forma perezosa y plegar la
 * sección deja de componerlos en vez de limitarse a ocultarlos.
 */
private fun androidx.compose.foundation.lazy.LazyListScope.testamentSection(
    title: String,
    books: List<Book>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onBook: (Int) -> Unit,
) {
    if (books.isEmpty()) return

    stickyHeader(key = title) {
        AccordionHeader(
            title = title,
            count = books.size,
            expanded = expanded,
            onToggle = onToggle,
        )
    }

    if (expanded) {
        items(books, key = { it.id }) { BookRow(it, onBook) }
    }
}

@Composable
private fun AccordionHeader(
    title: String,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 0f else -90f,
        label = "chevron",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Default.ExpandMore,
            contentDescription = if (expanded) "Plegar $title" else "Desplegar $title",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp).rotate(rotation),
        )
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontFeatureSettings = TABULAR_FIGURES,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
