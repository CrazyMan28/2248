package com.trace.game.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.trace.game.data.UserPrefs
import com.trace.game.ui.theme.BgDeep
import com.trace.game.ui.theme.BgMid
import com.trace.game.ui.theme.Ink
import com.trace.game.ui.theme.InkDim
import com.trace.game.ui.theme.TraceTypography

@Composable
fun SettingsScreen(
    prefs: UserPrefs,
    onHaptics: (Boolean) -> Unit,
    onVolume: (Float) -> Unit,
    onMute: (Boolean) -> Unit,
    onReduceMotion: (Boolean) -> Unit,
    onBack: () -> Unit,
    onUiTap: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgDeep, BgMid)))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onUiTap(); onBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Ink)
            }
            Text("Settings", style = TraceTypography.headlineMedium)
        }
        Spacer(Modifier.height(16.dp))

        SettingRow("Haptics", prefs.hapticsEnabled) {
            onUiTap()
            onHaptics(it)
        }
        SettingRow("Mute SFX", prefs.sfxMuted) {
            onUiTap()
            onMute(it)
        }
        SettingRow("Reduce motion", prefs.reduceMotion) {
            onUiTap()
            onReduceMotion(it)
        }

        Spacer(Modifier.height(12.dp))
        Text("SFX volume", style = TraceTypography.titleMedium, color = Ink)
        Slider(
            value = prefs.sfxVolume,
            onValueChange = onVolume,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))
        Text("TRACE v1.0", style = TraceTypography.bodyMedium, color = InkDim)
        Text("Draw equals. Climb forever.", style = TraceTypography.bodyMedium, color = InkDim)
    }
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = TraceTypography.titleMedium, color = Ink, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
