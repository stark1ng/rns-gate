package com.rnsgate.app.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rnsgate.app.data.RnsNode
import com.rnsgate.app.data.model.GateSnapshot
import com.rnsgate.app.data.model.IdentityInfo
import com.rnsgate.app.data.model.PeerInfo
import com.rnsgate.app.data.model.TcpEndpoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ToolsViewModel(private val rnsNode: RnsNode) : ViewModel() {

    val snapshot: StateFlow<GateSnapshot> = rnsNode.snapshot.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), rnsNode.snapshot.value
    )
    val peers: StateFlow<List<PeerInfo>> = rnsNode.peers.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    private val _shownIdentity = MutableStateFlow<IdentityInfo?>(null)
    val shownIdentity: StateFlow<IdentityInfo?> = _shownIdentity

    private val _host = MutableStateFlow(rnsNode.snapshot.value.tcpEndpoint.host)
    val host: StateFlow<String> = _host

    private val _port = MutableStateFlow(rnsNode.snapshot.value.tcpEndpoint.port.toString())
    val port: StateFlow<String> = _port

    private val _savedToast = MutableStateFlow(false)
    val savedToast: StateFlow<Boolean> = _savedToast

    init {
        viewModelScope.launch {
            rnsNode.snapshot.collect { snap ->
                if (_host.value != snap.tcpEndpoint.host && !_editingHost) {
                    _host.value = snap.tcpEndpoint.host
                }
                if (_port.value != snap.tcpEndpoint.port.toString() && !_editingPort) {
                    _port.value = snap.tcpEndpoint.port.toString()
                }
            }
        }
    }

    private var _editingHost = false
    private var _editingPort = false

    fun showIdentity() {
        _shownIdentity.value = rnsNode.currentIdentity()
    }

    fun regenerateIdentity() {
        viewModelScope.launch {
            _shownIdentity.value = rnsNode.regenerateIdentity()
        }
    }

    fun onHostChange(v: String) {
        _editingHost = true
        _host.value = v
    }

    fun onPortChange(v: String) {
        _editingPort = true
        _port.value = v.filter { it.isDigit() }.take(5)
    }

    fun saveTcp() {
        viewModelScope.launch {
            val portInt = _port.value.toIntOrNull() ?: TcpEndpoint.Default.port
            rnsNode.setTcpEndpoint(TcpEndpoint(_host.value, portInt))
            _editingHost = false
            _editingPort = false
            _savedToast.value = true
        }
    }

    fun clearSavedToast() {
        _savedToast.value = false
    }

    companion object {
        fun factory(rnsNode: RnsNode) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ToolsViewModel(rnsNode) as T
            }
        }
    }
}
