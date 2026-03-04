package com.example.astronomic

data class Planet(
    val name: String,
    val size: Float,
    val distance: Float,
    val speed: Float,
    val rotationSpeed: Float,
    val textureFile: String,
    val hasMoon: Boolean = false,
    val hasRings: Boolean = false,
    val ringInnerRadius: Float = 0f,
    val ringOuterRadius: Float = 0f,
    val ringTextureFile: String = "",
    val isMoon: Boolean = false,
    val parentPlanet: Int = -1
)