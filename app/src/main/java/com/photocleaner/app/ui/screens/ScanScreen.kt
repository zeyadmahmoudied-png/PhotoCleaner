package com.photocleaner.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.photocleaner.app.R
import com.photocleaner.app.domain.model.AppMode
import com.photocleaner.app.ui.ScanViewModel

@Composable
fun ScanScreen(viewModel: ScanViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = stringResource(R.string.scan), style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (state.mode == AppMode.SCAN_ONLY)
                        "وضع الفحص فقط (آمن — لا حذف)"
                    else
                        "وضع التنظيف (يتطلب مراجعة وتأكيد)",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "الوضع الافتراضي هو الفحص فقط. الحذف لا يحدث أبداً أثناء الفحص.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (state.isScanning) {
            LinearProgressIndicator(
                progress = { state.progress.progressFraction },
                modifier = Modifier.fillMaxWidth()
            )
            Text("${state.progress.processed} / ${state.progress.total} — ${state.progress.currentPhase}")
            OutlinedButton(
                onClick = { viewModel.pauseOrCancelScan() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("إيقاف الفحص") }
        } else {
            Button(
                onClick = { viewModel.startScan(computeHashes = true) },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.start_scan)) }
        }

        Text(state.statusMessage)
        state.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(8.dp))

        if (state.exactGroups.isNotEmpty() || state.similarGroups.isNotEmpty()) {
            Text(
                "مجموعات متطابقة: ${state.exactGroups.size}  |  مجموعات متشابهة: ${state.similarGroups.size}",
                style = MaterialTheme.typography.titleMedium
            )
            Text("صور مفحوصة: ${state.stats.totalPhotos}")
            Text(
                "ملاحظة: النتائج للعرض فقط. لا يتم تحديد أي ملف للحذف تلقائياً في وضع الفحص.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { viewModel.setMode(AppMode.SCAN_ONLY) },
                enabled = state.mode != AppMode.SCAN_ONLY
            ) { Text("وضع الفحص") }
            OutlinedButton(
                onClick = { viewModel.setMode(AppMode.CLEAN) },
                enabled = state.mode != AppMode.CLEAN
            ) { Text("تفعيل وضع التنظيف") }
        }
    }
}
