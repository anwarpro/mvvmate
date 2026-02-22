package com.helloanwar.mvvmate.home

import com.helloanwar.mvvmate.core.UiAction
import com.helloanwar.mvvmate.core.UiEffect
import com.helloanwar.mvvmate.core.UiState

data class HomeState(
    val isLoading: Boolean = false,
    val counter: Int = 0,
    val items: List<String> = emptyList()
) : UiState

sealed interface HomeAction : UiAction {
    data object LoadData : HomeAction
    data object Increment : HomeAction
    data object Decrement : HomeAction
    data object Reset : HomeAction
    data class AddItem(val item: String) : HomeAction
}

sealed interface HomeEffect : UiEffect {
    data class ShowToast(val message: String) : HomeEffect
}