package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.example.ui.theme.MyApplicationTheme
import java.util.UUID

// 1. Data Models
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val isError: Boolean = false
)
data class GeminiRequest(val contents: List<Content>)
data class Content(val parts: List<Part>, val role: String? = null)
data class Part(val text: String? = null)
data class GeminiResponse(val candidates: List<Candidate>? = null)
data class Candidate(val content: Content? = null)

// 2. Retrofit API
interface GeminiApi {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiRequest
    ): retrofit2.Response<GeminiResponse>
}

// 3. ViewModel
class AppViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage(text = "مرحباً يا بطل! تم حل المشكلة بفضل الله. ضع مفتاح API في الخانة أعلاه واسألني أي شيء.", isUser = false))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    var apiKey = MutableStateFlow("")

    private val api: GeminiApi by lazy {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApi::class.java)
    }

    fun updateInput(text: String) { _inputText.value = text }
    
    fun updateKey(key: String) { apiKey.value = key }

    fun send() {
        val text = _inputText.value.trim()
        val key = apiKey.value.trim()
        if (text.isEmpty()) return
        if (key.isEmpty()) {
            _messages.update { it + ChatMessage(text = "عذراً! يجب إدخال مفتاح API أولاً في الخانة بالأعلى.", isUser = false, isError = true) }
            return
        }

        _messages.update { it + ChatMessage(text = text, isUser = true) }
        _inputText.value = ""
        _isLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val req = GeminiRequest(listOf(Content(listOf(Part(text)), "user")))
                val res = api.generateContent("gemini-1.5-flash", key, req)
                if (res.isSuccessful) {
                    val reply = res.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "لا يوجد رد"
                    _messages.update { it + ChatMessage(text = reply.trim(), isUser = false) }
                } else {
                    _messages.update { it + ChatMessage(text = "خطأ: ${res.code()}", isUser = false, isError = true) }
                }
            } catch (e: Exception) {
                _messages.update { it + ChatMessage(text = "خطأ في الاتصال: ${e.message}", isUser = false, isError = true) }
            } finally {
                _isLoading.value = false
            }
        }
    }
}

// 4. UI 
class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: AppViewModel) {
    val messages by viewModel.messages.collectAsState()
    val input by viewModel.inputText.collectAsState()
    val loading by viewModel.isLoading.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(top = 48.dp, bottom = 16.dp)) {
        // Header Title
        Text(
            text = "المساعد الذكي",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // API Key Input
        OutlinedTextField(
            value = apiKey,
            onValueChange = { viewModel.updateKey(it) },
            label = { Text("ضع مفتاح API هنا للبدء") },
            leadingIcon = { Icon(Icons.Outlined.Key, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Chat Area
        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            items(messages) { msg ->
                ChatBubble(msg)
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (loading) {
                item {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp).size(24.dp))
                }
            }
        }

        // Input Area
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { viewModel.updateInput(it) },
                placeholder = { Text("اكتب سؤالك...") },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilledIconButton(
                onClick = { viewModel.send() },
                modifier = Modifier.size(52.dp),
                shape = CircleShape
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "إرسال")
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (msg.isError) MaterialTheme.colorScheme.errorContainer 
                    else if (msg.isUser) MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(
                topStart = 20.dp, topEnd = 20.dp,
                bottomStart = if (msg.isUser) 20.dp else 4.dp,
                bottomEnd = if (msg.isUser) 4.dp else 20.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = msg.text,
                modifier = Modifier.padding(14.dp),
                color = if (msg.isError) MaterialTheme.colorScheme.onErrorContainer 
                        else if (msg.isUser) MaterialTheme.colorScheme.onPrimary 
                        else MaterialTheme.colorScheme.onSecondaryContainer,
                fontSize = 15.sp
            )
        }
    }
}
