package com.example.tugis3.ui.survey

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.tugis3.R

class SurveyPointFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_survey_point, container, false)
        val switchMap = view.findViewById<Switch>(R.id.switchMap)
        val mapContainer = view.findViewById<FrameLayout>(R.id.mapContainer)
        val btnTakePoint = view.findViewById<Button>(R.id.btnTakePoint)
        val txtNorthing = view.findViewById<TextView>(R.id.txtNorthing)
        val txtEasting = view.findViewById<TextView>(R.id.txtEasting)
        val txtHeight = view.findViewById<TextView>(R.id.txtHeight)
        val txtRefDistance = view.findViewById<TextView>(R.id.txtRefDistance)
        val txtAntennaHeight = view.findViewById<TextView>(R.id.txtAntennaHeight)

        // Harita gösterimi toggle
        switchMap.setOnCheckedChangeListener { _, isChecked ->
            mapContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Nokta alımı örnek veriyle güncelle
        btnTakePoint.setOnClickListener {
            // Gerçek GNSS/konum verisiyle entegre edilebilir
            txtNorthing.text = "N: 456789.12"
            txtEasting.text = "E: 123456.34"
            txtHeight.text = "Z: 32.5"
            txtRefDistance.text = "Referans Uzaklık: 1.2m"
            txtAntennaHeight.text = "Anten Yüksekliği: 2.0m"
        }
        return view
    }
}
