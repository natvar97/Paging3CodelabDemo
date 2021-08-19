package com.indialone.paging3codelabdemo.ui

import androidx.lifecycle.*
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.indialone.paging3codelabdemo.model.Repo
import com.indialone.paging3codelabdemo.model.RepoSearchResult
import com.indialone.paging3codelabdemo.repository.GithubRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SearchRepositoriesViewModel(
    private val repository: GithubRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    //    val state: LiveData<UiState>
    val state: StateFlow<UiState>
    val accept: (UiAction) -> Unit

    init {
        val initialQuery: String = savedStateHandle.get(LAST_SEARCH_QUERY) ?: DEFAULT_QUERY
        val lastQueryScrolled: String = savedStateHandle.get(LAST_QUERY_SCROLLED) ?: DEFAULT_QUERY
        val actionStateFlow = MutableSharedFlow<UiAction>()
        val searches = actionStateFlow
            .filterIsInstance<UiAction.Search>()
            .distinctUntilChanged()
        val queriesScrolled = actionStateFlow
            .filterIsInstance<UiAction.Scroll>()
            .distinctUntilChanged()
            // This is shared to keep the flow "hot" while caching the last query scrolled,
            // otherwise each flatMapLatest invocation would lose the last query scrolled,
            .shareIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                replay = 1
            )
            .onStart { emit(UiAction.Scroll(currentQuery = lastQueryScrolled)) }

        state = searches
            .flatMapLatest { search ->
                combine(
                    queriesScrolled,
                    searchRepo(queryString = search.query),
                    ::Pair
                )
                    // Each unique PagingData should be submitted once, take the latest from
                    // queriesScrolled
                    .distinctUntilChangedBy { it.second }
                    .map { (scroll, pagingData) ->
                        UiState(
                            query = search.query,
                            pagingData = pagingData as PagingData<Repo>,
                            lastQueryScrolled = scroll.currentQuery,
                            // If the search query matches the scroll query, the user has scrolled
                            hasNotScrolledForCurrentSearch = search.query != scroll.currentQuery
                        )
                    }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                initialValue = UiState()
            )

        accept = { action ->
            viewModelScope.launch { actionStateFlow.emit(action) }
        }
    }
    /*
    val queryLiveData =
        MutableLiveData(savedStateHandle.get(LAST_SEARCH_QUERY) ?: DEFAULT_QUERY)

    state = queryLiveData
        .distinctUntilChanged()
        .switchMap { queryString ->
            liveData {
                val uiState = repository.getSearchResultStream(queryString)
                    .map {
                        UiState(
                            query = queryString,
                            searchResult = it
                        )
                    }
                    .asLiveData(Dispatchers.Main)
                emitSource(uiState)
            }
        }

    accept = { action ->
        when (action) {
            is UiAction.Search -> queryLiveData.postValue(action.query)
            is UiAction.Scroll -> if (action.shouldFetchMore) {
                val immutableQuery = queryLiveData.value
                if (immutableQuery != null) {
                    viewModelScope.launch {
                        repository.requestMore(immutableQuery)
                    }
                }
            }
        }
    }

     */


    override fun onCleared() {
        savedStateHandle[LAST_SEARCH_QUERY] = state.value?.query
        super.onCleared()
    }

    private fun searchRepo(queryString: String): Flow<PagingData<UiModel>> {

        var currentSearchResult: Flow<PagingData<UiModel>>? = null
        val lastResult: Flow<PagingData<UiModel>>? = currentSearchResult
        var currentQueryValue: String? = null
        if (queryString == currentQueryValue && lastResult != null) {
            return lastResult
        }
        currentQueryValue = queryString
        val newResult = repository.getSearchResultStream(query = queryString)
            .map { pagingData -> pagingData.map { UiModel.RepoItem(it) } }
            .map {
                it.insertSeparators<UiModel.RepoItem, UiModel>{ before, after ->
                    if (after == null) {
                        // we're at the end of the list
                        return@insertSeparators null
                    }

                    if (before == null) {
                        // we're at the beginning of the list
                        return@insertSeparators UiModel.SeparatorItem("${after.roundedStarCount}0.000+ stars")
                    }

                    if (before.roundedStarCount > after.roundedStarCount ) {
                        if (after.roundedStarCount >= 1) {
                            UiModel.SeparatorItem("${after.roundedStarCount}0.000+ stars")
                        } else {
                            UiModel.SeparatorItem("< 10.000+ stars")
                        }
                    } else {
                        null
                    }

                }
            }.cachedIn(viewModelScope)
        currentSearchResult = newResult
        return newResult
    }
//    = repository.getSearchResultStream(
//            query = queryString
//        ).cachedIn(viewModelScope)

}

//private val UiAction.Scroll.shouldFetchMore
//    get() = visibleItemCount + lastVisibleItemPosition + VISIBLE_THRESHOLD >= totalItemCount

sealed class UiAction {
    data class Search(val query: String) : UiAction()

    //    data class Scroll(
//        val visibleItemCount: Int,
//        val lastVisibleItemPosition: Int,
//        val totalItemCount: Int
//    ) : UiAction()
    data class Scroll(val currentQuery: String) : UiAction()
}

sealed class UiModel {
    data class RepoItem(val repo: Repo) : UiModel()
    data class SeparatorItem(val description: String) : UiModel()
}

private val UiModel.RepoItem.roundedStarCount: Int
    get() = this.repo.stars / 10_000

data class UiState(
    val query: String = DEFAULT_QUERY,
//    val searchResult: RepoSearchResult
    val lastQueryScrolled: String = DEFAULT_QUERY,
    val hasNotScrolledForCurrentSearch: Boolean = false,
    val pagingData: PagingData<Repo> = PagingData.empty()
)


private const val VISIBLE_THRESHOLD = 5
private const val LAST_SEARCH_QUERY: String = "last_search_query"
private const val DEFAULT_QUERY = "Android"
private const val LAST_QUERY_SCROLLED: String = "last_query_scrolled"