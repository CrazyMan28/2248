package com.trace.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trace.game.nav.TraceNavHost
import com.trace.game.ui.play.GameViewModel
import com.trace.game.ui.play.GameViewModelFactory
import com.trace.game.ui.theme.TraceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as TraceApp
        setContent {
            val factory = remember {
                GameViewModelFactory(app.prefs, app.haptics, app.sfx)
            }
            val vm: GameViewModel = viewModel(factory = factory)
            val prefs by vm.userPrefs.collectAsState()
            TraceTheme(reduceMotion = prefs.reduceMotion) {
                TraceNavHost(vm = vm)
            }
        }
    }
}
