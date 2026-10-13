package com.shrey.reelize.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.shrey.reelize.ui.theme.ReelizeTheme

private enum class Screen { DASHBOARD, LOG, BUBBLE }

class MainActivity : ComponentActivity() {
    private val vm: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReelizeTheme {
                var screen by rememberSaveable { mutableStateOf(Screen.DASHBOARD) }
                val state by vm.state.collectAsState()
                val home = { screen = Screen.DASHBOARD; vm.refresh() }
                when (screen) {
                    Screen.DASHBOARD -> DashboardScreen(
                        state, vm,
                        onOpenLog = { screen = Screen.LOG },
                        onOpenBubble = { screen = Screen.BUBBLE },
                    )
                    Screen.LOG -> {
                        BackHandler(onBack = home)
                        LogScreen(onBack = home)
                    }
                    Screen.BUBBLE -> {
                        BackHandler(onBack = home)
                        BubbleSettingsScreen(onBack = home)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refresh()
    }
}
