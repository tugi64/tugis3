package com.example.tugis3.ui.survey.model

data class SurveyPoint(
    val id: String = "",
    val name: String,
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val accuracy: Double = 0.0,
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
