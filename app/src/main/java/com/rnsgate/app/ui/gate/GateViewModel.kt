package com.rnsgate.app.ui.gate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rnsgate.app.data.RnsNode
import com.rnsgate.app.data.model.ConnectionState
import com.rnsgate.app.data.model.GateSnapshot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GateViewModel(private val rnsNode: RnsNode) : ViewModel() {

    val snapshot: StateFlow<GateSnapshot> = rnsNode.snapshot.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        rnsNode.snapshot.value
    )

    fun toggleConnect() {
        viewModelScope.launch {
            when (snapshot.value.connectionState) {
                ConnectionState.Offline -> rnsNode.connect()
                ConnectionState.Online -> rnsNode.disconnect()
                ConnectionState.Connecting -> { /* ignore */ }
            }
        }
    }

    companion object {
        fun factory(rnsNode: RnsNode) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return GateViewModel(rnsNode) as T
            }
        }
    }
}
