package com.example.tugis3.ui.device

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.tugis3.R
import com.google.android.material.switchmaterial.SwitchMaterial

class StaticSettingsActivity : AppCompatActivity() {

    private lateinit var editPointName: EditText
    private lateinit var editPdopLimit: EditText
    private lateinit var spinnerElevationAngle: Spinner
    private lateinit var spinnerCollectionInterval: Spinner
    private lateinit var switchAutoRecord: SwitchMaterial
    private lateinit var editAntennaHeight: EditText
    private lateinit var spinnerAntennaType: Spinner
    private lateinit var btnApply: Button
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var tvRecordStatus: TextView
    private lateinit var tvRecordDuration: TextView

    private var isRecording = false
    private var startTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_static_settings)

        supportActionBar?.title = "Statik Ölçüm Ayarları"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initializeViews()
        setupSpinners()
        setupListeners()
        loadSavedSettings()
    }

    private fun initializeViews() {
        editPointName = findViewById(R.id.edit_static_point_name)
        editPdopLimit = findViewById(R.id.edit_pdop_limit)
        spinnerElevationAngle = findViewById(R.id.spinner_static_elevation_angle)
        spinnerCollectionInterval = findViewById(R.id.spinner_static_collection_interval)
        switchAutoRecord = findViewById(R.id.switch_auto_record)
        editAntennaHeight = findViewById(R.id.edit_static_antenna_height)
        spinnerAntennaType = findViewById(R.id.spinner_static_antenna_type)
        btnApply = findViewById(R.id.btn_apply_static)
        btnStart = findViewById(R.id.btn_start_static)
        btnStop = findViewById(R.id.btn_stop_static)
        tvRecordStatus = findViewById(R.id.tv_record_status)
        tvRecordDuration = findViewById(R.id.tv_record_duration)
    }

    private fun setupSpinners() {
        // Yükseklik Açısı
        val elevationAngles = arrayOf("5°", "10°", "15°", "20°")
        spinnerElevationAngle.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, elevationAngles)

        // Toplama Aralığı
        val intervals = arrayOf("1HZ", "2HZ", "5HZ", "2s", "5s", "10s", "15s", "30s", "60s")
        spinnerCollectionInterval.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, intervals)

        // Anten Ölçüm Türü
        val antennaTypes = arrayOf(
            "Faz Merkezinden Ölçülen Yükseklik",
            "Ölçüm Hattından Eğim Yüksekliği",
            "Ölçüm Hattından Yükseklik",
            "Altimetri Parçasından Eğim Yüksekliği",
            "Cihazın Altından Yükseklik"
        )
        spinnerAntennaType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, antennaTypes)
    }

    private fun setupListeners() {
        btnApply.setOnClickListener {
            saveSettings()
            Toast.makeText(this, "Ayarlar kaydedildi", Toast.LENGTH_SHORT).show()
        }

        btnStart.setOnClickListener {
            startStaticRecording()
        }

        btnStop.setOnClickListener {
            stopStaticRecording()
        }
    }

    private fun startStaticRecording() {
        val pointName = editPointName.text.toString()

        if (pointName.isEmpty()) {
            Toast.makeText(this, "Nokta adı giriniz", Toast.LENGTH_SHORT).show()
            return
        }

        if (editAntennaHeight.text.isEmpty()) {
            Toast.makeText(this, "Anten yüksekliğini giriniz", Toast.LENGTH_SHORT).show()
            return
        }

        isRecording = true
        startTime = System.currentTimeMillis()

        btnStart.isEnabled = false
        btnStop.isEnabled = true

        tvRecordStatus.text = "✓ Statik Kayıt Aktif"
        tvRecordStatus.setTextColor(getColor(android.R.color.holo_green_dark))

        Toast.makeText(this, "Statik kayıt başlatıldı: $pointName", Toast.LENGTH_LONG).show()

        // Süre sayacını başlat
        updateDuration()
    }

    private fun stopStaticRecording() {
        if (!isRecording) return

        isRecording = false

        btnStart.isEnabled = true
        btnStop.isEnabled = false

        tvRecordStatus.text = "Statik Kayıt Durdu"
        tvRecordStatus.setTextColor(getColor(android.R.color.darker_gray))

        val duration = (System.currentTimeMillis() - startTime) / 1000
        val hours = duration / 3600
        val minutes = (duration % 3600) / 60
        val seconds = duration % 60

        Toast.makeText(
            this,
            "Kayıt tamamlandı\nSüre: ${hours}s ${minutes}d ${seconds}s",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun updateDuration() {
        if (!isRecording) return

        val duration = (System.currentTimeMillis() - startTime) / 1000
        val hours = duration / 3600
        val minutes = (duration % 3600) / 60
        val seconds = duration % 60

        tvRecordDuration.text = "Kayıt Süresi: ${hours}s ${minutes}d ${seconds}s"

        // Her saniye güncelle
        android.os.Handler(mainLooper).postDelayed({
            updateDuration()
        }, 1000)
    }

    private fun saveSettings() {
        val prefs = getSharedPreferences("static_settings", MODE_PRIVATE)
        prefs.edit().apply {
            putString("point_name", editPointName.text.toString())
            putString("pdop_limit", editPdopLimit.text.toString())
            putInt("elevation_angle", spinnerElevationAngle.selectedItemPosition)
            putInt("collection_interval", spinnerCollectionInterval.selectedItemPosition)
            putBoolean("auto_record", switchAutoRecord.isChecked)
            putString("antenna_height", editAntennaHeight.text.toString())
            putInt("antenna_type", spinnerAntennaType.selectedItemPosition)
            apply()
        }
    }

    private fun loadSavedSettings() {
        val prefs = getSharedPreferences("static_settings", MODE_PRIVATE)
        editPointName.setText(prefs.getString("point_name", "STATIC1"))
        editPdopLimit.setText(prefs.getString("pdop_limit", "15.0"))
        spinnerElevationAngle.setSelection(prefs.getInt("elevation_angle", 1))
        spinnerCollectionInterval.setSelection(prefs.getInt("collection_interval", 0))
        switchAutoRecord.isChecked = prefs.getBoolean("auto_record", false)
        editAntennaHeight.setText(prefs.getString("antenna_height", "1.56"))
        spinnerAntennaType.setSelection(prefs.getInt("antenna_type", 0))
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

