package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.GeminiScreen
import com.example.ui.GeminiViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    
    // الحل النهائي: هذه الطريقة القياسية والأكثر أماناً في أندرويد 
    // لتهيئة الـ ViewModel وضمان عدم انهيار التطبيق
    private val viewModel: GeminiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // نمرر الـ ViewModel الجاهز والمستقر للواجهة
                    GeminiScreen(viewModel = viewModel)
                }
            }
        }
    }
}
