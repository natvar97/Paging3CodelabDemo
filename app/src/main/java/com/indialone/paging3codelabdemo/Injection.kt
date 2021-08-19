package com.indialone.paging3codelabdemo

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.savedstate.SavedStateRegistryOwner
import com.indialone.paging3codelabdemo.api.GithubService
import com.indialone.paging3codelabdemo.db.RepoDatabase
import com.indialone.paging3codelabdemo.repository.GithubRepository
import com.indialone.paging3codelabdemo.ui.ViewModelFactory

object Injection {

    private fun provideGithubRepository(context: Context): GithubRepository {
        return GithubRepository(GithubService.create(), RepoDatabase.getInstance(context = context))
    }

    fun provideViewModelFactory(
        context: Context,
        owner: SavedStateRegistryOwner
    ): ViewModelProvider.Factory {
        return ViewModelFactory(owner, provideGithubRepository(context))
    }

}