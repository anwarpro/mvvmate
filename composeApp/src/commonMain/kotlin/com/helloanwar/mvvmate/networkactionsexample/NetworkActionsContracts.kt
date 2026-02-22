package com.helloanwar.mvvmate.networkactionsexample

import com.helloanwar.mvvmate.core.UiAction
import com.helloanwar.mvvmate.core.UiState

data class NetworkActionsExampleState(
    val isLoading: Boolean = false,
    val posts: List<String> = emptyList(),
    val error: String? = null,
    val logs: List<String> = emptyList(),
    val loadingKeys: Set<String> = emptySet()
) : UiState

sealed interface NetworkActionsExampleAction : UiAction {
    data object FetchAllPosts : NetworkActionsExampleAction
    data object FetchPostsSeries : NetworkActionsExampleAction
    data object FetchWithRetryAndBatch : NetworkActionsExampleAction
    data object CancelAll : NetworkActionsExampleAction
    data object ClearAll : NetworkActionsExampleAction
    data class AddLog(val message: String) : NetworkActionsExampleAction
    data class AddPost(val post: String) : NetworkActionsExampleAction
}
