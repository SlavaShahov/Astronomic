package com.example.astronomic

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class BackgroundSquare {

    private val vertexBuffer: FloatBuffer
    private val textureBuffer: FloatBuffer

    // Вершины квадрата
    private val squareVertices = floatArrayOf(
        -3.0f, -3.0f, -5.0f,  // левый нижний
        3.0f, -3.0f, -5.0f,  // правый нижний
        -3.0f,  3.0f, -5.0f,  // левый верхний
        3.0f,  3.0f, -5.0f   // правый верхний
    )

    // Текстурные координаты
    private val textureVertices = floatArrayOf(
        0.0f, 1.0f,  // левый нижний
        1.0f, 1.0f,  // правый нижний
        0.0f, 0.0f,  // левый верхний
        1.0f, 0.0f   // правый верхний
    )

    init {
        val vertexByteBuffer = ByteBuffer.allocateDirect(squareVertices.size * 4)
        vertexByteBuffer.order(ByteOrder.nativeOrder())
        vertexBuffer = vertexByteBuffer.asFloatBuffer()
        vertexBuffer.put(squareVertices)
        vertexBuffer.position(0)

        val textureByteBuffer = ByteBuffer.allocateDirect(textureVertices.size * 4)
        textureByteBuffer.order(ByteOrder.nativeOrder())
        textureBuffer = textureByteBuffer.asFloatBuffer()
        textureBuffer.put(textureVertices)
        textureBuffer.position(0)
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

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(textureCoordHandle)
    }
}