package com.indialone.paging3codelabdemo.ui

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.savedstate.SavedStateRegistryOwner
import com.indialone.paging3codelabdemo.repository.GithubRepository
import kotlinx.coroutines.handleCoroutineException
import java.lang.IllegalArgumentException

class ViewModelFactory(
    owner: SavedStateRegistryOwner,
    private val provideGithubRepository: GithubRepository
) :
    AbstractSavedStateViewModelFactory(owner, null) {
    override fun <T : ViewModel?> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        if (modelClass.isAssignableFrom(SearchRepositoriesViewModel::class.java)) {
            return SearchRepositoriesViewModel(provideGithubRepository, handle) as T
        }
        throw IllegalArgumentException("Unknown View Model class")
    }

}
