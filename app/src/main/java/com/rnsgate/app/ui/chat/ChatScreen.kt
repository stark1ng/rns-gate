package com.rnsgate.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rnsgate.app.R
import com.rnsgate.app.data.model.ChatMessage
import com.rnsgate.app.data.model.Conversation
import com.rnsgate.app.ui.theme.RnsGreen
import com.rnsgate.app.ui.theme.RnsMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: ChatViewModel) {
    val conversations by vm.conversations.collectAsStateWithLifecycle()
    val activeId by vm.activeConversationId.collectAsStateWithLifecycle()
    val messages by vm.messages.collectAsStateWithLifecycle()
    val draft by vm.draft.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        if (activeId == null) {
            TopAppBar(
                title = { Text(stringResource(R.string.chat_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
            if (conversations.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.chat_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = RnsMuted
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = vm::startDemoChat) {
                            Text(stringResource(R.string.chat_new))
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(conversations, key = { it.id }) { conv ->
                        ConversationRow(conv) { vm.openConversation(conv.id) }
                    }
                }
            }
        } else {
            val title = conversations.firstOrNull { it.id == activeId }?.peerName
                ?: stringResource(R.string.chat_demo_peer)
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = vm::closeThread) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.chat_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
            ThreadPane(
                messages = messages,
                draft = draft,
                onDraftChange = vm::onDraftChange,
                onSend = vm::send,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ConversationRow(conv: Conversation, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Text(conv.peerName, style = MaterialTheme.typography.titleMedium)
        Text(
            text = conv.lastPreview.ifEmpty { "—" },
            style = MaterialTheme.typography.bodyMedium,
            color = RnsMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ThreadPane(
    messages: List<ChatMessage>,
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }
    Column(modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                MessageBubble(msg)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.chat_hint)) },
                singleLine = true
            )
            IconButton(onClick = onSend, enabled = draft.isNotBlank()) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.chat_send),
                    tint = if (draft.isNotBlank()) RnsGreen else RnsMuted
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage) {
    val align = if (msg.fromMe) Alignment.CenterEnd else Alignment.CenterStart
    val bg = if (msg.fromMe) RnsGreen.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
    Box(Modifier.fillMaxWidth(), contentAlignment = align) {
        Text(
            text = msg.body,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bg)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
