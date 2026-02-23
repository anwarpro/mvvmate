package com.helloanwar.mvvmate.generator.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helloanwar.mvvmate.debug.CommandType
import com.helloanwar.mvvmate.debug.DebugCommand
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextArea

/**
 * Preset quick-inject action for common edge-case simulation.
 */
private data class PresetAction(
    val label: String,
    val icon: String,
    val actionPayload: String
)

private val presetActions = listOf(
    PresetAction("Simulate Error", "💥", "SimulateError"),
    PresetAction("Simulate Timeout", "⏱️", "SimulateTimeout"),
    PresetAction("Simulate Empty", "📭", "SimulateEmptyState"),
    PresetAction("Force Refresh", "🔄", "ForceRefresh"),
    PresetAction("Toggle Loading", "⏳", "ToggleLoading"),
    PresetAction("Simulate Auth Expire", "🔐", "SimulateAuthExpired"),
)

/**
 * Action Injector panel.
 *
 * Allows developers to inject UiAction payloads directly from the IDE
 * into a running app's ViewModel without clicking through the UI.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActionInjectorPanel(
    knownViewModels: List<String>,
    actionSuggestionsMap: Map<String, Map<String, String>> = emptyMap(),
    modifier: Modifier = Modifier
) {
    var selectedViewModel by remember { mutableStateOf<String?>(null) }
    val actionFieldState = rememberTextFieldState()
    var injectionHistory by remember { mutableStateOf(listOf<String>()) }

    val suggestions = remember(selectedViewModel, actionSuggestionsMap) {
        if (selectedViewModel == null) emptyList()
        else actionSuggestionsMap[selectedViewModel]?.values?.toList() ?: emptyList()
    }

    Column(modifier.fillMaxSize().padding(12.dp)) {
        Text(
            "Action Injector",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Inject actions directly into a ViewModel to simulate edge cases",
            color = Color.Gray,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(12.dp))

        // ViewModel selector
        Text("Target ViewModel", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))

        if (knownViewModels.isEmpty()) {
            Text(
                "No ViewModels connected yet. Start your app with RemoteDebugLogger.",
                color = Color(0xFFFFAB40),
                fontSize = 12.sp
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                knownViewModels.forEach { vm ->
                    val isSelected = selectedViewModel == vm
                    if (isSelected) {
                        DefaultButton(onClick = { selectedViewModel = null }) {
                            Text(vm, fontSize = 12.sp)
                        }
                    } else {
                        OutlinedButton(onClick = { selectedViewModel = vm }) {
                            Text(vm, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Action suggestions (Auto-collected and Filtered)
        if (suggestions.isNotEmpty()) {
            Text("Dispatched Actions (Suggestions)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                suggestions.forEach { suggestion ->
                    OutlinedButton(
                        onClick = {
                            actionFieldState.edit { 
                                replace(0, length, suggestion)
                            }
                        }
                    ) {
                        Text(suggestion, fontSize = 11.sp, color = Color(0xFF64B5F6))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Action input
        Text("Action Payload", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        TextArea(
            state = actionFieldState,
            placeholder = { Text("e.g. LoadUsers or {\"type\":\"Search\",\"query\":\"test\"}", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().height(80.dp)
        )

        Spacer(Modifier.height(8.dp))

        // Inject button
        DefaultButton(
            onClick = {
                val actionPayload = actionFieldState.text.toString()
                if (selectedViewModel != null && actionPayload.isNotBlank()) {
                    RemoteDebugServer.sendCommand(
                        DebugCommand(
                            type = CommandType.INJECT_ACTION,
                            viewModelName = selectedViewModel!!,
                            payload = actionPayload.trim()
                        )
                    )
                    injectionHistory = injectionHistory + "→ $selectedViewModel :: $actionPayload"
                    actionFieldState.edit { replace(0, length, "") }
                }
            },
            enabled = selectedViewModel != null && actionFieldState.text.isNotBlank()
        ) {
            Text("🚀 Inject Action")
        }

        Spacer(Modifier.height(16.dp))

        // Quick-action presets
        Text("Quick Presets", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            presetActions.forEach { preset ->
                OutlinedButton(
                    onClick = {
                        if (selectedViewModel != null) {
                            RemoteDebugServer.sendCommand(
                                DebugCommand(
                                    type = CommandType.INJECT_ACTION,
                                    viewModelName = selectedViewModel!!,
                                    payload = preset.actionPayload
                                )
                            )
                            injectionHistory = injectionHistory + "→ $selectedViewModel :: ${preset.actionPayload}"
                        }
                    },
                    enabled = selectedViewModel != null
                ) {
                    Text("${preset.icon} ${preset.label}", fontSize = 11.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Injection history
        if (injectionHistory.isNotEmpty()) {
            Text("Injection Log", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, Color(0xFF444444), RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                LazyColumn {
                    items(injectionHistory.reversed()) { entry ->
                        Text(
                            entry,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFF81C784),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
