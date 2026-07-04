package com.oalevel.resources.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.oalevel.resources.data.repository.AiRepository
import java.util.UUID
import javax.inject.Inject

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String, // "user" | "assistant"
    val content: String,
    val attachmentName: String? = null,
    val attachmentType: String? = null, // "image" | "file"
    val imagePreviewUri: Uri? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class AiAttachment(
    val type: String,            // "image" | "file"
    val displayName: String,
    val imageBase64: String? = null,
    val pdfText: String? = null,
    val previewUri: Uri? = null
)

data class AiChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val error: String? = null,
    val sessionId: String = UUID.randomUUID().toString(),
    val attachment: AiAttachment? = null
)

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun onImageAttached(base64: String, fileName: String, previewUri: Uri) {
        _uiState.update {
            it.copy(
                attachment = AiAttachment(
                    type = "image",
                    displayName = fileName,
                    imageBase64 = base64,
                    previewUri = previewUri
                )
            )
        }
    }

    fun onFileAttached(fileName: String) {
        _uiState.update {
            it.copy(
                attachment = AiAttachment(
                    type = "file",
                    displayName = fileName,
                    pdfText = "[User attached file: $fileName. Please help with questions about this document.]"
                )
            )
        }
    }

    fun clearAttachment() {
        _uiState.update { it.copy(attachment = null) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        val attachment = _uiState.value.attachment
        if ((text.isBlank() && attachment == null) || _uiState.value.isSending) return

        val displayText = text.ifBlank {
            when (attachment?.type) {
                "image" -> "What's in this image?"
                else -> "Tell me about this document."
            }
        }

        val userMsg = ChatMessage(
            role = "user",
            content = displayText,
            attachmentName = attachment?.displayName,
            attachmentType = attachment?.type,
            imagePreviewUri = attachment?.previewUri
        )

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMsg,
                inputText = "",
                attachment = null,
                isSending = true,
                error = null
            )
        }

        viewModelScope.launch {
            val result = aiRepository.sendMessage(
                message = displayText,
                sessionId = _uiState.value.sessionId,
                imageBase64 = attachment?.imageBase64,
                pdfText = attachment?.pdfText
            )
            result.onSuccess { reply ->
                val assistantMsg = ChatMessage(role = "assistant", content = reply.reply)
                _uiState.update { state ->
                    state.copy(messages = state.messages + assistantMsg, isSending = false)
                }
            }.onFailure { e ->
                _uiState.update { state ->
                    state.copy(isSending = false, error = e.message)
                }
            }
        }
    }

    fun clearSession() {
        viewModelScope.launch {
            aiRepository.clearSession(_uiState.value.sessionId)
            _uiState.update { AiChatUiState(sessionId = UUID.randomUUID().toString()) }
        }
    }
}
