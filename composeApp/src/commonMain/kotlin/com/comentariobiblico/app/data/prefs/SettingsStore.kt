package com.comentariobiblico.app.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.comentariobiblico.app.domain.model.LineHeight
import com.comentariobiblico.app.domain.model.ReaderSettings
import com.comentariobiblico.app.domain.model.ThemeMode
import com.comentariobiblico.app.domain.model.VerseId
import com.comentariobiblico.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

const val PREFERENCES_FILE = "comentario.preferences_pb"

class SettingsStore(private val store: DataStore<Preferences>) : SettingsRepository {

    override val settings: Flow<ReaderSettings> = store.data
        .map { it.toReaderSettings() }
        .distinctUntilChanged()

    /**
     * La posición se guarda como un único entero (el propio id del versículo)
     * en vez de tres claves: el usuario cambia de versículo constantemente y
     * así cada movimiento es una sola escritura.
     */
    override val lastRead: Flow<VerseId?> = store.data
        .map { preferences -> preferences[Keys.LAST_READ]?.let(::VerseId) }
        .distinctUntilChanged()

    override suspend fun update(transform: (ReaderSettings) -> ReaderSettings) {
        val current = settings.first()
        val next = transform(current)
        store.edit { preferences ->
            preferences[Keys.THEME] = next.theme.name
            preferences[Keys.FONT_SCALE] = next.fontScale
            preferences[Keys.LINE_HEIGHT] = next.lineHeight.name
            preferences[Keys.SERIF] = next.serif
            preferences[Keys.KEEP_SCREEN_ON] = next.keepScreenOn
        }
    }

    override suspend fun setLastRead(id: VerseId) {
        store.edit { it[Keys.LAST_READ] = id.raw }
    }

    private fun Preferences.toReaderSettings() = ReaderSettings(
        theme = this[Keys.THEME]?.let { name ->
            ThemeMode.entries.firstOrNull { it.name == name }
        } ?: ThemeMode.SYSTEM,
        fontScale = this[Keys.FONT_SCALE] ?: 1.0f,
        lineHeight = this[Keys.LINE_HEIGHT]?.let { name ->
            LineHeight.entries.firstOrNull { it.name == name }
        } ?: LineHeight.NORMAL,
        serif = this[Keys.SERIF] ?: true,
        keepScreenOn = this[Keys.KEEP_SCREEN_ON] ?: false,
    )

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val LINE_HEIGHT = stringPreferencesKey("line_height")
        val SERIF = booleanPreferencesKey("serif")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val LAST_READ = intPreferencesKey("last_read")
    }
}
