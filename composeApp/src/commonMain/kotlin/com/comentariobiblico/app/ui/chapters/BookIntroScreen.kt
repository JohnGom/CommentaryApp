package com.comentariobiblico.app.ui.chapters

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.comentariobiblico.app.ui.common.StateHost
import com.comentariobiblico.app.ui.theme.LocalReaderTypography
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookIntroScreen(
    bookId: Int,
    onBack: () -> Unit,
    viewModel: BookIntroViewModel = koinViewModel(),
) {
    LaunchedEffect(bookId) { viewModel.load(bookId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Introducción") },
                navigationIcon = {
                    IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") }
                },
            )
        },
    ) { padding ->
        StateHost(state, Modifier.padding(padding)) { data ->
            LazyColumn {
                item {
                    Text(
                        data.book.title,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(20.dp),
                    )
                }

                val sections = listOf(
                    "Descripción" to data.intro.description,
                    "Autor" to data.intro.author,
                    "Escenario" to data.intro.scenery,
                    "Tema" to data.intro.theme,
                    "Bosquejo" to data.intro.outline,
                ).filter { it.second.isNotBlank() }

                items(sections.size) { index ->
                    val (title, body) = sections[index]
                    ExpandableSection(title, body, expandedByDefault = index == 0)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ExpandableSection(title: String, body: String, expandedByDefault: Boolean) {
    var expanded by remember { mutableStateOf(expandedByDefault) }

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Ocultar" else "Mostrar",
            )
        }
        AnimatedVisibility(expanded) {
            Text(
                body,
                style = LocalReaderTypography.current.body,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            )
        }
    }
}
