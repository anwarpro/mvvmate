package com.helloanwar.mvvmate.home

import com.helloanwar.mvvmate.core.UiAction
import com.helloanwar.mvvmate.core.UiEffect
import com.helloanwar.mvvmate.core.UiState

data class HomeState(
    val isLoading: Boolean = false
) : UiState

sealed interface HomeAction : UiAction {
    data object LoadData : HomeAction
}

sealed interface HomeEffect : UiEffect {
    data class ShowToast(val message: String) : HomeEffect
}