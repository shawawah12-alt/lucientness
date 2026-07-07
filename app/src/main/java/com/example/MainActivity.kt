package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.FileRepository
import com.example.data.SettingsRepository
import com.example.ui.MainScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.EditorViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val repository = FileRepository(applicationContext)
    val settings = SettingsRepository(applicationContext)
    
    setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
            val viewModel: EditorViewModel = viewModel(
                factory = EditorViewModelFactory(repository, settings)
            )
            MainScreen(viewModel)
        }
      }
    }
  }
}
