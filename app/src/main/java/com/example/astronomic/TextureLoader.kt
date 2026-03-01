package com.example.astronomic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils

class TextureLoader(private val context: Context) {

    fun loadTextureFromAsset(fileName: String): Int {
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)

        if (textureIds[0] == 0) {
            return 0
        }

        try {
            val inputStream = context.assets.open(fileName)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) {
                return 0
            }

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0])

            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            bitmap.recycle()

        } catch (e: Exception) {
            e.printStackTrace()
            return 0
        }

        return textureIds[0]
    }

    // Создаем процедурную текстуру, если нет изображения
    fun createProceduralTexture(): Int {
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)

        if (textureIds[0] == 0) {
            return 0
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0])

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        val pixels = IntArray(256 * 256)
        for (i in pixels.indices) {
            val r = (Math.random() * 255).toInt()
            val g = (Math.random() * 255).toInt()
            val b = (Math.random() * 255).toInt()
            pixels[i] = if (r > 240 && g > 240 && b > 240) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
        }

        val buffer = java.nio.IntBuffer.wrap(pixels)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 256, 256, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer)

        return textureIds[0]
    }
}