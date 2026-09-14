package com.rnsgate.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rnsgate.app.R
import com.rnsgate.app.ui.theme.RnsMuted

@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    onOpenLogs: () -> Unit = {}
) {
    val name by vm.displayName.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val savedMsg = stringResource(R.string.settings_saved)

    LaunchedEffect(saved) {
        if (saved) {
            snackbar.showSnackbar(savedMsg)
            vm.clearSaved()
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
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = vm::onNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.settings_display_name)) },
                        placeholder = { Text(stringResource(R.string.settings_display_name_hint)) },
                        singleLine = true
                    )
                    Button(onClick = vm::save, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.settings_save))
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

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(R.string.settings_language_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = RnsMuted
                    )
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_about), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(R.string.settings_about_body, "0.2.3"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = RnsMuted
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        SnackbarHost(hostState = snackbar)
    }
}
