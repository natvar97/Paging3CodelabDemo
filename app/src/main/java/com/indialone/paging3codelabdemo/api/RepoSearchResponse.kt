package com.indialone.paging3codelabdemo.api

import com.google.gson.annotations.SerializedName
import com.indialone.paging3codelabdemo.model.Repo

data class RepoSearchResponse(
    @SerializedName("total_count") val total: Int = 0,
    @SerializedName("items") val items: List<Repo> = emptyList(),
    val nextPage: Int? = null
)