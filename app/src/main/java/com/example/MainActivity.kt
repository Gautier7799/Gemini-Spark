package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.GeminiScreen
import com.example.ui.GeminiViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // التعديل السحري: جلبنا الـ Application ومررناه عبر Factory مخصص
                    // لضمان عدم انهيار التطبيق عند تهيئة الـ ViewModel
                    val context = LocalContext.current
                    val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as Application)
                    val viewModel: GeminiViewModel = viewModel(factory = factory)
                    
                    GeminiScreen(viewModel = viewModel)
                }
            }
        }
    }
}
