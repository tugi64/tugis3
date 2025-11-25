package com.example.tugis3.ui.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tugis3.model.GnssDeviceSettings
import com.example.tugis3.model.GnssReceivingMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GnssDeviceViewModel @Inject constructor() : ViewModel() {

    private val _deviceSettings = MutableStateFlow(GnssDeviceSettings())
    val deviceSettings: StateFlow<GnssDeviceSettings> = _deviceSettings.asStateFlow()

    private val _currentReceivingMode = MutableStateFlow(GnssReceivingMode.STANDARD)
    val currentReceivingMode: StateFlow<GnssReceivingMode> = _currentReceivingMode.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _currentEpoch = MutableStateFlow(0)
    val currentEpoch: StateFlow<Int> = _currentEpoch.asStateFlow()

    private val _sessionTimer = MutableStateFlow(0)
    val sessionTimer: StateFlow<Int> = _sessionTimer.asStateFlow()

    private val _hrmsValue = MutableStateFlow(0.0)
    val hrmsValue: StateFlow<Double> = _hrmsValue.asStateFlow()

    private val _vrmsValue = MutableStateFlow(0.0)
    val vrmsValue: StateFlow<Double> = _vrmsValue.asStateFlow()

    private val _pdopValue = MutableStateFlow(0.0)
    val pdopValue: StateFlow<Double> = _pdopValue.asStateFlow()

    fun updateReceivingMode(mode: GnssReceivingMode) {
        _currentReceivingMode.value = mode
        _deviceSettings.value = _deviceSettings.value.copy(receivingMode = mode)
    }

    fun updateDeviceSettings(settings: GnssDeviceSettings) {
        _deviceSettings.value = settings
    }

    fun updateHrmsLimit(limit: Double) {
        _deviceSettings.value = _deviceSettings.value.copy(hrmsLimit = limit)
    }

    fun updateVrmsLimit(limit: Double) {
        _deviceSettings.value = _deviceSettings.value.copy(vrmsLimit = limit)
    }

    fun updatePdopLimit(limit: Double) {
        _deviceSettings.value = _deviceSettings.value.copy(pdopLimit = limit)
    }

    fun updatePointName(name: String) {
        _deviceSettings.value = _deviceSettings.value.copy(pointName = name)
    }

    fun updatePointIncrement(increment: Int) {
        _deviceSettings.value = _deviceSettings.value.copy(pointIncrement = increment)
    }

    fun updateSessionDuration(duration: Int) {
        _deviceSettings.value = _deviceSettings.value.copy(sessionDuration = duration)
    }

    fun updateEpochCount(count: Int) {
        _deviceSettings.value = _deviceSettings.value.copy(epochCount = count)
    }

    fun toggleSameOperation() {
        _deviceSettings.value = _deviceSettings.value.copy(
            useSameOperation = !_deviceSettings.value.useSameOperation
        )
    }

    fun toggleFocusLastPoint() {
        _deviceSettings.value = _deviceSettings.value.copy(
            focusLastPoint = !_deviceSettings.value.focusLastPoint
        )
    }

    fun startRecording() {
        _isRecording.value = true
        _currentEpoch.value = 0
        _sessionTimer.value = 0

        viewModelScope.launch {
            when (_currentReceivingMode.value) {
                GnssReceivingMode.STANDARD -> handleStandardMode()
                GnssReceivingMode.EPOCH -> handleEpochMode()
                GnssReceivingMode.FAST -> handleFastMode()
                GnssReceivingMode.AUTOMATIC -> handleAutomaticMode()
            }
        }
    }

    fun stopRecording() {
        _isRecording.value = false
        _currentEpoch.value = 0
        _sessionTimer.value = 0
    }

    private suspend fun handleStandardMode() {
        // Standart mod - koordinat değerleri ve hata oranları gösterilir
        // Kullanıcı manuel olarak kayıt yapar
    }

    private suspend fun handleEpochMode() {
        // Epoklu mod - 10 epok okuyarak grafik gösterim
        val targetEpochs = _deviceSettings.value.epochCount
        repeat(targetEpochs) { epoch ->
            if (!_isRecording.value) return
            _currentEpoch.value = epoch + 1
            kotlinx.coroutines.delay(1000) // 1 saniye bekle
        }
        if (_isRecording.value) {
            stopRecording()
        }
    }

    private suspend fun handleFastMode() {
        // Hızlı mod - direkt kayıt
        kotlinx.coroutines.delay(500)
        if (_isRecording.value) {
            stopRecording()
        }
    }

    private suspend fun handleAutomaticMode() {
        // Otomatik mod - kriterlere göre otomatik durdur
        val sessionDuration = _deviceSettings.value.sessionDuration
        repeat(sessionDuration) { second ->
            if (!_isRecording.value) return
            _sessionTimer.value = second + 1
            kotlinx.coroutines.delay(1000)

            // Kriterleri kontrol et
            if (checkAutomaticCriteria()) {
                stopRecording()
                return
            }
        }
        if (_isRecording.value) {
            stopRecording()
        }
    }

    private fun checkAutomaticCriteria(): Boolean {
        val settings = _deviceSettings.value
        return _hrmsValue.value <= settings.hrmsLimit &&
                _vrmsValue.value <= settings.vrmsLimit &&
                _pdopValue.value <= settings.pdopLimit
    }

    fun updateRealTimeValues(hrms: Double, vrms: Double, pdop: Double) {
        _hrmsValue.value = hrms
        _vrmsValue.value = vrms
        _pdopValue.value = pdop
    }

    fun resetToDefaults() {
        _deviceSettings.value = GnssDeviceSettings()
        _currentReceivingMode.value = GnssReceivingMode.STANDARD
    }
}
