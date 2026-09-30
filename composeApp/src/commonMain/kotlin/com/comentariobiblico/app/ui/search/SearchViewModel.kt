package com.comentariobiblico.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.comentariobiblico.app.domain.model.Book
import com.comentariobiblico.app.domain.model.SearchKind
import com.comentariobiblico.app.domain.model.SearchPage
import com.comentariobiblico.app.domain.repository.CommentaryRepository
import com.comentariobiblico.app.domain.repository.SearchRepository
import com.comentariobiblico.app.ui.common.EmptyReason
import com.comentariobiblico.app.ui.common.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(
    private val search: SearchRepository,
    private val commentary: CommentaryRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _kind = MutableStateFlow<SearchKind?>(null)
    val kind: StateFlow<SearchKind?> = _kind.asStateFlow()

    private val _bookFilter = MutableStateFlow<Int?>(null)
    val bookFilter: StateFlow<Int?> = _bookFilter.asStateFlow()

    private val debouncedQuery = _query.debounce(DEBOUNCE_MS).distinctUntilChanged()

    val state: StateFlow<UiState<SearchPage>> =
        combine(debouncedQuery, _kind, _bookFilter) { query, kind, book -> Triple(query, kind, book) }
            .flatMapLatest { (query, kind, book) ->
                if (query.trim().length < MIN_QUERY) {
                    flowOf<UiState<SearchPage>>(UiState.Empty(EmptyReason.TYPE_TO_SEARCH))
                } else {
                    flow<UiState<SearchPage>> {
                        emit(UiState.Loading)
                        val page = search.search(query, kind, book)
                        emit(
                            if (page.hits.isEmpty()) UiState.Empty(EmptyReason.NO_RESULTS)
                            else UiState.Success(page),
                        )
                    }
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT),
                UiState.Empty(EmptyReason.TYPE_TO_SEARCH),
            )

    /**
     * Quien escribe "genesis" casi siempre quiere ir a Génesis, no leer 300
     * comentarios que mencionan el libro. Las coincidencias de nombre se
     * ofrecen aparte y antes que los resultados de texto.
     */
    val bookSuggestions: StateFlow<List<Book>> = _query
        .debounce(SUGGEST_DEBOUNCE_MS)
        .mapLatest { query ->
            if (query.trim().length >= MIN_QUERY) commentary.findBooks(query) else emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun onKindChange(value: SearchKind?) {
        _kind.value = value
    }

    fun onBookFilterChange(value: Int?) {
        _bookFilter.value = value
    }

    private companion object {
        const val DEBOUNCE_MS = 250L
        const val SUGGEST_DEBOUNCE_MS = 120L
        const val MIN_QUERY = 2
        const val STOP_TIMEOUT = 5_000L
    }
}
