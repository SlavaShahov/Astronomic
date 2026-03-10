package com.example.astronomic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import kotlin.math.sin

class TextureLoader(private val context: Context) {

    fun loadTextureFromAsset(fileName: String): Int {
        return try {
            val inputStream = context.assets.open(fileName)
            val options = BitmapFactory.Options()
            options.inScaled = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            if (bitmap == null) {
                return 0
            }

            val textureIds = IntArray(1)
            GLES20.glGenTextures(1, textureIds, 0)

            if (textureIds[0] == 0) {
                return 0
            }

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0])

            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

            // Загружаем текстуру
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

            GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)

            bitmap.recycle()

            textureIds[0]
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    fun createProceduralTexture(): Int {
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)

        if (textureIds[0] == 0) {
            return 0
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0])

        val width = 2048
        val height = 2048
        val pixels = IntArray(width * height)

        for (i in pixels.indices) {
            val x = i % width
            val y = i / width

            val r = (10 + Math.random() * 20).toInt()
            val g = (10 + Math.random() * 20).toInt()
            val b = (20 + Math.random() * 30).toInt()

            var finalR = r
            var finalG = g
            var finalB = b

            if (Math.random() < 0.005) {
                val brightness = (200 + Math.random() * 55).toInt()
                finalR = brightness
                finalG = brightness
                finalB = brightness
            }

            if (Math.random() < 0.001) {
                finalR = 255
                finalG = 255
                finalB = 255
            }

            val nebula = (sin(x * 0.01) * sin(y * 0.01) * 20).toInt()
            finalR = (finalR + nebula).coerceIn(0, 255)
            finalG = (finalG + nebula).coerceIn(0, 255)
            finalB = (finalB + nebula).coerceIn(0, 255)

            pixels[i] = (0xFF shl 24) or (finalR shl 16) or (finalG shl 8) or finalB
        }

        val buffer = java.nio.IntBuffer.wrap(pixels)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer)

        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)

        return textureIds[0]
    }
}