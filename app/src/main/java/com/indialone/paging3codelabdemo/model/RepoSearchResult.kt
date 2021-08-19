package com.indialone.paging3codelabdemo.model

import java.lang.Exception

sealed class RepoSearchResult {
    data class Success(val data: List<Repo>): RepoSearchResult()
    data class Error(val error: Exception): RepoSearchResult()
}
