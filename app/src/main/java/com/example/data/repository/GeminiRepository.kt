package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.GeminiApiService
import com.example.data.api.NetworkClient
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : Result<Nothing>()
}

class GeminiRepository(
    private val apiService: GeminiApiService = NetworkClient.geminiService
) {
    fun resolveApiKey(customKey: String?): String? {
        val trimmed = customKey?.trim()
        if (!trimmed.isNullOrEmpty()) return trimmed
        val buildKey = runCatching { BuildConfig.GEMINI_API_KEY }.getOrNull()?.trim()
        if (!buildKey.isNullOrEmpty() && buildKey != "MY_GEMINI_API_KEY") return buildKey
        return null
    }

    suspend fun generateContent(
        prompt: String,
        apiKey: String,
        modelName: String = "gemini-1.5-flash"
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.Error("يرجى إدخال مفتاح Gemini API في إعدادات التطبيق.")
        }

        val request = GeminiRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)), role = "user")
            ),
            generationConfig = GenerationConfig(temperature = 0.7f, maxOutputTokens = 2048),
            systemInstruction = Content(
                parts = listOf(Part(text = "You are Gemini, an intelligent and friendly AI assistant. Answer clearly in Arabic when asked in Arabic."))
            )
        )

        try {
            val response = apiService.generateContent(modelName, apiKey, request)
            if (response.isSuccessful) {
                val text = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank()) {
                    Result.Success(text.trim())
                } else {
                    Result.Error("لم يتم استلام أي نص من النموذج.")
                }
            } else {
                val code = response.code()
                val msg = when (code) {
                    400 -> "مفتاح API غير صالح أو الطلب غير صحيح."
                    429 -> "تم تجاوز حد الاستخدام المؤقت (Rate Limit)."
                    else -> "فشل الطلب مع رمز الخطأ: $code"
                }
                Result.Error(msg)
            }
        } catch (e: IOException) {
            Result.Error("تعذر الاتصال بالإنترنت. تأكد من اتصال هاتفك بالشبكة.", e)
        } catch (e: Exception) {
            Result.Error("حدث خطأ غير متوقع: ${e.localizedMessage ?: e.message}", e)
        }
    }
}
