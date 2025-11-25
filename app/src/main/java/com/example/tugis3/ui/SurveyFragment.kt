package com.example.tugis3.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.appcompat.widget.Toolbar
import com.example.tugis3.R
import com.example.tugis3.ui.survey.*

class SurveyFragment : Fragment(R.layout.fragment_survey) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<Toolbar>(R.id.toolbar_survey)
        toolbar?.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }

        // Nokta Alımı (Point Survey)
        view.findViewById<Button>(R.id.btn_point_survey)?.setOnClickListener {
            startActivity(Intent(requireContext(), PointSurveyActivity::class.java))
        }

        // Hızlı Alım (Quick Survey)
        view.findViewById<Button>(R.id.btn_quick_survey)?.setOnClickListener {
            startActivity(Intent(requireContext(), QuickPointSurveyActivity::class.java))
        }

        // Grafik Alım (Graphical Survey)
        view.findViewById<Button>(R.id.btn_graphical_survey)?.setOnClickListener {
            startActivity(Intent(requireContext(), GraphicalSurveyActivity::class.java))
        }

        // Detay Alımı (Detail Survey)
        view.findViewById<Button>(R.id.btn_detail_survey)?.setOnClickListener {
            startActivity(Intent(requireContext(), DetailSurveyActivity::class.java))
        }

        // CAD
        view.findViewById<Button>(R.id.btn_cad)?.setOnClickListener {
            startActivity(Intent(requireContext(), com.example.tugis3.ui.map.MapActivity::class.java))
        }

        // Nokta Aplikasyonu (Point Stakeout)
        view.findViewById<Button>(R.id.btn_point_stakeout)?.setOnClickListener {
            startActivity(Intent(requireContext(), PointStakeoutActivity::class.java))
        }

        // Hat Aplikasyonu (Line Stakeout)
        view.findViewById<Button>(R.id.btn_line_stakeout)?.setOnClickListener {
            startActivity(Intent(requireContext(), LineStakeoutActivity::class.java))
        }

        // Yol Aplikasyonu (Road Stakeout)
        view.findViewById<Button>(R.id.btn_road_stakeout)?.setOnClickListener {
            startActivity(Intent(requireContext(), RoadStakeoutActivity::class.java))
        }

        // GIS Veri Toplama
        view.findViewById<Button>(R.id.btn_gis_collection)?.setOnClickListener {
            startActivity(Intent(requireContext(), GisDataCollectionActivity::class.java))
        }

        // Kesit Ölçümü (Cross Section)
        view.findViewById<Button>(R.id.btn_cross_section)?.setOnClickListener {
            startActivity(Intent(requireContext(), CrossSectionMeasurementActivity::class.java))
        }

        // Statik Ölçüm
        view.findViewById<Button>(R.id.btn_static_survey)?.setOnClickListener {
            startActivity(Intent(requireContext(), StaticSurveyActivity::class.java))
        }

        // Katman Ayarları
        view.findViewById<Button>(R.id.btn_layer_settings)?.setOnClickListener {
            startActivity(Intent(requireContext(), LayerSettingsActivity::class.java))
        }
    }
}

