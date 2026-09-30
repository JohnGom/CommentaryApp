package com.comentariobiblico.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.comentariobiblico.app.domain.model.VerseId
import com.comentariobiblico.app.ui.books.BooksScreen
import com.comentariobiblico.app.ui.chapters.BookIntroScreen
import com.comentariobiblico.app.ui.chapters.ChaptersScreen
import com.comentariobiblico.app.ui.nav.BookIntroRoute
import com.comentariobiblico.app.ui.nav.BooksRoute
import com.comentariobiblico.app.ui.nav.ChaptersRoute
import com.comentariobiblico.app.ui.nav.ReaderRoute
import com.comentariobiblico.app.ui.nav.SearchRoute
import com.comentariobiblico.app.ui.nav.VersesRoute
import com.comentariobiblico.app.ui.reader.ReaderScreen
import com.comentariobiblico.app.ui.search.SearchScreen
import com.comentariobiblico.app.ui.settings.SettingsSheet
import com.comentariobiblico.app.ui.settings.SettingsViewModel
import com.comentariobiblico.app.ui.theme.ComentarioTheme
import com.comentariobiblico.app.ui.verses.VersesScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

    ComentarioTheme(settings) {
        val navController = rememberNavController()
        var showSettings by remember { mutableStateOf(false) }

        fun openReader(id: VerseId, query: String? = null) {
            navController.navigate(
                ReaderRoute(id.bookId, id.chapter, id.verse, query),
            )
        }

        NavHost(navController, startDestination = BooksRoute) {
            composable<BooksRoute> {
                BooksScreen(
                    onBook = { navController.navigate(ChaptersRoute(it)) },
                    onSearch = { navController.navigate(SearchRoute) },
                    onSettings = { showSettings = true },
                    onContinue = { openReader(it) },
                )
            }

            composable<ChaptersRoute> { entry ->
                val route: ChaptersRoute = entry.toRoute()
                ChaptersScreen(
                    bookId = route.book,
                    onChapter = { navController.navigate(VersesRoute(route.book, it)) },
                    onIntro = { navController.navigate(BookIntroRoute(route.book)) },
                    onBack = navController::popBackStack,
                )
            }

            composable<BookIntroRoute> { entry ->
                BookIntroScreen(
                    bookId = entry.toRoute<BookIntroRoute>().book,
                    onBack = navController::popBackStack,
                )
            }

            composable<VersesRoute> { entry ->
                val route: VersesRoute = entry.toRoute()
                VersesScreen(
                    bookId = route.book,
                    chapter = route.chapter,
                    onVerse = { openReader(VerseId.of(route.book, route.chapter, it)) },
                    onBack = navController::popBackStack,
                )
            }

            composable<ReaderRoute> { entry ->
                val route: ReaderRoute = entry.toRoute()
                ReaderScreen(
                    bookId = route.book,
                    chapter = route.chapter,
                    verse = route.verse,
                    query = route.query,
                    onNavigate = { openReader(it, route.query) },
                    onSettings = { showSettings = true },
                    onBack = navController::popBackStack,
                )
            }

            composable<SearchRoute> {
                SearchScreen(
                    onHit = { id, query -> openReader(id, query) },
                    onBook = { navController.navigate(ChaptersRoute(it)) },
                    onBack = navController::popBackStack,
                )
            }
        }

        if (showSettings) {
            SettingsSheet(onDismiss = { showSettings = false })
        }
    }
}
