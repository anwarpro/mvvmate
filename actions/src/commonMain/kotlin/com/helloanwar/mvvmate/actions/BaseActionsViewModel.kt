package com.helloanwar.mvvmate.actions

import com.helloanwar.mvvmate.core.BaseViewModel
import com.helloanwar.mvvmate.core.UiAction
import com.helloanwar.mvvmate.core.UiState
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

abstract class BaseActionsViewModel<S : UiState, A : UiAction>(
    initialState: S
) : BaseViewModel<S, A>(initialState) {

    // Dispatch actions one by one (series execution)
    protected suspend fun dispatchActionsInSeries(actions: List<A>) {
        for (action in actions) {
            onAction(action)  // Execute each action one after another
        }
    }

    // Dispatch actions in parallel and wait for all to complete
    protected suspend fun dispatchActionsInParallel(actions: List<A>) = coroutineScope {
        actions.map { action ->
            async { onAction(action) }  // Execute all actions concurrently
        }.forEach { it.await() }  // Wait for all actions to complete
    }

    // Dispatch actions in chain (one depends on another)
    protected suspend fun <T> dispatchChainedActions(
        actions: List<suspend (T?) -> T?>,
        initialData: T? = null
    ): T? {
        var data: T? = initialData
        for (action in actions) {
            data = action(data)  // Pass result from one action to the next
        }
        return data
    }

    // Dispatch a batch of actions (launch all without waiting for completion)
    protected suspend fun dispatchBatchActions(actions: List<A>) = coroutineScope {
        actions.forEach { action ->
            launch { onAction(action) }  // Launch each action asynchronously
        }
    }

    // Abstract method to handle action execution
    abstract override suspend fun onAction(action: A)
}