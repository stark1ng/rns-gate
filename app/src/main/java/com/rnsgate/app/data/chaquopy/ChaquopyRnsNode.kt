package com.rnsgate.app.data.chaquopy

import android.content.Context
import android.util.Log
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
import org.json.JSONObject
import java.io.File

/**
 * Real Reticulum node driven through Chaquopy → rns_bridge Python module.
 *
 * Bridge functions return JSON strings (json.dumps). We parse with JSONObject —
 * do not cast PyObject to Map or use PyObject.get for dict keys (attribute/get
 * confusion caused false demo fallback when ok was True).
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
            val initRes = bridgeJson(mod, "init_storage", storage)
            if (!initRes.optBoolean("ok", false)) {
                return@withContext initRes.failureMessage("init_storage")
            }
            val ping = bridgeJson(mod, "ping")
            if (!ping.optBoolean("ok", false)) {
                return@withContext ping.failureMessage("RNS ping")
            }
            bridge = mod
            isPythonReady = true
            val ver = ping.jsonStr("rns_version").orEmpty()
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
                bridgeJson(mod, "start", ep.host, ep.port, name)
            }
            if (!startRes.optBoolean("ok", false)) {
                val err = startRes.jsonStr("error") ?: "RNS start failed"
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

            val hash = startRes.jsonStr("identity_hash")
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
                            up = startRes.optBoolean("interface_up", false),
                            detail = startRes.jsonStr("interface_detail")
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
                runCatching { bridgeJson(mod, "probe_announce", name) }
            }
            delay(300)

            val statusMsg = when {
                startRes.optBoolean("announce_ok", false) -> "RNS online"
                else -> startRes.jsonStr("last_error") ?: "RNS online"
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
            runCatching { if (mod != null) bridgeJson(mod, "stop") }
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
            bridgeJson(mod, "regenerate_identity", displayName)
        }
        val hash = res.jsonStr("identity_hash") ?: "error"
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
                    runCatching { bridgeJson(mod, "status") }.getOrNull()
                } ?: continue
                if (!st.optBoolean("ok", false)) continue

                val hash = st.jsonStr("identity_hash")
                val up = st.optBoolean("interface_up", false)
                val detail = st.jsonStr("interface_detail").orEmpty()
                val uptime = st.optLong("uptime_ms", 0L)
                val peersEst = st.optInt("peer_estimate", 0)

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
        private const val TAG = "ChaquopyRnsNode"

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

/**
 * Call a rns_bridge function that returns a JSON string and parse it.
 * Avoids fragile PyObject-as-Map / PyObject.get(dict key) paths.
 */
private fun bridgeJson(mod: PyObject, attr: String, vararg args: Any?): JSONObject {
    val raw = mod.callAttr(attr, *args)
    val text = raw?.toString() ?: return JSONObject().put("ok", false).put("error", "$attr returned null")
    return try {
        JSONObject(text)
    } catch (e: Exception) {
        Log.e("ChaquopyRnsNode", "$attr: not valid JSON: $text", e)
        JSONObject()
            .put("ok", false)
            .put("error", "$attr: invalid JSON (${e.message}): ${text.take(200)}")
    }
}

/** JSON null → Kotlin null; blank → null. */
private fun JSONObject.jsonStr(key: String): String? {
    if (!has(key) || isNull(key)) return null
    val s = optString(key, "")
    return s.takeIf { it.isNotBlank() && it != "null" }
}

private fun JSONObject.failureMessage(label: String): String {
    val err = jsonStr("error")
    if (!err.isNullOrBlank()) return err
    val dump = toString()
    Log.e("ChaquopyRnsNode", "$label failed without error field: $dump")
    return "$label failed ($dump)"
}
