package com.rnsgate.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rnsgate.app.data.model.TcpEndpoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rns_gate_prefs")

class SettingsStore(private val context: Context) {

    private val displayNameKey = stringPreferencesKey("display_name")
    private val tcpHostKey = stringPreferencesKey("tcp_host")
    private val tcpPortKey = intPreferencesKey("tcp_port")

    val displayName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[displayNameKey] ?: "Operator"
    }

    val tcpEndpoint: Flow<TcpEndpoint> = context.dataStore.data.map { prefs ->
        TcpEndpoint(
            host = prefs[tcpHostKey] ?: TcpEndpoint.Default.host,
            port = prefs[tcpPortKey] ?: TcpEndpoint.Default.port
        )
    }

    suspend fun setDisplayName(name: String) {
        context.dataStore.edit { it[displayNameKey] = name.trim().ifEmpty { "Operator" } }
    }

    suspend fun setTcpEndpoint(endpoint: TcpEndpoint) {
        context.dataStore.edit {
            it[tcpHostKey] = endpoint.host.trim().ifEmpty { TcpEndpoint.Default.host }
            it[tcpPortKey] = endpoint.port.coerceIn(1, 65535)
        }
    }
}
