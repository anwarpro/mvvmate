package com.helloanwar.mvvmate
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.helloanwar.mvvmate.home.HomeScreen
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        HomeScreen()
    }
}