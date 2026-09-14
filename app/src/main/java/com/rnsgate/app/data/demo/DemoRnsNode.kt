package com.rnsgate.app.data.demo

import com.rnsgate.app.data.RnsNode
import com.rnsgate.app.data.SettingsStore
import com.rnsgate.app.data.model.ConnectStep
import com.rnsgate.app.data.model.ConnectionState
import com.rnsgate.app.data.model.GateSnapshot
import com.rnsgate.app.data.model.IdentityInfo
import com.rnsgate.app.data.model.InterfaceKind
import com.rnsgate.app.data.model.InterfaceStatus
import com.rnsgate.app.data.model.PeerInfo
import com.rnsgate.app.data.model.TcpEndpoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.security.SecureRandom

/**
 * In-memory simulated Reticulum node for the MVP.
 *
 * TODO(real-rns): Replace with Chaquopy + `rns` Python package:
 *  - start RNS.Reticulum from Application scope
 *  - map Identity / Interfaces / Transport announce into [GateSnapshot]
 *  - expose real destination hashes and link status
 */
class DemoRnsNode(
    private val scope: CoroutineScope,
    private val settingsStore: SettingsStore
) : RnsNode {

    private val _snapshot = MutableStateFlow(GateSnapshot(interfaces = defaultInterfaces(false)))
    override val snapshot: StateFlow<GateSnapshot> = _snapshot.asStateFlow()

    private val _peers = MutableStateFlow<List<PeerInfo>>(emptyList())
    override val peers: StateFlow<List<PeerInfo>> = _peers.asStateFlow()

    private var identity: IdentityInfo = generateIdentity("Operator")
    private var connectJob: Job? = null
    private var uptimeJob: Job? = null
    private var onlineSinceMs: Long = 0L

    init {
        scope.launch {
            settingsStore.displayName.collect { name ->
                identity = identity.copy(displayName = name)
                if (_snapshot.value.connectionState == ConnectionState.Online) {
                    _snapshot.update { it.copy(identity = identity) }
                }
            }
        }
        scope.launch {
            settingsStore.tcpEndpoint.collect { ep ->
                _snapshot.update { it.copy(tcpEndpoint = ep) }
            }
        }
    }

    override fun currentIdentity(): IdentityInfo? = identity

    override suspend fun connect() {
        if (_snapshot.value.connectionState != ConnectionState.Offline) return
        connectJob?.cancel()
        connectJob = scope.launch {
            _snapshot.update {
                it.copy(
                    connectionState = ConnectionState.Connecting,
                    step = ConnectStep.Identity,
                    identity = null,
                    uptimeMs = 0L,
                    interfaces = defaultInterfaces(false)
                )
            }
            delay(700)
            _snapshot.update {
                it.copy(step = ConnectStep.Interfaces, identity = identity)
            }
            delay(700)
            val ep = _snapshot.value.tcpEndpoint
            _snapshot.update {
                it.copy(
                    step = ConnectStep.PathAnnounce,
                    interfaces = listOf(
                        InterfaceStatus(InterfaceKind.Tcp, enabled = true, up = true, detail = "${ep.host}:${ep.port}"),
                        InterfaceStatus(InterfaceKind.Auto, enabled = true, up = true, detail = "auto-discovery"),
                        InterfaceStatus(InterfaceKind.RNode, enabled = false, up = false, detail = "placeholder", isPlaceholder = true)
                    )
                )
            }
            delay(800)
            onlineSinceMs = System.currentTimeMillis()
            _snapshot.update {
                it.copy(
                    connectionState = ConnectionState.Online,
                    step = ConnectStep.Ready,
                    identity = identity,
                    uptimeMs = 0L
                )
            }
            _peers.value = listOf(
                PeerInfo(
                    hashHex = "a1b2c3d4e5f60718",
                    displayName = "Demo Peer",
                    lastSeenEpochMs = System.currentTimeMillis()
                ),
                PeerInfo(
                    hashHex = "f00dcafe12345678",
                    displayName = "Mesh Echo",
                    lastSeenEpochMs = System.currentTimeMillis() - 60_000
                )
            )
            startUptimeTicker()
        }
    }

    override suspend fun disconnect() {
        connectJob?.cancel()
        connectJob = null
        uptimeJob?.cancel()
        uptimeJob = null
        _peers.value = emptyList()
        _snapshot.update {
            it.copy(
                connectionState = ConnectionState.Offline,
                step = ConnectStep.Idle,
                identity = null,
                uptimeMs = 0L,
                interfaces = defaultInterfaces(false)
            )
        }
    }

    override suspend fun regenerateIdentity(): IdentityInfo {
        identity = generateIdentity(identity.displayName)
        if (_snapshot.value.connectionState == ConnectionState.Online) {
            _snapshot.update { it.copy(identity = identity) }
        }
        return identity
    }

    override suspend fun setTcpEndpoint(endpoint: TcpEndpoint) {
        settingsStore.setTcpEndpoint(endpoint)
        _snapshot.update { snap ->
            val ifaces = snap.interfaces.map { iface ->
                if (iface.kind == InterfaceKind.Tcp && iface.up) {
                    iface.copy(detail = "${endpoint.host}:${endpoint.port}")
                } else iface
            }
            snap.copy(tcpEndpoint = endpoint, interfaces = ifaces)
        }
    }

    private fun startUptimeTicker() {
        uptimeJob?.cancel()
        uptimeJob = scope.launch {
            while (isActive) {
                delay(1000)
                val up = System.currentTimeMillis() - onlineSinceMs
                _snapshot.update { it.copy(uptimeMs = up) }
            }
        }
    }

    companion object {
        private fun defaultInterfaces(up: Boolean) = listOf(
            InterfaceStatus(InterfaceKind.Tcp, enabled = true, up = up, detail = ""),
            InterfaceStatus(InterfaceKind.Auto, enabled = true, up = up, detail = ""),
            InterfaceStatus(InterfaceKind.RNode, enabled = false, up = false, detail = "placeholder", isPlaceholder = true)
        )

        private fun generateIdentity(displayName: String): IdentityInfo {
            val bytes = ByteArray(8)
            SecureRandom().nextBytes(bytes)
            val hex = bytes.joinToString("") { b -> "%02x".format(b) }
            return IdentityInfo(hashHex = hex, displayName = displayName)
        }
    }
}
