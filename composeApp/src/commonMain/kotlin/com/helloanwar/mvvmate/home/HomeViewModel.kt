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
                updateState {
                    copy(isLoading = true)
                }
                delay(2000)
                updateState { copy(isLoading = false) }
                emitSideEffect(HomeEffect.ShowToast("Data loaded"))
            }
        }
    }
}