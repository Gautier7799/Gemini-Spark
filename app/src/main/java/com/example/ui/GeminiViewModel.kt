package com.example.ui

import androidx.lifecycle.ViewModel
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

class GeminiViewModel : ViewModel() {
    private val repository = GeminiRepository()

    private val _uiState = MutableStateFlow(GeminiUiState())
    val uiState: StateFlow<GeminiUiState> = _uiState.asStateFlow()

    val availableModels = listOf(
        "gemini-1.5-flash",
        "gemini-flash-latest",
        "gemini-2.5-flash"
    )

    val quickPrompts = listOf(
        "Explain how AI works in a few words",
        "اشرح لي كيف يعمل الذكاء الاصطناعي باختصار",
        "اكتب لي كود Kotlin لـ StateFlow في Compose",
        "أهم 3 ممارسات لتقليل استهلاك البطارية في أندرويد",
        "اقترح فكرة تطبيق مميزة بالذكاء الاصطناعي"
    )

    init {
        val effectiveKey = repository.resolveApiKey(null)
        
        _uiState.update {
            it.copy(
                activeApiKey = effectiveKey,
                messages = listOf(
                    ChatMessage(
                        text = "مرحباً يا شريك! أنا مساعدك الذكي Gemini.\nلقد تخطينا العقبات والتطبيق يعمل الآن بامتياز! جرب أن تسألني أي شيء.",
                        isUser = false,
                        modelName = "gemini-1.5-flash"
                    )
                )
            )
        }
    }

    fun onInputTextChanged(newText: String) {
        _uiState.update { it.copy(inputText = newText) }
    }

    fun selectModel(model: String) {
        _uiState.update { it.copy(selectedModel = model) }
    }

    fun setApiKeyDialogOpen(open: Boolean) {
        _uiState.update { it.copy(showApiKeyDialog = open) }
    }

    fun saveCustomApiKey(key: String) {
        val trimmed = key.trim()
        val effectiveKey = repository.resolveApiKey(trimmed)
        _uiState.update {
            it.copy(
                customApiKey = trimmed,
                activeApiKey = effectiveKey,
                showApiKeyDialog = false
            )
        }
    }

    fun clearChat() {
        _uiState.update {
            it.copy(
                messages = listOf(
                    ChatMessage(
                        text = "تم بدء جلسة محادثة جديدة. ما الذي ترغب في استكشافه اليوم؟",
                        isUser = false,
                        modelName = it.selectedModel
                    )
                ),
                errorMessage = null
            )
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun sendMessage(promptText: String? = null) {
        val messageToSend = (promptText ?: _uiState.value.inputText).trim()
        if (messageToSend.isBlank() || _uiState.value.isLoading) return

        val key = repository.resolveApiKey(_uiState.value.customApiKey)
        if (key.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    showApiKeyDialog = true,
                    errorMessage = "يرجى إضافة مفتاح Gemini API للمتابعة."
                )
            }
            return
        }

        val userMessage = ChatMessage(
            text = messageToSend,
            isUser = true
        )
        val currentModel = _uiState.value.selectedModel

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage,
                inputText = if (promptText == null) "" else state.inputText,
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            when (val result = repository.generateContent(messageToSend, key, currentModel)) {
                is Result.Success -> {
                    val aiMessage = ChatMessage(
                        text = result.data,
                        isUser = false,
                        modelName = currentModel
                    )
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages + aiMessage,
                            isLoading = false
                        )
                    }
                }
                is Result.Error -> {
                    val errorUiMessage = ChatMessage(
                        text = "⚠️ ${result.message}",
                        isUser = false,
                        modelName = currentModel,
                        isError = true
                    )
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages + errorUiMessage,
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }
}
