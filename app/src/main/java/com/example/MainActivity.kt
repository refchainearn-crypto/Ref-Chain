package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.data.db.AppDatabase
import com.example.data.repository.RefChainRepository
import com.example.ui.RefChainApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Core database and repository initialization
    val database = AppDatabase.getDatabase(applicationContext)
    val dao = database.dao()
    val repository = RefChainRepository(dao)

    setContent {
      Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        RefChainApp(
          repository = repository,
          modifier = Modifier.padding(innerPadding)
        )
      }
    }
  }
}

