package com.example.tugis3.ui.survey

import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tugis3.coord.transform.NoOpTransformer
import com.example.tugis3.coord.transform.ProjectionEngine
import com.example.tugis3.data.db.entity.PointEntity
import com.example.tugis3.data.repository.PointRepository
import com.example.tugis3.data.repository.ProjectRepository
import com.example.tugis3.data.repository.SurveyPointRepository
import com.example.tugis3.gnss.GnssEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@HiltViewModel
class ARViewModel @Inject constructor(
    app: Application,
    private val projectRepo: ProjectRepository,
    private val pointRepo: PointRepository,
    private val surveyPointRepo: SurveyPointRepository,
    private val gnss: GnssEngine
) : AndroidViewModel(app), SensorEventListener {

    data class Target(
        val name: String,
        val e: Double,
        val n: Double,
        val z: Double?
    )

    data class ARState(
        val hasFix: Boolean = false,
        val fixLabel: String = "",
        val lat: Double? = null,
        val lon: Double? = null,
        val ellH: Double? = null,
        val curE: Double? = null,
        val curN: Double? = null,
        val curBearingDeg: Double? = null,
        val target: Target? = null,
        val distance: Double? = null,
        val bearingToTarget: Double? = null,
        val bearingDiff: Double? = null,
        val message: String = "Hedef seçiniz",
        val canCapture: Boolean = false
    )

    private val sensorManager = app.getSystemService(SensorManager::class.java)
    private var lastAzimuth: Double? = null
    private val rot = FloatArray(9)
    private val orient = FloatArray(3)
    private var haveMag = false
    private var haveAcc = false
    private val accValues = FloatArray(3)
    private val magValues = FloatArray(3)

    private val _manualTarget = MutableStateFlow<Target?>(null)
    private val _selectedPointName = MutableStateFlow<String?>(null)

    val activeProject = projectRepo.observeActiveProject()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val observation = gnss.observation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val projectPoints: StateFlow<List<PointEntity>> = activeProject
        .flatMapLatest { p -> if (p == null) flowOf(emptyList()) else pointRepo.observePoints(p.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val points: StateFlow<List<PointEntity>> = projectPoints

    private val _state = MutableStateFlow(ARState())
    val state: StateFlow<ARState> = _state

    init {
        startEngine()
        registerSensors()
        viewModelScope.launch {
            combine(observation, activeProject, _manualTarget, _selectedPointName, points) { obs, proj, manual, selName, pts ->
                Triple(obs, proj, resolveTarget(manual, selName, pts))
            }.collect { (obs, _, tgt) -> recompute(obs, tgt) }
        }
    }

    private fun resolveTarget(manual: Target?, selName: String?, pts: List<PointEntity>): Target? {
        manual?.let { return it }
        if (selName == null) return null
        val p = pts.firstOrNull { it.name == selName } ?: return null
        return Target(p.name, p.easting, p.northing, p.ellipsoidalHeight)
    }

    fun setManualTarget(e: Double?, n: Double?, z: Double?) {
        if (e==null || n==null) { _manualTarget.value = null; return }
        _manualTarget.value = Target("MANUAL", e, n, z)
        _selectedPointName.value = null
    }
    fun selectPoint(name: String) { _selectedPointName.value = name; _manualTarget.value = null }
    fun clearTarget() { _selectedPointName.value = null; _manualTarget.value = null }

    private fun recompute(obsRaw: com.example.tugis3.gnss.model.GnssObservation?, target: Target?) {
        val obs = obsRaw
        if (obs == null || obs.latDeg==null || obs.lonDeg==null) {
            _state.value = ARState(message = "GNSS bekleniyor")
            return
        }
        val proj = activeProject.value
        val transformer = if (proj != null) ProjectionEngine.forProject(proj) else NoOpTransformer
        val (easting, northing) = try {
            if (transformer !== NoOpTransformer) transformer.forward(obs.latDeg, obs.lonDeg) else (obs.lonDeg * 111000) to (obs.latDeg * 111000)
        } catch (_:Exception) { (obs.lonDeg*111000) to (obs.latDeg*111000) }
        val curBearing = lastAzimuth?.let { (it + 360) % 360 }
        var distance: Double? = null
        var bearingTo: Double? = null
        var bearingDiff: Double? = null
        if (target != null) {
            val dE = target.e - easting
            val dN = target.n - northing
            distance = hypot(dE,dN)
            bearingTo = (Math.toDegrees(atan2(dE, dN)) + 360) % 360
            if (curBearing != null) {
                var diff = bearingTo - curBearing
                while (diff > 180) diff -= 360
                while (diff < -180) diff += 360
                bearingDiff = diff
            }
        }
        val canCap = distance!=null && distance < 0.10 && (bearingDiff==null || kotlin.math.abs(bearingDiff) < 5)
        _state.value = ARState(
            hasFix = true,
            fixLabel = obs.fixType.name,
            lat = obs.latDeg,
            lon = obs.lonDeg,
            ellH = obs.ellipsoidalHeight,
            curE = easting,
            curN = northing,
            curBearingDeg = curBearing,
            target = target,
            distance = distance,
            bearingToTarget = bearingTo,
            bearingDiff = bearingDiff,
            canCapture = canCap,
            message = when {
                target == null -> "Hedef seçiniz"
                distance == null -> "Hesaplanıyor"
                canCap -> "✅ Hedefe ulaşıldı"
                else -> "Δ=${String.format("%.2f", distance)}m  yöndev=${bearingDiff?.let { String.format("%.1f°", it) } ?: "-"}"
            }
        )
    }

    fun captureStake() {
        val st = _state.value
        val proj = activeProject.value ?: return
        val tgt = st.target ?: return
        if (!st.canCapture) return
        viewModelScope.launch {
            surveyPointRepo.insert(
                com.example.tugis3.data.db.entity.SurveyPointEntity(
                    projectId = proj.id,
                    name = (tgt.name + "_AR" + System.currentTimeMillis().toString().takeLast(3)),
                    code = "AR_STK",
                    latitude = st.lat,
                    longitude = st.lon,
                    elevation = st.ellH,
                    northing = st.curN,
                    easting = st.curE,
                    zone = proj.utmZone?.let { it.toString()+ if (proj.utmNorthHemisphere) "N" else "S" },
                    hrms = observation.value?.hrms,
                    vrms = observation.value?.vrms,
                    pdop = observation.value?.pdop,
                    satellites = observation.value?.satellitesInUse,
                    fixType = observation.value?.fixType?.name,
                    antennaHeight = null,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    private fun registerSensors() {
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        } ?: run {
            // fallback: orientation from accel+mag
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    fun startEngine() = gnss.start()
    fun stopEngine() = gnss.stop()

    override fun onCleared() {
        sensorManager.unregisterListener(this)
        stopEngine()
        super.onCleared()
    }

    override fun onSensorChanged(event: SensorEvent) {
        when(event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rot, event.values)
                SensorManager.getOrientation(rot, orient)
                val az = Math.toDegrees(orient[0].toDouble())
                lastAzimuth = (az + 360) % 360
                recompute(observation.value, resolveTarget(_manualTarget.value,_selectedPointName.value, points.value))
            }
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values,0,accValues,0,3); haveAcc = true; if (haveMag) updateAccMag()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values,0,magValues,0,3); haveMag = true; if (haveAcc) updateAccMag()
            }
        }
    }

    private fun updateAccMag() {
        if (SensorManager.getRotationMatrix(rot, null, accValues, magValues)) {
            SensorManager.getOrientation(rot, orient)
            val az = Math.toDegrees(orient[0].toDouble())
            lastAzimuth = (az + 360) % 360
            recompute(observation.value, resolveTarget(_manualTarget.value,_selectedPointName.value, points.value))
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }
}
