package com.example.tugis3.ui.device

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.tugis3.R
import com.google.android.material.switchmaterial.SwitchMaterial

class BaseSettingsActivity : AppCompatActivity() {

    private lateinit var editBaseId: EditText
    private lateinit var spinnerStartupMode: Spinner
    private lateinit var spinnerRtkFormat: Spinner
    private lateinit var spinnerDataLink: Spinner
    private lateinit var switchAutoStart: SwitchMaterial
    private lateinit var switchStaticRecord: SwitchMaterial
    private lateinit var layoutStaticOptions: LinearLayout
    private lateinit var editPointName: EditText
    private lateinit var spinnerCollectionInterval: Spinner
    private lateinit var layoutCoordinateInput: LinearLayout
    private lateinit var editCoordX: EditText
    private lateinit var editCoordY: EditText
    private lateinit var editCoordZ: EditText
    private lateinit var editAntennaHeight: EditText
    private lateinit var spinnerAntennaMeasurementType: Spinner
    private lateinit var spinnerRadioChannel: Spinner
    private lateinit var editRadioFrequency: EditText
    private lateinit var btnApply: Button
    private lateinit var btnGetCurrentPosition: Button
    private lateinit var tvBaseStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base_settings)

        supportActionBar?.title = "Sabit İstasyon Ayarları"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initializeViews()
        setupSpinners()
        setupListeners()
        loadSavedSettings()
    }

    private fun initializeViews() {
        editBaseId = findViewById(R.id.edit_base_id)
        spinnerStartupMode = findViewById(R.id.spinner_startup_mode)
        spinnerRtkFormat = findViewById(R.id.spinner_rtk_format)
        spinnerDataLink = findViewById(R.id.spinner_base_data_link)
        switchAutoStart = findViewById(R.id.switch_auto_start)
        switchStaticRecord = findViewById(R.id.switch_static_record)
        layoutStaticOptions = findViewById(R.id.layout_static_options)
        editPointName = findViewById(R.id.edit_point_name)
        spinnerCollectionInterval = findViewById(R.id.spinner_collection_interval)
        layoutCoordinateInput = findViewById(R.id.layout_coordinate_input)
        editCoordX = findViewById(R.id.edit_coord_x)
        editCoordY = findViewById(R.id.edit_coord_y)
        editCoordZ = findViewById(R.id.edit_coord_z)
        editAntennaHeight = findViewById(R.id.edit_antenna_height)
        spinnerAntennaMeasurementType = findViewById(R.id.spinner_antenna_measurement_type)
        spinnerRadioChannel = findViewById(R.id.spinner_radio_channel)
        editRadioFrequency = findViewById(R.id.edit_radio_frequency)
        btnApply = findViewById(R.id.btn_apply_base)
        btnGetCurrentPosition = findViewById(R.id.btn_get_current_position)
        tvBaseStatus = findViewById(R.id.tv_base_status)
    }

    private fun setupSpinners() {
        // Başlangıç Modu
        val startupModes = arrayOf(
            "Bilinen Noktada Kurulum",
            "Herhangi Bir Noktada Kurulum",
            "Önceki Noktayı Kullan"
        )
        spinnerStartupMode.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, startupModes)

        // RTK Veri Formatı
        val rtkFormats = arrayOf("RTCM3.2", "RTCM3", "CMR", "CMR+", "DGPS")
        spinnerRtkFormat.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, rtkFormats)

        // Veri Bağlantısı
        val dataLinks = arrayOf("Dahili Radyo", "Harici Radyo", "WiFi-Harici Radyo", "Alıcı İnternet")
        spinnerDataLink.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, dataLinks)

        // Veri Toplama Aralığı
        val intervals = arrayOf("1HZ", "2HZ", "5HZ", "2s", "5s", "10s", "15s", "30s", "60s")
        spinnerCollectionInterval.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, intervals)

        // Anten Ölçüm Türü
        val antennaTypes = arrayOf(
            "Eğik Yükseklik Çizgisi",
            "Jalon Yüksekliği",
            "Faz Merkezinden Yükseklik",
            "Ölçüm Hattından Eğim Yüksekliği",
            "Cihazın Altından Yükseklik"
        )
        spinnerAntennaMeasurementType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, antennaTypes)

        // Radyo Kanalı
        val channels = arrayOf("Kanal 1", "Kanal 2", "Kanal 3", "Kanal 4", "Kanal 5", "Kanal 6", "Kanal 7", "Kanal 8 (Ayarlanabilir)")
        spinnerRadioChannel.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, channels)
    }

    private fun setupListeners() {
        // Başlangıç moduna göre koordinat giriş alanını göster/gizle
        spinnerStartupMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                layoutCoordinateInput.visibility = if (position == 0) android.view.View.VISIBLE else android.view.View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Statik kayıt açıksa seçenekleri göster
        switchStaticRecord.setOnCheckedChangeListener { _, isChecked ->
            layoutStaticOptions.visibility = if (isChecked) android.view.View.VISIBLE else android.view.View.GONE
        }

        // Mevcut pozisyonu al
        btnGetCurrentPosition.setOnClickListener {
            getCurrentPosition()
        }

        // Ayarları uygula
        btnApply.setOnClickListener {
            applyBaseSettings()
        }

        // Radyo kanalı seçimi - Kanal 8 için frekans girişini aktif et
        spinnerRadioChannel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                editRadioFrequency.isEnabled = position == 7 // Kanal 8
                if (position < 7) {
                    val fixedFrequencies = arrayOf("462.5", "463.0", "463.5", "464.0", "464.5", "465.0", "465.5")
                    editRadioFrequency.setText(fixedFrequencies[position])
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun getCurrentPosition() {
        // Simüle edilmiş mevcut pozisyon
        Toast.makeText(this, "Mevcut pozisyon alınıyor...", Toast.LENGTH_SHORT).show()

        android.os.Handler(mainLooper).postDelayed({
            editCoordX.setText("485068.1578")
            editCoordY.setText("4415705.6843")
            editCoordZ.setText("1127.3931")
            Toast.makeText(this, "Pozisyon alındı", Toast.LENGTH_SHORT).show()
        }, 1000)
    }

    private fun applyBaseSettings() {
        val baseId = editBaseId.text.toString()
        val startupMode = spinnerStartupMode.selectedItemPosition

        if (baseId.isEmpty()) {
            Toast.makeText(this, "Baz Kimliği giriniz", Toast.LENGTH_SHORT).show()
            return
        }

        if (startupMode == 0) { // Bilinen Noktada Kurulum
            if (editCoordX.text.isEmpty() || editCoordY.text.isEmpty() || editCoordZ.text.isEmpty()) {
                Toast.makeText(this, "Koordinatları giriniz", Toast.LENGTH_SHORT).show()
                return
            }
        }

        if (editAntennaHeight.text.isEmpty()) {
            Toast.makeText(this, "Anten yüksekliğini giriniz", Toast.LENGTH_SHORT).show()
            return
        }

        saveSettings()

        // Sabit istasyonu başlat
        tvBaseStatus.text = "✓ Sabit İstasyon Aktif - RTK Yayını Başladı"
        tvBaseStatus.setTextColor(getColor(android.R.color.holo_green_dark))

        Toast.makeText(this, "Sabit istasyon başlatıldı - Baz ID: $baseId", Toast.LENGTH_LONG).show()
    }

    private fun saveSettings() {
        val prefs = getSharedPreferences("base_settings", MODE_PRIVATE)
        prefs.edit().apply {
            putString("base_id", editBaseId.text.toString())
            putInt("startup_mode", spinnerStartupMode.selectedItemPosition)
            putInt("rtk_format", spinnerRtkFormat.selectedItemPosition)
            putInt("data_link", spinnerDataLink.selectedItemPosition)
            putBoolean("auto_start", switchAutoStart.isChecked)
            putBoolean("static_record", switchStaticRecord.isChecked)
            putString("point_name", editPointName.text.toString())
            putInt("collection_interval", spinnerCollectionInterval.selectedItemPosition)
            putString("coord_x", editCoordX.text.toString())
            putString("coord_y", editCoordY.text.toString())
            putString("coord_z", editCoordZ.text.toString())
            putString("antenna_height", editAntennaHeight.text.toString())
            putInt("antenna_measurement_type", spinnerAntennaMeasurementType.selectedItemPosition)
            putInt("radio_channel", spinnerRadioChannel.selectedItemPosition)
            putString("radio_frequency", editRadioFrequency.text.toString())
            apply()
        }
    }

    private fun loadSavedSettings() {
        val prefs = getSharedPreferences("base_settings", MODE_PRIVATE)
        editBaseId.setText(prefs.getString("base_id", "1000"))
        spinnerStartupMode.setSelection(prefs.getInt("startup_mode", 0))
        spinnerRtkFormat.setSelection(prefs.getInt("rtk_format", 0))
        spinnerDataLink.setSelection(prefs.getInt("data_link", 0))
        switchAutoStart.isChecked = prefs.getBoolean("auto_start", false)
        switchStaticRecord.isChecked = prefs.getBoolean("static_record", false)
        editPointName.setText(prefs.getString("point_name", "BASE1"))
        spinnerCollectionInterval.setSelection(prefs.getInt("collection_interval", 0))
        editCoordX.setText(prefs.getString("coord_x", ""))
        editCoordY.setText(prefs.getString("coord_y", ""))
        editCoordZ.setText(prefs.getString("coord_z", ""))
        editAntennaHeight.setText(prefs.getString("antenna_height", "1.56"))
        spinnerAntennaMeasurementType.setSelection(prefs.getInt("antenna_measurement_type", 0))
        spinnerRadioChannel.setSelection(prefs.getInt("radio_channel", 0))
        editRadioFrequency.setText(prefs.getString("radio_frequency", "462.5"))
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

