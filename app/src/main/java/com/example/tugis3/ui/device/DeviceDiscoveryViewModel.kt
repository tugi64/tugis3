package com.example.tugis3.ui.device

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tugis3.bluetooth.BluetoothGnssManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Sadece discovery (tarama) ve eşleştirme mantığını örnekleyen hafif ViewModel.
 */
class DeviceDiscoveryViewModel : ViewModel() {
    private val bt = BluetoothGnssManager()
    private val devicesMap = mutableMapOf<String, BluetoothDevice>()

    data class UiDevice(val mac: String, val name: String, val bonded: Boolean)

    private val _devices = MutableStateFlow<List<UiDevice>>(emptyList())
    val devices: StateFlow<List<UiDevice>> = _devices

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning

    private val _log = MutableStateFlow("")
    val log: StateFlow<String> = _log

    private fun log(msg: String) { _log.update { it + msg + "\n" } }

    @SuppressLint("MissingPermission")
    fun start(context: Context) {
        if (_scanning.value) return
        if (!bt.isBluetoothSupported()) { log("Destek yok"); return }
        if (!bt.isEnabled()) { log("Bluetooth kapalı"); return }
        _scanning.value = true
        devicesMap.clear(); _devices.value = emptyList()
        bt.startScan(context, object : BluetoothGnssManager.ScanCallback {
            override fun onDeviceFound(device: BluetoothDevice) { add(device) }
            override fun onFinished() { _scanning.value = false; log("Tarama bitti") }
            override fun onError(message: String) { _scanning.value = false; log("Hata: $message") }
        })
    }

    fun stop(context: Context) { if (_scanning.value) bt.stopScan(context) }

    @SuppressLint("MissingPermission")
    fun pair(mac: String) {
        val d = devicesMap[mac] ?: return
        val bonded = try { d.bondState == BluetoothDevice.BOND_BONDED } catch (se: SecurityException) { false }
        if (bonded) { log("Zaten eşleşmiş"); return }
        val ok = bt.pairDevice(d)
        log(if (ok) "Eşleştirme isteği gönderildi" else "Eşleştirme isteği başarısız")
    }

    @SuppressLint("MissingPermission")
    private fun add(device: BluetoothDevice) {
        val key = device.address ?: return
        devicesMap[key] = device
        val list = devicesMap.values.map { dev ->
            val bonded = try { dev.bondState == BluetoothDevice.BOND_BONDED } catch (_: SecurityException) { false }
            UiDevice(dev.address, dev.name ?: "(Adsız)", bonded)
        }.sortedBy { it.name }
        _devices.value = list
    }

    @SuppressLint("MissingPermission")
    fun refreshBondState(device: BluetoothDevice) { add(device) }

    override fun onCleared() {
        viewModelScope.launch { }
        super.onCleared()
    }
}
