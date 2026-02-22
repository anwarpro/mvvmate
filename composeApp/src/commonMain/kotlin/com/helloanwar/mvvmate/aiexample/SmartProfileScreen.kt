package com.helloanwar.mvvmate.aiexample

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartProfileScreen(
    onBack: () -> Unit,
    viewModel: SmartProfileViewModel = viewModel { SmartProfileViewModel() }
) {
    val state by viewModel.state.collectAsState()
    var apiKey by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is SmartProfileEffect.ShowToast -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Smart Form (JetBrains Koog)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // AI Configuration
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("1. Configure AI", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("Gemini API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // AI Natural Language Input
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("2. Natural Language Autofill", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Try: 'Change my name to Alice, ignore the city, and set email to alice@test.com'",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        label = { Text("What should I update?") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    
                    if (state.aiError != null) {
                        Text(
                            text = "Error: ${state.aiError}", 
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Button(
                        onClick = { 
                            viewModel.handleAction(SmartProfileAction.ProcessNaturalLanguage(prompt, apiKey)) 
                        },
                        modifier = Modifier.align(Alignment.End).padding(top = 8.dp),
                        enabled = !state.isAiThinking && prompt.isNotBlank() && apiKey.isNotBlank()
                    ) {
                        if (state.isAiThinking) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Text("Autofill with AI")
                        }
                    }
                }
            }

            // The actual "Form" showing state
            Card {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("3. Standard Strict Profile Form", style = MaterialTheme.typography.titleMedium)
                    
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { viewModel.handleAction(SmartProfileAction.UpdateField("name", it)) },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = state.city,
                        onValueChange = { viewModel.handleAction(SmartProfileAction.UpdateField("city", it)) },
                        label = { Text("City") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = { viewModel.handleAction(SmartProfileAction.UpdateField("email", it)) },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
