package com.example.tugis3.core.cad.dto

import com.example.tugis3.core.cad.model.*

/** Mapper: Domain <-> DTO */
object CadDtoMapper {
    fun toDto(entity: CadEntity): CadEntityDto = when(entity) {
        is CadPoint -> PointDto(entity.position.x, entity.position.y, entity.layer)
        is CadLine -> LineDto(entity.start.x, entity.start.y, entity.end.x, entity.end.y, entity.layer)
        is CadPolyline -> PolylineDto(
            closed = entity.isClosed,
            coords = entity.points.flatMap { listOf(it.x, it.y) },
            layer = entity.layer
        )
        is CadPolygon -> PolygonDto(
            rings = entity.rings.map { ring -> ring.flatMap { listOf(it.x, it.y) } },
            layer = entity.layer
        )
        is CadCircle -> CircleDto(entity.center.x, entity.center.y, entity.radius, entity.layer)
        is CadArc -> ArcDto(entity.center.x, entity.center.y, entity.radius, entity.startAngleDeg, entity.endAngleDeg, entity.layer)
        is CadText -> TextDto(entity.position.x, entity.position.y, entity.height, entity.text, entity.rotationDeg, entity.layer)
    }

    fun fromDto(dto: CadEntityDto): CadEntity = when(dto) {
        is PointDto -> CadPoint(Vec2(dto.x, dto.y), layer = dto.layer)
        is LineDto -> CadLine(Vec2(dto.x1, dto.y1), Vec2(dto.x2, dto.y2), layer = dto.layer)
        is PolylineDto -> {
            val pts = dto.coords.chunked(2).map { (x,y) -> Vec2(x,y) }
            CadPolyline(points = pts, isClosed = dto.closed, layer = dto.layer)
        }
        is PolygonDto -> {
            val rings = dto.rings.map { flat -> flat.chunked(2).map { (x,y) -> Vec2(x,y) } }
            CadPolygon(rings = rings, layer = dto.layer)
        }
        is CircleDto -> CadCircle(center = Vec2(dto.cx, dto.cy), radius = dto.r, layer = dto.layer)
        is ArcDto -> CadArc(center = Vec2(dto.cx, dto.cy), radius = dto.r, startAngleDeg = dto.startDeg, endAngleDeg = dto.endDeg, layer = dto.layer)
        is TextDto -> CadText(position = Vec2(dto.x, dto.y), height = dto.h, text = dto.text, rotationDeg = dto.rot, layer = dto.layer)
    }

    fun toDtoList(list: List<CadEntity>): List<CadEntityDto> = list.map { toDto(it) }
    fun fromDtoList(list: List<CadEntityDto>): List<CadEntity> = list.map { fromDto(it) }
}
