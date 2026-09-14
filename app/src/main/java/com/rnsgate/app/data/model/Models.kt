package com.rnsgate.app.data.model

/** High-level connection lifecycle for the Gate screen. */
enum class ConnectionState {
    Offline,
    Connecting,
    Online
}

/** Steps shown in the connect infographic. */
enum class ConnectStep {
    Idle,
    Identity,
    Interfaces,
    PathAnnounce,
    Ready
}

enum class InterfaceKind {
    Tcp,
    Auto,
    RNode
}

data class InterfaceStatus(
    val kind: InterfaceKind,
    val enabled: Boolean,
    val up: Boolean,
    val detail: String = "",
    val isPlaceholder: Boolean = false
)

data class IdentityInfo(
    val hashHex: String,
    val displayName: String
)

data class PeerInfo(
    val hashHex: String,
    val displayName: String,
    val lastSeenEpochMs: Long
)

data class Conversation(
    val id: String,
    val peerHash: String,
    val peerName: String,
    val lastPreview: String,
    val updatedAtMs: Long
)

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val body: String,
    val fromMe: Boolean,
    val timestampMs: Long,
    val delivered: Boolean = true
)

data class TcpEndpoint(
    val host: String,
    val port: Int
) {
    companion object {
        val Default = TcpEndpoint("127.0.0.1", 4242)
    }
}

data class GateSnapshot(
    val connectionState: ConnectionState = ConnectionState.Offline,
    val step: ConnectStep = ConnectStep.Idle,
    val identity: IdentityInfo? = null,
    val interfaces: List<InterfaceStatus> = emptyList(),
    val uptimeMs: Long = 0L,
    val tcpEndpoint: TcpEndpoint = TcpEndpoint.Default
)
