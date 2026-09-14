package com.rnsgate.app.data.demo

import com.rnsgate.app.data.LxmfMessenger
import com.rnsgate.app.data.model.ChatMessage
import com.rnsgate.app.data.model.Conversation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory LXMF chat simulator.
 *
 * TODO(real-rns): Send/receive real LXMF messages via Reticulum destinations.
 */
class DemoLxmfMessenger(
    private val scope: CoroutineScope
) : LxmfMessenger {

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    override val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val messageFlows = ConcurrentHashMap<String, MutableStateFlow<List<ChatMessage>>>()

    private val autoReplies = listOf(
        "Copy that — mesh link looks good.",
        "Ack. Standing by on LXMF.",
        "Received. Demo peer online.",
        "Roger. Path is stable (demo)."
    )

    override fun messages(conversationId: String): StateFlow<List<ChatMessage>> {
        return messageFlows.getOrPut(conversationId) {
            MutableStateFlow(emptyList())
        }.asStateFlow()
    }

    override suspend fun ensureDemoConversation(): Conversation {
        val existing = _conversations.value.firstOrNull()
        if (existing != null) return existing
        val id = UUID.randomUUID().toString()
        val conv = Conversation(
            id = id,
            peerHash = "a1b2c3d4e5f60718",
            peerName = "Demo Peer",
            lastPreview = "",
            updatedAtMs = System.currentTimeMillis()
        )
        messageFlows[id] = MutableStateFlow(emptyList())
        _conversations.value = listOf(conv)
        return conv
    }

    override suspend fun send(conversationId: String, body: String): ChatMessage {
        val text = body.trim()
        require(text.isNotEmpty()) { "empty message" }
        ensureConversationExists(conversationId)
        val flow = messageFlows.getOrPut(conversationId) { MutableStateFlow(emptyList()) }
        val msg = ChatMessage(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            body = text,
            fromMe = true,
            timestampMs = System.currentTimeMillis()
        )
        flow.update { it + msg }
        touchConversation(conversationId, text)
        scope.launch {
            delay(900)
            val reply = ChatMessage(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                body = autoReplies.random(),
                fromMe = false,
                timestampMs = System.currentTimeMillis()
            )
            flow.update { it + reply }
            touchConversation(conversationId, reply.body)
        }
        return msg
    }

    private fun ensureConversationExists(conversationId: String) {
        if (_conversations.value.none { it.id == conversationId }) {
            val conv = Conversation(
                id = conversationId,
                peerHash = "unknown",
                peerName = "Peer",
                lastPreview = "",
                updatedAtMs = System.currentTimeMillis()
            )
            _conversations.update { it + conv }
        }
    }

    private fun touchConversation(conversationId: String, preview: String) {
        _conversations.update { list ->
            list.map { c ->
                if (c.id == conversationId) {
                    c.copy(lastPreview = preview, updatedAtMs = System.currentTimeMillis())
                } else c
            }.sortedByDescending { it.updatedAtMs }
        }
    }
}
