package com.example.astronomic

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.*

class TexturedSphere(private val segments: Int) {

    private val vertexBuffer: FloatBuffer
    private val textureBuffer: FloatBuffer
    private val indexBuffer: ShortBuffer

    init {
        val vertices = mutableListOf<Float>()
        val texCoords = mutableListOf<Float>()
        val indices = mutableListOf<Short>()

        for (i in 0..segments) {
            val phi = PI * i / segments

            for (j in 0..segments) {
                val theta = 2 * PI * j / segments

                val x = (sin(phi) * cos(theta)).toFloat()
                val y = (cos(phi)).toFloat()
                val z = (sin(phi) * sin(theta)).toFloat()

                vertices.add(x)
                vertices.add(y)
                vertices.add(z)

                texCoords.add(j.toFloat() / segments)
                texCoords.add(i.toFloat() / segments)
            }
        }

        for (i in 0 until segments) {
            for (j in 0 until segments) {
                val p1 = (i * (segments + 1) + j).toShort()
                val p2 = (i * (segments + 1) + j + 1).toShort()
                val p3 = ((i + 1) * (segments + 1) + j).toShort()
                val p4 = ((i + 1) * (segments + 1) + j + 1).toShort()

                indices.add(p1)
                indices.add(p2)
                indices.add(p3)
                indices.add(p2)
                indices.add(p4)
                indices.add(p3)
            }
        }

        val vertexByteBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
        vertexByteBuffer.order(ByteOrder.nativeOrder())
        vertexBuffer = vertexByteBuffer.asFloatBuffer()
        vertexBuffer.put(vertices.toFloatArray())
        vertexBuffer.position(0)

        val texByteBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
        texByteBuffer.order(ByteOrder.nativeOrder())
        textureBuffer = texByteBuffer.asFloatBuffer()
        textureBuffer.put(texCoords.toFloatArray())
        textureBuffer.position(0)

        val indexByteBuffer = ByteBuffer.allocateDirect(indices.size * 2)
        indexByteBuffer.order(ByteOrder.nativeOrder())
        indexBuffer = indexByteBuffer.asShortBuffer()
        indexBuffer.put(indices.toShortArray())
        indexBuffer.position(0)
    }

    fun draw(mvpMatrix: FloatArray, textureId: Int) {
        // Включаем использование текстуры
        val useTextureHandle = GLES20.glGetUniformLocation(ShaderManager.program, "useTexture")
        GLES20.glUniform1i(useTextureHandle, 1)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        val positionHandle = GLES20.glGetAttribLocation(ShaderManager.program, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)

        val texCoordHandle = GLES20.glGetAttribLocation(ShaderManager.program, "inputTextureCoordinate")
        if (texCoordHandle >= 0) {
            GLES20.glEnableVertexAttribArray(texCoordHandle)
            GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 8, textureBuffer)
        }

        val mvpMatrixHandle = GLES20.glGetUniformLocation(ShaderManager.program, "uMVPMatrix")
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexBuffer.capacity(),
            GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        GLES20.glDisableVertexAttribArray(positionHandle)
        if (texCoordHandle >= 0) {
            GLES20.glDisableVertexAttribArray(texCoordHandle)
        }
    }
}