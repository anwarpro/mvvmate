package com.helloanwar.mvvmate.generator.ui


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import org.jetbrains.jewel.bridge.addComposeTab
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextArea
import org.jetbrains.jewel.ui.component.TextField

class TestGeneratorToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun shouldBeAvailable(project: Project) = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        testGeneratorApp(project, toolWindow)
    }

    private fun testGeneratorApp(project: Project, toolWindow: ToolWindow) {
        toolWindow.addComposeTab("AI Test Generator", focusOnClickInside = true) {
            TestGeneratorScreen(project)
        }
    }
}

@androidx.compose.runtime.Composable
fun TestGeneratorScreen(project: Project) {
    val apiKeyFieldState = androidx.compose.foundation.text.input.rememberTextFieldState()
    val targetClassFieldState = androidx.compose.foundation.text.input.rememberTextFieldState()
    val crashTraceFieldState = androidx.compose.foundation.text.input.rememberTextFieldState()
    val generatedTestFieldState = androidx.compose.foundation.text.input.rememberTextFieldState()

    var isGenerating by remember { androidx.compose.runtime.mutableStateOf(false) }

    val testGenerator = remember { com.helloanwar.mvvmate.generator.AiTestGenerator() }

    androidx.compose.runtime.LaunchedEffect(isGenerating) {
        if (isGenerating) {
            val apiKey = apiKeyFieldState.text.toString()
            val crashTrace = crashTraceFieldState.text.toString()
            val targetClassName = targetClassFieldState.text.toString()
            val target = targetClassName.ifBlank { "GivenViewModel" }
            
            val result = testGenerator.generateTest(crashTrace, target, apiKey)
            generatedTestFieldState.edit {
                replace(0, length, result)
            }
            isGenerating = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(
            "MVVMate AI Test Generator",
            style = JewelTheme.defaultTextStyle
        )
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                state = apiKeyFieldState,
                placeholder = { Text("Gemini API Key") },
                modifier = Modifier.weight(1f),
            )
            TextField(
                state = targetClassFieldState,
                placeholder = { Text("Target Class Name (e.g., LoginViewModel)") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(8.dp))

        TextArea(
            state = crashTraceFieldState,
            placeholder = { Text("Paste AiCrashTrace JSON Output") },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )

        Spacer(Modifier.height(16.dp))

        DefaultButton(
            onClick = {
                isGenerating = true
            },
            enabled = apiKeyFieldState.text.isNotBlank() && crashTraceFieldState.text.isNotBlank() && !isGenerating,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isGenerating) "Generating..." else "✨ Generate Test")
        }

        Spacer(Modifier.height(16.dp))

        TextArea(
            state = generatedTestFieldState,
            placeholder = { Text("Generated Kotlin Unit Test") },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
    }
}
