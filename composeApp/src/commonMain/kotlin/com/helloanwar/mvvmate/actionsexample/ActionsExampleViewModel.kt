package com.helloanwar.mvvmate.actionsexample

import com.helloanwar.mvvmate.actions.BaseActionsViewModel
import kotlinx.coroutines.delay

class ActionsExampleViewModel : BaseActionsViewModel<ActionsExampleState, ActionsExampleAction>(
    initialState = ActionsExampleState()
) {

    override suspend fun onAction(action: ActionsExampleAction) {
        when (action) {
            is ActionsExampleAction.AddLog -> {
                delay(500) // Simulate work
                updateState { copy(logs = logs + action.message) }
            }

            ActionsExampleAction.RunSeriesActions -> runSeriesDemo()
            ActionsExampleAction.RunParallelActions -> runParallelDemo()
            ActionsExampleAction.RunChainedActions -> runChainedDemo()
            ActionsExampleAction.RunBatchActions -> runBatchDemo()
            ActionsExampleAction.ClearLogs -> updateState { copy(logs = emptyList()) }
        }
    }

    private suspend fun runSeriesDemo() {
        updateState { copy(isProcessing = true, logs = logs + "▶️ Starting Series Dispatch...") }

        // Dispatch 3 AddLog actions one after another
        dispatchActionsInSeries(
            listOf(
                ActionsExampleAction.AddLog("  [Series] Step 1: Validate input"),
                ActionsExampleAction.AddLog("  [Series] Step 2: Process data"),
                ActionsExampleAction.AddLog("  [Series] Step 3: Save result")
            )
        )

        updateState { copy(isProcessing = false, logs = logs + "✅ Series dispatch complete (executed sequentially)") }
    }

    private suspend fun runParallelDemo() {
        updateState { copy(isProcessing = true, logs = logs + "▶️ Starting Parallel Dispatch...") }

        // Dispatch 3 AddLog actions in parallel
        dispatchActionsInParallel(
            listOf(
                ActionsExampleAction.AddLog("  [Parallel] Task A: Fetch user profile"),
                ActionsExampleAction.AddLog("  [Parallel] Task B: Fetch notifications"),
                ActionsExampleAction.AddLog("  [Parallel] Task C: Fetch settings")
            )
        )

        updateState { copy(isProcessing = false, logs = logs + "✅ Parallel dispatch complete (all ran concurrently)") }
    }

    private suspend fun runChainedDemo() {
        updateState { copy(isProcessing = true, logs = logs + "▶️ Starting Chained Dispatch...") }

        // Chain actions where each depends on the previous result
        val result = dispatchChainedActions<String>(
            actions = listOf(
                { _ ->
                    delay(400)
                    updateState { copy(logs = logs + "  [Chain] Step 1: Generated token → 'abc123'") }
                    "abc123"
                },
                { token ->
                    delay(400)
                    val userData = "User(token=$token)"
                    updateState { copy(logs = logs + "  [Chain] Step 2: Fetched user → '$userData'") }
                    userData
                },
                { userData ->
                    delay(400)
                    val cached = "Cached($userData)"
                    updateState { copy(logs = logs + "  [Chain] Step 3: Cached data → '$cached'") }
                    cached
                }
            )
        )

        updateState {
            copy(
                isProcessing = false,
                logs = logs + "✅ Chained dispatch complete → Final result: $result"
            )
        }
    }

    private suspend fun runBatchDemo() {
        updateState { copy(isProcessing = true, logs = logs + "▶️ Starting Batch Dispatch (fire & forget)...") }

        // Launch all actions without waiting for completion
        dispatchBatchActions(
            listOf(
                ActionsExampleAction.AddLog("  [Batch] Analytics event sent"),
                ActionsExampleAction.AddLog("  [Batch] Cache invalidated"),
                ActionsExampleAction.AddLog("  [Batch] Background sync triggered")
            )
        )

        updateState { copy(isProcessing = false, logs = logs + "✅ Batch dispatch complete (fire & forget)") }
    }
}
