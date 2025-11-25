package com.example.tugis3.ui.survey

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SurveyConfigurationViewModel @Inject constructor() : ViewModel() {

    // RTK Radio Configuration
    private val _rtkChannel = MutableStateFlow(8)
    val rtkChannel: StateFlow<Int> = _rtkChannel

    private val _rtkFrequency = MutableStateFlow(448.25)
    val rtkFrequency: StateFlow<Double> = _rtkFrequency

    private val _rtkProtocol = MutableStateFlow("TrimMark III(19200)")
    val rtkProtocol: StateFlow<String> = _rtkProtocol

    // Antenna Configuration
    private val _antennaHeight = MutableStateFlow(1.56)
    val antennaHeight: StateFlow<Double> = _antennaHeight

    private val _antennaMeasureType = MutableStateFlow("Faz Merkezinden Ölçülen Yükseklik")
    val antennaMeasureType: StateFlow<String> = _antennaMeasureType

    // Static Configuration
    private val _pdopLimit = MutableStateFlow(18.0)
    val pdopLimit: StateFlow<Double> = _pdopLimit

    private val _elevationAngle = MutableStateFlow(10)
    val elevationAngle: StateFlow<Int> = _elevationAngle

    private val _collectInterval = MutableStateFlow("1HZ")
    val collectInterval: StateFlow<String> = _collectInterval

    private val _staticAutoRecord = MutableStateFlow(true)
    val staticAutoRecord: StateFlow<Boolean> = _staticAutoRecord

    // GNSS Status
    private val _gnssStatus = MutableStateFlow("RTK Fix")
    val gnssStatus: StateFlow<String> = _gnssStatus

    private val _satelliteCount = MutableStateFlow(12)
    val satelliteCount: StateFlow<Int> = _satelliteCount

    private val _hdop = MutableStateFlow(0.8)
    val hdop: StateFlow<Double> = _hdop

    private val _vdop = MutableStateFlow(1.2)
    val vdop: StateFlow<Double> = _vdop

    private val _pdop = MutableStateFlow(1.4)
    val pdop: StateFlow<Double> = _pdop

    private val _batteryLevel = MutableStateFlow(95)
    val batteryLevel: StateFlow<Int> = _batteryLevel

    // Dropdown Lists
    val channelList = (1..20).toList()
    val protocolList = listOf(
        "TrimMark III(19200)",
        "TrimMark III(9600)",
        "TrimMark III(4800)",
        "TrimTalk 450s",
        "RTCM 2.3",
        "RTCM 3.0",
        "RTCM 3.1",
        "RTCM 3.2"
    )

    // Update Functions
    fun updateRtkChannel(channel: Int) {
        _rtkChannel.value = channel
    }

    fun updateRtkFrequency(frequency: Double) {
        _rtkFrequency.value = frequency
    }

    fun updateRtkProtocol(protocol: String) {
        _rtkProtocol.value = protocol
    }

    fun updateAntennaHeight(height: Double) {
        _antennaHeight.value = height
    }

    fun updateAntennaMeasureType(type: String) {
        _antennaMeasureType.value = type
    }

    fun updatePdopLimit(limit: Double) {
        _pdopLimit.value = limit
    }

    fun updateElevationAngle(angle: Int) {
        _elevationAngle.value = angle
    }

    fun updateCollectInterval(interval: String) {
        _collectInterval.value = interval
    }

    fun updateStaticAutoRecord(autoRecord: Boolean) {
        _staticAutoRecord.value = autoRecord
    }

    fun updateGnssStatus(status: String) {
        _gnssStatus.value = status
    }

    fun updateSatelliteCount(count: Int) {
        _satelliteCount.value = count
    }

    fun updateHdop(hdop: Double) {
        _hdop.value = hdop
    }

    fun updateVdop(vdop: Double) {
        _vdop.value = vdop
    }

    fun updatePdop(pdop: Double) {
        _pdop.value = pdop
    }

    fun updateBatteryLevel(level: Int) {
        _batteryLevel.value = level
    }
}
