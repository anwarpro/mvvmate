package com.helloanwar.mvvmate.generator

import com.helloanwar.mvvmate.generator.debug.ActionInjectorTab
import com.helloanwar.mvvmate.generator.debug.RemoteDebugServer
import com.helloanwar.mvvmate.generator.debug.TimeTravelTab
import com.helloanwar.mvvmate.generator.debug.TimelineTab
import com.helloanwar.mvvmate.generator.ui.TestGeneratorScreen
import com.intellij.diagnostic.LoadingState
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import org.jetbrains.jewel.bridge.addComposeTab
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.Text

class MVVMateToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun shouldBeAvailable(project: Project) = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // Timeline Tab (Primary tab)
        toolWindow.addComposeTab("Timeline", focusOnClickInside = true) {
            // Ensure server is started only when UI is actually composed and IDE is ready
            LaunchedEffect(Unit) {
                if (LoadingState.COMPONENTS_LOADED.isOccurred) {
                    RemoteDebugServer.start(8080)
                }
            }
            
            if (!LoadingState.COMPONENTS_LOADED.isOccurred) {
                Box(Modifier.fillMaxSize()) {
                    Text("Initializing Studio components...", modifier = Modifier.padding(16.dp))
                }
            } else {
                TimelineTab()
            }
        }

        toolWindow.addComposeTab("Time Travel", focusOnClickInside = true) {
            if (!LoadingState.COMPONENTS_LOADED.isOccurred) {
                Box(Modifier.fillMaxSize()) {
                    Text("Loading Time Travel...", modifier = Modifier.padding(16.dp))
                }
            } else {
                TimeTravelTab()
            }
        }

        toolWindow.addComposeTab("Action Injector", focusOnClickInside = true) {
            if (!LoadingState.COMPONENTS_LOADED.isOccurred) {
                Box(Modifier.fillMaxSize()) {
                    Text("Loading Action Injector...", modifier = Modifier.padding(16.dp))
                }
            } else {
                ActionInjectorTab()
            }
        }

        toolWindow.addComposeTab("AI Test Generator", focusOnClickInside = true) {
            TestGeneratorScreen(project)
        }
    }
}
