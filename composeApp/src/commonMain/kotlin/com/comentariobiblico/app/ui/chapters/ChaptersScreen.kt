package com.comentariobiblico.app.ui.chapters

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.comentariobiblico.app.domain.model.Chapter
import com.comentariobiblico.app.ui.common.NumberCell
import com.comentariobiblico.app.ui.common.StateHost
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChaptersScreen(
    bookId: Int,
    onChapter: (Int) -> Unit,
    onIntro: () -> Unit,
    onBack: () -> Unit,
    viewModel: ChaptersViewModel = koinViewModel(),
) {
    LaunchedEffect(bookId) { viewModel.load(bookId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = (state as? com.comentariobiblico.app.ui.common.UiState.Success)
                        ?.data?.book?.title
                    Text(title ?: "")
                },
                navigationIcon = {
                    IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") }
                },
                actions = {
                    val book = (state as? com.comentariobiblico.app.ui.common.UiState.Success)
                        ?.data?.book
                    // Oculto en 2 Samuel, 2 Reyes, 2 Crónicas y Nehemías, que
                    // no traen material introductorio.
                    if (book?.hasIntro == true) {
                        IconButton(onIntro) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, "Introducción al libro")
                        }
                    }
                },
            )
        },
    ) { padding ->
        StateHost(state, Modifier.padding(padding)) { data ->
            LazyVerticalGrid(
                columns = GridCells.Adaptive(64.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(data.chapters, key = { it.id.raw }) { chapter ->
                    NumberCell(
                        number = chapter.number,
                        onClick = { onChapter(chapter.number) },
                        highlighted = chapter.commentCount > 0,
                        outlined = chapter.commentCount == 0,
                        footer = { CommentDensityBar(chapter) },
                    )
                }
            }
        }
    }
}

/**
 * Una barra de 3 dp cuyo ancho es la proporción de versículos comentados del
 * capítulo. Dice de un vistazo dónde hay material sin gastar espacio ni texto.
 */
@Composable
private fun BoxScope.CommentDensityBar(chapter: Chapter) {
    if (chapter.commentCount == 0) return
    Box(
        Modifier
            .align(Alignment.BottomStart)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .fillMaxWidth(chapter.commentRatio)
            .height(3.dp)
            .background(MaterialTheme.colorScheme.primary),
    )
}
