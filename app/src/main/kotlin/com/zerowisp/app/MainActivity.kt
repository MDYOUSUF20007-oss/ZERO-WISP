package com.zerowisp.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.zerowisp.app.service.FloatingBubbleService
import com.zerowisp.app.ui.screen.HistoryScreen
import com.zerowisp.app.ui.screen.HomeScreen
import com.zerowisp.app.ui.screen.ModelsScreen
import com.zerowisp.app.ui.theme.ColorBackground
import com.zerowisp.app.ui.theme.ColorOnSurfaceVariant
import com.zerowisp.app.ui.theme.ColorPrimary
import com.zerowisp.app.ui.theme.ZeroWispTheme
import com.zerowisp.app.ui.viewmodel.MainViewModel

data class NavTab(val label: String, val icon: ImageVector)

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var testTranscription by mutableStateOf("")

    private val transcriptionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == FloatingBubbleService.ACTION_TRANSCRIPTION_READY) {
                testTranscription = intent.getStringExtra(FloatingBubbleService.EXTRA_TRANSCRIPTION) ?: ""
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Edge-to-edge AMOLED: pitch black status bar
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK

        ContextCompat.registerReceiver(
            this,
            transcriptionReceiver,
            IntentFilter(FloatingBubbleService.ACTION_TRANSCRIPTION_READY),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        setContent {
            ZeroWispTheme {
                ZeroWispApp(viewModel = viewModel, testTranscription = testTranscription)
            }
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(transcriptionReceiver)
        } catch (_: Exception) {}
        super.onDestroy()
    }
}

@Composable
fun ZeroWispApp(viewModel: MainViewModel, testTranscription: String) {
    val tabs = listOf(
        NavTab("Home", Icons.Default.Home),
        NavTab("Models", Icons.Default.Storage),
        NavTab("History", Icons.Default.History)
    )
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = ColorBackground,
        bottomBar = {
            NavigationBar(
                containerColor = ColorBackground,
                contentColor = ColorPrimary
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ColorPrimary,
                            selectedTextColor = ColorPrimary,
                            unselectedIconColor = ColorOnSurfaceVariant,
                            unselectedTextColor = ColorOnSurfaceVariant,
                            indicatorColor = ColorBackground
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ColorBackground)
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(viewModel = viewModel, testTranscription = testTranscription)
                1 -> ModelsScreen(viewModel = viewModel)
                2 -> HistoryScreen(viewModel = viewModel)
            }
        }
    }
}
