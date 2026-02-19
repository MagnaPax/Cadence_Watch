package com.example.cadence_metronome.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.example.cadence_metronome.presentation.theme.Cadence_metronomeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CadenceApp()
        }
    }
}

@Composable
fun CadenceApp() {
    val context = LocalContext.current
    val sharedPreferences = remember {
        context.getSharedPreferences("cadence_settings", Context.MODE_PRIVATE)
    }
    
    // 저장된 설정 불러오기
    var cadenceBpm by remember { 
        mutableIntStateOf(sharedPreferences.getInt("saved_bpm", 180)) 
    }
    var useVibration by remember {
        mutableStateOf(sharedPreferences.getBoolean("use_vibration", true))
    }
    var useSound by remember {
        mutableStateOf(sharedPreferences.getBoolean("use_sound", false))
    }
    var isRunning by remember { mutableStateOf(false) }

    fun updateService() {
        if (isRunning) {
            val intent = Intent(context, CadenceService::class.java).apply {
                action = "START"
                putExtra("BPM", cadenceBpm)
                putExtra("VIBRATION", useVibration)
                putExtra("SOUND", useSound)
            }
            context.startForegroundService(intent)
        }
    }

    fun toggleMetronome() {
        val intent = Intent(context, CadenceService::class.java)
        if (isRunning) {
            intent.action = "STOP"
            context.startService(intent)
            isRunning = false
        } else {
            intent.action = "START"
            intent.putExtra("BPM", cadenceBpm)
            intent.putExtra("VIBRATION", useVibration)
            intent.putExtra("SOUND", useSound)
            context.startForegroundService(intent)
            isRunning = true
        }
    }

    // 설정 변경 시 저장 및 서비스 업데이트
    LaunchedEffect(cadenceBpm, useVibration, useSound) {
        sharedPreferences.edit().apply {
            putInt("saved_bpm", cadenceBpm)
            putBoolean("use_vibration", useVibration)
            putBoolean("use_sound", useSound)
            apply()
        }
        updateService()
    }

    Cadence_metronomeTheme {
        AppScaffold {
            ScreenScaffold {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "BPM: $cadenceBpm",
                            style = MaterialTheme.typography.titleLarge
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // BPM 조절 버튼
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { if (cadenceBpm > 60) cadenceBpm -= 5 },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Text("-")
                            }
                            Button(
                                onClick = { if (cadenceBpm < 240) cadenceBpm += 5 },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Text("+")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 진동/소리 토글 버튼
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = { useVibration = !useVibration },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Text(if (useVibration) "📳" else "📴")
                            }
                            Button(
                                onClick = { useSound = !useSound },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Text(if (useSound) "🔔" else "🔕")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { toggleMetronome() },
                            modifier = Modifier.fillMaxWidth(0.7f)
                        ) {
                            Text(if (isRunning) "STOP" else "START")
                        }
                    }
                }
            }
        }
    }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
fun DefaultPreview() {
    CadenceApp()
}
