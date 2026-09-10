package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.GeminiRepository
import com.example.data.repository.Result
import com.example.ui.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GeminiUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val selectedModel: String = "gemini-1.5-flash",
    val customApiKey: String = "",
    val activeApiKey: String? = null,
    val errorMessage: String? = null,
    val inputText: String = "",
    val showApiKeyDialog: Boolean = false
)

class GeminiViewModel(
    application: Application,
    private val repository: GeminiRepository = GeminiRepository()
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("gemini_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(GeminiUiState())
    val uiState: StateFlow<GeminiUiState> = _uiState.asStateFlow()

    val availableModels = listOf("gemini-1.5-flash", "gemini-flash-latest")

    val quickPrompts = listOf(
        "Explain how AI works in a few words",
        "اشرح لي كيف يعمل الذكاء الاصطناعي باختصار",
        "أهم ممارسات تقليل استهلاك البطارية في أندرويد"
    )

    init {
        val savedKey = prefs.getString("custom_api_key", "") ?: ""
        _uiState.update {
            it.copy(
                customApiKey = savedKey,
                activeApiKey = repository.resolveApiKey(savedKey),
                messages = listOf(
                    ChatMessage(
                        text = "مرحباً يا شريك! أنا مساعدك الذكي Gemini بتصميم Material You.\nكيف يمكنني مساعدتك اليوم؟",
                        isUser = false
                    )
                )
            )
        }
    }

    fun onInputTextChanged(text: String) = _uiState.update { it.copy(inputText = text) }
    fun selectModel(model: String) = _uiState.update { it.copy(selectedModel = model) }
    fun setApiKeyDialogOpen(open: Boolean) = _uiState.update { it.copy(showApiKeyDialog = open) }

    fun saveCustomApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString("custom_api_key", trimmed).apply()
        _uiState.update {
            it.copy(
                customApiKey = trimmed,
                activeApiKey = repository.resolveApiKey(trimmed),
                showApiKeyDialog = false
            )
        }
    }

    fun clearChat() {
        _uiState.update {
            it.copy(
                messages = listOf(ChatMessage(text = "تم بدء جلسة جديدة. تفضل بسؤالك!", isUser = false)),
                errorMessage = null
            )
        }
    }

    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }

    fun sendMessage(promptText: String? = null) {
        val query = (promptText ?: _uiState.value.inputText).trim()
        if (query.isBlank() || _uiState.value.isLoading) return

        val key = repository.resolveApiKey(_uiState.value.customApiKey)
        if (key.isNullOrBlank()) {
            _uiState.update { it.copy(showApiKeyDialog = true, errorMessage = "يرجى إدخال مفتاح API للمتابعة.") }
            return
        }

        val userMessage = ChatMessage(text = query, isUser = true)
        val model = _uiState.value.selectedModel

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = if (promptText == null) "" else it.inputText,
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            when (val result = repository.generateContent(query, key, model)) {
                is Result.Success -> {
                    val aiMsg = ChatMessage(text = result.data, isUser = false, modelName = model)
                    _uiState.update { it.copy(messages = it.messages + aiMsg, isLoading = false) }
                }
                is Result.Error -> {
                    val errorMsg = ChatMessage(text = "⚠️ ${result.message}", isUser = false, isError = true)
                    _uiState.update { it.copy(messages = it.messages + errorMsg, isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }
}
