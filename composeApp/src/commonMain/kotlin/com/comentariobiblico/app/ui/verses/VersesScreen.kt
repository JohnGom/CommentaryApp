package com.comentariobiblico.app.ui.verses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.comentariobiblico.app.ui.common.NumberCell
import com.comentariobiblico.app.ui.common.StateHost
import com.comentariobiblico.app.ui.common.UiState
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersesScreen(
    bookId: Int,
    chapter: Int,
    onVerse: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: VersesViewModel = koinViewModel(),
) {
    LaunchedEffect(bookId, chapter) { viewModel.load(bookId, chapter) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val onlyWithComment by viewModel.onlyWithComment.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val book = (state as? UiState.Success)?.data?.book?.title
                    Text(if (book != null) "$book $chapter" else "")
                },
                navigationIcon = {
                    IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") }
                },
            )
        },
    ) { padding ->
        StateHost(state, Modifier.padding(padding)) { data ->
            LazyVerticalGrid(
                columns = GridCells.Adaptive(56.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    FilterChip(
                        selected = onlyWithComment,
                        onClick = viewModel::toggleOnlyWithComment,
                        label = { Text("Solo con comentario") },
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }

                // Los números salen de la base, nunca de 1..verseCount: Génesis 40
                // no tiene versículo 22, y lo mismo pasa en Levítico 11,
                // Números 29 y Salmos 110.
                items(data.verses, key = { it.id.raw }) { verse ->
                    NumberCell(
                        number = verse.number,
                        onClick = { onVerse(verse.number) },
                        // Sin comentario se atenúa, pero sigue siendo pulsable:
                        // el texto bíblico se muestra igual.
                        highlighted = verse.hasComment,
                    )
                }
            }
        }
    }
}
