package com.rnsgate.app.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rnsgate.app.R
import com.rnsgate.app.data.model.InterfaceKind
import com.rnsgate.app.ui.theme.RnsGreen
import com.rnsgate.app.ui.theme.RnsMuted

@Composable
fun ToolsScreen(
    vm: ToolsViewModel,
    onOpenLogs: () -> Unit = {}
) {
    val snap by vm.snapshot.collectAsStateWithLifecycle()
    val peers by vm.peers.collectAsStateWithLifecycle()
    val identity by vm.shownIdentity.collectAsStateWithLifecycle()
    val host by vm.host.collectAsStateWithLifecycle()
    val port by vm.port.collectAsStateWithLifecycle()
    val saved by vm.savedToast.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val savedMsg = stringResource(R.string.tools_tcp_saved)

    LaunchedEffect(saved) {
        if (saved) {
            snackbar.showSnackbar(savedMsg)
            vm.clearSavedToast()
        }
    }

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.tools_title), style = MaterialTheme.typography.headlineMedium)

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.tools_identity), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = vm::showIdentity) {
                            Text(stringResource(R.string.tools_identity_show))
                        }
                        Button(onClick = vm::regenerateIdentity) {
                            Text(stringResource(R.string.tools_identity_regen))
                        }
                    }
                    identity?.let { id ->
                        Text(
                            text = "${id.displayName}\n${id.hashHex}",
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.tools_peers), style = MaterialTheme.typography.titleMedium)
                    if (peers.isEmpty()) {
                        Text(stringResource(R.string.tools_peers_empty), color = RnsMuted)
                    } else {
                        peers.forEach { peer ->
                            Text(
                                text = "${peer.displayName} · ${peer.hashHex}",
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.tools_interfaces), style = MaterialTheme.typography.titleMedium)
                    snap.interfaces.forEach { iface ->
                        val name = when (iface.kind) {
                            InterfaceKind.Tcp -> stringResource(R.string.gate_iface_tcp)
                            InterfaceKind.Auto -> stringResource(R.string.gate_iface_auto)
                            InterfaceKind.RNode -> stringResource(R.string.gate_iface_rnode)
                        }
                        val status = when {
                            iface.isPlaceholder -> stringResource(R.string.tools_status_placeholder)
                            iface.up -> stringResource(R.string.tools_status_up)
                            else -> stringResource(R.string.tools_status_down)
                        }
                        val color = when {
                            iface.up -> RnsGreen
                            iface.isPlaceholder -> RnsMuted
                            else -> MaterialTheme.colorScheme.error
                        }
                        Text(
                            text = "$name — $status" + if (iface.detail.isNotEmpty()) " (${iface.detail})" else "",
                            color = color,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = vm::onHostChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.tools_tcp_host)) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = port,
                        onValueChange = vm::onPortChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.tools_tcp_port)) },
                        singleLine = true
                    )
                    Button(onClick = vm::saveTcp, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.tools_tcp_save))
                    }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_diagnostics), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(R.string.settings_diagnostics_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = RnsMuted
                    )
                    OutlinedButton(onClick = onOpenLogs, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.settings_open_logs))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        SnackbarHost(hostState = snackbar)
    }
}
