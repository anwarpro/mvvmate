package com.helloanwar.mvvmate.formsexample

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormsExampleScreen(
    onBack: () -> Unit,
    viewModel: RegistrationViewModel = androidx.lifecycle.viewmodel.compose.viewModel { RegistrationViewModel() }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Declarative Form Validation") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Mvvmate-forms provides type-safe and declarative way to validate fields locally entirely through your UiState.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (state.isSuccess) {
                RegistrationSuccessCard(onReset = { viewModel.handleAction(RegistrationAction.Reset) })
            } else {
                RegistrationForm(
                    state = state,
                    onNameChange = { viewModel.handleAction(RegistrationAction.FullNameChanged(it)) },
                    onEmailChange = { viewModel.handleAction(RegistrationAction.EmailChanged(it)) },
                    onAgeChange = { viewModel.handleAction(RegistrationAction.AgeChanged(it)) },
                    onSubmit = { viewModel.handleAction(RegistrationAction.Submit) }
                )
            }
        }
    }
}

@Composable
private fun RegistrationForm(
    state: RegistrationState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    OutlinedTextField(
        value = state.fullName.value,
        onValueChange = onNameChange,
        label = { Text("Full Name") },
        modifier = Modifier.fillMaxWidth(),
        isError = state.fullName.isTouched && state.fullName.isInvalid,
        supportingText = {
            if (state.fullName.isTouched && state.fullName.isInvalid) {
                Text(state.fullName.errors.first(), color = MaterialTheme.colorScheme.error)
            }
        }
    )

    OutlinedTextField(
        value = state.email.value,
        onValueChange = onEmailChange,
        label = { Text("Email Address") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        isError = state.email.isTouched && state.email.isInvalid,
        supportingText = {
            if (state.email.isTouched && state.email.isInvalid) {
                Text(state.email.errors.first(), color = MaterialTheme.colorScheme.error)
            }
        }
    )

    OutlinedTextField(
        value = state.age.value,
        onValueChange = onAgeChange,
        label = { Text("Age") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = state.age.isTouched && state.age.isInvalid,
        supportingText = {
            if (state.age.isTouched && state.age.isInvalid) {
                Text(state.age.errors.first(), color = MaterialTheme.colorScheme.error)
            }
        }
    )

    Spacer(Modifier.height(8.dp))

    Button(
        onClick = onSubmit,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        enabled = !state.isSubmitting
    ) {
        if (state.isSubmitting) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Text("Register securely")
        }
    }
}

@Composable
private fun RegistrationSuccessCard(onReset: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8F5E9)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "🎉 Registration Successful!",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF2E7D32)
            )
            Text(
                "Your form passed all validation rules effortlessly.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1B5E20)
            )
            Button(
                onClick = onReset,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text("Start Again")
            }
        }
    }
}
