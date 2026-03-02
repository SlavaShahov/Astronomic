package com.example.astronomic

data class Planet(
    val name: String,
    val size: Float,
    val distance: Float,
    val speed: Float,
    val rotationSpeed: Float,
    val textureFile: String,
    val hasMoon: Boolean = false
)