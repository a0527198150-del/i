package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.BudgetScreen
import com.example.ui.BudgetViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.resolveDarkTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val viewModel: BudgetViewModel = viewModel()
      val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
      MyApplicationTheme(darkTheme = resolveDarkTheme(themeMode)) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          BudgetScreen(
              viewModel = viewModel,
              modifier = Modifier.padding(innerPadding)
          )
        }
      }
    }
  }
}

