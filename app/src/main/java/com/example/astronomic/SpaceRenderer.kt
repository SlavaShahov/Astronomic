package com.example.astronomic

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class SpaceRenderer(private val context: Context) : GLSurfaceView.Renderer {

    private lateinit var backgroundSquare: BackgroundSquare
    private lateinit var cube: Cube
    private lateinit var textureLoader: TextureLoader

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var backgroundTextureId = 0
    private var angle = 0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        ShaderManager.initShaders()
        ShaderManager.useProgram()

        backgroundSquare = BackgroundSquare()
        cube = Cube()
        textureLoader = TextureLoader(context)

        backgroundTextureId = textureLoader.loadTextureFromAsset("galaxy.jpg")
        if (backgroundTextureId == 0) {
            backgroundTextureId = textureLoader.createProceduralTexture()
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        val ratio = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 50f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        ShaderManager.useProgram()
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 15f, 0f, 0f, 0f, 0f, 1f, 0f)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, backgroundTextureId)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, 0f, -20f)
        Matrix.scaleM(modelMatrix, 0, 8f, 8f, 1f)
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
        backgroundSquare.draw(mvpMatrix)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, 0f, 0f)
        Matrix.scaleM(modelMatrix, 0, 0.5f, 0.5f, 0.5f)
        // Куб крутится вот тут
        angle += 0.8f
        Matrix.rotateM(modelMatrix, 0, angle, 1f, 1f, 0.5f)

        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
        cube.draw(mvpMatrix)
    }
}