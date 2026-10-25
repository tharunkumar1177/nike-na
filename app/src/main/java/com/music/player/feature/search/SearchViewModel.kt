/*
 * Type-ahead search state, debounced at 150ms.
 */
package com.music.player.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.player.core.data.LibraryRepository
import com.music.player.core.model.SearchResults
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn

/** Search screen state. */
@OptIn(FlowPreview::class)
class SearchViewModel(library: LibraryRepository) : ViewModel() {
    private val _query = MutableStateFlow("")

    /** Current query text. */
    val query: StateFlow<String> = _query.asStateFlow()

    /** Results for the debounced query. */
    val results: StateFlow<SearchResults> = library.search(_query.debounce(DEBOUNCE_MS))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchResults("", emptyList(), emptyList(), emptyList()))

    /** Updates the query. */
    fun setQuery(text: String) {
        _query.value = text
    }

    private companion object {
        const val DEBOUNCE_MS = 150L
    }
}
