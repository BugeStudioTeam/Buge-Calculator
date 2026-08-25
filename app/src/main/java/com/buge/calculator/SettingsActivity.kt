package com.buge.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.buge.calculator.data.strings
import com.buge.calculator.ui.SettingsScreen
import com.buge.calculator.ui.theme.BugeTheme
import com.buge.calculator.viewmodel.CalculatorViewModel

@OptIn(ExperimentalMaterial3Api::class)
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: CalculatorViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val strings = settings.language.strings()
            BugeTheme(settings) {
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text(strings.settings, fontWeight = FontWeight.Medium) },
                            navigationIcon = {
                                IconButton(onClick = ::finish) {
                                    Text("‹", fontSize = 32.sp)
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                        )
                    }
                ) { padding ->
                    SettingsScreen(
                        modifier = Modifier.padding(padding),
                        settings = settings,
                        strings = strings,
                        onChange = viewModel::updateSettings
                    )
                }
            }
        }
    }
}
