package com.indialone.paging3codelabdemo.repository

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.indialone.paging3codelabdemo.api.GithubService
import com.indialone.paging3codelabdemo.api.IN_QUALIFIER
import com.indialone.paging3codelabdemo.api.RepoSearchResponse
import com.indialone.paging3codelabdemo.db.RepoDatabase
import com.indialone.paging3codelabdemo.model.Repo
import com.indialone.paging3codelabdemo.model.RepoSearchResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import retrofit2.HttpException
import java.io.IOException

private const val GITHUB_STARTING_PAGE_REQUEST = 1

class GithubRepository(private val service: GithubService, private val database: RepoDatabase) {

    /*
    private val inMemoryCache = mutableListOf<Repo>()

    private val searchResults = MutableSharedFlow<RepoSearchResult>(replay = 1)

    private var lastRequestedPage = GITHUB_STARTING_PAGE_REQUEST

    private var isRequestInProgress = false

     */


    @OptIn(ExperimentalPagingApi::class)
    fun getSearchResultStream(query: String): Flow<PagingData<Repo>> {
        val dbQuery = "%${query.replace(' ', '%')}%"
        val pagingSourceFactory = { database.repoDao().reposByName(dbQuery) }
        return Pager(
            config = PagingConfig(
                pageSize = NETWORK_PAGE_SIZE,
                enablePlaceholders = false
            ),
            remoteMediator = GithubRemoteMediator(
                query = query,
                repoDatabase = database,
                service = service
            ),
            pagingSourceFactory = pagingSourceFactory
//            pagingSourceFactory = {
//                GithubPagingSource(service, query)
//            }
        ).flow

//        lastRequestedPage = 1
//        inMemoryCache.clear()
//        requestAndSaveData(query)
//
//        return searchResults
    }

    /*
        private suspend fun requestAndSaveData(query: String): Boolean {
            isRequestInProgress = true
            var successful = false

            val apiQuery = query + IN_QUALIFIER
            try {
                val response = service.searchRepos(apiQuery, lastRequestedPage, NETWORK_PAGE_SIZE)
                Log.d("GithubRepository", "response $response")
                val repos = response.items ?: emptyList()
                inMemoryCache.addAll(repos)
                val reposByName = reposByName(query)
                searchResults.emit(RepoSearchResult.Success(reposByName))
                successful = true
            } catch (exception: IOException) {
                searchResults.emit(RepoSearchResult.Error(exception))
            } catch (exception: HttpException) {
                searchResults.emit(RepoSearchResult.Error(exception))
            }
            isRequestInProgress = false
            return successful
        }

        suspend fun requestMore(query: String) {
            if (isRequestInProgress) return
            val successful = requestAndSaveData(query)
            if (successful) {
                lastRequestedPage++
            }
        }

        suspend fun retry(query: String) {
            if (isRequestInProgress) return
            requestAndSaveData(query)
        }

        private fun reposByName(query: String): List<Repo> {
            return inMemoryCache.filter {
                it.name.contains(query, true) ||
                        (it.description != null && it.description.contains(query, true))
            }.sortedWith(compareByDescending<Repo> { it.stars }.thenBy { it.name })
        }


     */
    companion object {
        const val NETWORK_PAGE_SIZE = 30
    }


}