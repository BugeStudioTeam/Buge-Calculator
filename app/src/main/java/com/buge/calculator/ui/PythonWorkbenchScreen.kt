package com.buge.calculator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.buge.calculator.data.BugeStrings
import com.buge.calculator.data.PythonWorkspace

/**
 * Preferred height of the output panel. Keeping it constant means the editor above
 * and the panel below never jump when a program produces more or fewer lines;
 * overflow simply becomes scrollable inside the panel.
 */
private val OUTPUT_PANEL_HEIGHT = 168.dp

@Composable
fun PythonWorkbenchScreen(
    modifier: Modifier,
    workspace: PythonWorkspace,
    strings: BugeStrings,
    onCodeChange: (String) -> Unit,
    onRun: () -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    // The editor keeps a guaranteed minimum share of the screen; the output panel
    // then takes the preferred fixed height, shrinking only on very short screens
    // so nothing is ever pushed off the bottom.
    val outputHeight = OUTPUT_PANEL_HEIGHT.coerceAtMost(maxHeight * 0.34f)
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(strings.pythonHelp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(3.dp))
                Text(strings.pythonHelpDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        // No minLines/maxLines here: the field's height is driven purely by the
        // Column weight, so the editable area always fills the whole box. Capping
        // maxLines made the inner text area shorter than the weighted frame and
        // left a large dead (non-editable) blank space at the bottom.
        OutlinedTextField(
            value = workspace.code,
            onValueChange = onCodeChange,
            modifier = Modifier.fillMaxWidth().weight(1f).heightIn(min = 120.dp),
            label = { Text(strings.pythonCode) },
            placeholder = { Text(strings.pythonCodeHint) },
            leadingIcon = { Icon(Icons.Filled.Code, null) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            visualTransformation = PythonSyntaxVisualTransformation(MaterialTheme.colorScheme)
        )
        Button(onClick = onRun, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Code, null)
            Spacer(Modifier.padding(horizontal = 4.dp))
            Text(strings.runPython)
        }
        // The output panel keeps a constant height regardless of how much text it
        // holds; long output scrolls inside the panel instead of resizing the layout.
        val outputScroll = rememberScrollState()
        Card(
            modifier = Modifier.fillMaxWidth().height(outputHeight),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(strings.pythonResult, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = workspace.error ?: workspace.output.ifBlank { "—" },
                    modifier = Modifier.fillMaxSize().verticalScroll(outputScroll),
                    color = if (workspace.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
    }
}
