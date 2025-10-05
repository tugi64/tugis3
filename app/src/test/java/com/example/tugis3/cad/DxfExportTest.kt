package com.example.tugis3.cad

import com.example.tugis3.ui.cad.CadViewModel
import com.example.tugis3.core.cad.model.Point
import org.junit.Assert.*
import org.junit.Test

class DxfExportTest {
    @Test
    fun polylineOpenDxf() {
        val pts = listOf(Point(0.0,0.0), Point(5.0,0.0), Point(5.0,5.0))
        val dxf = CadViewModel.buildMeasurementDxf(pts, closed = false)
        assertTrue(dxf.contains("LWPOLYLINE"))
        assertTrue(dxf.contains("90"))
        assertTrue(dxf.contains("5.0"))
        assertTrue(dxf.contains("70"))
        // closed=0
        assertTrue(dxf.contains("\n70\n0\n") || dxf.contains("\r\n70\r\n0\r\n"))
    }
    @Test
    fun polylineClosedDxf() {
        val pts = listOf(Point(0.0,0.0), Point(5.0,0.0), Point(5.0,5.0))
        val dxf = CadViewModel.buildMeasurementDxf(pts, closed = true)
        assertTrue(dxf.contains("LWPOLYLINE"))
        // closed=1
        assertTrue(dxf.contains("\n70\n1\n") || dxf.contains("\r\n70\r\n1\r\n"))
    }
    @Test(expected = IllegalArgumentException::class)
    fun dxfTooFewPointsThrows() {
        CadViewModel.buildMeasurementDxf(listOf(Point(0.0,0.0)), closed = false)
    }
}

