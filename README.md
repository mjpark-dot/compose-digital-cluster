# 🚘 Compose Digital Instrument Cluster

**Jetpack Compose Canvas, StateFlow, 그리고 Low-level AudioTrack PCM 주파수 합성**을 결합하여 구현한 인터랙티브 자동차 디지털 클러스터(계기판) 시뮬레이터입니다.

실차 계기판(Instrument Cluster)의 시각적 정밀도와 주행 물리 역학, 그리고 릴레이·차임벨의 청각적 피드백을 외부 에셋이나 서드파티 게임 엔진 없이 순수 Kotlin과 Android 네이티브 API만으로 구현했습니다.

---

## ✨ Key Features

### 1. 듀얼 원형 게이지 & 레드존 렌더링 (`Compose Canvas`)
- **삼각함수 기반 정밀 좌표 연산:** 원형 아크(Arc)와 눈금(Tick), 숫자 라벨의 위치를 각도($\theta$) 기반 삼각함수($\cos, \sin$)로 계산하여 해상도에 독립적인 레이아웃 구현
- **고회전 레드존(Redline) 분리:** 타코미터 6,000 ~ 8,000 RPM 구간에 경고 색상 아크 및 눈금 하이라이트 적용
- **시동 셀프 체크(Needle Sweep):** 스마트키 시동(`ENGINE START`) 시 모든 경고등 점등(Bulb Check)과 함께 양쪽 바늘이 최대치까지 왕복 후 공회전(800 RPM)에 안착하는 3초 세리머니 연출

### 2. 6단 자동변속기 물리 엔진 & 오토 크루즈 (`StateFlow` & `Coroutines`)
- **속도-기어비-RPM 연동 수식:** 정지 아이들링(800 RPM)부터 1~6단 각 단수별 기어비 기울기와 진입 속도를 반영해, 변속 직후 RPM 드롭(Drop)과 재가속 리바운드 거동 재현
- **오토 크루즈 주행 시뮬레이션:** 비동기 코루틴 루프를 통한 가·감속 자동 주행 및 실시간 누적 주행거리(ODO) 연산

### 3. 인터랙티브 중앙 MID 트립 컴퓨터
- **멀티 스크린 터치 전환 (`Crossfade`):** 중앙 패널 탭 시 **트립 컴퓨터 모드**(외기 온도, 실시간 기어 단수 `D1~D6`, 평균 연비, ODO)와 **4륜 TPMS 모드**(차량 섀시 그래픽 및 개별 타이어 공기압 `PSI`) 간 부드러운 화면 전환

### 4. 순수 코드 기반 PCM 오디오 합성 (`AudioTrack`)
- 외부 음원(`.mp3`, `.wav`) 파일 없이 `44.1kHz` 샘플링 레이트의 PCM 파형을 직접 연산하여 즉각 재생
- **시동 웰컴 차임(Welcome Chime):** 지수 감쇄(Exponential Decay)를 적용한 3화음(`G4 → C5 → E5`) 사인파 합성
- **기계식 릴레이 사운드:** 고주파 정현파와 화이트 노이즈를 믹스하여 방향지시등 점등(`1350Hz` '딱')과 소등(`920Hz` '똑') 시 접점 타격감 재현

---

## 🛠 Tech Stack

| Category | Technology |
| :--- | :--- |
| **Language** | Kotlin |
| **UI & Graphics** | Jetpack Compose, Compose Canvas (`DrawScope`), Crossfade Animation |
| **Architecture** | MVVM, Unidirectional Data Flow (UDF) |
| **Concurrency & State** | Kotlin Coroutines, `StateFlow` |
| **Audio Engine** | Android `AudioTrack` (`MODE_STATIC` PCM Synthesis) |

---

## 🏗 Architecture & Project Structure

```text
com.example.myfirstapp
├── MainActivity.kt           # 메인 클러스터 화면, Canvas 게이지 렌더링, 점멸/사운드 동기화 루프
├── ClusterViewModel.kt       # 통합 상태(ClusterState) 관리, 6단 변속기 물리 연산, 시동/주행 코루틴 제어
├── CenterTripComputer.kt     # 중앙 MID 패널 (트립 컴퓨터 ↔ 4륜 TPMS 화면 전환 UI)
└── ClusterSoundManager.kt    # AudioTrack 기반 웰컴 차임 및 릴레이 사운드 합성 엔진
