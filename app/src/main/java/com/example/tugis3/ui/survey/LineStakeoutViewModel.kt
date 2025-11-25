package com.example.tugis3.ui.survey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tugis3.model.CADPoint
import com.example.tugis3.model.LineStakeout
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.*

/**
 * Hat Aplikasyonu için ViewModel
 * GPS konumu, hat hesaplamaları ve aplikasyon mantığını yönetir
 */
@HiltViewModel
class LineStakeoutViewModel @Inject constructor(
    // GPS servisi buraya inject edilecek
    // private val gpsService: GpsService
) : ViewModel() {

    private val _uiState = MutableStateFlow(LineStakeoutUiState())
    val uiState: StateFlow<LineStakeoutUiState> = _uiState.asStateFlow()

    init {
        loadAvailableLines()
        startGpsUpdates()
    }

    /**
     * Mevcut hatları yükle
     */
    private fun loadAvailableLines() {
        viewModelScope.launch {
            // Gerçek uygulamada database'den yüklenecek
            val sampleLines = listOf(
                LineStakeout(
                    id = "line_1",
                    name = "Ana Hat 1",
                    startPoint = CADPoint(1000.0, 1000.0, 100.0),
                    endPoint = CADPoint(1100.0, 1100.0, 105.0)
                ),
                LineStakeout(
                    id = "line_2",
                    name = "Tali Hat A",
                    startPoint = CADPoint(2000.0, 2000.0, 95.0),
                    endPoint = CADPoint(2080.0, 2020.0, 98.0)
                ),
                LineStakeout(
                    id = "line_3",
                    name = "Kanal Hattı",
                    startPoint = CADPoint(500.0, 500.0, 110.0),
                    endPoint = CADPoint(700.0, 600.0, 115.0)
                )
            )

            _uiState.update { it.copy(availableLines = sampleLines) }
        }
    }

    /**
     * GPS güncellemelerini başlat
     */
    private fun startGpsUpdates() {
        viewModelScope.launch {
            // Simüle edilmiş GPS güncellemeleri
            // Gerçek uygulamada LocationManager kullanılacak
            _uiState.update {
                it.copy(
                    gpsEnabled = true,
                    satelliteCount = 12,
                    accuracy = 2.5
                )
            }
        }
    }

    /**
     * Hat seç
     */
    fun selectLine(line: LineStakeout) {
        _uiState.update {
            it.copy(
                currentLine = line,
                targetStation = 0.0
            )
        }
        updateLineCalculations()
    }

    /**
     * Mevcut konumu güncelle (GPS'ten gelecek)
     */
    fun updateCurrentPosition(point: CADPoint) {
        _uiState.update { it.copy(currentPosition = point) }
        updateLineCalculations()
    }

    /**
     * Hat hesaplamalarını güncelle
     */
    private fun updateLineCalculations() {
        val state = _uiState.value
        val line = state.currentLine ?: return
        val position = state.currentPosition

        // Hatta en yakın noktayı bul
        val closest = findClosestPointOnLine(
            position,
            line.startPoint,
            line.endPoint
        )

        // Hat boyunca mesafeyi hesapla
        val distanceAlongLine = calculateDistanceAlongLine(
            closest,
            line.startPoint,
            line.endPoint
        )

        // Hattan ofseti hesapla
        val offsetFromLine = calculateDistance(position, closest)
        val offsetSign = calculateOffsetSign(
            position,
            line.startPoint,
            line.endPoint
        )

        _uiState.update {
            it.copy(
                closestPointOnLine = closest,
                distanceAlongLine = distanceAlongLine,
                offsetFromLine = offsetFromLine * offsetSign
            )
        }
    }

    /**
     * Hatta en yakın noktayı bul
     */
    private fun findClosestPointOnLine(
        point: CADPoint,
        lineStart: CADPoint,
        lineEnd: CADPoint
    ): CADPoint {
        val dx = lineEnd.x - lineStart.x
        val dy = lineEnd.y - lineStart.y
        val lengthSquared = dx * dx + dy * dy

        if (lengthSquared == 0.0) return lineStart

        val t = ((point.x - lineStart.x) * dx + (point.y - lineStart.y) * dy) / lengthSquared
        val tClamped = t.coerceIn(0.0, 1.0)

        return CADPoint(
            x = lineStart.x + tClamped * dx,
            y = lineStart.y + tClamped * dy,
            z = lineStart.z + tClamped * (lineEnd.z - lineStart.z)
        )
    }

    /**
     * Hat boyunca mesafeyi hesapla
     */
    private fun calculateDistanceAlongLine(
        point: CADPoint,
        lineStart: CADPoint,
        lineEnd: CADPoint
    ): Double {
        return calculateDistance(lineStart, point)
    }

    /**
     * İki nokta arası mesafe
     */
    private fun calculateDistance(p1: CADPoint, p2: CADPoint): Double {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * Ofset işaretini hesapla (sol: -, sağ: +)
     */
    private fun calculateOffsetSign(
        point: CADPoint,
        lineStart: CADPoint,
        lineEnd: CADPoint
    ): Double {
        // Çapraz çarpım ile sol/sağ belirleme
        val crossProduct = (lineEnd.x - lineStart.x) * (point.y - lineStart.y) -
                          (lineEnd.y - lineStart.y) * (point.x - lineStart.x)
        return if (crossProduct >= 0) 1.0 else -1.0
    }

    /**
     * Belirli bir istasyona git
     */
    fun navigateToStation(station: Double) {
        _uiState.update {
            it.copy(
                targetStation = station,
                isNavigating = true
            )
        }
    }

    /**
     * Navigasyonu durdur
     */
    fun stopNavigation() {
        _uiState.update { it.copy(isNavigating = false) }
    }

    /**
     * Aplikasyonu kaydet
     */
    fun recordStakeout() {
        val state = _uiState.value
        val line = state.currentLine ?: return

        val record = LineStakeoutRecord(
            station = state.distanceAlongLine,
            offset = state.offsetFromLine,
            measuredPoint = state.currentPosition
        )

        _uiState.update {
            it.copy(
                completedStakeouts = it.completedStakeouts + record
            )
        }

        // Gerçek uygulamada database'e kaydet
        viewModelScope.launch {
            // saveToDatabase(record)
        }
    }

    /**
     * Ofset mesafesini ayarla
     */
    fun setOffsetDistance(distance: Double) {
        _uiState.update { it.copy(offsetDistance = distance) }
    }

    /**
     * İstasyon aralığını ayarla
     */
    fun setStationInterval(interval: Double) {
        _uiState.update { it.copy(stationInterval = interval) }
    }

    override fun onCleared() {
        super.onCleared()
        // GPS güncellemelerini durdur
        // gpsService.stopUpdates()
    }
}

