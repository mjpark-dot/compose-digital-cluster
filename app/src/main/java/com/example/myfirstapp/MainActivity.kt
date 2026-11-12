package com.example.myfirstapp

import android.R
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myfirstapp.ui.theme.CircularGauge
import kotlinx.coroutines.delay
import java.nio.file.WatchEvent


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpeedometerScreen()
        }
    }
}




@Composable
fun SpeedometerScreen(viewModel: ClusterViewModel = viewModel()) {

    val clusterState by viewModel.uiState.collectAsStateWithLifecycle()
    var blinkerVisible by remember { mutableStateOf(true) }

    val soundManager = remember { ClusterSoundManager() }

    DisposableEffect(Unit) {
        onDispose {
            soundManager.release()
        }
    }

    LaunchedEffect(clusterState.isIgnitionOn) {
        if (clusterState.isIgnitionOn) {
            soundManager.playWelcomeChime()
        }
    }

    LaunchedEffect(clusterState.isLeftBlinkerOn, clusterState.isRightBlinkerOn, clusterState.isHazardOn) {
        if (clusterState.isLeftBlinkerOn || clusterState.isRightBlinkerOn || clusterState.isHazardOn) {
            while (true) {
                soundManager.playTick()
                blinkerVisible = true
                delay(500L)

                soundManager.playTok()
                blinkerVisible = false
                delay(500L)

            }
        } else {
            blinkerVisible = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(0.92f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "◀",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = if (clusterState.isLeftBlinkerOn && blinkerVisible) Color(0xFF00E676) else Color(0xFF333333),
                modifier = Modifier.clickable{
                    viewModel.toggleLeftBlinker()
                }
            )



            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {

               EngineStartButton(
                   isIgnitionOn = clusterState.isIgnitionOn,
                   onClick = { viewModel.toggleIgnition() }
               )

                Text(
                    text = "▲",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (clusterState.isHazardOn) {
                        if (blinkerVisible) Color(0xFFFF1744) else Color(0xFF4A0000)
                    } else {
                        Color(0xFF331515)
                    },
                    modifier = Modifier
                        .clickable { viewModel.toggleHazard() }
                        .padding(6.dp)
                )

                Row (horizontalArrangement = Arrangement.spacedBy(12.dp)){
                    listOf("P", "R", "N", "D").forEach { gearItem ->
                        Text(
                            text = gearItem,
                            fontSize = 18.sp,
                            fontWeight = if (gearItem == clusterState.gear) FontWeight.Bold else FontWeight.Medium,
                            color = if (!clusterState.isIgnitionOn) Color(0xFF2A2A2A)
                                    else if (gearItem == clusterState.gear) Color(0xFF00E5FF)
                                    else Color(0xFF555555),
                            modifier = Modifier.clickable {
                                viewModel.changeGear(gearItem)
                            }
                        )
                    }
                }
            }

            Text(
                text = "▶",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = if (clusterState.isRightBlinkerOn && blinkerVisible) Color(0xFF00E676) else Color(0xFF262626),
                modifier = Modifier.clickable {
                    viewModel.toggleRightBlinker()
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularGauge(
                value = clusterState.speed.toFloat(),
                maxValue = 240f,
                unit = "km/h",
                modifier = Modifier.size(240.dp)
            )

            // 중앙 트립 컴퓨터 MID 패널
            CenterTripComputer(
                state = clusterState,
                onPanelClick = { viewModel.toggleMidMode() }
            )

            CircularGauge(
                value = clusterState.rpm,
                maxValue = 8000f,
                unit = "RPM x1000",
                modifier = Modifier.size(240.dp)
            )
        }


        Row (
            modifier = Modifier.fillMaxWidth(0.92f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ){
            Button(
                onClick = { viewModel.decelerate() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("-50", color = Color.White, fontSize = 12.sp)
            }

            Row(
                modifier = Modifier.background(
                    color = Color(0xFF181818),
                    shape = RoundedCornerShape(8.dp)
                )
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.toggleAutoCruise()},
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (clusterState.isAutoCruiseOn) Color(0xFF00C853) else Color(0xFF2C2C2C)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if(clusterState.isAutoCruiseOn) "CRUISE ON" else "CRUISE OFF",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                WarningIndicator(
                    symbol = "⛭",
                    label = "CHECK",
                    isActive = clusterState.isEngineWarningOn,
                    activeColor = Color(0xFFFF9100),
                    onClick = { viewModel.toggleEngineWarning() }
                )

                WarningIndicator(
                    symbol = "▤",
                    label = "BATT",
                    isActive = clusterState.isBatteryWarningOn,
                    activeColor = Color(0xFFFF1744),
                    onClick = { viewModel.toggleBatteryWarning() }
                )

                WarningIndicator(
                    symbol = "⫘",
                    label = "BELT",
                    isActive = clusterState.isSeatbeltWarningOn,
                    activeColor = Color(0xFFFF1744),
                    onClick = { viewModel.toggleSeatbeltWarning() }
                )
            }

            Button(
                onClick = { viewModel.accelerate() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "+50", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun WarningIndicator(
    symbol: String,         // 경고등 심볼 문자 또는 라벨
    label: String,          // 식별용 텍스트
    isActive: Boolean,      //점등 여부
    activeColor: Color,     //점등 시 색상(엔진: 주황, 배터리/벨트: 빨강)
    onClick: () -> Unit
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 8.dp)
    ) {
        Text(
            text = symbol,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) activeColor else Color(0xFF262626)
        )

        Text(
            text = label,
            fontSize = 10.sp,
            color = if (isActive) activeColor.copy(alpha = 0.8f) else Color(0xFF404040)
        )
    }
}