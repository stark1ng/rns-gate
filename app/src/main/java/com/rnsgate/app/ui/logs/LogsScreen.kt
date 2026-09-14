package com.rnsgate.app.ui.logs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rnsgate.app.R
import com.rnsgate.app.ui.theme.RnsMuted
import com.rnsgate.app.util.DiagLog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val logText by DiagLog.text.collectAsStateWithLifecycle()
    val header = remember { buildHeader(context) }
    val display = if (logText.isBlank()) stringResource(R.string.logs_empty) else logText
    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.logs_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.logs_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = header,
                style = MaterialTheme.typography.bodySmall,
                color = RnsMuted,
                fontFamily = FontFamily.Monospace
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val body = header + "\n\n" + DiagLog.snapshot()
                        copyToClipboard(context, body)
                        Toast.makeText(
                            context,
                            context.getString(R.string.logs_copied),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.logs_copy_all))
                }
                OutlinedButton(
                    onClick = {
                        val body = header + "\n\n" + DiagLog.snapshot()
                        shareLogs(context, body)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.logs_share))
                }
                OutlinedButton(
                    onClick = { DiagLog.clear() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.logs_clear))
                }
            }
            Text(
                text = display,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(vScroll)
                    .horizontalScroll(hScroll),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                style = MaterialTheme.typography.bodySmall,
                color = if (logText.isBlank()) RnsMuted else MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

private fun buildHeader(context: Context): String {
    val app = context.applicationContext as com.rnsgate.app.RnsGateApp
    val (versionName, versionCode) = appVersion(context)
    val mode = if (app.usingRealRns) {
        "Real RNS"
    } else {
        "Demo fallback" + (app.fallbackReason?.let { " ($it)" } ?: "")
    }
    val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date())
    return "RNS Gate $versionName ($versionCode) · $mode · $ts"
}

private fun appVersion(context: Context): Pair<String, Long> {
    return try {
        val pm = context.packageManager
        val pkg = context.packageName
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(pkg, 0)
        }
        val name = info.versionName ?: "?"
        val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
        name to code
    } catch (_: Exception) {
        "?" to -1L
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("RNS Gate logs", text))
}

private fun shareLogs(context: Context, text: String) {
    try {
        val cacheDir = File(context.cacheDir, "logs").apply { mkdirs() }
        val file = File(cacheDir, "rns-gate-logs.txt")
        file.writeText(text)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "RNS Gate logs")
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(
            Intent.createChooser(send, context.getString(R.string.logs_share)).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )
        )
    } catch (t: Throwable) {
        Toast.makeText(context, t.message ?: "Share failed", Toast.LENGTH_SHORT).show()
    }
}
