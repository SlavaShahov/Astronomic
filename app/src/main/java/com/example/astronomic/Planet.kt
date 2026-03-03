package com.example.astronomic

data class Planet(
    val name: String,
    val size: Float,
    val distance: Float,
    val speed: Float,
    val rotationSpeed: Float,
    val textureFile: String,
    val hasMoon: Boolean = false,
    val hasRings: Boolean = false,        // есть ли кольца
    val ringInnerRadius: Float = 0f,       // внутренний радиус колец
    val ringOuterRadius: Float = 0f,       // внешний радиус колец
    val ringTextureFile: String = "",      // текстура колец
    val isMoon: Boolean = false,
    val parentPlanet: Int = -1
)