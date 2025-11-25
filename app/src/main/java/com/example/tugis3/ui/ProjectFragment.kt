package com.example.tugis3.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.appcompat.widget.Toolbar
import com.example.tugis3.R

class ProjectFragment : Fragment(R.layout.fragment_project) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toolbar = view.findViewById<Toolbar>(R.id.toolbar_project)
        toolbar?.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }

        // Proje Yöneticisi
        view.findViewById<Button>(R.id.btn_project_manager)?.setOnClickListener {
            Toast.makeText(requireContext(), "Proje Yöneticisi açılıyor...", Toast.LENGTH_SHORT).show()
            // Proje yöneticisi ekranı açılacak
        }

        // Koordinat Sistemi
        view.findViewById<Button>(R.id.btn_coordinate_system)?.setOnClickListener {
            showCoordinateSystemDialog()
        }

        // Kalibrasyon
        view.findViewById<Button>(R.id.btn_calibration)?.setOnClickListener {
            Toast.makeText(requireContext(), "Nokta Kalibrasyonu başlatılıyor...", Toast.LENGTH_SHORT).show()
        }

        // Nokta Veritabanı
        view.findViewById<Button>(R.id.btn_point_database)?.setOnClickListener {
            Toast.makeText(requireContext(), "Nokta Veritabanı açılıyor...", Toast.LENGTH_SHORT).show()
        }

        // Dışa Aktar
        view.findViewById<Button>(R.id.btn_export)?.setOnClickListener {
            Toast.makeText(requireContext(), "Dosya dışa aktarılıyor...", Toast.LENGTH_SHORT).show()
        }

        // İçe Aktar
        view.findViewById<Button>(R.id.btn_import)?.setOnClickListener {
            Toast.makeText(requireContext(), "Dosya içe aktarılıyor...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCoordinateSystemDialog() {
        val coordinateSystems = arrayOf(
            "WGS84/TM27",
            "WGS84/TM30",
            "WGS84/TM33",
            "WGS84/TM36",
            "WGS84/TM39",
            "WGS84/TM42",
            "ITRF96/TM30",
            "ED50/UTM zone 35",
            "ED50/UTM zone 36",
            "ED50/UTM zone 37"
        )

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Koordinat Sistemi Seçin")
            .setItems(coordinateSystems) { _, which ->
                Toast.makeText(
                    requireContext(),
                    "Seçilen: ${coordinateSystems[which]}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("İptal", null)
            .show()
    }
}
