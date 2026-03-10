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
    private lateinit var saturnRing: SaturnRing
    private lateinit var textureLoader: TextureLoader

    // НОВОЕ: черная дыра
    private lateinit var blackHole: BlackHole
    private var blackHoleTextureId = 0

    // Параметры орбиты черной дыры (вертикальный овал)
    private var blackHoleAngle = 0f
    private val blackHoleOrbitSpeed = 0.001f  // ОЧЕНЬ МЕДЛЕННО
    private val blackHoleRadiusX = 15f  // Ширина орбиты
    private val blackHoleRadiusY = 10f  // Высота орбиты
    private val blackHoleRadiusZ = 8f   // Глубина орбиты
    private val blackHoleOffsetY = 3f   // Смещение по высоте
    private val blackHoleDistance = -30f // Далеко на заднем плане

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    // Управление камерой
    private var cameraDistance = 40f
    private var cameraAngleX = 15f
    private var cameraAngleY = 20f
    private val minCameraDistance = 15f
    private val maxCameraDistance = 80f

    private val textureIds = mutableMapOf<String, Int>()
    private var saturnRingTextureId = 0

    private var selectedObjectIndex = 3
    private var selectedIsMoon = false

    private val planets = listOf(
        Planet("Солнце", 2.5f, 0f, 0f, 0.5f, "sun.jpg"),
        Planet("Меркурий", 0.4f, 5f, 0.004f, 0.8f, "mercury.jpg"),
        Planet("Венера", 0.6f, 7f, 0.003f, 0.7f, "venus.jpg"),
        Planet("Земля", 0.6f, 9f, 0.0025f, 0.6f, "earth.jpg", true),
        Planet("Марс", 0.5f, 11f, 0.002f, 0.6f, "mars.jpg"),
        Planet("Юпитер", 1.3f, 15f, 0.001f, 1.2f, "jupiter.jpg"),
        Planet("Сатурн", 1.1f, 19f, 0.0008f, 1.0f, "saturn.jpg",
            false, true, 1.8f, 2.8f, "saturn_ring.jpg"),
        Planet("Уран", 0.9f, 23f, 0.0005f, 0.8f, "uranus.jpg"),
        Planet("Нептун", 0.9f, 27f, 0.0004f, 0.8f, "neptune.jpg")
    )

    private val moon = Planet(
        "Луна", 0.2f, 1.8f, 0.0f, 0.1f, "moon.jpg",
        false, false, 0f, 0f, "", true, 3
    )

    private val orbitAngles = FloatArray(planets.size) { 0f }
    private val rotationAngles = FloatArray(planets.size) { 0f }
    private var moonAngle = 0f
    private var moonRotation = 0f

    fun getSelectedObjectName(): String {
        return if (selectedIsMoon) "Луна" else planets[selectedObjectIndex].name
    }

    fun nextObject() {
        if (selectedIsMoon) {
            selectedIsMoon = false
            selectedObjectIndex = (moon.parentPlanet + 1) % planets.size
        } else {
            val currentPlanet = planets[selectedObjectIndex]
            if (currentPlanet.hasMoon && selectedObjectIndex == 3) {
                selectedIsMoon = true
            } else {
                selectedObjectIndex = (selectedObjectIndex + 1) % planets.size
            }
        }
    }

    fun prevObject() {
        if (selectedIsMoon) {
            selectedIsMoon = false
            selectedObjectIndex = moon.parentPlanet
        } else {
            val prevIndex = (selectedObjectIndex - 1 + planets.size) % planets.size
            val prevPlanet = planets[prevIndex]

            if (prevPlanet.hasMoon && prevIndex == 3) {
                selectedIsMoon = true
                selectedObjectIndex = prevIndex
            } else {
                selectedObjectIndex = prevIndex
            }
        }
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

        BlackHoleShader.init()

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
        selectionCube = SelectionCube()

        // НОВОЕ: создаем черную дыру (диск)
        blackHole = BlackHole(128)

        val saturn = planets[6]
        saturnRing = SaturnRing(saturn.ringInnerRadius, saturn.ringOuterRadius, 128, 8)

        textureLoader = TextureLoader(context)

        planets.forEach { planet ->
            val textureId = textureLoader.loadTextureFromAsset(planet.textureFile)
            textureIds[planet.name] = textureId
        }

        saturnRingTextureId = textureLoader.loadTextureFromAsset("saturn_ring.jpg")
        if (saturnRingTextureId == 0) {
            saturnRingTextureId = createProceduralRingTexture()
        }

        blackHoleTextureId = createProceduralBlackHoleTexture()

        val moonTextureId = textureLoader.loadTextureFromAsset("moon.jpg")
        textureIds["Луна"] = moonTextureId
    }

    private fun createProceduralRingTexture(): Int {
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)
        if (textureIds[0] == 0) return 0

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0])

        val width = 1024
        val height = 128
        val pixels = IntArray(width * height)

        for (i in pixels.indices) {
            val x = i % width
            val y = i / height

            var r = 200
            var g = 180
            var b = 150
            var alpha = 180

            val band = (x / 40) % 10

            when (band) {
                0, 1 -> { r = 140; g = 120; b = 100; alpha = 80 }
                2 -> { r = 100; g = 90; b = 80; alpha = 40 }
                3, 4 -> { r = 180; g = 160; b = 140; alpha = 140 }
                5 -> { r = 120; g = 100; b = 90; alpha = 60 }
                6, 7 -> { r = 240; g = 220; b = 180; alpha = 220 }
                8 -> { r = 160; g = 140; b = 120; alpha = 80 }
                9 -> { r = 220; g = 200; b = 170; alpha = 200 }
            }

            if (y < 30 || y > 90) {
                alpha = (alpha * 0.6).toInt()
            }

            val noise = (Math.random() * 20 - 10).toInt()
            r = (r + noise).coerceIn(60, 255)
            g = (g + noise).coerceIn(60, 255)
            b = (b + noise).coerceIn(60, 255)

            pixels[i] = (alpha shl 24) or (r shl 16) or (g shl 8) or b
        }

        val buffer = java.nio.IntBuffer.wrap(pixels)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer)

        return textureIds[0]
    }

    private fun createProceduralBlackHoleTexture(): Int {
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)
        if (textureIds[0] == 0) return 0

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0])

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)

        val width = 1024
        val height = 256
        val pixels = IntArray(width * height)

        for (i in pixels.indices) {
            val x = i % width
            val y = i / width

            val v = y.toFloat() / height

            val u = x.toFloat() / width

            var r = 0
            var g = 0
            var b = 0
            var a = 255

            val brightness = (255 * (1f - v * 0.7f)).toInt()

            val spiral = sin(u * 20 * PI.toFloat()) * 0.3f + 0.7f

            if (v < 0.1f) {
                r = 0; g = 0; b = 0
            } else {
                when {
                    v < 0.3f -> {
                        r = (brightness * 0.9f).toInt()
                        g = (brightness * 0.7f).toInt()
                        b = brightness
                    }
                    v < 0.6f -> {
                        r = brightness
                        g = (brightness * 0.5f).toInt()
                        b = (brightness * 0.3f).toInt()
                    }
                    else -> {
                        r = brightness
                        g = (brightness * 0.2f).toInt()
                        b = (brightness * 0.1f).toInt()
                    }
                }

                r = (r * spiral).toInt()
                g = (g * spiral).toInt()
                b = (b * spiral).toInt()
            }

            if (v > 0.95f) {
                a = ((1f - v) / 0.05f * 255).toInt()
            }

            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }

        val buffer = java.nio.IntBuffer.wrap(pixels)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer)

        return textureIds[0]
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        val ratio = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 1f, 1000f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        drawBackground()

        // НОВОЕ: рисуем черную дыру на заднем плане
        drawBlackHole()

        drawSolarSystem()
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

    private fun drawBlackHole() {
        blackHoleAngle += blackHoleOrbitSpeed
        if (blackHoleAngle > 2 * PI) blackHoleAngle -= (2 * PI).toFloat()

        val x = blackHoleRadiusX * sin(blackHoleAngle.toDouble()).toFloat()
        val y = blackHoleOffsetY + blackHoleRadiusY * cos(blackHoleAngle.toDouble()).toFloat()
        val z = blackHoleDistance + blackHoleRadiusZ * sin(blackHoleAngle.toDouble() * 0.5).toFloat()

        val depthTestEnabled = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_DEPTH_TEST, depthTestEnabled, 0)

        // Отключаем тест глубины для фонового эффекта
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(false)

        // Используем шейдер черной дыры
        GLES20.glUseProgram(BlackHoleShader.program)

        val timeHandle = GLES20.glGetUniformLocation(BlackHoleShader.program, "uTime")
        GLES20.glUniform1f(timeHandle, System.currentTimeMillis() / 1000f)

        val resHandle = GLES20.glGetUniformLocation(BlackHoleShader.program, "uResolution")
        GLES20.glUniform2f(resHandle, 1080f, 1920f)

        val mouseHandle = GLES20.glGetUniformLocation(BlackHoleShader.program, "uMouse")
        GLES20.glUniform2f(mouseHandle, 0f, 0f)

        val bhXHandle = GLES20.glGetUniformLocation(BlackHoleShader.program, "uBlackHoleX")
        GLES20.glUniform1f(bhXHandle, x / 10f)

        val bhYHandle = GLES20.glGetUniformLocation(BlackHoleShader.program, "uBlackHoleY")
        GLES20.glUniform1f(bhYHandle, y / 10f)

        val bhZHandle = GLES20.glGetUniformLocation(BlackHoleShader.program, "uBlackHoleZ")
        GLES20.glUniform1f(bhZHandle, z / 20f)

        val bgTextureHandle = GLES20.glGetUniformLocation(BlackHoleShader.program, "uBackgroundTexture")
        GLES20.glUniform1i(bgTextureHandle, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, backgroundTextureId)

        val vertices = floatArrayOf(
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f
        )

        val vertexBuffer = java.nio.ByteBuffer.allocateDirect(vertices.size * 4)
            .order(java.nio.ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        vertexBuffer.position(0)

        val posHandle = GLES20.glGetAttribLocation(BlackHoleShader.program, "aPosition")
        GLES20.glEnableVertexAttribArray(posHandle)
        GLES20.glVertexAttribPointer(posHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(posHandle)

        GLES20.glDepthMask(true)
        if (depthTestEnabled[0] != 0) {
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        }

        GLES20.glUseProgram(ShaderManager.program)
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

        if (planet.hasRings) {
            drawRings(planet, x, z, rotation)
        }
    }

    private fun drawRings(planet: Planet, x: Float, z: Float, rotation: Float) {
        for (offset in -1..1) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, x, offset * 0.03f, z)
            Matrix.rotateM(modelMatrix, 0, 27f, 1f, 0f, 0f)
            Matrix.rotateM(modelMatrix, 0, rotation, 0f, 1f, 0f)

            Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

            if (offset == 0) {
                saturnRing.draw(mvpMatrix, saturnRingTextureId)
            } else {
                GLES20.glUniform1f(GLES20.glGetUniformLocation(ShaderManager.program, "uAlpha"), 0.3f)
                saturnRing.draw(mvpMatrix, saturnRingTextureId)
                GLES20.glUniform1f(GLES20.glGetUniformLocation(ShaderManager.program, "uAlpha"), 1.0f)
            }
        }
    }

    private fun drawMoon(earthX: Float, earthZ: Float) {
        moonAngle += 0.05f
        moonRotation += moon.rotationSpeed
        if (moonAngle > 360f) moonAngle -= 360f
        if (moonRotation > 360f) moonRotation -= 360f

        val moonY = moon.distance * sin(moonAngle.toDouble()).toFloat()
        val moonZ = earthZ + moon.distance * cos(moonAngle.toDouble()).toFloat()
        val moonX = earthX

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, moonX, moonY, moonZ)
        Matrix.rotateM(modelMatrix, 0, moonRotation, 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, moon.size, moon.size, moon.size)

        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        val moonTextureId = textureIds["Луна"] ?: 0
        moonSphere.draw(mvpMatrix, moonTextureId)
    }

    private fun drawSelectionCube() {
        val posX: Float
        val posY: Float
        val posZ: Float
        val size: Float

        if (selectedIsMoon) {
            val earthIndex = moon.parentPlanet
            val earthAngle = orbitAngles[earthIndex]
            val earthX = planets[earthIndex].distance * cos(earthAngle.toDouble()).toFloat()
            val earthZ = planets[earthIndex].distance * sin(earthAngle.toDouble()).toFloat()

            posX = earthX
            posY = moon.distance * sin(moonAngle.toDouble()).toFloat()
            posZ = earthZ + moon.distance * cos(moonAngle.toDouble()).toFloat()
            size = moon.size * 1.5f
        } else {
            val planet = planets[selectedObjectIndex]
            if (selectedObjectIndex == 0) {
                posX = 0f
                posY = 0f
                posZ = 0f
            } else {
                val angle = orbitAngles[selectedObjectIndex]
                posX = planet.distance * cos(angle.toDouble()).toFloat()
                posY = 0f
                posZ = planet.distance * sin(angle.toDouble()).toFloat()
            }
            size = planet.size * 1.3f
        }

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, posX, posY, posZ)
        Matrix.scaleM(modelMatrix, 0, size, size, size)

        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        selectionCube.draw(mvpMatrix)
    }
}