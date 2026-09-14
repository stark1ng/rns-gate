package com.rnsgate.app

import android.app.Application
import android.util.Log
import com.rnsgate.app.data.LxmfMessenger
import com.rnsgate.app.data.RnsNode
import com.rnsgate.app.data.SettingsStore
import com.rnsgate.app.data.chaquopy.ChaquopyRnsNode
import com.rnsgate.app.data.demo.DemoLxmfMessenger
import com.rnsgate.app.data.demo.DemoRnsNode
import com.rnsgate.app.data.model.BackendMode
import com.rnsgate.app.data.model.GateSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class RnsGateApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var settingsStore: SettingsStore
        private set
    lateinit var rnsNode: RnsNode
        private set
    lateinit var messenger: LxmfMessenger
        private set

    /** True when Connect is backed by real RNS; false when Demo fallback is active. */
    @Volatile
    var usingRealRns: Boolean = false
        private set

    /** Set when falling back to demo so Gate can show "demo fallback: …". */
    var fallbackReason: String? = null
        private set

    /** Chat remains demo until LXMF is wired; always true for now. */
    val chatIsDemo: Boolean = true

    override fun onCreate() {
        super.onCreate()
        settingsStore = SettingsStore(this)
        messenger = DemoLxmfMessenger(appScope)

        val chaquopy = ChaquopyRnsNode(this, appScope, settingsStore)
        val initError = try {
            runBlocking(Dispatchers.IO) { chaquopy.initializePython() }
        } catch (t: Throwable) {
            t.message ?: t.javaClass.simpleName
        }

        if (initError == null && chaquopy.isPythonReady) {
            rnsNode = chaquopy
            usingRealRns = true
            fallbackReason = null
            Log.i(TAG, "Using ChaquopyRnsNode (real RNS)")
        } else {
            val reason = initError ?: "Python/RNS not ready"
            fallbackReason = reason
            rnsNode = DemoRnsNodeWithFallback(
                inner = DemoRnsNode(appScope, settingsStore),
                reason = reason,
                scope = appScope
            )
            usingRealRns = false
            Log.w(TAG, "Falling back to DemoRnsNode: $reason")
        }
    }

    companion object {
        private const val TAG = "RnsGateApp"
    }
}

/**
 * Wraps [DemoRnsNode] and keeps [GateSnapshot.statusMessage] / [BackendMode.Demo] visible.
 */
private class DemoRnsNodeWithFallback(
    private val inner: DemoRnsNode,
    private val reason: String,
    scope: CoroutineScope
) : RnsNode by inner {

    private val _snapshot = MutableStateFlow(
        inner.snapshot.value.copy(
            backendMode = BackendMode.Demo,
            statusMessage = "demo fallback: $reason"
        )
    )
    override val snapshot: StateFlow<GateSnapshot> = _snapshot.asStateFlow()

    init {
        scope.launch {
            inner.snapshot.collect { snap ->
                _snapshot.value = snap.copy(
                    backendMode = BackendMode.Demo,
                    statusMessage = snap.statusMessage ?: "demo fallback: $reason"
                )
            }
        }
    }
}

fun Application.asRnsGate(): RnsGateApp = this as RnsGateApp
