package com.buge.calculator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.buge.calculator.data.AngleUnit
import com.buge.calculator.data.BugeStrings
import com.buge.calculator.data.SurfaceSettings

@Composable
fun Model3DScreen(
    modifier: Modifier,
    surface: SurfaceSettings,
    angleUnit: AngleUnit,
    strings: BugeStrings,
    onExpressionChange: (String) -> Unit,
    onMeshChange: (Boolean) -> Unit,
    onCameraChange: (Float, Float, Float) -> Unit,
    onResetCamera: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(strings.surfaceHelp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(3.dp))
                Text(strings.surfaceHelpDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }
        OutlinedTextField(
            value = surface.expression,
            onValueChange = onExpressionChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("z = ${strings.expression}") },
            placeholder = { Text(strings.surfaceExpressionHint) },
            leadingIcon = { Icon(Icons.Filled.Functions, null) }
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
                selected = surface.showMesh,
                onClick = { onMeshChange(!surface.showMesh) },
                label = { Text(strings.showMesh) }
            )
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = onResetCamera) {
                Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(strings.resetCamera)
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            FormulaSurfaceCanvas(
                surface = surface,
                angleUnit = angleUnit,
                modifier = Modifier.padding(6.dp),
                onCameraChange = onCameraChange
            )
        }
    }
}
