package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.data.local.AppDatabase
import com.example.data.repository.WorkshopRepository
import com.example.ui.MainApp
import com.example.ui.theme.WindowWorkshopTheme

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize Room Database and Repository
    val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
    val repository = WorkshopRepository(database.appDao())

    setContent {
      WindowWorkshopTheme {
        MainApp(repository = repository)
      }
    }
  }
}
