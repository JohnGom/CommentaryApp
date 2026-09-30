package com.comentariobiblico.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.comentariobiblico.app.data.db.DatabaseBundle
import com.comentariobiblico.app.data.prefs.PREFERENCES_FILE
import java.io.File
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { DatabaseBundle(androidContext()) }
    single<DataStore<Preferences>> { createDataStore(androidContext()) }
}

private fun createDataStore(context: Context): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath {
        File(context.filesDir, PREFERENCES_FILE).absolutePath.toPath()
    }
