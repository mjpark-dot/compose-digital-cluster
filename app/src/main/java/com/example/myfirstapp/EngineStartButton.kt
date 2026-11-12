package com.example.myfirstapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EngineStartButton(
    isIgnitionOn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF3A3A3A), Color(0xFF1E1E1E), Color(0xFF0E0E0E))
                )
            )
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF888888), Color(0xFF333333), Color(0xFF666666))
                ),
                shape = CircleShape
            )
            .clickable{ onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(
                        if (isIgnitionOn) Color(0xFFFF1744) else Color(0xFF4A3B2C)
                    )
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "ENGINE",
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFAAAAAA),
                letterSpacing = 0.5.sp
            )

            Text(
                text = "START",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = if (isIgnitionOn) Color(0xFFFFFFFF) else Color(0xFF00E676), // 켜졌을 땐 백색, 켜기 전엔 초록 유도
                lineHeight = 10.sp
            )

            Text(
                text = "STOP",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = if (isIgnitionOn) Color(0xFFFF5252) else Color(0xFF777777), // 켜졌을 땐 빨간색 활성,
                lineHeight = 9.sp
            )












        }
    }
}