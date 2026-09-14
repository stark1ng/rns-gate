package com.rnsgate.app.data

import com.rnsgate.app.data.model.ChatMessage
import com.rnsgate.app.data.model.Conversation
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction over LXMF messaging.
 *
 * Demo implementation: [com.rnsgate.app.data.demo.DemoLxmfMessenger].
 * TODO(real-rns): wire to LXMF over Reticulum via Chaquopy.
 */
interface LxmfMessenger {
    val conversations: StateFlow<List<Conversation>>
    fun messages(conversationId: String): StateFlow<List<ChatMessage>>

    suspend fun ensureDemoConversation(): Conversation
    suspend fun send(conversationId: String, body: String): ChatMessage
}
