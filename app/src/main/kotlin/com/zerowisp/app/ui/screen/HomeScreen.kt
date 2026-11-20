package com.zerowisp.app.ui.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerowisp.app.service.FloatingBubbleService
import com.zerowisp.app.service.WispAccessibilityService
import com.zerowisp.app.ui.theme.*
import com.zerowisp.app.ui.viewmodel.MainViewModel

@Composable
fun HomeScreen(viewModel: MainViewModel, testTranscription: String) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled via recompose */ }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Text(
            text = "Zero Wisp",
            color = ColorOnBackground,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Offline Voice-to-Text",
            color = ColorOnSurfaceVariant,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Launch Bubble Toggle
        var isRunning by remember { mutableStateOf(FloatingBubbleService.isRunning) }
        LaunchedEffect(Unit) {
            while (true) {
                isRunning = FloatingBubbleService.isRunning
                kotlinx.coroutines.delay(500)
            }
        }
        Button(
            onClick = {
                if (isRunning) viewModel.stopBubbleService()
                else viewModel.startBubbleService()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) Color(0xFF2A2A2E) else ColorPrimary,
                contentColor = if (isRunning) ColorOnSurface else ColorOnPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Mic, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isRunning) "Stop Floating Bubble" else "Launch Floating Bubble",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        // Permissions Checklist
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ColorSurface),
            border = BorderStroke(1.dp, ColorBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Permissions", color = ColorOnBackground, fontWeight = FontWeight.SemiBold)

                PermissionRow(
                    label = "Microphone",
                    isGranted = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED,
                    onGrant = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                )

                PermissionRow(
                    label = "Draw Over Apps",
                    isGranted = Settings.canDrawOverlays(context),
                    onGrant = {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")))
                    }
                )

                PermissionRow(
                    label = "Accessibility (Auto-Paste)",
                    isGranted = WispAccessibilityService.isEnabled,
                    onGrant = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                )

                PermissionRow(
                    label = "Unrestricted Battery",
                    isGranted = isIgnoringBatteryOptimizations(context),
                    onGrant = {
                        context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${context.packageName}")))
                    }
                )
            }
        }

        // Test box
        if (testTranscription.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ColorSurfaceVariant),
                border = BorderStroke(1.dp, ColorBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Last Transcription", color = ColorOnSurfaceVariant, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(testTranscription, color = ColorOnBackground, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(label: String, isGranted: Boolean, onGrant: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = if (isGranted) ColorPrimary else Color(0xFF555560),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = ColorOnBackground, fontSize = 14.sp)
        }
        if (!isGranted) {
            TextButton(onClick = onGrant) {
                Text("Grant", color = ColorPrimary, fontSize = 13.sp)
            }
        }
    }
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}
