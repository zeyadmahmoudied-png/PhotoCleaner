package com.photocleaner.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.photocleaner.app.R

@Composable
fun AlbumsScreen() {
    PlaceholderScreen(title = stringResource(R.string.albums), message = "Album listing & selective scan will appear here.")
}

@Composable
fun TrashScreen() {
    PlaceholderScreen(
        title = stringResource(R.string.trash),
        message = "Permanent trash with restore & permanent delete. Files are copied to app-private storage before original deletion."
    )
}

@Composable
fun StatsScreen() {
    PlaceholderScreen(title = stringResource(R.string.stats), message = "Detailed statistics and history.")
}

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = stringResource(R.string.settings), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.language) + ": العربية / English")
        Text(stringResource(R.string.theme) + ": " + stringResource(R.string.system_default))
        Text(stringResource(R.string.similarity_threshold) + ": adjustable")
        Text(stringResource(R.string.privacy_note))
        Text(stringResource(R.string.version))
    }
}

@Composable
private fun PlaceholderScreen(title: String, message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
