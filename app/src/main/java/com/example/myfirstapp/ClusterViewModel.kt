package com.example.myfirstapp

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class MidDisplayMode {
    TRIP, // 외기온도, 기어단수, 연비, ODO
    TPMS  // 4륜 타이어 공기압 모니터링
}
data class ClusterState(
    val isIgnitionOn: Boolean = false,
    val speed: Int = 0,
    val rpm: Float = 800f,
    val gear: String = "P",
    val gearNumber: Int = 1,                    // 현재 체결 단수 (1~6)
    val isAutoCruiseOn: Boolean = false,
    val isLeftBlinkerOn: Boolean = false,
    val isRightBlinkerOn: Boolean = false,
    val isHazardOn: Boolean = false,
    val isEngineWarningOn: Boolean = false,
    val isBatteryWarningOn: Boolean = false,
    val isSeatbeltWarningOn: Boolean = true,
    // [MID 트립 컴퓨터 데이터]
    val outTemp: Int = 21,                      // 외기 온도 (°C)
    val fuelEfficiency: Double = 14.8,          // 평균 연비 (km/L)
    val odoKm: Double = 12845.2,                 // 누적 주행거리 (km)
    // [MID 화면 모드 및 4륜 공기압 (단위 : PSI)
    val midMode: MidDisplayMode = MidDisplayMode.TRIP,
    val tirePressure: List<Int> = listOf(34, 34, 33, 33) // 전좌, 전우, 후좌, 후우
)


// 데이터와 비즈니스 로직(CAN 시뮬레이션 루프)을 전담하는 매니저 클래스
class ClusterViewModel : androidx.lifecycle.ViewModel() {

    private val _uiState = MutableStateFlow(ClusterState())
    val uiState: StateFlow<ClusterState> = _uiState.asStateFlow()

    private var leftBlinkerJob: Job? = null
    private var rightBlinkerJob: Job? = null
    private var simulationJob: Job? = null
    private var selfCheckJob: Job? = null

    private fun calculateGearAndRpm(speed: Int) : Pair<Int, Float> {
        // 정지 상태일 때는 1단 공회전(아이들링 800 RPM)
        if (speed <= 0) return Pair(1, 800f)

        // 1. 속도 구간별 기어 단수(1~6단) 및 기어비 가중치 정의
        val (gear, baseRpm, gearRatio) = when (speed){
            in 1..30    -> Triple(1, 1000f, 115f) // 1단: 30km/h에서 약 4450 RPM
            in 31..60   -> Triple(2, 2200f, 85f)  // 2단: 60km/h에서 약 4750 RPM
            in 61..95   -> Triple(3, 2400f, 70f)  // 3단: 95km/h에서 약 4850 RPM
            in 96..130  -> Triple(4, 2600f, 65f)  // 4단: 130km/h에서 약 4810 RPM
            in 131..165 -> Triple(5, 2900f, 55f)  // 5단: 162km/h에서 약 4660 RPM (현실적!)
            else        -> Triple(6, 3300f, 40f)        // 6단: 170km/h 3500 RPM, 200km/h 4700 RPM
        }

        // 2. 각 기어별 체결 시작 기준 속도
        val baseSpeedForGear = when(gear) {
            1 -> 0
            2 -> 30
            3 -> 60
            4 -> 95
            5 -> 130
            else -> 165
        }

        // 3. 회전수 계산 (160km/h 부근에서 4,500 RPM 수준으로 호쾌하게 회전)
        val currentGearRpm = baseRpm + ((speed - baseSpeedForGear) * gearRatio)

        // 최소 800 RPM ~ 최대 7500 RPM으로 클램핑
        return Pair(gear, currentGearRpm.coerceIn(800f, 7500f))
    }

    fun toggleIgnition() {
        val willTurnOn = !_uiState.value.isIgnitionOn

        selfCheckJob?.cancel()
        simulationJob?.cancel()
        leftBlinkerJob?.cancel()
        rightBlinkerJob?.cancel()

        if (willTurnOn) {
            selfCheckJob = viewModelScope.launch {



                _uiState.update {
                    it.copy(
                        isIgnitionOn = true,
                        speed = 0,
                        rpm = 800f,
                        gear = "P",
                        gearNumber = 1,
                        isAutoCruiseOn = false,
                        isLeftBlinkerOn = true,
                        isRightBlinkerOn = true,
                        isEngineWarningOn = true,
                        isBatteryWarningOn = true,
                        isSeatbeltWarningOn = true
                    )
                }

                delay(150L)

                _uiState.update { current ->
                    current.copy(speed = 240, rpm = 8000f)
                }
                delay(800L)

                _uiState.update { current ->
                    current.copy(speed = 0, rpm = 0f)
                }
                delay(800L)

                _uiState.update { current ->
                    current.copy(speed = 0, rpm = 800f)
                }
                delay(1250L)

                _uiState.update { current ->
                    if (current.isIgnitionOn) {
                        current.copy(
                            isLeftBlinkerOn = false,
                            isRightBlinkerOn = false,
                            isEngineWarningOn = false,
                            isBatteryWarningOn = false,
                            isSeatbeltWarningOn = true
                        )
                    } else current
                }
            }
        } else {
            _uiState.value = ClusterState(isIgnitionOn = false)
        }
    }



    fun toggleAutoCruise() {
        if (!_uiState.value.isIgnitionOn) return

        val willRun = !_uiState.value.isAutoCruiseOn
        simulationJob?.cancel()

        _uiState.update { current ->
            current.copy(
                isAutoCruiseOn = willRun,
                gear = if (willRun) "D" else current.gear
            )
        }

        if (willRun) {
            simulationJob = viewModelScope.launch {
                var isAccelerating = true
                while (true) {
                    delay(50L)
                    _uiState.update { current ->
                        val delta = if (isAccelerating) 1 else -1
                        val nextSpeed = (current.speed + delta).coerceIn(0, 200)

                        if (nextSpeed >= 180) isAccelerating = false
                        if (nextSpeed <= 0) isAccelerating = true

                        val (gearNum, nextRpm) = calculateGearAndRpm(nextSpeed)
                        val nextOdo = current.odoKm + (nextSpeed * 0.000015) // 주행 속도에 비례해 거리 누적
                        current.copy(
                            speed = nextSpeed,
                            rpm = nextRpm,
                            gear = "D",
                            gearNumber = gearNum,
                            odoKm = nextOdo
                        )
                    }
                }
            }
        }
    }



    fun accelerate() {
        if (!_uiState.value.isIgnitionOn) return
        simulationJob?.cancel()

        _uiState.update { current ->
            val newSpeed = (current.speed + 15).coerceIn(0, 240)
            val (gearNum, nextRpm) = calculateGearAndRpm(newSpeed)
            val nextOdo = current.odoKm + 0.1 // 버튼 탭당 100m 누적

            current.copy(
                speed = newSpeed,
                rpm = nextRpm,
                gear = "D",
                gearNumber = gearNum,
                isAutoCruiseOn = false,
                odoKm = nextOdo
            )
        }
    }

    fun decelerate() {
        if (!_uiState.value.isIgnitionOn) return
        simulationJob?.cancel()
        _uiState.update { current ->
            val newSpeed = (current.speed - 15).coerceAtLeast(0)
            val (gearNum, nextRpm) = calculateGearAndRpm(newSpeed)

            current.copy(
                speed = newSpeed,
                rpm = nextRpm ,
                gearNumber = gearNum,
                isAutoCruiseOn = false
            )
        }
    }

    fun changeGear(newGear: String) {
        if (!_uiState.value.isIgnitionOn) return

        if (newGear != "D") {
            simulationJob?.cancel()
        }

        _uiState.update { current ->
            current.copy(
                gear = newGear,
                isAutoCruiseOn = if (newGear != "D") false else current.isAutoCruiseOn
            )
        }
    }

    fun toggleHazard() {
        val willTurnOn = !_uiState.value.isHazardOn

        leftBlinkerJob?.cancel()
        rightBlinkerJob?.cancel()

        _uiState.update { current ->
            current.copy(
                isHazardOn = willTurnOn,
                isLeftBlinkerOn = willTurnOn,
                isRightBlinkerOn = willTurnOn
            )
        }
    }

    fun toggleLeftBlinker() {
        if (_uiState.value.isHazardOn) return

        val willTurnOn = !_uiState.value.isLeftBlinkerOn

        leftBlinkerJob?.cancel()
        rightBlinkerJob?.cancel()

        _uiState.update { it.copy(
                isLeftBlinkerOn = willTurnOn,
                isRightBlinkerOn = false
            )
        }

        if (willTurnOn) {
            leftBlinkerJob = viewModelScope.launch {
                delay(5000L)
                _uiState.update { it.copy(isLeftBlinkerOn = false) }
            }
        }
    }

    fun toggleRightBlinker() {
        if (_uiState.value.isHazardOn) return

        val willTurnOn = !_uiState.value.isRightBlinkerOn

        leftBlinkerJob?.cancel()
        rightBlinkerJob?.cancel()

        _uiState.update { it.copy(
                isRightBlinkerOn = willTurnOn,
                isLeftBlinkerOn =  false
            )
        }

        if (willTurnOn) {
            rightBlinkerJob = viewModelScope.launch {
                delay(5000L)
                _uiState.update { it.copy(isRightBlinkerOn = false) }
            }
        }
    }

    fun toggleEngineWarning() {
        _uiState.update { it.copy(isEngineWarningOn = !it.isEngineWarningOn) }
    }

    fun toggleBatteryWarning() {
        _uiState.update { it.copy(isBatteryWarningOn = !it.isBatteryWarningOn) }
    }

    fun toggleSeatbeltWarning() {
        _uiState.update { it.copy(isSeatbeltWarningOn = !it.isSeatbeltWarningOn) }
    }

    fun toggleMidMode() {
        if (!_uiState.value.isIgnitionOn) return
        _uiState.update { current ->
            val nextMode = if (current.midMode == MidDisplayMode.TRIP) {
                MidDisplayMode.TPMS
            } else {
                MidDisplayMode.TRIP
            }
            current.copy(midMode = nextMode)
        }
    }
}