package com.helloanwar.mvvmate.home

import com.helloanwar.mvvmate.core.BaseViewModelWithEffect
import kotlinx.coroutines.delay

class HomeViewModel : BaseViewModelWithEffect<HomeState, HomeAction, HomeEffect>(
    initialState = HomeState()
) {
    override suspend fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.LoadData -> {
                emitSideEffect(HomeEffect.ShowToast("Loading data..."))
                updateState { copy(isLoading = true) }
                delay(2000)
                updateState {
                    copy(
                        isLoading = false,
                        items = listOf("Item A", "Item B", "Item C")
                    )
                }
                emitSideEffect(HomeEffect.ShowToast("Data loaded — 3 items"))
            }

            HomeAction.Increment -> {
                updateState { copy(counter = counter + 1) }
                emitSideEffect(HomeEffect.ShowToast("Counter: ${state.value.counter + 1}"))
            }

            HomeAction.Decrement -> {
                updateState { copy(counter = counter - 1) }
                emitSideEffect(HomeEffect.ShowToast("Counter: ${state.value.counter - 1}"))
            }

            HomeAction.Reset -> {
                updateState { copy(counter = 0, items = emptyList()) }
                emitSideEffect(HomeEffect.ShowToast("State reset"))
            }

            is HomeAction.AddItem -> {
                updateState { copy(items = items + action.item) }
            }
        }
    }

    override fun mapDebugAction(payload: String): HomeAction? {
        return when {
            payload == "Reset" -> HomeAction.Reset
            payload == "Increment" -> HomeAction.Increment
            payload == "Decrement" -> HomeAction.Decrement
            payload == "LoadData" -> HomeAction.LoadData
            payload.startsWith("AddItem") -> {
                val item = payload.substringAfter("item=", "New Item").substringBefore(")")
                HomeAction.AddItem(item)
            }
            else -> null
        }
    }
}