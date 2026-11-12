package com.example.myfirstapp

import android.R
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun CenterTripComputer (
    state: ClusterState,
    onPanelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(140.dp)
            .height(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color = Color(0xFF141414))
            .border(
                width = 1.dp,
                color = Color(0xFF262626),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable{ onPanelClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Crossfade(
            targetState = state.midMode,
            animationSpec = tween(durationMillis = 300),
            label = "MidCrossfade"
        ) {  mode ->
            when(mode) {
                MidDisplayMode.TRIP -> TripModeView(state)
                MidDisplayMode.TPMS -> TpmsModeView(state)
            }
        }
    }
}


@Composable
private fun TripModeView(state: ClusterState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        // [1] 상단: 외기 온도 (OUTSIDE 21°C)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "OUTSIDE",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
            Text(
                text = if (state.isIgnitionOn) "${state.outTemp}°C" else "--°C",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (state.isIgnitionOn) Color.White else Color(0xFF444444)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // [2] 중앙: 체결 단수 (D일 때는 D1, D2.. 표시 / P, R, N(주차/후진/중립)은 그대로 표시)
        val gearDisplay = when {
            !state.isIgnitionOn -> "--"
            state.gear == "D" -> "D${state.gearNumber}"
            else -> state.gear
        }

        Text(
            text = gearDisplay,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = if (state.isIgnitionOn) Color(0xFF00E5FF) else Color(0xFF333333)
        )

        Spacer(modifier = Modifier.height(2.dp))

        // [3] 중하단: 평균 연비 (14.8 km/L)
        Text(
            text = if (state.isIgnitionOn) "${String.format(Locale.US, "%.1f", state.fuelEfficiency)} km/L" else "--.- km/L",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (state.isIgnitionOn) Color(0xFF76FF03) else Color(0xFF333333)
        )

        Spacer(modifier = Modifier.height(2.dp))

        // [4] 최하단: 적산거리계 (ODO 12,845.2 km)
        Text(
            text = if (state.isIgnitionOn) "ODO ${String.format(Locale.US, "%,.1f", state.odoKm)} km" else "ODO ----- km",
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = if (state.isIgnitionOn) Color(0xFFAAAAAA) else Color(0xFF444444)
        )
    }
}

// 4륜 TPMS 타이어 공기압 화면
@Composable
private fun TpmsModeView(state: ClusterState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxSize().padding(vertical = 2.dp)
    ) {
        Text(
            text = "TIRE PRESSURE (PSI)",
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = if (state.isIgnitionOn) Color(0xFF00E5FF) else Color(0xFF555555)
        )

        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PressureLabel(pressure = state.tirePressure[0], state.isIgnitionOn)
                Spacer(modifier = Modifier.height(14.dp))
                PressureLabel(pressure = state.tirePressure[2], state.isIgnitionOn)
            }

            // 중앙 미니 차량 섀시 실루엣
            CarChassisGraphic(isIgnition = state.isIgnitionOn)

            // 우측 타이머 2개 수치 (전우 / 후우)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PressureLabel(pressure = state.tirePressure[1], state.isIgnitionOn)
                Spacer(modifier = Modifier.height(14.dp))
                PressureLabel(pressure = state.tirePressure[3], state.isIgnitionOn)
            }
        }

        Text(
            text = if (state.isIgnitionOn) "ALL OK" else "STANDBY",
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = if (state.isIgnitionOn) Color(0xFF76FF03) else Color(0xFF555555)
        )

    }
}

@Composable
private fun PressureLabel(pressure: Int, isIgnition: Boolean) {
    val isNormal = pressure >= 30 // 30 PSI 미만 시 경고 색상
    val textColor = when {
        !isIgnition -> Color(0xFF444444)
        !isNormal -> Color(0xFFFF5252)
        else -> Color.White
    }

    Text(
        text = if (isIgnition) "$pressure" else "--",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = textColor
    )
}


@Composable
private fun CarChassisGraphic(isIgnition: Boolean) {
    Box(
        modifier = Modifier
            .width(26.dp)
            .height(48.dp)
            .background(
                color = if (isIgnition) Color(0xFF222222) else Color(0xFF1A1A1A),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
            )
            .border(
                width = 1.dp,
                if (isIgnition) Color(0xFF00E5FF).copy(alpha = 0.4f) else Color(0xFF333333),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(18.dp)
                .height(6.dp)
                .background(
                    color = if (isIgnition) Color(0xFF333333) else Color(0xFF252525),
                    shape = RoundedCornerShape(2.dp)
                )
        )
    }
}














