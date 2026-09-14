package com.rnsgate.app.ui.gate

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rnsgate.app.R
import com.rnsgate.app.data.model.ConnectStep
import com.rnsgate.app.data.model.ConnectionState
import com.rnsgate.app.data.model.InterfaceKind
import com.rnsgate.app.ui.theme.RnsGreen
import com.rnsgate.app.ui.theme.RnsMuted
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GateScreen(vm: GateViewModel) {
    val snap by vm.snapshot.collectAsStateWithLifecycle()
    val online = snap.connectionState == ConnectionState.Online
    val connecting = snap.connectionState == ConnectionState.Connecting

    val pulse by animateFloatAsState(
        targetValue = if (online) 1f else 0.35f,
        animationSpec = tween(600),
        label = "pulse"
    )
    val statusColor by animateColorAsState(
        targetValue = when (snap.connectionState) {
            ConnectionState.Online -> RnsGreen
            ConnectionState.Connecting -> MaterialTheme.colorScheme.secondary
            ConnectionState.Offline -> RnsMuted
        },
        label = "statusColor"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.gate_title),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = stringResource(R.string.gate_demo_badge),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Spacer(Modifier.height(28.dp))

        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(160.dp)) {
                drawCircle(
                    color = statusColor.copy(alpha = 0.15f * pulse),
                    radius = size.minDimension / 2f
                )
                drawCircle(
                    color = statusColor.copy(alpha = 0.35f * pulse),
                    radius = size.minDimension / 2.6f
                )
            }
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (connecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = statusColor,
                        strokeWidth = 3.dp
                    )
                } else {
                    Text(
                        text = if (online) "ON" else "OFF",
                        style = MaterialTheme.typography.titleLarge,
                        color = statusColor
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = when (snap.connectionState) {
                ConnectionState.Offline -> stringResource(R.string.gate_status_offline)
                ConnectionState.Connecting -> stringResource(R.string.gate_status_connecting)
                ConnectionState.Online -> stringResource(R.string.gate_status_online)
            },
            style = MaterialTheme.typography.titleMedium,
            color = statusColor
        )

        Spacer(Modifier.height(24.dp))

        ConnectInfographic(step = snap.step)

        Spacer(Modifier.height(20.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            snap.interfaces.forEach { iface ->
                val label = when (iface.kind) {
                    InterfaceKind.Tcp -> stringResource(R.string.gate_iface_tcp)
                    InterfaceKind.Auto -> stringResource(R.string.gate_iface_auto)
                    InterfaceKind.RNode -> stringResource(R.string.gate_iface_rnode)
                }
                val suffix = if (iface.isPlaceholder) {
                    " " + stringResource(R.string.gate_iface_placeholder)
                } else if (iface.up) " ●" else ""
                AssistChip(
                    onClick = {},
                    enabled = !iface.isPlaceholder,
                    label = { Text(label + suffix) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (iface.up) {
                            RnsGreen.copy(alpha = 0.18f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                )
            }
        }

        if (online && snap.identity != null) {
            Spacer(Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.gate_hash_label), style = MaterialTheme.typography.labelLarge, color = RnsMuted)
                    Text(
                        text = snap.identity!!.hashHex,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.gate_uptime_label), style = MaterialTheme.typography.labelLarge, color = RnsMuted)
                    Text(formatUptime(snap.uptimeMs), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        if (online) {
            OutlinedButton(
                onClick = vm::toggleConnect,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(R.string.gate_disconnect))
            }
        } else {
            Button(
                onClick = vm::toggleConnect,
                enabled = !connecting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RnsGreen)
            ) {
                Text(stringResource(R.string.gate_connect))
            }
        }
    }
}

@Composable
private fun ConnectInfographic(step: ConnectStep) {
    val steps = listOf(
        ConnectStep.Identity to stringResource(R.string.gate_step_identity),
        ConnectStep.Interfaces to stringResource(R.string.gate_step_interfaces),
        ConnectStep.PathAnnounce to stringResource(R.string.gate_step_path),
        ConnectStep.Ready to stringResource(R.string.gate_step_ready)
    )
    val activeIndex = when (step) {
        ConnectStep.Idle -> -1
        ConnectStep.Identity -> 0
        ConnectStep.Interfaces -> 1
        ConnectStep.PathAnnounce -> 2
        ConnectStep.Ready -> 3
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            steps.forEachIndexed { index, (_, label) ->
                val done = index <= activeIndex
                val current = index == activeIndex && step != ConnectStep.Ready
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    done -> RnsGreen
                                    else -> MaterialTheme.colorScheme.outline
                                }
                            )
                    )
                    Spacer(Modifier.size(12.dp))
                    Text(
                        text = label,
                        color = if (done) MaterialTheme.colorScheme.onSurface else RnsMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (current) {
                        Spacer(Modifier.size(8.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = RnsGreen
                        )
                    }
                }
                if (index < steps.lastIndex) {
                    Canvas(
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .height(14.dp)
                            .fillMaxWidth(0.02f)
                    ) {
                        drawLine(
                            color = if (index < activeIndex) RnsGreen else RnsMuted,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 3f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

private fun formatUptime(ms: Long): String {
    val h = TimeUnit.MILLISECONDS.toHours(ms)
    val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return "%02d:%02d:%02d".format(h, m, s)
}
