package com.example.tugis3.ui

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.appcompat.widget.Toolbar
import com.example.tugis3.R
import com.example.tugis3.ui.device.BaseSettingsActivity
import com.example.tugis3.ui.device.RoverSettingsActivity
import com.example.tugis3.ui.device.StaticSettingsActivity
import com.google.android.material.card.MaterialCardView

class DeviceFragment : Fragment(R.layout.fragment_device) {

    private lateinit var connectionStatusTextView: TextView
    private var isConnected = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<Toolbar>(R.id.toolbar_device)
        toolbar?.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }

        connectionStatusTextView = view.findViewById(R.id.tv_connection_status)

        // İletişim butonları
        view.findViewById<Button>(R.id.btn_connect_device)?.setOnClickListener {
            connectToDevice()
        }

        // Gezici modu ayarları
        view.findViewById<Button>(R.id.btn_rover_settings)?.setOnClickListener {
            startActivity(Intent(requireContext(), RoverSettingsActivity::class.java))
        }

        // Sabit modu ayarları
        view.findViewById<Button>(R.id.btn_base_settings)?.setOnClickListener {
            startActivity(Intent(requireContext(), BaseSettingsActivity::class.java))
        }

        // Statik modu ayarları
        view.findViewById<Button>(R.id.btn_static_settings)?.setOnClickListener {
            startActivity(Intent(requireContext(), StaticSettingsActivity::class.java))
        }

        // Cihaz bilgileri
        view.findViewById<Button>(R.id.btn_device_info)?.setOnClickListener {
            showDeviceInfo()
        }

        // Bluetooth cihazlarını listele
        view.findViewById<Button>(R.id.btn_bluetooth_list)?.setOnClickListener {
            startActivity(Intent(requireContext(), com.example.tugis3.ui.device.BluetoothListActivity::class.java))
        }
    }

    private fun connectToDevice() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(requireContext(), getString(R.string.bluetooth_not_supported), Toast.LENGTH_SHORT).show()
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(requireContext(), getString(R.string.bluetooth_enable_msg), Toast.LENGTH_SHORT).show()
            return
        }
        isConnected = !isConnected
        if (isConnected) {
            connectionStatusTextView.text = getString(R.string.connection_status_connected)
            Toast.makeText(requireContext(), getString(R.string.bluetooth_connected), Toast.LENGTH_SHORT).show()
        } else {
            connectionStatusTextView.text = getString(R.string.connection_status_disconnected)
            Toast.makeText(requireContext(), getString(R.string.bluetooth_disconnected), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeviceInfo() {
        val deviceInfo = """
            Cihaz Modeli: eSurvey E600
            Firmware: v4.2.1
            Seri No: E600351920028
            GPS: Aktif (12 uydu)
            GLONASS: Aktif (8 uydu)
            BeiDou: Aktif (6 uydu)
            Galileo: Aktif (5 uydu)
            Toplam Uydu: 31
            Batarya: %87
            RTK Durumu: Fixed
            HDOP: 0.8
            Doğruluk: ±1 cm
        """.trimIndent()

        Toast.makeText(requireContext(), deviceInfo, Toast.LENGTH_LONG).show()
    }
}
