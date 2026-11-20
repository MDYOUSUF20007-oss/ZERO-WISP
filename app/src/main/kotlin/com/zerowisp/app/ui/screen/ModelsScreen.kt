package com.zerowisp.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerowisp.app.ui.theme.*
import com.zerowisp.app.ui.viewmodel.MainViewModel
import com.zerowisp.app.ui.viewmodel.ModelUiState

@Composable
fun ModelsScreen(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
            .padding(16.dp)
    ) {
        Text("Models", color = ColorOnBackground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Stored in app private storage. 100% offline.", color = ColorOnSurfaceVariant, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val states = viewModel.getModelStates()
            items(states) { state ->
                ModelCard(
                    state = state,
                    onDownload = { viewModel.startDownload(state.model) },
                    onCancel = { viewModel.cancelDownload(state.model.id) },
                    onDelete = { viewModel.deleteModel(state.model) },
                    onSelect = { viewModel.selectModel(state.model) }
                )
            }
        }
    }
}

@Composable
private fun ModelCard(
    state: ModelUiState,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit
) {
    val sizeMb = state.model.sizeBytes / 1_000_000

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ColorSurface),
        border = BorderStroke(
            width = if (state.isActive) 1.5.dp else 1.dp,
            color = if (state.isActive) ColorPrimary else ColorBorder
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(state.model.displayName, color = ColorOnBackground, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        if (state.isActive) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = ColorPrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("Active", color = ColorPrimary, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(state.model.description, color = ColorOnSurfaceVariant, fontSize = 13.sp)
                    Text("~${sizeMb} MB", color = ColorOnSurfaceVariant, fontSize = 12.sp)
                }
                if (state.isDownloaded) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFF666677))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (state.downloadProgress >= 0) {
                LinearProgressIndicator(
                    progress = { state.downloadProgress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = ColorPrimary,
                    trackColor = ColorSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${state.downloadProgress}%", color = ColorOnSurfaceVariant, fontSize = 12.sp)
                    TextButton(onClick = onCancel) {
                        Text("Cancel", color = Color(0xFFFF6B6B), fontSize = 12.sp)
                    }
                }
            } else if (state.isDownloaded) {
                if (!state.isActive) {
                    Button(
                        onClick = onSelect,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ColorSurfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Select", color = ColorOnSurface, fontSize = 14.sp)
                    }
                }
            } else {
                Button(
                    onClick = onDownload,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Download ~${sizeMb} MB", color = ColorOnPrimary, fontSize = 14.sp)
                }
            }
        }
    }
}
