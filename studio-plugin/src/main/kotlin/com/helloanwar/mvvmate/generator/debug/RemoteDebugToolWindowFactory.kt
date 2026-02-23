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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
        
        toolWindow.addComposeTab("State", focusOnClickInside = true) {
            RemoteDebugScreen()
        }
    }
}

@Composable
fun RemoteDebugScreen() {
    var refreshTrigger by remember { mutableStateOf(0) }
    
    androidx.compose.runtime.DisposableEffect(Unit) {
        RemoteDebugServer.onUpdate = {
            refreshTrigger++
        }
        onDispose {
            RemoteDebugServer.onUpdate = null
        }
    }

    val logs = remember(refreshTrigger) { RemoteDebugServer.logs }
    val states = remember(refreshTrigger) { RemoteDebugServer.latestStates }
    var selectedViewModel by remember { mutableStateOf<String?>(null) }
    
    val viewModels = remember(states) { states.keys.toList() }
    
    Row(Modifier.fillMaxSize()) {
        // Left Panel - Timeline/Logs
        Column(Modifier.weight(1f).fillMaxHeight().padding(8.dp)) {
            Text("Action Timeline")
            Spacer(Modifier.height(8.dp))
            DefaultButton(onClick = { RemoteDebugServer.clearLogs() }) {
                Text("Clear logs")
            }
            Spacer(Modifier.height(8.dp))
            val filteredLogs = logs.filter { selectedViewModel == null || it.viewModelName == selectedViewModel }
            LazyColumn(Modifier.weight(1f)) {
                items(filteredLogs) { log ->
                    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color.Gray).padding(8.dp)) {
                        Column {
                            Text("[${log.viewModelName}] ${log.type.name}")
                            Text(log.payload)
                            if (log.additionalData != null) {
                                Text("Prev/Metadata: ${log.additionalData}")
                            }
                        }
                    }
                }
            }
        }
        
        Box(Modifier.width(1.dp).fillMaxHeight().background(Color.Gray))
        
        // Right Panel - Current State
        Column(Modifier.weight(1f).fillMaxHeight().padding(8.dp)) {
            Text("Current State")
            Spacer(Modifier.height(8.dp))
            
            // ViewModel selector
            Row {
                viewModels.forEach { vm ->
                    val isSelected = selectedViewModel == vm
                    val onClick = { selectedViewModel = if (isSelected) null else vm }
                    if (isSelected) {
                        DefaultButton(onClick = onClick, modifier = Modifier.padding(end = 4.dp)) {
                            Text(vm)
                        }
                    } else {
                        OutlinedButton(onClick = onClick, modifier = Modifier.padding(end = 4.dp)) {
                            Text(vm)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            
            val currentState = selectedViewModel?.let { states[it] }
            if (currentState != null) {
                LazyColumn(Modifier.weight(1f)) {
                    item {
                        Text(currentState)
                    }
                }
            } else {
                Text("Select a ViewModel or perform an action to see its state")
            }
        }
    }
}
