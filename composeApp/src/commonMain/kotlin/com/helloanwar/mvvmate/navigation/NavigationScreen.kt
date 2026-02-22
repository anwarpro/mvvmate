package com.helloanwar.mvvmate.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class Screen {
    Main, CoreExample, NetworkExample, ActionsExample, NetworkActionsExample
}

data class ExampleItem(
    val title: String,
    val subtitle: String,
    val emoji: String,
    val screen: Screen
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(
    onNavigate: (Screen) -> Unit
) {
    val examples = listOf(
        ExampleItem(
            title = "Core",
            subtitle = "BaseViewModel & BaseViewModelWithEffect — State, Actions, Side Effects",
            emoji = "🧩",
            screen = Screen.CoreExample
        ),
        ExampleItem(
            title = "Network",
            subtitle = "BaseNetworkViewModel — Retry, Timeout, Cancellation, Loading States",
            emoji = "🌐",
            screen = Screen.NetworkExample
        ),
        ExampleItem(
            title = "Actions",
            subtitle = "BaseActionsViewModel — Series, Parallel, Chained, Batch Dispatching",
            emoji = "⚡",
            screen = Screen.ActionsExample
        ),
        ExampleItem(
            title = "Network + Actions",
            subtitle = "BaseNetworkActionsViewModel — Combined Network Calls & Action Dispatching",
            emoji = "🚀",
            screen = Screen.NetworkActionsExample
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MVVMate Examples") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Tap an example to explore each library module:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            examples.forEach { example ->
                ExampleCard(
                    item = example,
                    onClick = { onNavigate(example.screen) }
                )
            }
        }
    }
}

@Composable
private fun ExampleCard(
    item: ExampleItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.emoji,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
