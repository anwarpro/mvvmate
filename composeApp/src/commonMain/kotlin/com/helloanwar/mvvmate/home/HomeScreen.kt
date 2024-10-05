package com.helloanwar.mvvmate.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel { HomeViewModel() }
) {
    val state = viewModel.state.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    HomeScreenContent(
        state = state.value,
        modifier = modifier,
        snackbarHostState = {
            snackbarHostState
        },
        postAction = {
            viewModel.handleAction(it)
        }
    )
    // Handle side effects
    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is HomeEffect.ShowToast -> {
                    // Show toast
                    println(effect.message)
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    modifier: Modifier = Modifier,
    state: HomeState,
    snackbarHostState: () -> SnackbarHostState,
    postAction: (HomeAction) -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text("Home") }, actions = {})
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    postAction(HomeAction.LoadData)
                }
            ) {
                Text("Load Data")
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState())
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(it),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.isLoading) {
                CircularProgressIndicator()
            }
        }
    }
}