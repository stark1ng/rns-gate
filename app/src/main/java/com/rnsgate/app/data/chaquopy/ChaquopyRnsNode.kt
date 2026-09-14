package com.rnsgate.app.data.chaquopy

import android.content.Context
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.rnsgate.app.data.RnsNode
import com.rnsgate.app.data.SettingsStore
import com.rnsgate.app.data.model.BackendMode
import com.rnsgate.app.data.model.ConnectStep
import com.rnsgate.app.data.model.ConnectionState
import com.rnsgate.app.data.model.GateSnapshot
import com.rnsgate.app.data.model.IdentityInfo
import com.rnsgate.app.data.model.InterfaceKind
import com.rnsgate.app.data.model.InterfaceStatus
import com.rnsgate.app.data.model.PeerInfo
import com.rnsgate.app.data.model.TcpEndpoint
import com.rnsgate.app.service.RnsNodeService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Real Reticulum node driven through Chaquopy → rns_bridge Python module.
 */
class ChaquopyRnsNode(
    private val appContext: Context,
    private val scope: CoroutineScope,
    private val settingsStore: SettingsStore
) : RnsNode {

    private val _snapshot = MutableStateFlow(
        GateSnapshot(
            interfaces = defaultInterfaces(false),
            backendMode = BackendMode.RealRns
        )
    )
    override val snapshot: StateFlow<GateSnapshot> = _snapshot.asStateFlow()

    private val _peers = MutableStateFlow<List<PeerInfo>>(emptyList())
    override val peers: StateFlow<List<PeerInfo>> = _peers.asStateFlow()

    private var bridge: PyObject? = null
    private var connectJob: Job? = null
    private var pollJob: Job? = null
    private var displayName: String = "Operator"
    private var lastKnownIdentity: IdentityInfo? = null

    @Volatile
    var isPythonReady: Boolean = false
        private set

    init {
        scope.launch {
            settingsStore.displayName.collect { name ->
                displayName = name
                lastKnownIdentity = lastKnownIdentity?.copy(displayName = name)
                val id = _snapshot.value.identity
                if (id != null) {
                    _snapshot.update { it.copy(identity = id.copy(displayName = name)) }
                }
            }
        }
        scope.launch {
            settingsStore.tcpEndpoint.collect { ep ->
                _snapshot.update { it.copy(tcpEndpoint = ep) }
            }
        }
    }

    /** Start Python + import RNS. Returns null on success, or an error string. */
    suspend fun initializePython(): String? = withContext(Dispatchers.IO) {
        try {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(appContext))
            }
            val py = Python.getInstance()
            val mod = py.getModule("rns_bridge")
            val storage = File(appContext.filesDir, "rns").absolutePath
            val initRes = mod.callAttr("init_storage", storage)
            if (!initRes.pyBool("ok")) {
                return@withContext initRes.pyStr("error") ?: "init_storage failed"
            }
            val ping = mod.callAttr("ping")
            if (!ping.pyBool("ok")) {
                return@withContext ping.pyStr("error") ?: "RNS ping failed"
            }
            bridge = mod
            isPythonReady = true
            val ver = ping.pyStr("rns_version").orEmpty()
            _snapshot.update {
                it.copy(
                    backendMode = BackendMode.RealRns,
                    statusMessage = if (ver.isNotBlank()) "RNS $ver" else "RNS ready"
                )
            }
            null
        } catch (t: Throwable) {
            isPythonReady = false
            bridge = null
            t.message ?: t.javaClass.simpleName
        }
    }

    override fun currentIdentity(): IdentityInfo? =
        _snapshot.value.identity ?: lastKnownIdentity

    override suspend fun connect() {
        if (_snapshot.value.connectionState != ConnectionState.Offline) return
        val mod = bridge
        if (mod == null) {
            _snapshot.update { it.copy(statusMessage = "Python bridge not ready") }
            return
        }
        connectJob?.cancel()
        connectJob = scope.launch {
            _snapshot.update {
                it.copy(
                    connectionState = ConnectionState.Connecting,
                    step = ConnectStep.Identity,
                    identity = null,
                    uptimeMs = 0L,
                    interfaces = defaultInterfaces(false),
                    backendMode = BackendMode.RealRns,
                    statusMessage = null
                )
            }

            val ep = settingsStore.tcpEndpoint.first()
            val name = settingsStore.displayName.first()
            displayName = name

            delay(200)
            _snapshot.update { it.copy(step = ConnectStep.Interfaces) }

            val startRes = withContext(Dispatchers.IO) {
                mod.callAttr("start", ep.host, ep.port, name)
            }
            if (!startRes.pyBool("ok")) {
                val err = startRes.pyStr("error") ?: "RNS start failed"
                _snapshot.update {
                    it.copy(
                        connectionState = ConnectionState.Offline,
                        step = ConnectStep.Idle,
                        statusMessage = err,
                        interfaces = defaultInterfaces(false)
                    )
                }
                return@launch
            }

            val hash = startRes.pyStr("identity_hash")
            val identity = if (!hash.isNullOrBlank()) {
                IdentityInfo(hashHex = hash, displayName = name)
            } else null
            lastKnownIdentity = identity

            _snapshot.update {
                it.copy(
                    step = ConnectStep.PathAnnounce,
                    identity = identity,
                    tcpEndpoint = ep,
                    interfaces = listOf(
                        InterfaceStatus(
                            InterfaceKind.Tcp,
                            enabled = true,
                            up = startRes.pyBool("interface_up"),
                            detail = startRes.pyStr("interface_detail")
                                ?: "${ep.host}:${ep.port}"
                        ),
                        InterfaceStatus(
                            InterfaceKind.Auto,
                            enabled = false,
                            up = false,
                            detail = "disabled on mobile"
                        ),
                        InterfaceStatus(
                            InterfaceKind.RNode,
                            enabled = false,
                            up = false,
                            detail = "placeholder",
                            isPlaceholder = true
                        )
                    )
                )
            }

            withContext(Dispatchers.IO) {
                runCatching { mod.callAttr("probe_announce", name) }
            }
            delay(300)

            val statusMsg = when {
                startRes.pyBool("announce_ok") -> "RNS online"
                else -> startRes.pyStr("last_error") ?: "RNS online"
            }
            _snapshot.update {
                it.copy(
                    connectionState = ConnectionState.Online,
                    step = ConnectStep.Ready,
                    identity = identity,
                    statusMessage = statusMsg
                )
            }

            RnsNodeService.start(appContext)
            startPolling(mod)
        }
    }

    override suspend fun disconnect() {
        connectJob?.cancel()
        connectJob = null
        pollJob?.cancel()
        pollJob = null
        val mod = bridge
        withContext(Dispatchers.IO) {
            runCatching { mod?.callAttr("stop") }
        }
        RnsNodeService.stop(appContext)
        _peers.value = emptyList()
        _snapshot.update {
            it.copy(
                connectionState = ConnectionState.Offline,
                step = ConnectStep.Idle,
                identity = lastKnownIdentity,
                uptimeMs = 0L,
                interfaces = defaultInterfaces(false),
                statusMessage = null
            )
        }
    }

    override suspend fun regenerateIdentity(): IdentityInfo {
        val mod = bridge ?: return IdentityInfo("unavailable", displayName)
        if (_snapshot.value.connectionState != ConnectionState.Offline) {
            disconnect()
        }
        val res = withContext(Dispatchers.IO) {
            mod.callAttr("regenerate_identity", displayName)
        }
        val hash = res.pyStr("identity_hash") ?: "error"
        val info = IdentityInfo(hashHex = hash, displayName = displayName)
        lastKnownIdentity = info
        _snapshot.update { it.copy(identity = info) }
        return info
    }

    override suspend fun setTcpEndpoint(endpoint: TcpEndpoint) {
        settingsStore.setTcpEndpoint(endpoint)
        _snapshot.update { snap ->
            val ifaces = snap.interfaces.map { iface ->
                if (iface.kind == InterfaceKind.Tcp) {
                    iface.copy(detail = "${endpoint.host}:${endpoint.port}")
                } else iface
            }
            snap.copy(tcpEndpoint = endpoint, interfaces = ifaces)
        }
    }

    private fun startPolling(mod: PyObject) {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive) {
                delay(1500)
                val st = withContext(Dispatchers.IO) {
                    runCatching { mod.callAttr("status") }.getOrNull()
                } ?: continue
                if (!st.pyBool("ok")) continue

                val hash = st.pyStr("identity_hash")
                val up = st.pyBool("interface_up")
                val detail = st.pyStr("interface_detail").orEmpty()
                val uptime = st.pyLong("uptime_ms")
                val peersEst = st.pyInt("peer_estimate")

                if (!hash.isNullOrBlank()) {
                    lastKnownIdentity = IdentityInfo(hash, displayName)
                }

                _snapshot.update { snap ->
                    snap.copy(
                        identity = lastKnownIdentity ?: snap.identity,
                        uptimeMs = uptime,
                        interfaces = snap.interfaces.map { iface ->
                            when (iface.kind) {
                                InterfaceKind.Tcp ->
                                    iface.copy(up = up, detail = detail.ifEmpty { iface.detail })
                                else -> iface
                            }
                        }
                    )
                }
                _peers.value = if (peersEst > 0) {
                    List(peersEst.coerceAtMost(8)) { i ->
                        PeerInfo(
                            hashHex = "path-$i",
                            displayName = "Known path #$i",
                            lastSeenEpochMs = System.currentTimeMillis()
                        )
                    }
                } else {
                    emptyList()
                }
            }
        }
    }

    companion object {
        private fun defaultInterfaces(up: Boolean) = listOf(
            InterfaceStatus(InterfaceKind.Tcp, enabled = true, up = up, detail = ""),
            InterfaceStatus(InterfaceKind.Auto, enabled = false, up = false, detail = ""),
            InterfaceStatus(
                InterfaceKind.RNode,
                enabled = false,
                up = false,
                detail = "placeholder",
                isPlaceholder = true
            )
        )
    }
}

private fun PyObject.pyStr(key: String): String? {
    val v = this.get(key) ?: return null
    val s = v.toString()
    return if (s == "None" || s.isBlank()) null else s
}

private fun PyObject.pyBool(key: String): Boolean {
    val v = this.get(key) ?: return false
    return when (v.toString().lowercase()) {
        "true", "1", "yes" -> true
        else -> false
    }
}

private fun PyObject.pyLong(key: String): Long {
    val v = this.get(key) ?: return 0L
    return v.toString().toLongOrNull() ?: 0L
}

private fun PyObject.pyInt(key: String): Int {
    val v = this.get(key) ?: return 0
    return v.toString().toDoubleOrNull()?.toInt() ?: 0
}
