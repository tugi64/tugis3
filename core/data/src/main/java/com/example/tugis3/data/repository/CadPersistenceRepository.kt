package com.example.tugis3.data.repository

import com.example.tugis3.core.cad.codec.CadCodec
import com.example.tugis3.core.cad.model.CadEntity
import com.example.tugis3.core.cad.model.CadLine
import com.example.tugis3.core.cad.model.CadPolyline
import com.example.tugis3.core.cad.model.CadCircle
import com.example.tugis3.core.cad.model.CadArc
import com.example.tugis3.core.cad.model.CadText
import com.example.tugis3.core.cad.model.Point
import com.example.tugis3.data.db.dao.CadEntityDao
import com.example.tugis3.data.db.dao.CadLayerDao
import com.example.tugis3.data.db.entity.CadEntityEntity
import com.example.tugis3.data.db.entity.CadLayerEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/** CAD geometri & layer kalıcılığı (encode/decode pipe format). */
@Singleton
class CadPersistenceRepository @Inject constructor(
    private val layerDao: CadLayerDao,
    private val entityDao: CadEntityDao
) {
    data class CadItem(val id: Long, val entity: CadEntity)

    fun observe(projectId: Long): Flow<List<CadItem>> = combine(
        layerDao.layers(projectId),
        entityDao.entities(projectId)
    ) { layers, ents ->
        try {
            val layerMap = layers.associateBy { it.id }
            ents.mapNotNull { e ->
                try {
                    val layer = layerMap[e.layerId]
                    if (layer == null) return@mapNotNull null
                    decodeEntity(e, layer.name)?.let { CadItem(e.id, it) }
                } catch (ex: Exception) {
                    // Decode hatası durumunda entity'yi atla
                    null
                }
            }
        } catch (e: Exception) {
            // Genel hata durumunda boş liste döndür
            emptyList()
        }
    }

    suspend fun ensureLayer(projectId: Long, name: String, colorIndex: Int? = null): CadLayerEntity? {
        return try {
            val existing = layerDao.findByName(projectId, name)
            if (existing != null) return existing
            val id = layerDao.insert(
                CadLayerEntity(
                    projectId = projectId,
                    name = name,
                    colorIndex = colorIndex,
                    visible = 1
                )
            )
            layerDao.findByName(projectId, name)
        } catch (e: Exception) {
            // Layer oluşturma hatası
            null
        }
    }

    suspend fun addEntity(projectId: Long, entity: CadEntity): Long? {
        return try {
            val layer = ensureLayer(projectId, entity.layer, entity.colorIndex)
            if (layer == null) return null
            val enc = CadCodec.encode(entity)
            entityDao.insert(
                CadEntityEntity(
                    projectId = projectId,
                    layerId = layer.id,
                    type = enc.first,
                    dataEncoded = enc.second,
                    colorIndex = entity.colorIndex
                )
            )
        } catch (e: Exception) {
            // Entity ekleme hatası
            null
        }
    }

    suspend fun deleteEntity(id: Long) = entityDao.delete(id)
    suspend fun deleteAllForProject(projectId: Long) = entityDao.deleteForProject(projectId)

    // --- Encoding / Decoding ---
    private fun decodeEntity(e: CadEntityEntity, layerName: String): CadEntity? =
        CadCodec.decode(e.type, e.dataEncoded, layerName, e.colorIndex)
}
