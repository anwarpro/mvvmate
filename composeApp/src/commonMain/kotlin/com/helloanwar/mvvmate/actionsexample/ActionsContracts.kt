package com.helloanwar.mvvmate.actionsexample

import com.helloanwar.mvvmate.core.UiAction
import com.helloanwar.mvvmate.core.UiState

data class ActionsExampleState(
    val isProcessing: Boolean = false,
    val logs: List<String> = emptyList()
) : UiState

sealed interface ActionsExampleAction : UiAction {
    data class AddLog(val message: String) : ActionsExampleAction
    data object RunSeriesActions : ActionsExampleAction
    data object RunParallelActions : ActionsExampleAction
    data object RunChainedActions : ActionsExampleAction
    data object RunBatchActions : ActionsExampleAction
    data object ClearLogs : ActionsExampleAction
}
