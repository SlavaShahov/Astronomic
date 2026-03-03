package com.example.astronomic

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

class PlanetRenderer(private val context: Context) : GLSurfaceView.Renderer {

    // Фоновый квадрат
    private lateinit var backgroundSquare: BackgroundSquare
    private lateinit var backgroundTextureLoader: TextureLoader
    private var backgroundTextureId = 0

    // Сферы для планет
    private lateinit var sunSphere: TexturedSphere
    private lateinit var planetSphere: TexturedSphere
    private lateinit var moonSphere: TexturedSphere
    private lateinit var selectionCube: SelectionCube
    private lateinit var saturnRing: SaturnRing  // Кольца Сатурна
    private lateinit var textureLoader: TextureLoader

    // Матрицы
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

    // Хранилище ID текстур
    private val textureIds = mutableMapOf<String, Int>()
    private var saturnRingTextureId = 0  // Текстура для колец Сатурна

    // Индексы выбранных объектов
    private var selectedObjectIndex = 3 // Земля по умолчанию
    private var selectedIsMoon = false  // true если выбрана Луна

    // ---------- ДАННЫЕ О ПЛАНЕТАХ ----------
    private val planets = listOf(
        Planet("Солнце", 2.5f, 0f, 0f, 0.5f, "sun.jpg"),
        Planet("Меркурий", 0.4f, 5f, 0.004f, 0.8f, "mercury.jpg"),
        Planet("Венера", 0.6f, 7f, 0.003f, 0.7f, "venus.jpg"),
        Planet("Земля", 0.6f, 9f, 0.0025f, 0.6f, "earth.jpg", true),
        Planet("Марс", 0.5f, 11f, 0.002f, 0.6f, "mars.jpg"),
        Planet("Юпитер", 1.3f, 15f, 0.001f, 1.2f, "jupiter.jpg"),
        // САТУРН С КОЛЬЦАМИ
        Planet("Сатурн", 1.1f, 19f, 0.0008f, 1.0f, "saturn.jpg",
            false, true, 1.8f, 2.8f, "saturn_ring.jpg"),
        Planet("Уран", 0.9f, 23f, 0.0005f, 0.8f, "uranus.jpg"),
        Planet("Нептун", 0.9f, 27f, 0.0004f, 0.8f, "neptune.jpg")
    )

    // ---------- ДАННЫЕ О ЛУНЕ ----------
    private val moon = Planet(
        "Луна", 0.2f, 1.8f, 0.0f, 0.1f, "moon.jpg",
        false, false, 0f, 0f, "", true, 3  // parentPlanet = 3 (Земля)
    )

    // Углы вращения
    private val orbitAngles = FloatArray(planets.size) { 0f }
    private val rotationAngles = FloatArray(planets.size) { 0f }
    private var moonAngle = 0f
    private var moonRotation = 0f

    // ---------- МЕТОДЫ УПРАВЛЕНИЯ ----------

    fun getSelectedObjectName(): String {
        return if (selectedIsMoon) "Луна" else planets[selectedObjectIndex].name
    }

    fun nextObject() {
        if (selectedIsMoon) {
            // Если сейчас Луна, переходим к следующей планете
            selectedIsMoon = false
            selectedObjectIndex = (moon.parentPlanet + 1) % planets.size
        } else {
            val currentPlanet = planets[selectedObjectIndex]
            if (currentPlanet.hasMoon && selectedObjectIndex == 3) { // Земля
                selectedIsMoon = true  // Переходим на Луну
            } else {
                selectedObjectIndex = (selectedObjectIndex + 1) % planets.size
            }
        }
    }

    fun prevObject() {
        if (selectedIsMoon) {
            // Если сейчас Луна, возвращаемся к Земле
            selectedIsMoon = false
            selectedObjectIndex = moon.parentPlanet
        } else {
            val prevIndex = (selectedObjectIndex - 1 + planets.size) % planets.size
            val prevPlanet = planets[prevIndex]

            if (prevPlanet.hasMoon && prevIndex == 3) { // Земля
                selectedIsMoon = true
                selectedObjectIndex = prevIndex
            } else {
                selectedObjectIndex = prevIndex
            }
        }
    }

    fun selectPlanet(index: Int) {
        selectedObjectIndex = index
        selectedIsMoon = false
    }

    fun handlePinch(scaleFactor: Float) {
        cameraDistance = (cameraDistance / scaleFactor).coerceIn(minCameraDistance, maxCameraDistance)
    }

    fun handleDrag(dx: Float, dy: Float) {
        cameraAngleY += dx * 0.5f
        cameraAngleX = (cameraAngleX + dy * 0.5f).coerceIn(5f, 45f)
    }

    // ---------- МЕТОДЫ OpenGL ----------

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        // Инициализируем шейдеры
        ShaderManager.initShaders()
        ShaderManager.useProgram()

        // Фон
        backgroundSquare = BackgroundSquare()
        backgroundTextureLoader = TextureLoader(context)
        backgroundTextureId = backgroundTextureLoader.loadTextureFromAsset("galaxy.jpg")
        if (backgroundTextureId == 0) {
            backgroundTextureId = backgroundTextureLoader.createProceduralTexture()
        }

        // Создаем сферы
        sunSphere = TexturedSphere(64)
        planetSphere = TexturedSphere(48)
        moonSphere = TexturedSphere(32)
        selectionCube = SelectionCube()

        // СОЗДАЕМ КОЛЬЦА ДЛЯ САТУРНА (8 отдельных полосок)
        val saturn = planets[6] // Сатурн под индексом 6
        saturnRing = SaturnRing(saturn.ringInnerRadius, saturn.ringOuterRadius, 128, 8)

        // Загружаем текстуры
        textureLoader = TextureLoader(context)

        planets.forEach { planet ->
            val textureId = textureLoader.loadTextureFromAsset(planet.textureFile)
            textureIds[planet.name] = textureId
        }

        // Загружаем текстуру для колец Сатурна
        saturnRingTextureId = textureLoader.loadTextureFromAsset("saturn_ring.jpg")
        if (saturnRingTextureId == 0) {
            saturnRingTextureId = createProceduralRingTexture()  // Создаем процедурную если нет файла
        }

        // Текстура Луны
        val moonTextureId = textureLoader.loadTextureFromAsset("moon.jpg")
        textureIds["Луна"] = moonTextureId
    }

    /**
     * Создание процедурной текстуры для колец Сатурна
     * Имитирует полосатую структуру настоящих колец
     */
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

            // Базовый цвет - золотисто-коричневый
            var r = 200
            var g = 180
            var b = 150
            var alpha = 180

            // СОЗДАЕМ ПОЛОСЫ РАЗНОЙ ШИРИНЫ И ЦВЕТА
            val band = (x / 40) % 10  // Каждая полоса шириной ~40 пикселей

            when (band) {
                0, 1 -> { // Кольцо D (самое тусклое)
                    r = 140; g = 120; b = 100; alpha = 80
                }
                2 -> { // Деление между D и C
                    r = 100; g = 90; b = 80; alpha = 40
                }
                3, 4 -> { // Кольцо C (полупрозрачное)
                    r = 180; g = 160; b = 140; alpha = 140
                }
                5 -> { // Деление Кассини (темная полоса)
                    r = 120; g = 100; b = 90; alpha = 60
                }
                6, 7 -> { // Кольцо B (самое яркое)
                    r = 240; g = 220; b = 180; alpha = 220
                }
                8 -> { // Деление Энке
                    r = 160; g = 140; b = 120; alpha = 80
                }
                9 -> { // Кольцо A (внешнее)
                    r = 220; g = 200; b = 170; alpha = 200
                }
            }

            // Добавляем вариации по вертикали (толщина кольца)
            if (y < 30 || y > 90) {
                alpha = (alpha * 0.6).toInt()  // Края более прозрачные
            }

            // Текстурный шум для реалистичности
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

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        val ratio = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 1f, 1000f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // 1. Рисуем фон
        drawBackground()

        // 2. Рисуем солнечную систему (планеты и Луну)
        drawSolarSystem()
    }

    /**
     * Отрисовка фонового изображения галактики
     */
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

    /**
     * Отрисовка всей солнечной системы
     */
    private fun drawSolarSystem() {
        // Вычисляем позицию камеры на основе углов и расстояния
        val camX = cameraDistance * sin(Math.toRadians(cameraAngleY.toDouble())).toFloat() *
                cos(Math.toRadians(cameraAngleX.toDouble())).toFloat()
        val camY = cameraDistance * sin(Math.toRadians(cameraAngleX.toDouble())).toFloat()
        val camZ = cameraDistance * cos(Math.toRadians(cameraAngleY.toDouble())).toFloat() *
                cos(Math.toRadians(cameraAngleX.toDouble())).toFloat()

        Matrix.setLookAtM(viewMatrix, 0,
            camX, camY, camZ,
            0f, 0f, 0f,
            0f, 1f, 0f)

        // Рисуем все планеты
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

                // Рисуем Луну для Земли
                if (planet.hasMoon) {
                    drawMoon(x, z)
                }
            }
        }

        // Рисуем выделяющий куб поверх всего
        drawSelectionCube()
    }

    /**
     * Отрисовка отдельной планеты
     */
    private fun drawPlanet(planet: Planet, x: Float, z: Float, rotation: Float) {
        // Матрица для планеты
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, 0f, z)
        Matrix.rotateM(modelMatrix, 0, rotation, 0f, 1f, 0f)  // Вращение вокруг оси
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

        // Если у планеты есть кольца (Сатурн), рисуем их
        if (planet.hasRings) {
            drawRings(planet, x, z, rotation)
        }
    }

    /**
     * Отрисовка колец Сатурна
     * Кольца состоят из нескольких полосок с разной прозрачностью
     */
    private fun drawRings(planet: Planet, x: Float, z: Float, rotation: Float) {
        // Рисуем три слоя для создания эффекта толщины
        for (offset in -1..1) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, x, offset * 0.03f, z)  // Небольшая толщина
            Matrix.rotateM(modelMatrix, 0, 27f, 1f, 0f, 0f)  // Наклон колец Сатурна ~27°
            Matrix.rotateM(modelMatrix, 0, rotation, 0f, 1f, 0f)  // Вращение вместе с планетой

            Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

            // Разная прозрачность для разных слоев
            if (offset == 0) {
                // Центральный слой - нормальная прозрачность
                saturnRing.draw(mvpMatrix, saturnRingTextureId)
            } else {
                // Верхний и нижний слои - более прозрачные
                // Для изменения прозрачности можно использовать uniform-переменную в шейдере
                // или временно изменить параметры текстуры
                GLES20.glUniform1f(GLES20.glGetUniformLocation(ShaderManager.program, "uAlpha"), 0.3f)
                saturnRing.draw(mvpMatrix, saturnRingTextureId)
                GLES20.glUniform1f(GLES20.glGetUniformLocation(ShaderManager.program, "uAlpha"), 1.0f)
            }
        }
    }

    /**
     * Отрисовка Луны (вращается вокруг Земли перпендикулярно эклиптике)
     */
    private fun drawMoon(earthX: Float, earthZ: Float) {
        moonAngle += 0.05f
        moonRotation += moon.rotationSpeed
        if (moonAngle > 360f) moonAngle -= 360f
        if (moonRotation > 360f) moonRotation -= 360f

        // Луна вращается в вертикальной плоскости (YZ) - перпендикулярно эклиптике
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

    /**
     * Отрисовка выделяющего куба вокруг выбранного объекта (планеты или Луны)
     */
    private fun drawSelectionCube() {
        val posX: Float
        val posY: Float
        val posZ: Float
        val size: Float

        if (selectedIsMoon) {
            // Вычисляем позицию Луны
            val earthIndex = moon.parentPlanet
            val earthAngle = orbitAngles[earthIndex]
            val earthX = planets[earthIndex].distance * cos(earthAngle.toDouble()).toFloat()
            val earthZ = planets[earthIndex].distance * sin(earthAngle.toDouble()).toFloat()

            posX = earthX
            posY = moon.distance * sin(moonAngle.toDouble()).toFloat()
            posZ = earthZ + moon.distance * cos(moonAngle.toDouble()).toFloat()
            size = moon.size * 1.5f
        } else {
            // Позиция планеты
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