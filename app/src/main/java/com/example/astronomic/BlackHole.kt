package com.example.astronomic

import android.opengl.GLES20
import java.nio.*
import kotlin.math.*

class BlackHole(segments: Int) {

    private val vertexBuffer: FloatBuffer
    private val vertexCount: Int

    init {

        val vertices = FloatArray((segments + 2) * 3)

        // центр
        vertices[0] = 0f
        vertices[1] = 0f
        vertices[2] = 0f

        val radius = 1.3f

        for (i in 0..segments) {

            val angle = 2f * PI.toFloat() * i / segments

            val x = cos(angle) * radius
            val y = sin(angle) * radius

            vertices[(i + 1) * 3] = x
            vertices[(i + 1) * 3 + 1] = y
            vertices[(i + 1) * 3 + 2] = 0f
        }

        vertexCount = segments + 2

        val bb = ByteBuffer.allocateDirect(vertices.size * 4)
        bb.order(ByteOrder.nativeOrder())

        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer.put(vertices)
        vertexBuffer.position(0)
    }
}