package com.example.astronomic

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class Cube {

    private val vertexBuffer: FloatBuffer
    private val textureBuffer: FloatBuffer
    private val indexBuffer: ShortBuffer

    // Вершины куба
    private val cubeVertices = floatArrayOf(
        -0.5f, -0.5f,  0.5f,
        0.5f, -0.5f,  0.5f,
        -0.5f,  0.5f,  0.5f,
        0.5f,  0.5f,  0.5f,
        -0.5f, -0.5f, -0.5f,
        0.5f, -0.5f, -0.5f,
        -0.5f,  0.5f, -0.5f,
        0.5f,  0.5f, -0.5f
    )

    private val cubeTextureVertices = floatArrayOf(
        0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
        1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f
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

        val textureByteBuffer = ByteBuffer.allocateDirect(cubeTextureVertices.size * 4)
        textureByteBuffer.order(ByteOrder.nativeOrder())
        textureBuffer = textureByteBuffer.asFloatBuffer()
        textureBuffer.put(cubeTextureVertices)
        textureBuffer.position(0)

        val indexByteBuffer = ByteBuffer.allocateDirect(drawOrder.size * 2)
        indexByteBuffer.order(ByteOrder.nativeOrder())
        indexBuffer = indexByteBuffer.asShortBuffer()
        indexBuffer.put(drawOrder)
        indexBuffer.position(0)
    }

    fun draw(mvpMatrix: FloatArray) {
        val positionHandle = GLES20.glGetAttribLocation(ShaderManager.program, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)

        val textureCoordHandle = GLES20.glGetAttribLocation(ShaderManager.program, "inputTextureCoordinate")
        GLES20.glEnableVertexAttribArray(textureCoordHandle)
        GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 8, textureBuffer)

        val mvpMatrixHandle = GLES20.glGetUniformLocation(ShaderManager.program, "uMVPMatrix")
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.size, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(textureCoordHandle)
    }
}