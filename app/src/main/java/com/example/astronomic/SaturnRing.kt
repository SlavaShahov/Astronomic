package com.example.astronomic

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.*

class SaturnRing {

    private val vertexBuffer: FloatBuffer
    private val textureBuffer: FloatBuffer
    private val indexBuffer: ShortBuffer

    /**
     * @param innerRadius внутренний радиус кольца
     * @param outerRadius внешний радиус кольца
     * @param segments количество сегментов по окружности
     * @param ringCount количество концентрических колец
     */
    constructor(innerRadius: Float, outerRadius: Float, segments: Int, ringCount: Int = 5) {
        val vertices = mutableListOf<Float>()
        val texCoords = mutableListOf<Float>()
        val indices = mutableListOf<Short>()

        val ringStep = (outerRadius - innerRadius) / ringCount

        // Создаем несколько концентрических колец
        for (ring in 0 until ringCount) {
            val r1 = innerRadius + ring * ringStep
            val r2 = r1 + ringStep * 0.8f // промежутки между кольцами

            for (i in 0..segments) {
                val angle = 2 * PI * i / segments
                val cosA = cos(angle.toDouble()).toFloat()
                val sinA = sin(angle.toDouble()).toFloat()

                // Внешняя вершина этого кольца
                vertices.add(r2 * cosA)
                vertices.add(0f)
                vertices.add(r2 * sinA)

                // Внутренняя вершина этого кольца
                vertices.add(r1 * cosA)
                vertices.add(0f)
                vertices.add(r1 * sinA)

                // Текстурные координаты
                val u = i.toFloat() / segments
                texCoords.add(u)  // внешняя
                texCoords.add(0f)
                texCoords.add(u)  // внутренняя
                texCoords.add(1f)
            }
        }

        // Индексы для каждого кольца
        for (ring in 0 until ringCount) {
            val ringOffset = ring * (segments + 1) * 2

            for (i in 0 until segments) {
                val base = ringOffset + i * 2
                val nextBase = ringOffset + ((i + 1) % segments) * 2

                indices.add(base.toShort())
                indices.add((base + 1).toShort())
                indices.add(nextBase.toShort())

                indices.add((base + 1).toShort())
                indices.add((nextBase + 1).toShort())
                indices.add(nextBase.toShort())
            }
        }

        // Буфер вершин
        val vertexByteBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
        vertexByteBuffer.order(ByteOrder.nativeOrder())
        vertexBuffer = vertexByteBuffer.asFloatBuffer()
        vertexBuffer.put(vertices.toFloatArray())
        vertexBuffer.position(0)

        // Буфер текстурных координат
        val texByteBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
        texByteBuffer.order(ByteOrder.nativeOrder())
        textureBuffer = texByteBuffer.asFloatBuffer()
        textureBuffer.put(texCoords.toFloatArray())
        textureBuffer.position(0)

        // Буфер индексов
        val indexByteBuffer = ByteBuffer.allocateDirect(indices.size * 2)
        indexByteBuffer.order(ByteOrder.nativeOrder())
        indexBuffer = indexByteBuffer.asShortBuffer()
        indexBuffer.put(indices.toShortArray())
        indexBuffer.position(0)
    }

    fun draw(mvpMatrix: FloatArray, textureId: Int) {
        val useTextureHandle = GLES20.glGetUniformLocation(ShaderManager.program, "useTexture")
        GLES20.glUniform1i(useTextureHandle, 1)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        val positionHandle = GLES20.glGetAttribLocation(ShaderManager.program, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)

        val texCoordHandle = GLES20.glGetAttribLocation(ShaderManager.program, "inputTextureCoordinate")
        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 8, textureBuffer)

        val mvpMatrixHandle = GLES20.glGetUniformLocation(ShaderManager.program, "uMVPMatrix")
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexBuffer.capacity(),
            GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
        GLES20.glDisable(GLES20.GL_BLEND)
    }
}