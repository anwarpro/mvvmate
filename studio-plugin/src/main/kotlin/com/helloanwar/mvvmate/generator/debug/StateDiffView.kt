package com.helloanwar.mvvmate.generator.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.ui.component.Text

/**
 * Git-style visual diff for state changes.
 *
 * Shows each changed field with 🔴 old → 🟢 new coloring,
 * and dims unchanged fields.
 */
@Composable
fun StateDiffView(
    oldStateString: String?,
    newStateString: String?,
    modifier: Modifier = Modifier
) {
    if (newStateString == null) {
        Text("(no state)", modifier = modifier)
        return
    }

    val oldFields = remember(oldStateString) { parseFieldsFromString(oldStateString ?: "") }
    val newFields = remember(newStateString) { parseFieldsFromString(newStateString) }

    // If we can't parse fields, show raw text
    if (newFields.isEmpty()) {
        Column(modifier) {
            if (oldStateString != null && oldStateString != newStateString) {
                DiffLine(field = "state", oldValue = oldStateString, newValue = newStateString, changed = true)
            } else {
                Text(newStateString, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }
        return
    }

    var expanded by remember { mutableStateOf(false) }
    val changedKeys = remember(oldFields, newFields) {
        newFields.keys.filter { key -> oldFields[key] != newFields[key] }.toSet()
    }
    val unchangedKeys = remember(oldFields, newFields) {
        newFields.keys.filter { key -> oldFields[key] == newFields[key] }.toSet()
    }

    Column(modifier.fillMaxWidth()) {
        // Show changed fields first
        changedKeys.forEach { key ->
            DiffLine(
                field = key,
                oldValue = oldFields[key] ?: "(new)",
                newValue = newFields[key] ?: "",
                changed = true
            )
        }

        // Collapsible unchanged fields
        if (unchangedKeys.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .clickable { expanded = !expanded }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (expanded) "▾" else "▸",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "${unchangedKeys.size} unchanged field(s)",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            if (expanded) {
                unchangedKeys.forEach { key ->
                    DiffLine(
                        field = key,
                        oldValue = newFields[key] ?: "",
                        newValue = newFields[key] ?: "",
                        changed = false
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiffLine(
    field: String,
    oldValue: String,
    newValue: String,
    changed: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = if (changed) Color(0x14FFEB3B) else Color.Transparent

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Field name on its own line if it's long or just to give more space
        Text(
            text = field,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = if (changed) FontWeight.Bold else FontWeight.Normal,
            color = if (changed) Color.White else Color.Gray,
            modifier = Modifier.padding(bottom = 2.dp)
        )

        if (changed) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Old value (red)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFE57373))
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = formatForWrap(oldValue),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color(0xFFE57373)
                    )
                }

                Text("→", fontSize = 12.sp, color = Color.Gray)

                // New value (green)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF81C784))
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = formatForWrap(newValue),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color(0xFF81C784)
                    )
                }
            }
        } else {
            Text(
                text = formatForWrap(newValue),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
    }
}

/**
 * Adds spaces after commas in a string to help with wrapping.
 * e.g. "LoginState(isLoading=true,error=null)" -> "LoginState(isLoading=true, error=null)"
 */
private fun formatForWrap(text: String): String {
    return text.replace(",", ", ")
}

/**
 * Parse a data class `toString()` output into field→value pairs.
 * e.g. `LoginState(isLoading=true, error=null)` → `{isLoading: "true", error: "null"}`
 */
internal fun parseFieldsFromString(toString: String): Map<String, String> {
    val parenOpen = toString.indexOf('(')
    if (parenOpen < 0 || !toString.endsWith(")")) return emptyMap()

    val content = toString.substring(parenOpen + 1, toString.length - 1)
    if (content.isEmpty()) return emptyMap()

    return splitFields(content)
}

private fun splitFields(content: String): Map<String, String> {
    val result = mutableMapOf<String, String>()
    var depth = 0
    var currentStart = 0

    for (i in content.indices) {
        when (content[i]) {
            '(', '[', '{' -> depth++
            ')', ']', '}' -> depth--
            ',' -> if (depth == 0) {
                parseField(content.substring(currentStart, i).trim())?.let { (k, v) ->
                    result[k] = v
                }
                currentStart = i + 1
            }
        }
    }
    parseField(content.substring(currentStart).trim())?.let { (k, v) ->
        result[k] = v
    }
    return result
}

private fun parseField(field: String): Pair<String, String>? {
    val eqIndex = field.indexOf('=')
    if (eqIndex <= 0) return null
    val key = field.substring(0, eqIndex).trim()
    val value = field.substring(eqIndex + 1).trim()
    return key to value
}
