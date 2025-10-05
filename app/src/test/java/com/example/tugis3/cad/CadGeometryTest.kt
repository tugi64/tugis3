package com.example.tugis3.cad

import com.example.tugis3.ui.cad.CadViewModel
import com.example.tugis3.core.cad.model.Point
import org.junit.Assert.*
import org.junit.Test

class CadGeometryTest {
    @Test
    fun triangleArea() {
        val pts = listOf(Point(0.0,0.0), Point(10.0,0.0), Point(0.0,10.0))
        val area = CadViewModel.polygonAreaOf(pts)
        assertEquals(50.0, area, 1e-6)
    }
    @Test
    fun squareArea() {
        val pts = listOf(Point(0.0,0.0), Point(10.0,0.0), Point(10.0,10.0), Point(0.0,10.0))
        val area = CadViewModel.polygonAreaOf(pts)
        assertEquals(100.0, area, 1e-6)
    }
    @Test
    fun degenerateArea() {
        val pts = listOf(Point(0.0,0.0), Point(5.0,0.0))
        val area = CadViewModel.polygonAreaOf(pts)
        assertEquals(0.0, area, 1e-9)
    }
}

