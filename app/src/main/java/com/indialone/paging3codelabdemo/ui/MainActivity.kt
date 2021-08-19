package com.indialone.paging3codelabdemo.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.*
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.indialone.paging3codelabdemo.Injection
import com.indialone.paging3codelabdemo.R
import com.indialone.paging3codelabdemo.databinding.ActivityMainBinding
import com.indialone.paging3codelabdemo.model.RepoSearchResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        val viewModel = ViewModelProvider(
            this,
            Injection.provideViewModelFactory(context = this@MainActivity, owner = this)
        )
            .get(SearchRepositoriesViewModel::class.java)

        // add dividers between RecyclerView's row items
        val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        mBinding.list.addItemDecoration(decoration)

        // bind the state
        mBinding.bindState(
            uiState = viewModel.state,
            uiActions = viewModel.accept
        )

    }

    private fun ActivityMainBinding.bindState(
        uiState: StateFlow<UiState>,
        uiActions: (UiAction) -> Unit
    ) {
        val repoAdapter = ReposAdapter()
        val header = ReposLoadStateAdapter { repoAdapter.retry() }
        list.adapter = repoAdapter.withLoadStateHeaderAndFooter(
            header = header,
            footer = ReposLoadStateAdapter { repoAdapter.retry() }
        )

        bindSearch(
            uiState = uiState,
            onQueryChanged = uiActions
        )

        bindList(
            header = header,
            repoAdapter = repoAdapter,
            uiState = uiState,
            onScrollChanged = uiActions
        )
    }

    private fun ActivityMainBinding.bindSearch(
        uiState: StateFlow<UiState>,
        onQueryChanged: (UiAction.Search) -> Unit
    ) {
        searchRepo.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                updateRepoListFromInput(onQueryChanged)
                true
            } else {
                false
            }
        }
        searchRepo.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                updateRepoListFromInput(onQueryChanged)
                true
            } else {
                false
            }
        }

        lifecycleScope.launch {
            uiState.map { it.query }
                .distinctUntilChanged()
                .collect(searchRepo::setText)
        }
//        uiState
//            .map(UiState::query)
//            .distinctUntilChanged()
//            .observe(this@MainActivity, searchRepo::setText)
    }

    private fun ActivityMainBinding.updateRepoListFromInput(onQueryChanged: (UiAction.Search) -> Unit) {
        searchRepo.text.trim().let {
            if (it.isNotEmpty()) {
                list.scrollToPosition(0)
                onQueryChanged(UiAction.Search(query = it.toString()))
            }
        }
    }

    private fun ActivityMainBinding.bindList(
        header: ReposLoadStateAdapter,
        repoAdapter: ReposAdapter,
        uiState: StateFlow<UiState>,
        onScrollChanged: (UiAction.Scroll) -> Unit
    ) {
//        setupScrollListener(onScrollChanged)

        retryButton.setOnClickListener { repoAdapter.retry() }

        list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy != 0) onScrollChanged(UiAction.Scroll(currentQuery = uiState.value.query))
            }
        })

        val notLoading = repoAdapter.loadStateFlow
            .distinctUntilChangedBy { it.refresh }
            .map { it.refresh is LoadState.NotLoading }

        val hasNotScrolledForCurrentSearch = uiState
            .map { it.hasNotScrolledForCurrentSearch }
            .distinctUntilChanged()

        val shouldScrollToTop = combine(
            notLoading,
            hasNotScrolledForCurrentSearch,
            Boolean::and
        )
            .distinctUntilChanged()

        val pagingData = uiState
            .map { it.pagingData }
            .distinctUntilChanged()

        lifecycleScope.launch {
            combine(shouldScrollToTop, pagingData, ::Pair)
                // Each unique PagingData should be submitted once, take the latest from
                // shouldScrollToTop
                .distinctUntilChangedBy { it.second }
                .collectLatest { (shouldScroll, pagingData) ->
                    repoAdapter.submitData(pagingData as PagingData<UiModel>)
                    if (shouldScroll) list.scrollToPosition(0)
                }
        }

        lifecycleScope.launch {
//            repoAdapter.loadStateFlow.collect { loadState ->
//                val isListEmpty =
//                    loadState.refresh is LoadState.NotLoading && repoAdapter.itemCount == 0
//                emptyList.isVisible = isListEmpty
//                list.isVisible = !isListEmpty
//            }
            repoAdapter.loadStateFlow.collect { loadState ->

                header.loadState = loadState.mediator
                    ?.refresh
                    ?.takeIf { it is LoadState.Error && repoAdapter.itemCount > 0 }
                    ?: loadState.prepend

                val isListEmpty =
                    loadState.refresh is LoadState.NotLoading && repoAdapter.itemCount == 0

                emptyList.isVisible = isListEmpty

                list.isVisible =
                    loadState.source.refresh is LoadState.NotLoading || loadState.mediator?.refresh is LoadState.NotLoading

                progressBar.isVisible = loadState.mediator?.refresh is LoadState.Loading

                retryButton.isVisible =
                    loadState.mediator?.refresh is LoadState.Error && repoAdapter.itemCount == 0

                val errorState = loadState.source.append as? LoadState.Error
                    ?: loadState.source.prepend as? LoadState.Error
                    ?: loadState.append as? LoadState.Error
                    ?: loadState.prepend as? LoadState.Error
                errorState?.let {
                    Toast.makeText(
                        this@MainActivity,
                        "\uD83D\uDE28 Wooops ${it.error}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


/*

        uiState
            .map(UiState::searchResult)
            .distinctUntilChanged()
            .observe(this@MainActivity) { result ->
                when (result) {
                    is RepoSearchResult.Success -> {
                        showEmptyList(result.data.isEmpty())
                        repoAdapter.submitList(result.data)
                    }
                    is RepoSearchResult.Error -> {
                        Toast.makeText(
                            this@MainActivity,
                            "\uD83D\uDE28 Wooops $result.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

 */
}

/*
private fun ActivityMainBinding.showEmptyList(show: Boolean) {
    emptyList.isVisible = show
    list.isVisible = !show
}

private fun ActivityMainBinding.setupScrollListener(
    onScrollChanged: (UiAction.Scroll) -> Unit
) {
    val layoutManager = list.layoutManager as LinearLayoutManager
    list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val totalItemCount = layoutManager.itemCount
            val visibleItemCount = layoutManager.childCount
            val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

            onScrollChanged(
                UiAction.Scroll(
                    visibleItemCount = visibleItemCount,
                    lastVisibleItemPosition = lastVisibleItem,
                    totalItemCount = totalItemCount
                )
            )
        }
    })
}

 */