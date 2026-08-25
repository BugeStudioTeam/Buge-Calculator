package com.buge.calculator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.buge.calculator.data.BugeStrings
import com.buge.calculator.data.PythonWorkspace

@Composable
fun PythonWorkbenchScreen(
    modifier: Modifier,
    workspace: PythonWorkspace,
    strings: BugeStrings,
    onCodeChange: (String) -> Unit,
    onRun: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
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
        OutlinedTextField(
            value = workspace.code,
            onValueChange = onCodeChange,
            modifier = Modifier.fillMaxWidth().weight(1f),
            label = { Text(strings.pythonCode) },
            placeholder = { Text(strings.pythonCodeHint) },
            leadingIcon = { Icon(Icons.Filled.Code, null) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            minLines = 8,
            maxLines = 14
        )
        Button(onClick = onRun, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Code, null)
            Spacer(Modifier.padding(horizontal = 4.dp))
            Text(strings.runPython)
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(strings.pythonResult, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = workspace.error ?: workspace.output.ifBlank { "—" },
                    color = if (workspace.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
