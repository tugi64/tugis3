package com.example.tugis3.core.cad.dto

import com.example.tugis3.core.cad.model.*
import org.junit.Assert.*
import org.junit.Test

class CadDtoMapperTest {
    @Test
    fun `toDto and fromDto preserve basic geometry`() {
        val entities: List<CadEntity> = listOf(
            CadPoint(Vec2(1.0,2.0), layer="L0"),
            CadLine(Vec2(0.0,0.0), Vec2(5.0,0.0), layer="L1"),
            CadPolyline(listOf(Vec2(0.0,0.0), Vec2(1.0,0.0), Vec2(1.0,1.0)), isClosed = true, layer="L2"),
            CadPolygon(listOf(listOf(Vec2(0.0,0.0), Vec2(2.0,0.0), Vec2(2.0,2.0), Vec2(0.0,2.0)))),
            CadCircle(Vec2(3.0,3.0), 1.5, layer="C"),
            CadArc(Vec2(10.0,0.0), 4.0, 0.0, 120.0, layer="A"),
            CadText(Vec2(5.0,5.0), 2.5, "HELLO", layer="TXT")
        )
        val dtoList = CadDtoMapper.toDtoList(entities)
        assertEquals(entities.size, dtoList.size)
        val back = CadDtoMapper.fromDtoList(dtoList)
        assertEquals(entities.size, back.size)
        // Spot checks
        val line = back.filterIsInstance<CadLine>().first()
        assertEquals(5.0, line.end.x, 1e-9)
        val poly = back.filterIsInstance<CadPolyline>().first()
        assertTrue(poly.isClosed)
        val polygon = back.filterIsInstance<CadPolygon>().first()
        assertEquals(4, polygon.rings.first().size)
        val txt = back.filterIsInstance<CadText>().first()
        assertEquals("HELLO", txt.text)
    }
}

