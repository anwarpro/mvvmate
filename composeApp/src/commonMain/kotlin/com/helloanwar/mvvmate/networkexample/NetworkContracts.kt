package com.helloanwar.mvvmate.networkexample

import com.helloanwar.mvvmate.core.UiAction
import com.helloanwar.mvvmate.core.UiState

data class NetworkExampleState(
    val isLoading: Boolean = false,
    val data: String? = null,
    val error: String? = null,
    val loadingKeys: Set<String> = emptySet(),
    val logs: List<String> = emptyList()
) : UiState

sealed interface NetworkExampleAction : UiAction {
    data object FetchData : NetworkExampleAction
    data object FetchWithRetry : NetworkExampleAction
    data object FetchWithTimeout : NetworkExampleAction
    data object FetchWithCancellation : NetworkExampleAction
    data object CancelFetch : NetworkExampleAction
    data object ClearAll : NetworkExampleAction
}
