package com.example.astronomic

data class News(
    val id: Int,
    val title: String,
    val content: String,
    val imageRes: Int? = null
)
