package com.example.astronomic

data class PlanetInfo(
    val name: String,
    val description: String,
    val imageRes: String, // имя файла изображения в assets
    val diameter: String,
    val distanceFromSun: String,
    val orbitalPeriod: String,
    val moons: Int,
    val temperature: String
)