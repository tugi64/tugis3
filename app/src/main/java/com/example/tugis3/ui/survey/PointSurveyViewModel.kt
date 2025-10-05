package com.example.tugis3.ui.survey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tugis3.coord.transform.ProjectionEngine
import com.example.tugis3.data.db.entity.SurveyPointEntity
import com.example.tugis3.data.repository.ProjectRepository
import com.example.tugis3.data.repository.SurveyPointRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PointSurveyViewModel @Inject constructor(
    private val projectRepo: ProjectRepository,
    private val surveyPointRepo: SurveyPointRepository
) : ViewModel() {

    val activeProject = projectRepo.observeActiveProject()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Son 50 kayıt (timestamp DESC zaten DAO'da) -> take(50)
    val recentPoints: StateFlow<List<SurveyPointEntity>> = activeProject
        .flatMapLatest { p -> if (p == null) flowOf(emptyList()) else surveyPointRepo.observePoints(p.id) }
        .map { it.take(50) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun savePoint(
        name: String,
        code: String?,
        latitude: Double?,
        longitude: Double?,
        elevation: Double?,
        northing: Double?,
        easting: Double?,
        zone: String?,
        hrms: Double?,
        vrms: Double?,
        pdop: Double?,
        satellites: Int?,
        fixType: String?,
        antennaHeight: Double?
    ) {
        val project = activeProject.value ?: return
        viewModelScope.launch {
            var n = northing
            var e = easting
            var z = zone
            if (latitude != null && longitude != null) {
                val transformer = ProjectionEngine.forProject(project)
                // Eğer kullanıcı dışarıdan northing/easting vermediyse veya NoOp değilse hesapla
                if (n == null || e == null || transformer !== com.example.tugis3.coord.transform.NoOpTransformer) {
                    runCatching {
                        val (tx, ty) = transformer.forward(latitude, longitude)
                        // UTM koordinatlarında forward (x=easting, y=northing) döndü; bizim alanlarımız northing/easting
                        e = tx
                        n = ty
                        if (project.utmZone != null) {
                            z = project.utmZone.toString() + if (project.utmNorthHemisphere) "N" else "S"
                        }
                    }
                }
            }
            val entity = SurveyPointEntity(
                projectId = project.id,
                name = name,
                code = code?.ifBlank { null },
                latitude = latitude,
                longitude = longitude,
                elevation = elevation,
                northing = n,
                easting = e,
                zone = z,
                hrms = hrms,
                vrms = vrms,
                pdop = pdop,
                satellites = satellites,
                fixType = fixType,
                antennaHeight = antennaHeight,
                timestamp = System.currentTimeMillis()
            )
            surveyPointRepo.insert(entity)
        }
    }

    fun exportRecentPointsToCsv(): String {
        val points = recentPoints.value
        if (points.isEmpty()) return "" // boş
        val header = listOf(
            "id","projectId","name","code","latitude","longitude","elevation","northing","easting","zone","hrms","vrms","pdop","satellites","fixType","antennaHeight","timestamp"
        ).joinToString(",")
        val rows = points.joinToString("\n") { p ->
            listOf(
                p.id,
                p.projectId,
                p.name,
                p.code ?: "",
                p.latitude ?: "",
                p.longitude ?: "",
                p.elevation ?: "",
                p.northing ?: "",
                p.easting ?: "",
                p.zone ?: "",
                p.hrms ?: "",
                p.vrms ?: "",
                p.pdop ?: "",
                p.satellites ?: "",
                p.fixType ?: "",
                p.antennaHeight ?: "",
                p.timestamp
            ).joinToString(",")
        }
        return header + "\n" + rows
    }
}
