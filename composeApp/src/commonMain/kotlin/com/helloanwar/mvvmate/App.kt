package com.helloanwar.mvvmate

import androidx.compose.animation.Crossfade
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.helloanwar.mvvmate.actionsexample.ActionsExampleScreen
import com.helloanwar.mvvmate.home.HomeScreen
import com.helloanwar.mvvmate.navigation.MainMenuScreen
import com.helloanwar.mvvmate.navigation.Screen
import com.helloanwar.mvvmate.networkactionsexample.NetworkActionsExampleScreen
import com.helloanwar.mvvmate.aiexample.SmartProfileScreen
import com.helloanwar.mvvmate.networkexample.NetworkExampleScreen
import com.helloanwar.mvvmate.formsexample.FormsExampleScreen
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.helloanwar.mvvmate.core.MvvMate
import com.helloanwar.mvvmate.debug.RemoteDebugLogger

private val initDebugLogger = lazy {
    MvvMate.logger = RemoteDebugLogger()
    MvvMate.isDebug = true
}

@Composable
@Preview
fun App() {
    initDebugLogger.value
    
    MaterialTheme {
        var currentScreen by remember { mutableStateOf(Screen.Main) }

        Crossfade(targetState = currentScreen) { screen ->
            when (screen) {
                Screen.Main -> MainMenuScreen(
                    onNavigate = { currentScreen = it }
                )

                Screen.AiExample -> SmartProfileScreen(
                    onBack = { currentScreen = Screen.Main }
                )

                Screen.CoreExample -> HomeScreen(
                    onBack = { currentScreen = Screen.Main }
                )

                Screen.NetworkExample -> NetworkExampleScreen(
                    onBack = { currentScreen = Screen.Main }
                )

                Screen.ActionsExample -> ActionsExampleScreen(
                    onBack = { currentScreen = Screen.Main }
                )

                Screen.NetworkActionsExample -> NetworkActionsExampleScreen(
                    onBack = { currentScreen = Screen.Main }
                )
                
                Screen.FormsExample -> FormsExampleScreen(
                    onBack = { currentScreen = Screen.Main }
                )
            }
        }
    }
}