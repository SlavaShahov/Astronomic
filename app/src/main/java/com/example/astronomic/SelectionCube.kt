package com.example.astronomic

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class SelectionCube {

    private val vertexBuffer: FloatBuffer
    private val indexBuffer: ShortBuffer

    private val cubeVertices = floatArrayOf(
        -1.0f, -1.0f,  1.0f,
        1.0f, -1.0f,  1.0f,
        -1.0f,  1.0f,  1.0f,
        1.0f,  1.0f,  1.0f,
        -1.0f, -1.0f, -1.0f,
        1.0f, -1.0f, -1.0f,
        -1.0f,  1.0f, -1.0f,
        1.0f,  1.0f, -1.0f
    )

    private val drawOrder = shortArrayOf(
        0, 1, 2, 1, 3, 2,
        4, 5, 6, 5, 7, 6,
        1, 5, 3, 5, 7, 3,
        0, 4, 2, 4, 6, 2,
        2, 3, 6, 3, 7, 6,
        0, 1, 4, 1, 5, 4
    )

    init {
        val vertexByteBuffer = ByteBuffer.allocateDirect(cubeVertices.size * 4)
        vertexByteBuffer.order(ByteOrder.nativeOrder())
        vertexBuffer = vertexByteBuffer.asFloatBuffer()
        vertexBuffer.put(cubeVertices)
        vertexBuffer.position(0)

        val indexByteBuffer = ByteBuffer.allocateDirect(drawOrder.size * 2)
        indexByteBuffer.order(ByteOrder.nativeOrder())
        indexBuffer = indexByteBuffer.asShortBuffer()
        indexBuffer.put(drawOrder)
        indexBuffer.position(0)
    }

    fun draw(mvpMatrix: FloatArray) {
        val useTextureHandle = GLES20.glGetUniformLocation(ShaderManager.program, "useTexture")
        GLES20.glUniform1i(useTextureHandle, 0)

        val positionHandle = GLES20.glGetAttribLocation(ShaderManager.program, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)

        val mvpMatrixHandle = GLES20.glGetUniformLocation(ShaderManager.program, "uMVPMatrix")
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        // БОЛЕЕ ПРОЗРАЧНЫЙ КУБ (альфа 0.2 вместо 0.3)
        val colorHandle = GLES20.glGetUniformLocation(ShaderManager.program, "vColor")
        GLES20.glUniform4fv(colorHandle, 1, floatArrayOf(1.0f, 1.0f, 1.0f, 0.15f), 0)

        // Рисуем как каркас (линии), а не сплошной
        GLES20.glLineWidth(2.0f) // Толщина линий

        // Рисуем только ребра куба
        val edgeIndices = shortArrayOf(
            0, 1, 1, 3, 3, 2, 2, 0,  // передняя грань
            4, 5, 5, 7, 7, 6, 6, 4,  // задняя грань
            0, 4, 1, 5, 3, 7, 2, 6    // соединяем грани
        )

        val edgeBuffer = ByteBuffer.allocateDirect(edgeIndices.size * 2)
        edgeBuffer.order(ByteOrder.nativeOrder())
        val edgeIndexBuffer = edgeBuffer.asShortBuffer()
        edgeIndexBuffer.put(edgeIndices)
        edgeIndexBuffer.position(0)

        GLES20.glDrawElements(GLES20.GL_LINES, edgeIndices.size, GLES20.GL_UNSIGNED_SHORT, edgeIndexBuffer)

        GLES20.glDisableVertexAttribArray(positionHandle)
    }
}