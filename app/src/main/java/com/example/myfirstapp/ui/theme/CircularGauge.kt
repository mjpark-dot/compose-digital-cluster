package com.example.myfirstapp.ui.theme

import android.R
import android.R.attr.textSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CircularGauge(
    value: Float,
    maxValue: Float = 240f,
    unit: String = "km/h",
    modifier: Modifier = Modifier.size(250.dp),
    activeColor: Color = Color(0xFF00E5FF),
    warningColor: Color = Color(0xFFFF5252),
    warningThreshold: Float = 100f                  //경고 기준치
) {
    val animateValue by animateFloatAsState(
        targetValue = value,                        // 목표값(끝값)
        animationSpec = tween(                      // tween animation
            durationMillis = 650,                   // 시작값과 끝값 간에 애니매이션을 처리
            easing = LinearOutSlowInEasing          // 진입할 때는 일정하게 들어오고 끝날 때 부드럽게 감속
        ),
        label = "GaugeAnimation"
    )

    val gaugeColor = if (value > warningThreshold) warningColor else activeColor // 경고 기준치에 따라 다른 색 적용
    val isRpmGauge = maxValue > 1000f   // 1000f보다 크면 rpm게이지

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center             // 센터
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()           // 최대로 채운다
        ) {

            val strokeW = 18f                           // 선굵기
            val startAngle = 135f                       // 시작 각도
            val totalSweep = 270f                       // 전체 그려질 총 각도

            drawArc(                                    // 배경 호 그리기
                color = Color(0xFF1E1E1E),
                startAngle = startAngle,                // 0도(3시 방향) 기준 시작각도
                sweepAngle = totalSweep,                // 그릴 각도
                useCenter = false,                      // 중심점 연결 여부
                style = Stroke(width = strokeW)
            )

            if (isRpmGauge) {
                val redlineStartRatio = 6000f / maxValue // 6/8지점 (75%). 레드라인 시작 지점 비율(0.0 ~ 1.0)
                val redlineStartAngle = startAngle + (redlineStartRatio * totalSweep) // 시작각도에서부터 전체 그릴 각도의 특정 비율만큼 더 간 지점
                val redlineSweepAngle = (1f - redlineStartRatio) * totalSweep // 마지막 25% 구간

                drawArc(
                    color = Color(0xFF5A0000), // 상시 짙은 와인/버건디 레드 배경
                    startAngle = redlineStartAngle,
                    sweepAngle = redlineSweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeW)
                )
            }

            val currentSweepAngle = (animateValue / maxValue).coerceIn(0f, 1f) * totalSweep // 전체중에서 목표값의 비율에서 총 그려질 각도를 곱함
            drawArc(
                color = gaugeColor,
                startAngle = startAngle,
                sweepAngle = currentSweepAngle,
                useCenter = false,
                style = Stroke(width = strokeW)
            )

            val totalTicks = if (isRpmGauge) 8 else 12              // 총 눈금 수
            val anglePerTick = totalSweep / totalTicks              // 눈금 당 간격각도?

            val defaultTextPaint = Paint().apply {                  // 눈금 숫자
                color = Color.White.copy(alpha = 0.65f).toArgb()
                textSize = 10.sp.toPx()
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                isFakeBoldText = true                               // 가짜 굵게
            }

            val redlineTextPaint = android.graphics.Paint().apply {
                color = Color(0xFFFF1744).toArgb()
                textSize = 10.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                isFakeBoldText = true
            }

            for (i in 0..totalTicks) {
                val tickAngle = startAngle + (i * anglePerTick)         // 눈금 앵글
                val tickLength = 10.dp.toPx()                           // 눈금 길이

                val isRedLine = isRpmGauge && i >= 6                    // RPM의 6,7,8

                rotate(degrees = tickAngle + 90f, pivot = center) {     // 기준선 12시 방향 -> 캔버스 0도 맞추기 위해 +90f
                    drawLine(
                        color = if (isRedLine) Color(0xFFFF1744) else Color.White.copy(alpha = 0.5f),
                        start = Offset(x = center.x, y = 0f),
                        end = Offset(x = center.x, y = tickLength),     // 12시 수직
                        strokeWidth = if (isRedLine) 3.dp.toPx() else 2.5.dp.toPx()

                    )
                }

                val labelText = if (isRpmGauge) {
                    // RPM: 0, 1, 2, 3, 4, 5, 6, 7, 8
                    "$i"
                } else {
                    // 속도계: 0, 20, 40, 60 ... 240
                    "${(i * (maxValue / totalTicks)).toInt()}"
                }

                val labelRadius = (size.width / 2) - 26.dp.toPx()
                val rad = Math.toRadians(tickAngle.toDouble())
                val labelX = center.x + (labelRadius * cos(rad)).toFloat()
                val labelY = center.y + (labelRadius * sin(rad)).toFloat() + (defaultTextPaint.textSize / 3)

                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        labelText,
                        labelX,
                        labelY,
                        if (isRedLine) redlineTextPaint else defaultTextPaint
                    )
                }
            }

                val needleAngle =
                    startAngle + (animateValue / maxValue).coerceIn(0f, 1f) * totalSweep
                val needleLength = (size.width / 2) - 34.dp.toPx()

                rotate(needleAngle + 90f, pivot = center) {
                    drawLine(
                        color = gaugeColor,
                        start = center,
                        end = Offset(x = center.x, y = center.y - needleLength),
                        strokeWidth = 3.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                drawCircle(
                    color = Color(0xFF1E1E1E),
                    radius = 12.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = gaugeColor,
                    radius = 5.dp.toPx(),
                    center = center
                )
            }

            Column(
                modifier = Modifier.padding(top = 95.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${animateValue.toInt()}",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = gaugeColor
                )

                Text(
                    text = unit,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                )
            }

    }
}