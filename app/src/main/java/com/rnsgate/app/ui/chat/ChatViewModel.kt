package com.rnsgate.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rnsgate.app.data.LxmfMessenger
import com.rnsgate.app.data.model.ChatMessage
import com.rnsgate.app.data.model.Conversation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(private val messenger: LxmfMessenger) : ViewModel() {

    val conversations: StateFlow<List<Conversation>> = messenger.conversations.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    private val _activeId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeId

    val messages: StateFlow<List<ChatMessage>> = _activeId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else messenger.messages(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _draft = MutableStateFlow("")
    val draft: StateFlow<String> = _draft

    fun openConversation(id: String) {
        _activeId.value = id
    }

    fun closeThread() {
        _activeId.value = null
    }

    fun onDraftChange(value: String) {
        _draft.value = value
    }

    fun startDemoChat() {
        viewModelScope.launch {
            val conv = messenger.ensureDemoConversation()
            _activeId.value = conv.id
        }
    }

    fun send() {
        val id = _activeId.value ?: return
        val body = _draft.value
        if (body.isBlank()) return
        viewModelScope.launch {
            messenger.send(id, body)
            _draft.value = ""
        }
    }

    companion object {
        fun factory(messenger: LxmfMessenger) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChatViewModel(messenger) as T
            }
        }
    }
}
