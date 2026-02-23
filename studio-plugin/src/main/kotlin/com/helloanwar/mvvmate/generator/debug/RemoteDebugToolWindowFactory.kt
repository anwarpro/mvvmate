package com.helloanwar.mvvmate.generator.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helloanwar.mvvmate.debug.PayloadType
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import org.jetbrains.jewel.bridge.addComposeTab
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text

class RemoteDebugToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        RemoteDebugServer.start(8080)
        
        toolWindow.addComposeTab("Timeline", focusOnClickInside = true) {
            TimelineTab()
        }
        toolWindow.addComposeTab("Time Travel", focusOnClickInside = true) {
            TimeTravelTab()
        }
        toolWindow.addComposeTab("Action Injector", focusOnClickInside = true) {
            ActionInjectorTab()
        }
    }
}

// ─────────────────────────────────────────────
// Tab 1: Enhanced Timeline with State Diffs
// ─────────────────────────────────────────────

@Composable
fun TimelineTab() {
    var refreshTrigger by remember { mutableStateOf(0) }

    DisposableEffect(Unit) {
        RemoteDebugServer.onUpdate = { refreshTrigger++ }
        onDispose { RemoteDebugServer.onUpdate = null }
    }

    val logs = remember(refreshTrigger) { RemoteDebugServer.logs }
    val states = remember(refreshTrigger) { RemoteDebugServer.latestStates }
    var selectedViewModel by remember { mutableStateOf<String?>(null) }
    val viewModels = remember(states) { states.keys.toList() }

    Row(Modifier.fillMaxSize()) {
        // Left Panel – Timeline / Logs
        Column(Modifier.weight(1f).fillMaxHeight().padding(8.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text("Action Timeline", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = { RemoteDebugServer.clearLogs() }) {
                    Text("🗑 Clear")
                }
            }
            Spacer(Modifier.height(8.dp))

            val filteredLogs = logs.filter { selectedViewModel == null || it.viewModelName == selectedViewModel }
            val listState = rememberLazyListState()

            // Auto-scroll to bottom when new logs arrive
            LaunchedEffect(filteredLogs.size) {
                if (filteredLogs.isNotEmpty()) {
                    listState.animateScrollToItem(filteredLogs.lastIndex)
                }
            }

            LazyColumn(Modifier.weight(1f), state = listState) {
                items(filteredLogs) { log ->
                    val typeColor = when (log.type) {
                        PayloadType.ACTION -> Color(0xFF64B5F6) // blue
                        PayloadType.STATE_CHANGE -> Color(0xFFFFD54F) // amber
                        PayloadType.EFFECT -> Color(0xFFBA68C8) // purple
                        PayloadType.ERROR -> Color(0xFFE57373) // red
                        PayloadType.NETWORK -> Color(0xFF4DB6AC) // teal
                    }
                    val typeIcon = when (log.type) {
                        PayloadType.ACTION -> "▶"
                        PayloadType.STATE_CHANGE -> "🔄"
                        PayloadType.EFFECT -> "⚡"
                        PayloadType.ERROR -> "❌"
                        PayloadType.NETWORK -> "🌐"
                    }

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .border(1.dp, typeColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .background(typeColor.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Row {
                                Text(
                                    "$typeIcon ${log.type.name}",
                                    color = typeColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    log.viewModelName,
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(Modifier.height(4.dp))

                            // For STATE_CHANGE, show visual diff
                            if (log.type == PayloadType.STATE_CHANGE) {
                                StateDiffView(
                                    oldStateString = log.additionalData,
                                    newStateString = log.payload
                                )
                            } else {
                                Text(
                                    log.payload,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                                val extra = log.additionalData
                                if (extra != null) {
                                    Text(
                                        extra,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Divider
        Box(Modifier.width(1.dp).fillMaxHeight().background(Color(0xFF444444)))

        // Right Panel – Current State + ViewModel selector
        Column(Modifier.weight(1f).fillMaxHeight().padding(8.dp)) {
            Text("Live State", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))

            // ViewModel selector chips
            Row {
                viewModels.forEach { vm ->
                    val isSelected = selectedViewModel == vm
                    val onClick = { selectedViewModel = if (isSelected) null else vm }
                    if (isSelected) {
                        DefaultButton(onClick = onClick, modifier = Modifier.padding(end = 4.dp)) {
                            Text(vm, fontSize = 12.sp)
                        }
                    } else {
                        OutlinedButton(onClick = onClick, modifier = Modifier.padding(end = 4.dp)) {
                            Text(vm, fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            val currentState = selectedViewModel?.let { states[it] }
            if (currentState != null) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF444444), RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    LazyColumn {
                        item {
                            Text(
                                currentState,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                Text(
                    "Select a ViewModel or perform an action to see its state",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// Tab 2: Time Travel
// ─────────────────────────────────────────────

@Composable
fun TimeTravelTab() {
    var refreshTrigger by remember { mutableStateOf(0) }

    DisposableEffect(Unit) {
        val prev = RemoteDebugServer.onUpdate
        RemoteDebugServer.onUpdate = {
            refreshTrigger++
            prev?.invoke()
        }
        onDispose { RemoteDebugServer.onUpdate = prev }
    }

    val history = remember(refreshTrigger) { RemoteDebugServer.stateHistory }

    TimeTravelPanel(stateHistory = history)
}

// ─────────────────────────────────────────────
// Tab 3: Action Injector
// ─────────────────────────────────────────────

@Composable
fun ActionInjectorTab() {
    var refreshTrigger by remember { mutableStateOf(0) }

    DisposableEffect(Unit) {
        val prev = RemoteDebugServer.onUpdate
        RemoteDebugServer.onUpdate = {
            refreshTrigger++
            prev?.invoke()
        }
        onDispose { RemoteDebugServer.onUpdate = prev }
    }

    val states = remember(refreshTrigger) { RemoteDebugServer.latestStates }
    val viewModels = remember(states) { states.keys.toList() }
    val suggestions = remember(refreshTrigger) { RemoteDebugServer.actionSuggestions }

    ActionInjectorPanel(
        knownViewModels = viewModels,
        actionSuggestionsMap = suggestions
    )
}
