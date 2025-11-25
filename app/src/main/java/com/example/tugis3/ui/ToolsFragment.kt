package com.example.tugis3.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.appcompat.widget.Toolbar
import com.example.tugis3.R
import com.example.tugis3.ui.tools.*

class ToolsFragment : Fragment(R.layout.fragment_tools) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<Toolbar>(R.id.toolbar_tools)
        toolbar?.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }

        // Lokalizasyon
        view.findViewById<Button>(R.id.btn_localization)?.setOnClickListener {
            startActivity(Intent(requireContext(), LocalizationActivity::class.java))
        }

        // Koordinat Dönüştürücü
        view.findViewById<Button>(R.id.btn_coord_converter)?.setOnClickListener {
            startActivity(Intent(requireContext(), CoordinateConverterActivity::class.java))
        }

        // Açı Dönüştürücü
        view.findViewById<Button>(R.id.btn_angle_converter)?.setOnClickListener {
            // TODO: AngleConverterActivity eklenecek
            android.widget.Toast.makeText(requireContext(), "Yakında eklenecek", android.widget.Toast.LENGTH_SHORT).show()
        }

        // Çevre ve Alan
        view.findViewById<Button>(R.id.btn_area_calculation)?.setOnClickListener {
            startActivity(Intent(requireContext(), AreaCalculationActivity::class.java))
        }

        // COGO Hesaplama
        view.findViewById<Button>(R.id.btn_cogo)?.setOnClickListener {
            startActivity(Intent(requireContext(), CogoToolsActivity::class.java))
        }

        // Hesap Makinesi
        view.findViewById<Button>(R.id.btn_calculator)?.setOnClickListener {
            startActivity(Intent(requireContext(), CalculatorActivity::class.java))
        }

        // Harici Radyo
        view.findViewById<Button>(R.id.btn_external_radio)?.setOnClickListener {
            startActivity(Intent(requireContext(), ExternalRadioConfigActivity::class.java))
        }

        // Hacim Hesaplama
        view.findViewById<Button>(R.id.btn_volume)?.setOnClickListener {
            startActivity(Intent(requireContext(), VolumeCalculationActivity::class.java))
        }

        // Grid'den Zemine
        view.findViewById<Button>(R.id.btn_grid_to_ground)?.setOnClickListener {
            startActivity(Intent(requireContext(), GridToGroundActivity::class.java))
        }

        // FTP
        view.findViewById<Button>(R.id.btn_ftp)?.setOnClickListener {
            startActivity(Intent(requireContext(), FtpSharedDataActivity::class.java))
        }
    }
}

