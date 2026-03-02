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
    private lateinit var selectionCube: SelectionCube
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
    private var selectedPlanetIndex = 0
    private var showInfo = false
    private var infoText = ""
    private var infoTimer = 0

    private val planets = listOf(
        Planet("Солнце", 2.5f, 0f, 0f, 0.5f, "sun.jpg"),
        Planet("Меркурий", 0.4f, 5f, 0.004f, 0.8f, "mercury.jpg"),
        Planet("Венера", 0.6f, 7f, 0.003f, 0.7f, "venus.jpg"),
        Planet("Земля", 0.6f, 9f, 0.0025f, 0.6f, "earth.jpg", true),
        Planet("Марс", 0.5f, 11f, 0.002f, 0.6f, "mars.jpg"),
        Planet("Юпитер", 1.3f, 15f, 0.001f, 1.2f, "jupiter.jpg"),
        Planet("Сатурн", 1.1f, 19f, 0.0008f, 1.0f, "saturn.jpg"),
        Planet("Уран", 0.9f, 23f, 0.0005f, 0.8f, "uranus.jpg"),
        Planet("Нептун", 0.9f, 27f, 0.0004f, 0.8f, "neptune.jpg")
    )

    private val orbitAngles = FloatArray(planets.size) { 0f }
    private val rotationAngles = FloatArray(planets.size) { 0f }
    private var moonAngle = 0f

    fun selectPlanet(index: Int) {
        selectedPlanetIndex = index
    }

    fun showPlanetInfo(index: Int) {
        val planet = planets[index]
        infoText = "${planet.name}\n" +
                "Размер: ${planet.size}\n" +
                "Расстояние: ${planet.distance}\n" +
                (if (planet.hasMoon) "Есть Луна" else "Нет лун")
        showInfo = true
        infoTimer = 300
    }

    fun getPlanetInfo(index: Int): String {
        val planet = planets[index]
        return "${planet.name}\n" +
                "Размер: ${planet.size}\n" +
                "Расстояние: ${planet.distance}\n" +
                (if (planet.hasMoon) "✓ Есть Луна" else "✗ Нет лун")
    }

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
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        ShaderManager.initShaders()
        ShaderManager.useProgram()

        backgroundTextureLoader = TextureLoader(context)
        backgroundTextureId = backgroundTextureLoader.loadTextureFromAsset("galaxy.jpg")

        if (backgroundTextureId == 0) {
            backgroundTextureId = backgroundTextureLoader.createProceduralTexture()
            android.util.Log.d("DEBUG", "Создана процедурная текстура, ID: $backgroundTextureId")
        } else {
            android.util.Log.d("DEBUG", "Загружена текстура galaxy.jpg, ID: $backgroundTextureId")
        }

        backgroundSquare = BackgroundSquare()

        sunSphere = TexturedSphere(64)
        planetSphere = TexturedSphere(48)
        moonSphere = TexturedSphere(32)
        selectionCube = SelectionCube()

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

        if (showInfo) {
            infoTimer--
            if (infoTimer <= 0) {
                showInfo = false
            }
        }
    }

    private fun drawBackground() {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(false)

        val orthoMatrix = FloatArray(16)
        Matrix.orthoM(orthoMatrix, 0, -1f, 1f, -1f, 1f, -1f, 1f)

        val savedViewMatrix = viewMatrix.clone()
        Matrix.setIdentityM(viewMatrix, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, backgroundTextureId)

        val useTextureHandle = GLES20.glGetUniformLocation(ShaderManager.program, "useTexture")
        GLES20.glUniform1i(useTextureHandle, 1)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, orthoMatrix, 0, modelMatrix, 0)

        backgroundSquare.draw(mvpMatrix)

        System.arraycopy(savedViewMatrix, 0, viewMatrix, 0, 16)
        GLES20.glDepthMask(true)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
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

        drawSelectionCube()
    }

    private fun drawPlanet(planet: Planet, x: Float, z: Float, rotation: Float) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, 0f, z)
        Matrix.rotateM(modelMatrix, 0, rotation, 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, planet.size, planet.size, planet.size)

        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        val textureId = textureIds[planet.name] ?: 0

        val useTextureHandle = GLES20.glGetUniformLocation(ShaderManager.program, "useTexture")
        GLES20.glUniform1i(useTextureHandle, 1)

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

        val useTextureHandle = GLES20.glGetUniformLocation(ShaderManager.program, "useTexture")
        GLES20.glUniform1i(useTextureHandle, 1)

        moonSphere.draw(mvpMatrix, moonTextureId)
    }

    private fun drawSelectionCube() {
        val planet = planets[selectedPlanetIndex]

        val posX: Float
        val posZ: Float

        if (selectedPlanetIndex == 0) {
            posX = 0f
            posZ = 0f
        } else {
            val angle = orbitAngles[selectedPlanetIndex]
            posX = planet.distance * cos(angle.toDouble()).toFloat()
            posZ = planet.distance * sin(angle.toDouble()).toFloat()
        }

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, posX, 0f, posZ)
        Matrix.scaleM(modelMatrix, 0, planet.size * 1.3f, planet.size * 1.3f, planet.size * 1.3f)

        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        selectionCube.draw(mvpMatrix)
    }
}