package com.comentariobiblico.app.di

import com.comentariobiblico.app.data.db.CommentaryDao
import com.comentariobiblico.app.data.db.DatabaseFactory
import com.comentariobiblico.app.data.prefs.SettingsStore
import com.comentariobiblico.app.data.repository.CommentaryRepositoryImpl
import com.comentariobiblico.app.data.repository.SearchRepositoryImpl
import com.comentariobiblico.app.domain.repository.CommentaryRepository
import com.comentariobiblico.app.domain.repository.SearchRepository
import com.comentariobiblico.app.domain.repository.SettingsRepository
import com.comentariobiblico.app.ui.books.BooksViewModel
import com.comentariobiblico.app.ui.chapters.BookIntroViewModel
import com.comentariobiblico.app.ui.chapters.ChaptersViewModel
import com.comentariobiblico.app.ui.reader.ReaderViewModel
import com.comentariobiblico.app.ui.search.SearchViewModel
import com.comentariobiblico.app.ui.settings.SettingsViewModel
import com.comentariobiblico.app.ui.verses.VersesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Lo que aporta cada plataforma: el bundle de la base y el DataStore. */
expect fun platformModule(): Module

val appModule = module {
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    // La apertura arranca aquí pero NO se espera: en Android hay que copiar
    // 43 MB de los assets la primera vez, y resolverla con runBlocking dentro
    // del grafo congelaría el arranque. El DAO espera al Deferred.
    single { DatabaseFactory(bundle = get(), scope = get()) }
    single { CommentaryDao(get<DatabaseFactory>().database) }

    single<SettingsRepository> { SettingsStore(get()) }
    single<CommentaryRepository> { CommentaryRepositoryImpl(get()) }
    single<SearchRepository> { SearchRepositoryImpl(get()) }

    viewModelOf(::BooksViewModel)
    viewModelOf(::ChaptersViewModel)
    viewModelOf(::BookIntroViewModel)
    viewModelOf(::VersesViewModel)
    viewModelOf(::ReaderViewModel)
    viewModelOf(::SearchViewModel)
    viewModelOf(::SettingsViewModel)
}
