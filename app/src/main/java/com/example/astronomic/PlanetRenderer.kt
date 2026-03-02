package com.example.astronomic

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

class PlanetRenderer(private val context: Context) : GLSurfaceView.Renderer {

    private lateinit var backgroundSquare: BackgroundSquare
    private lateinit var backgroundTextureLoader: TextureLoader
    private var backgroundTextureId = 0

    private lateinit var sunSphere: TexturedSphere
    private lateinit var planetSphere: TexturedSphere
    private lateinit var moonSphere: TexturedSphere
    private lateinit var textureLoader: TextureLoader

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var cameraDistance = 40f
    private var cameraAngleX = 15f
    private var cameraAngleY = 20f
    private val minCameraDistance = 15f
    private val maxCameraDistance = 80f

    private val textureIds = mutableMapOf<String, Int>()

    private val planets = listOf(
        Planet("Солнце", 2.5f, 0f, 0f, 0.5f, "sun.jpg"),        // 0.5 градуса за кадр = 30 град/сек
        Planet("Меркурий", 0.4f, 5f, 0.004f, 0.8f, "mercury.jpg"), // 48 град/сек
        Planet("Венера", 0.6f, 7f, 0.003f, 0.7f, "venus.jpg"),    // 42 град/сек
        Planet("Земля", 0.6f, 9f, 0.0025f, 0.6f, "earth.jpg", true), // 36 град/сек
        Planet("Марс", 0.5f, 11f, 0.002f, 0.6f, "mars.jpg"),      // 36 град/сек
        Planet("Юпитер", 1.3f, 15f, 0.001f, 1.2f, "jupiter.jpg"), // 72 град/сек (очень быстро)
        Planet("Сатурн", 1.1f, 19f, 0.0008f, 1.0f, "saturn.jpg"), // 60 град/сек
        Planet("Уран", 0.9f, 23f, 0.0005f, 0.8f, "uranus.jpg"),   // 48 град/сек
        Planet("Нептун", 0.9f, 27f, 0.0004f, 0.8f, "neptune.jpg") // 48 град/сек
    )

    private val orbitAngles = FloatArray(planets.size) { 0f }
    private val rotationAngles = FloatArray(planets.size) { 0f }
    private var moonAngle = 0f

    fun handlePinch(scaleFactor: Float) {
        cameraDistance = (cameraDistance / scaleFactor).coerceIn(minCameraDistance, maxCameraDistance)
    }

    fun handleDrag(dx: Float, dy: Float) {
        cameraAngleY += dx * 0.5f
        cameraAngleX = (cameraAngleX + dy * 0.5f).coerceIn(5f, 45f)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        ShaderManager.initShaders()
        ShaderManager.useProgram()

        backgroundSquare = BackgroundSquare()
        backgroundTextureLoader = TextureLoader(context)
        backgroundTextureId = backgroundTextureLoader.loadTextureFromAsset("galaxy.jpg")
        if (backgroundTextureId == 0) {
            backgroundTextureId = backgroundTextureLoader.createProceduralTexture()
        }

        sunSphere = TexturedSphere(64)
        planetSphere = TexturedSphere(48)
        moonSphere = TexturedSphere(32)

        textureLoader = TextureLoader(context)

        planets.forEach { planet ->
            val textureId = textureLoader.loadTextureFromAsset(planet.textureFile)
            textureIds[planet.name] = textureId
        }

        val moonTextureId = textureLoader.loadTextureFromAsset("moon.jpg")
        textureIds["Луна"] = moonTextureId
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        val ratio = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 1f, 1000f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        drawBackground()
        drawSolarSystem()
    }

    private fun drawBackground() {
        GLES20.glDepthMask(false)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)

        val orthoMatrix = FloatArray(16)
        Matrix.orthoM(orthoMatrix, 0, -1f, 1f, -1f, 1f, -1f, 1f)

        Matrix.setIdentityM(viewMatrix, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, backgroundTextureId)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, orthoMatrix, 0, modelMatrix, 0)

        backgroundSquare.draw(mvpMatrix)

        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(true)
    }

    private fun drawSolarSystem() {
        val camX = cameraDistance * sin(Math.toRadians(cameraAngleY.toDouble())).toFloat() *
                cos(Math.toRadians(cameraAngleX.toDouble())).toFloat()
        val camY = cameraDistance * sin(Math.toRadians(cameraAngleX.toDouble())).toFloat()
        val camZ = cameraDistance * cos(Math.toRadians(cameraAngleY.toDouble())).toFloat() *
                cos(Math.toRadians(cameraAngleX.toDouble())).toFloat()

        Matrix.setLookAtM(viewMatrix, 0,
            camX, camY, camZ,
            0f, 0f, 0f,
            0f, 1f, 0f)

        for (i in planets.indices) {
            val planet = planets[i]

            orbitAngles[i] += planet.speed

            rotationAngles[i] += planet.rotationSpeed
            // Чтобы вращение не ушло в бесконечность, ограничиваем 360
            if (rotationAngles[i] > 360f) rotationAngles[i] -= 360f

            if (i == 0) {
                drawPlanet(planet, 0f, 0f, rotationAngles[i])
            } else {
                val angle = orbitAngles[i]
                val x = planet.distance * cos(angle.toDouble()).toFloat()
                val z = planet.distance * sin(angle.toDouble()).toFloat()

                drawPlanet(planet, x, z, rotationAngles[i])

                if (planet.hasMoon) {
                    drawMoon(x, z)
                }
            }
        }
    }

    private fun drawPlanet(planet: Planet, x: Float, z: Float, rotation: Float) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, 0f, z)

        Matrix.rotateM(modelMatrix, 0, rotation, 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, planet.size, planet.size, planet.size)

        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        val textureId = textureIds[planet.name] ?: 0
        if (planet.name == "Солнце") {
            sunSphere.draw(mvpMatrix, textureId)
        } else {
            planetSphere.draw(mvpMatrix, textureId)
        }
    }

    private fun drawMoon(earthX: Float, earthZ: Float) {
        moonAngle += 0.05f
        if (moonAngle > 360f) moonAngle -= 360f

        val moonDistance = 1.8f
        val moonY = moonDistance * sin(moonAngle.toDouble()).toFloat()
        val moonZ = earthZ + moonDistance * cos(moonAngle.toDouble()).toFloat()
        val moonX = earthX

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, moonX, moonY, moonZ)
        Matrix.scaleM(modelMatrix, 0, 0.2f, 0.2f, 0.2f)

        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        val moonTextureId = textureIds["Луна"] ?: 0
        moonSphere.draw(mvpMatrix, moonTextureId)
    }
}