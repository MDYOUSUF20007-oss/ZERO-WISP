package com.zerowisp.app.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerowisp.app.data.TranscriptEntry
import com.zerowisp.app.ui.theme.*
import com.zerowisp.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: MainViewModel) {
    val history by viewModel.history.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("History & Settings", color = ColorOnBackground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            if (history.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearHistory() }) {
                    Text("Clear All", color = ColorOnSurfaceVariant, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Settings section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ColorSurface),
            border = BorderStroke(1.dp, ColorBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Settings", color = ColorOnBackground, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(4.dp))

                SettingToggle(
                    title = "Always Output English",
                    subtitle = "Translate any language to English",
                    checked = settings.alwaysOutputEnglish,
                    onCheckedChange = { viewModel.setAlwaysOutputEnglish(it) }
                )
                HorizontalDivider(color = ColorBorder, thickness = 0.5.dp)
                SettingToggle(
                    title = "Clean Filler Words",
                    subtitle = "Remove um, uh, er, ah from transcripts",
                    checked = settings.cleanFillerWords,
                    onCheckedChange = { viewModel.setCleanFillerWords(it) }
                )
                HorizontalDivider(color = ColorBorder, thickness = 0.5.dp)
                SettingToggle(
                    title = "Auto-Chunk Long Speech",
                    subtitle = "Handle 1000+ word dictations seamlessly",
                    checked = settings.autoChunk,
                    onCheckedChange = { viewModel.setAutoChunk(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Recent Transcriptions", color = ColorOnBackground, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(8.dp))

        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No transcriptions yet", color = ColorOnSurfaceVariant, fontSize = 14.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(history) { entry ->
                    HistoryCard(entry = entry, onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("zerowisp", entry.text))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    })
                }
            }
        }
    }
}

@Composable
private fun SettingToggle(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, color = ColorOnBackground, fontSize = 14.sp)
            Text(subtitle, color = ColorOnSurfaceVariant, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ColorOnPrimary,
                checkedTrackColor = ColorPrimary,
                uncheckedTrackColor = ColorSurfaceVariant
            )
        )
    }
}

@Composable
private fun HistoryCard(entry: TranscriptEntry, onClick: () -> Unit) {
    val dateStr = remember(entry.timestamp) {
        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(entry.timestamp))
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = ColorSurfaceVariant),
        border = BorderStroke(1.dp, ColorBorder),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(entry.text, color = ColorOnBackground, fontSize = 14.sp, maxLines = 3)
            Spacer(modifier = Modifier.height(4.dp))
            Text(dateStr, color = ColorOnSurfaceVariant, fontSize = 11.sp)
        }
    }
}
