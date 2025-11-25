package com.example.tugis3.ui.project

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.tugis3.coord.transform.ProjectionEngine
import com.google.maps.android.data.kml.KmlLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.zip.ZipInputStream
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.roundToInt

class AplikasyonActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private var userLocation: LatLng? = null
    private var targetLocation: LatLng? = null
    // KML/KMZ bekletme
    private var pendingKmlBytes: ByteArray? = null
    private var pendingKmlName: String? = null
    private var lastToastTime = 0L
    private val TOAST_INTERVAL = 3000L // 3 saniyede bir Toast göster
    private lateinit var directionText: TextView
    private lateinit var distanceText: TextView
    private lateinit var fusedClient: com.google.android.gms.location.FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    private val openKmlLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { handleKmlKmzUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Harita fragmenti ekleniyor
        val mapFragment = SupportMapFragment.newInstance()
        supportFragmentManager.beginTransaction().replace(android.R.id.content, mapFragment).commit()
        mapFragment.getMapAsync(this)

        // Fused Location init
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        // Alt yön/meseafe paneli (overlay)
        val overlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 16, 24, 24)
            setBackgroundColor(0xAA000000.toInt()) // yarı saydam siyah
        }
        directionText = TextView(this).apply { setTextColor(0xFFFFFFFF.toInt()); textSize = 16f; text = "Yön: -" }
        distanceText = TextView(this).apply { setTextColor(0xFFFFFFFF.toInt()); textSize = 16f; text = "Mesafe: -" }
        // Katman toggle butonu
        val layerBtn = Button(this).apply {
            text = "Katman: Uydu"
            setOnClickListener {
                if (!::mMap.isInitialized) return@setOnClickListener
                mMap.mapType = when (mMap.mapType) {
                    GoogleMap.MAP_TYPE_HYBRID -> { text = "Katman: Normal"; GoogleMap.MAP_TYPE_NORMAL }
                    GoogleMap.MAP_TYPE_NORMAL -> { text = "Katman: Uydu"; GoogleMap.MAP_TYPE_HYBRID }
                    GoogleMap.MAP_TYPE_SATELLITE -> { text = "Katman: Normal"; GoogleMap.MAP_TYPE_NORMAL }
                    GoogleMap.MAP_TYPE_TERRAIN -> { text = "Katman: Uydu"; GoogleMap.MAP_TYPE_HYBRID }
                    else -> { text = "Katman: Uydu"; GoogleMap.MAP_TYPE_HYBRID }
                }
            }
        }
        val importBtn = Button(this).apply {
            text = "KML/KMZ Aç"
            setOnClickListener {
                // KML ve KMZ için MIME tipleri + yedekler
                openKmlLauncher.launch(arrayOf(
                    "application/vnd.google-earth.kml+xml",
                    "application/vnd.google-earth.kmz",
                    "application/zip",
                    "application/octet-stream",
                    "*/*"
                ))
            }
        }
        overlay.addView(directionText)
        overlay.addView(distanceText)
        overlay.addView(layerBtn)
        overlay.addView(importBtn)
        val lp = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = android.view.Gravity.BOTTOM or android.view.Gravity.START
            leftMargin = 16; bottomMargin = 32
        }
        addContentView(overlay, lp)

        // Intent ile gelen koordinatları al ve hedefi çözümle
        val northing = intent.getDoubleExtra("northing", 0.0)
        val easting = intent.getDoubleExtra("easting", 0.0)
        val height = intent.getDoubleExtra("height", 0.0)
        val name = intent.getStringExtra("name") ?: "Hedef Nokta"
        val epsg = if (intent.hasExtra("epsgCode")) intent.getIntExtra("epsgCode", 0).takeIf { it > 0 } else null
        val utmZone = if (intent.hasExtra("utmZone")) intent.getIntExtra("utmZone", 0).takeIf { it in 1..60 } else null
        val utmNorth = if (intent.hasExtra("utmNorthHemisphere")) intent.getBooleanExtra("utmNorthHemisphere", true) else null
        android.util.Log.d("AplikasyonActivity", "Nokta: $name, N: $northing, E: $easting, H: $height, EPSG:$epsg, UTM:$utmZone ${utmNorth ?: '-'}")

        // 1) Eğer lat/lon doğrudan geldiyse onları kullan (en sağlamı)
        val latIn = intent.getDoubleExtra("latDeg", Double.NaN)
        val lonIn = intent.getDoubleExtra("lonDeg", Double.NaN)
        targetLocation = if (!latIn.isNaN() && !lonIn.isNaN()) {
            android.widget.Toast.makeText(this, "Enlem/Boylam doğrudan kullanıldı.", android.widget.Toast.LENGTH_SHORT).show()
            LatLng(latIn, lonIn)
        } else {
            resolveTargetLatLng(northing, easting, epsg, utmZone, utmNorth)
        }
        // Hedef belirlendiyse ve elimizde kullanıcı konumu varsa paneli güncelle
        userLocation?.let { u -> targetLocation?.let { t -> updateDirectionPanel(u, t) } }
        if (targetLocation == null) {
            android.widget.Toast.makeText(this, "Koordinatlar çözümlenemedi. Lütfen projeksiyon bilgilerini kontrol edin.", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // Varsayılan: Uydu + yol üstlük (HYBRID)
        mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
        // Önce hedef noktayı göster
        targetLocation?.let {
            mMap.clear()
            mMap.addMarker(MarkerOptions().position(it).title("Hedef Nokta"))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 17f))
        }
        // MyLocation sadece izin varsa aç (sadece görsel nokta için)
        val fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            startLocationUpdates()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }

        // Haritaya tıklayınca hedefi değiştir
        mMap.setOnMapClickListener { latLng ->
            targetLocation = latLng
            mMap.clear()
            mMap.addMarker(MarkerOptions().position(latLng).title("Hedef Nokta"))
            userLocation?.let { u -> updateDirectionPanel(u, latLng) }
        }

        // Bekleyen KML varsa şimdi yükle
        pendingKmlBytes?.let { bytes ->
            KmlLayer(mMap, bytes.inputStream(), this).apply { addLayerToMap() }
            android.widget.Toast.makeText(this, "KML/KMZ yüklendi: ${pendingKmlName ?: "(seçim)"}", android.widget.Toast.LENGTH_SHORT).show()
            pendingKmlBytes = null; pendingKmlName = null
        }
    }

    override fun onResume() {
        super.onResume()
        val fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if ((fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) && ::mMap.isInitialized) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        stopLocationUpdates()
        super.onPause()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (::mMap.isInitialized) {
                try { mMap.isMyLocationEnabled = true } catch (_: SecurityException) {}
            }
            startLocationUpdates()
        }
    }

    private fun startLocationUpdates() {
        if (locationCallback != null) return
        val fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) return
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(700L)
            .setWaitForAccurateLocation(false)
            .build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                userLocation = LatLng(loc.latitude, loc.longitude)
                targetLocation?.let { t -> updateDirectionPanel(userLocation!!, t) }
            }
        }
        fusedClient.requestLocationUpdates(req, locationCallback!!, Looper.getMainLooper())
    }

    private fun stopLocationUpdates() {
        locationCallback?.let { fusedClient.removeLocationUpdates(it) }
        locationCallback = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }

    // --- Yardımcılar ---
    private fun isInTurkey(lat: Double, lon: Double): Boolean = lat in 35.0..43.5 && lon in 25.0..45.5

    private fun resolveTargetLatLng(n: Double, e: Double, epsg: Int?, zone: Int?, northHem: Boolean?): LatLng? {
        // Önce UTM parametrelerini hazırla (TM başarısız olursa kullanılacak)
        val hemi = when {
            northHem != null -> northHem
            epsg != null && epsg in 32601..32660 -> true
            epsg != null && epsg in 32701..32760 -> false
            else -> true // bilinmiyorsa kuzey varsay
        }
        val z = when {
            zone != null -> zone
            epsg != null && (epsg in 32601..32660 || epsg in 32701..32760) -> epsg % 100
            else -> null
        }

        // A) TM (Gauss-Krüger) parametreleri sağlandıysa (ör. DOM 30) -> ters dönüşüm uygula
        val hasTm = intent.hasExtra("tm_centralMeridianDeg")
        if (hasTm) {
            val cm = intent.getDoubleExtra("tm_centralMeridianDeg", Double.NaN)
            if (!cm.isNaN()) {
                val lat0 = intent.getDoubleExtra("tm_latOriginDeg", 0.0)
                val k0 = intent.getDoubleExtra("tm_scaleFactor", 1.0)
                val fe = intent.getDoubleExtra("tm_falseE", 500000.0)
                val fn = intent.getDoubleExtra("tm_falseN", 0.0)
                val a = intent.getDoubleExtra("semiMajorA", 6378137.0)
                val invF = intent.getDoubleExtra("invFlattening", 298.257222101)
                val tm = ProjectionEngine.createTransverseMercator(
                    semiMajor = a,
                    invFlattening = invF,
                    centralMeridianDeg = cm,
                    latOriginDeg = lat0,
                    scaleFactor = k0,
                    falseE = fe,
                    falseN = fn
                )
                // Önce (e,n) dene, TR sınırı dışındaysa (n,e) swap dene
                val direct = runCatching { tm.inverse(e, n) }.getOrNull()
                val swapped = runCatching { tm.inverse(n, e) }.getOrNull()
                val directInTr = direct?.let { isInTurkey(it.first, it.second) } == true
                val swappedInTr = swapped?.let { isInTurkey(it.first, it.second) } == true
                when {
                    directInTr -> {
                        android.widget.Toast.makeText(this, "TM → WGS84 (doğrudan) uygulandı.", android.widget.Toast.LENGTH_SHORT).show()
                        return LatLng(direct!!.first, direct.second)
                    }
                    swappedInTr -> {
                        android.widget.Toast.makeText(this, "TM → WGS84: X/Y swap uygulandı.", android.widget.Toast.LENGTH_SHORT).show()
                        return LatLng(swapped!!.first, swapped.second)
                    }
                    else -> {
                        // TM sonuçları TR dışında kaldı; eğer UTM bilgisi mevcutsa onu dene ve TR içindeyse onu tercih et
                        if (z != null) {
                            val utm1 = runCatching { utmToLatLon(n, e, z, hemi) }.getOrNull()
                            val utm2 = runCatching { utmToLatLon(e, n, z, hemi) }.getOrNull()
                            val utm1InTr = utm1?.let { isInTurkey(it.latitude, it.longitude) } == true
                            val utm2InTr = utm2?.let { isInTurkey(it.latitude, it.longitude) } == true
                            when {
                                utm1InTr -> {
                                    android.widget.Toast.makeText(this, "UTM → WGS84 dönüşümü uygulandı (Z$z ${if (hemi) 'N' else 'S'}).", android.widget.Toast.LENGTH_SHORT).show()
                                    return utm1
                                }
                                utm2InTr -> {
                                    android.widget.Toast.makeText(this, "UTM → WGS84 (X/Y swap) uygulandı (Z$z ${if (hemi) 'N' else 'S'}).", android.widget.Toast.LENGTH_SHORT).show()
                                    return utm2
                                }
                                direct != null -> {
                                    android.widget.Toast.makeText(this, "TM → WGS84 uygulandı (TR kutusu dışında).", android.widget.Toast.LENGTH_SHORT).show()
                                    return LatLng(direct.first, direct.second)
                                }
                                swapped != null -> {
                                    android.widget.Toast.makeText(this, "TM → WGS84 uygulandı (swap, TR kutusu dışında).", android.widget.Toast.LENGTH_SHORT).show()
                                    return LatLng(swapped.first, swapped.second)
                                }
                            }
                        } else if (direct != null) {
                            android.widget.Toast.makeText(this, "TM → WGS84 uygulandı (TR kutusu dışında).", android.widget.Toast.LENGTH_SHORT).show()
                            return LatLng(direct.first, direct.second)
                        } else if (swapped != null) {
                            android.widget.Toast.makeText(this, "TM → WGS84 uygulandı (swap, TR kutusu dışında).", android.widget.Toast.LENGTH_SHORT).show()
                            return LatLng(swapped.first, swapped.second)
                        }
                    }
                }
            }
        }

        // B) Doğrudan lat/lon aralığı gibi görünüyorsa (ama TM yoksa) - çok küçük veri setlerinde yardımcı olur
        if (n in -90.0..90.0 && e in -180.0..180.0) return LatLng(n, e)
        if (e in -90.0..90.0 && n in -180.0..180.0) {
            android.widget.Toast.makeText(this, "Koordinatlar X/Y ters algılandı; swap uygulandı.", android.widget.Toast.LENGTH_SHORT).show()
            return LatLng(e, n)
        }

        // B2) Otomatik DOM 3° (Türkiye) denemesi: TM parametresi yoksa ve E,N aralıkları uygunsa
        if (!hasTm && e in 200000.0..800000.0 && n in 3000000.0..6000000.0) {
            tryDomCandidates(n, e)?.let { cand ->
                android.widget.Toast.makeText(this, "TM → WGS84 (otomatik DOM 3°) uygulandı.", android.widget.Toast.LENGTH_SHORT).show()
                return cand
            }
        }

        // C) UTM ise dönüştür (zone varsa en sağlıklısı) — swap kontrolü ile
        if (z != null) {
            val utm1 = runCatching { utmToLatLon(n, e, z, hemi) }.getOrNull()
            val utm2 = runCatching { utmToLatLon(e, n, z, hemi) }.getOrNull()
            val utm1InTr = utm1?.let { isInTurkey(it.latitude, it.longitude) } == true
            val utm2InTr = utm2?.let { isInTurkey(it.latitude, it.longitude) } == true
            return when {
                utm1InTr -> {
                    android.widget.Toast.makeText(this, "UTM → WGS84 dönüşümü uygulandı (Z$z ${if (hemi) 'N' else 'S'}).", android.widget.Toast.LENGTH_SHORT).show()
                    utm1
                }
                utm2InTr -> {
                    android.widget.Toast.makeText(this, "UTM → WGS84 (X/Y swap) uygulandı (Z$z ${if (hemi) 'N' else 'S'}).", android.widget.Toast.LENGTH_SHORT).show()
                    utm2
                }
                utm1 != null -> {
                    android.widget.Toast.makeText(this, "UTM → WGS84 uygulandı (TR kutusu dışında).", android.widget.Toast.LENGTH_SHORT).show()
                    utm1
                }
                else -> utm2 // her ikisi de TR dışında ise mevcut olanı döndür (null olabilir)
            }
        }
        return null
    }

    // Türkiye için yaygın DOM 3° merkez meridyenleri: 27,30,33,36 (gerektiğinde 39,42 eklenebilir)
    private fun tryDomCandidates(n: Double, e: Double): LatLng? {
        val cms = listOf(27.0, 30.0, 33.0, 36.0)
        val a = 6378137.0 // GRS80/WGS84
        val invF = 298.257222101
        val lat0 = 0.0
        val k0 = 1.0
        val fe = 500000.0
        val fn = 0.0
        for (cm in cms) {
            val tm = ProjectionEngine.createTransverseMercator(
                semiMajor = a,
                invFlattening = invF,
                centralMeridianDeg = cm,
                latOriginDeg = lat0,
                scaleFactor = k0,
                falseE = fe,
                falseN = fn
            )
            val direct = runCatching { tm.inverse(e, n) }.getOrNull()
            if (direct != null && isInTurkey(direct.first, direct.second)) return LatLng(direct.first, direct.second)
            val swapped = runCatching { tm.inverse(n, e) }.getOrNull()
            if (swapped != null && isInTurkey(swapped.first, swapped.second)) return LatLng(swapped.first, swapped.second)
        }
        // Türkiye dışında olsa da ilk başarılı sonucu döndür (doğrudan ya da swap)
        for (cm in cms) {
            val tm = ProjectionEngine.createTransverseMercator(a, invF, cm, lat0, k0, fe, fn)
            runCatching { tm.inverse(e, n) }.getOrNull()?.let { return LatLng(it.first, it.second) }
            runCatching { tm.inverse(n, e) }.getOrNull()?.let { return LatLng(it.first, it.second) }
        }
        return null
    }

    private fun updateDirectionPanel(user: LatLng, target: LatLng) {
        val results = FloatArray(1)
        Location.distanceBetween(user.latitude, user.longitude, target.latitude, target.longitude, results)
        val distance = results[0].roundToInt()
        val bearing = calculateBearing(user, target)
        val direction = getDirectionFromBearing(bearing)
        directionText.text = "Yön: $direction"
        distanceText.text = "Mesafe: ${distance} m"
    }

    // KML/KMZ dosyası açma ve haritaya ekleme
    private fun handleKmlKmzUri(uri: Uri) {
        lifecycleScope.launch {
            runCatching {
                val (name, bytes) = withContext(Dispatchers.IO) {
                    val name = getFileNameFromUri(uri) ?: uri.lastPathSegment.orEmpty()
                    val isKmz = name.endsWith(".kmz", ignoreCase = true) || name.endsWith(".zip", ignoreCase = true)
                    val bytes = contentResolver.openInputStream(uri)?.use { inStream ->
                        if (isKmz) {
                            ZipInputStream(inStream).use { zis ->
                                var entry = zis.nextEntry
                                var found: ByteArray? = null
                                while (entry != null) {
                                    if (!entry.isDirectory && entry.name.endsWith(".kml", ignoreCase = true)) {
                                        found = zis.readBytes(); break
                                    }
                                    entry = zis.nextEntry
                                }
                                requireNotNull(found) { "KMZ içinde KML girdisi bulunamadı" }
                            }
                        } else inStream.readBytes()
                    } ?: error("Akış açılamadı")
                    name to bytes
                }
                if (!::mMap.isInitialized) {
                    pendingKmlBytes = bytes
                    pendingKmlName = name
                    android.widget.Toast.makeText(this@AplikasyonActivity, "Harita hazırlanıyor, KML/KMZ yükleme sıraya alındı.", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    withContext(Dispatchers.Main) {
                        KmlLayer(mMap, bytes.inputStream(), this@AplikasyonActivity).apply { addLayerToMap() }
                        android.widget.Toast.makeText(this@AplikasyonActivity, "KML/KMZ yüklendi: ${name}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }.onFailure {
                android.widget.Toast.makeText(this@AplikasyonActivity, "KML/KMZ yükleme hatası: ${it.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        return try {
            val cursor = contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            cursor?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        } catch (t: Throwable) { null }
    }

    // WGS84 UTM → Lat/Lon (yaklaşık, metre girdiler; e: 100k-900k, n: 0-10,000,000)
    private fun utmToLatLon(northing: Double, easting: Double, zone: Int, northernHemisphere: Boolean): LatLng {
        // Sabitler (WGS84)
        val a = 6378137.0
        val f = 1 / 298.257223563
        val k0 = 0.9996
        val e2 = f * (2 - f)
        val e1sq = e2 / (1 - e2)
        val x = easting - 500000.0
        var y = northing
        if (!northernHemisphere) y -= 10000000.0
        val m = y / k0
        val mu = m / (a * (1 - e2 / 4 - 3 * e2 * e2 / 64 - 5 * e2 * e2 * e2 / 256))
        val e1 = (1 - Math.sqrt(1 - e2)) / (1 + Math.sqrt(1 - e2))
        val j1 = 3 * e1 / 2 - 27 * e1 * e1 * e1 / 32
        val j2 = 21 * e1 * e1 / 16 - 55 * e1 * e1 * e1 * e1 / 32
        val j3 = 151 * e1 * e1 * e1 / 96
        val j4 = 1097 * e1 * e1 * e1 * e1 / 512
        val fp = mu + j1 * Math.sin(2 * mu) + j2 * Math.sin(4 * mu) + j3 * Math.sin(6 * mu) + j4 * Math.sin(8 * mu)
        val sinfp = Math.sin(fp)
        val cosfp = Math.cos(fp)
        val tanfp = Math.tan(fp)
        val c1 = e1sq * cosfp * cosfp
        val t1 = tanfp * tanfp
        val r1 = a * (1 - e2) / Math.pow(1 - e2 * sinfp * sinfp, 1.5)
        val n1 = a / Math.sqrt(1 - e2 * sinfp * sinfp)
        val d = x / (n1 * k0)
        val q1 = n1 * tanfp / r1
        val q2 = (d * d) / 2
        val q3 = (5 + 3 * t1 + 10 * c1 - 4 * c1 * c1 - 9 * e1sq) * Math.pow(d, 4.0) / 24
        val q4 = (61 + 90 * t1 + 298 * c1 + 45 * t1 * t1 - 252 * e1sq - 3 * c1 * c1) * Math.pow(d, 6.0) / 720
        val lat = fp - q1 * (q2 - q3 + q4)
        val q5 = d
        val q6 = (1 + 2 * t1 + c1) * Math.pow(d, 3.0) / 6
        val q7 = (5 - 2 * c1 + 28 * t1 - 3 * c1 * c1 + 8 * e1sq + 24 * t1 * t1) * Math.pow(d, 5.0) / 120
        val lon = (q5 - q6 + q7) / cosfp
        val lonOrigin = (zone - 1) * 6 - 180 + 3
        val latDeg = Math.toDegrees(lat)
        val lonDeg = lonOrigin + Math.toDegrees(lon)
        return LatLng(latDeg, lonDeg)
    }

    // Yön ve mesafe hesaplama fonksiyonu - Toast gösterme sınırlandırıldı
    private fun showDirection(user: LatLng, target: LatLng) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastToastTime < TOAST_INTERVAL) {
            return // Çok sık Toast gösterme
        }
        lastToastTime = currentTime

        val results = FloatArray(1)
        Location.distanceBetween(user.latitude, user.longitude, target.latitude, target.longitude, results)
        val distance = results[0].roundToInt()
        val bearing = calculateBearing(user, target)
        val direction = getDirectionFromBearing(bearing)
        android.widget.Toast.makeText(this, "Yön: $direction, Mesafe: $distance metre", android.widget.Toast.LENGTH_SHORT).show()
    }

    // İki nokta arasındaki yön açısını hesaplar
    private fun calculateBearing(start: LatLng, end: LatLng): Double {
        val lat1 = Math.toRadians(start.latitude)
        val lon1 = Math.toRadians(start.longitude)
        val lat2 = Math.toRadians(end.latitude)
        val lon2 = Math.toRadians(end.longitude)
        val dLon = lon2 - lon1
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        val brng = atan2(y, x)
        return (Math.toDegrees(brng) + 360) % 360
    }

    // Yön açısına göre Kuzey-Güney-Doğu-Batı bilgisini döndürür
    private fun getDirectionFromBearing(bearing: Double): String {
        return when {
            bearing >= 45 && bearing < 135 -> "Doğu"
            bearing >= 135 && bearing < 225 -> "Güney"
            bearing >= 225 && bearing < 315 -> "Batı"
            else -> "Kuzey"
        }
    }
}
