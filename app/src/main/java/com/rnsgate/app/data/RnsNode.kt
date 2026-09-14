package com.rnsgate.app.data

import com.rnsgate.app.data.model.GateSnapshot
import com.rnsgate.app.data.model.IdentityInfo
import com.rnsgate.app.data.model.PeerInfo
import com.rnsgate.app.data.model.TcpEndpoint
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction over a Reticulum node.
 *
 * Preferred: [com.rnsgate.app.data.chaquopy.ChaquopyRnsNode].
 * Fallback: [com.rnsgate.app.data.demo.DemoRnsNode].
 */
interface RnsNode {
    val snapshot: StateFlow<GateSnapshot>
    val peers: StateFlow<List<PeerInfo>>

    suspend fun connect()
    suspend fun disconnect()
    suspend fun regenerateIdentity(): IdentityInfo
    suspend fun setTcpEndpoint(endpoint: TcpEndpoint)
    fun currentIdentity(): IdentityInfo?
}
