package com.comentariobiblico.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.comentariobiblico.app.domain.model.LineHeight
import com.comentariobiblico.app.domain.model.ReaderSettings
import com.comentariobiblico.app.domain.model.ThemeMode
import com.comentariobiblico.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val settings: StateFlow<ReaderSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, ReaderSettings())

    fun setTheme(value: ThemeMode) = update { it.copy(theme = value) }

    fun setFontScale(value: Float) = update { it.copy(fontScale = value) }

    fun setLineHeight(value: LineHeight) = update { it.copy(lineHeight = value) }

    fun setSerif(value: Boolean) = update { it.copy(serif = value) }

    fun setKeepScreenOn(value: Boolean) = update { it.copy(keepScreenOn = value) }

    private fun update(transform: (ReaderSettings) -> ReaderSettings) {
        viewModelScope.launch { repository.update(transform) }
    }
}
