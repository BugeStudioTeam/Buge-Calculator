package com.buge.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import com.buge.calculator.ui.BugeCalculatorApp
import com.buge.calculator.viewmodel.CalculatorViewModel

class MainActivity : ComponentActivity() {
    private val calculatorViewModel: CalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { BugeCalculatorApp(calculatorViewModel) }
    }

    override fun onResume() {
        super.onResume()
        calculatorViewModel.refreshFromActivities()
    }
}
