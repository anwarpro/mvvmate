package com.helloanwar.mvvmate.generator.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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

/**
 * Time-Travel Debugging panel.
 *
 * Shows a slider over the state history, lets the developer browse
 * past states, and optionally "apply" a historical state to the device.
 */
@Composable
fun TimeTravelPanel(
    stateHistory: List<IndexedPayload>,
    modifier: Modifier = Modifier
) {
    if (stateHistory.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⏳", fontSize = 32.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "No state history yet",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Interact with your app to capture state changes",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
        return
    }

    var currentIndex by remember(stateHistory.size) {
        mutableStateOf(stateHistory.lastIndex)
    }

    val currentSnapshot = stateHistory.getOrNull(currentIndex)
    val previousSnapshot = if (currentIndex > 0) stateHistory.getOrNull(currentIndex - 1) else null

    Column(modifier.fillMaxSize().padding(12.dp)) {
        // Header
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Time Travel",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                "${currentIndex + 1} / ${stateHistory.size}",
                color = Color.Gray,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(Modifier.height(12.dp))

        // Navigation buttons
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { currentIndex = 0 },
                enabled = currentIndex > 0,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text("⏮ First")
            }
            OutlinedButton(
                onClick = { if (currentIndex > 0) currentIndex-- },
                enabled = currentIndex > 0,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text("⏪ Rewind")
            }

            // Slider
            Box(
                Modifier
                    .weight(1f)
                    .height(32.dp)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                SliderBar(
                    value = currentIndex,
                    max = stateHistory.lastIndex,
                    onValueChange = { currentIndex = it }
                )
            }

            OutlinedButton(
                onClick = { if (currentIndex < stateHistory.lastIndex) currentIndex++ },
                enabled = currentIndex < stateHistory.lastIndex,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Text("Forward ⏩")
            }
            OutlinedButton(
                onClick = { currentIndex = stateHistory.lastIndex },
                enabled = currentIndex < stateHistory.lastIndex,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Text("Last ⏭")
            }
        }

        Spacer(Modifier.height(12.dp))

        // Apply to device button
        if (currentSnapshot != null) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        currentSnapshot.payload.viewModelName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        "Snapshot #${currentSnapshot.index}",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
                DefaultButton(
                    onClick = {
                        RemoteDebugServer.sendCommand(
                            DebugCommand(
                                type = CommandType.SET_STATE,
                                viewModelName = currentSnapshot.payload.viewModelName,
                                payload = currentSnapshot.index.toString()
                            )
                        )
                    }
                ) {
                    Text("📲 Apply to Device")
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // State diff between previous and current snapshot
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, Color(0xFF444444), RoundedCornerShape(8.dp))
                .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    if (currentSnapshot != null) {
                        StateDiffView(
                            oldStateString = previousSnapshot?.payload?.payload
                                ?: currentSnapshot.payload.additionalData,
                            newStateString = currentSnapshot.payload.payload
                        )
                    }
                }
            }
        }
    }
}

/**
 * Simple slider bar implemented with clickable segments.
 */
@Composable
private fun SliderBar(
    value: Int,
    max: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (max <= 0) {
        Box(modifier.fillMaxWidth().height(4.dp).background(Color.Gray, RoundedCornerShape(2.dp)))
        return
    }

    Row(
        modifier = modifier.fillMaxWidth().height(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val filledFraction = value.toFloat() / max.toFloat()
        Box(
            Modifier
                .weight(filledFraction.coerceAtLeast(0.01f))
                .height(4.dp)
                .background(Color(0xFF64B5F6), RoundedCornerShape(2.dp))
        )
        // Thumb
        Box(
            Modifier
                .width(12.dp)
                .height(12.dp)
                .background(Color(0xFF42A5F5), RoundedCornerShape(6.dp))
        )
        Box(
            Modifier
                .weight((1f - filledFraction).coerceAtLeast(0.01f))
                .height(4.dp)
                .background(Color(0xFF555555), RoundedCornerShape(2.dp))
        )
    }
}
