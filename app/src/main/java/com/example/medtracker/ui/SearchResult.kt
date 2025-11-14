package com.example.medtracker.ui

// Represents an entry shown in the search results list.
// `isLocal` == true means it comes from the user's saved drugs database database.
data class SearchResult(
    val label: String,
    val isLocal: Boolean,
    val genericName: String? = null,
    val brandName: String? = null
)
