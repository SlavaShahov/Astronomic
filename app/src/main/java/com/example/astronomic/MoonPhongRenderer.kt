package com.example.astronomic

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

/**
 * Рендерер для 3D модели Луны с освещением по модели Фонга
 * Луна вращается вокруг своей оси (как планеты в солнечной системе)
 */
class MoonPhongRenderer(private val context: Context) : GLSurfaceView.Renderer {

    // Матрицы
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    // Буферы для сферы
    private var vertexBuffer: FloatBuffer? = null
    private var normalBuffer: FloatBuffer? = null
    private var textureBuffer: FloatBuffer? = null
    private var indexBuffer: ShortBuffer? = null
    private var indexCount = 0

    // Параметры освещения
    private var lightX = 2f
    private var lightY = 2f
    private var lightZ = 2f

    // Вращение вокруг своей оси
    private var rotationAngle = 0f

    // Шейдерная программа
    private var program = 0
    private var textureId = 0

    /**
     * Создание сферы с текстурными координатами и нормалями
     */
    private fun createSphere(radius: Float, segments: Int = 48) {
        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val texCoords = mutableListOf<Float>()
        val indices = mutableListOf<Short>()

        for (i in 0..segments) {
            val phi = PI * i / segments  // широта (0 до PI)

            for (j in 0..segments) {
                val theta = 2 * PI * j / segments  // долгота (0 до 2PI)

                // Сферические координаты
                val x = (sin(phi) * cos(theta)).toFloat() * radius
                val y = (cos(phi)).toFloat() * radius
                val z = (sin(phi) * sin(theta)).toFloat() * radius

                // Позиция вершины
                vertices.add(x)
                vertices.add(y)
                vertices.add(z)

                // Нормаль (для сферы нормаль = позиция / радиус)
                normals.add(x / radius)
                normals.add(y / radius)
                normals.add(z / radius)

                // Текстурные координаты
                texCoords.add(j.toFloat() / segments)
                texCoords.add(i.toFloat() / segments)
            }
        }

        // Индексы для треугольников
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

        indexCount = indices.size

        // Создаем буфер вершин
        val vbb = ByteBuffer.allocateDirect(vertices.size * 4)
        vbb.order(ByteOrder.nativeOrder())
        vertexBuffer = vbb.asFloatBuffer()
        vertexBuffer?.put(vertices.toFloatArray())
        vertexBuffer?.position(0)

        // Создаем буфер нормалей
        val nbb = ByteBuffer.allocateDirect(normals.size * 4)
        nbb.order(ByteOrder.nativeOrder())
        normalBuffer = nbb.asFloatBuffer()
        normalBuffer?.put(normals.toFloatArray())
        normalBuffer?.position(0)

        // Создаем буфер текстурных координат
        val tbb = ByteBuffer.allocateDirect(texCoords.size * 4)
        tbb.order(ByteOrder.nativeOrder())
        textureBuffer = tbb.asFloatBuffer()
        textureBuffer?.put(texCoords.toFloatArray())
        textureBuffer?.position(0)

        // Создаем буфер индексов
        val ibb = ByteBuffer.allocateDirect(indices.size * 2)
        ibb.order(ByteOrder.nativeOrder())
        indexBuffer = ibb.asShortBuffer()
        indexBuffer?.put(indices.toShortArray())
        indexBuffer?.position(0)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)

        // Создаем сферу радиусом 1
        createSphere(1.0f, 48)

        // Вершинный шейдер
        val vertexShaderCode = """
            attribute vec4 a_vertex;
            attribute vec3 a_normal;
            attribute vec2 a_texCoord;
            
            varying vec2 v_texCoord;
            varying vec3 v_vertex;
            varying vec3 v_normal;
            
            uniform mat4 u_MVPMatrix;
            
            void main() {
                v_vertex = a_vertex.xyz;
                v_normal = a_normal;
                v_texCoord = a_texCoord;
                gl_Position = u_MVPMatrix * a_vertex;
            }
        """.trimIndent()

        // Фрагментный шейдер с моделью Фонга
        val fragmentShaderCode = """
            precision mediump float;
            
            uniform sampler2D u_texture;
            uniform vec3 u_camera;
            uniform vec3 u_lightPosition;
            
            varying vec2 v_texCoord;
            varying vec3 v_vertex;
            varying vec3 v_normal;
            
            void main() {
                // Нормализуем нормаль
                vec3 normal = normalize(v_normal);
                
                // Вектор к источнику света
                vec3 lightVector = normalize(u_lightPosition - v_vertex);
                
                // Вектор к камере
                vec3 viewVector = normalize(u_camera - v_vertex);
                
                // Отраженный вектор
                vec3 reflectVector = reflect(-lightVector, normal);
                
                // Модель Фонга
                float ambient = 0.2;
                float k_diffuse = 0.8;
                float k_specular = 0.5;
                float shininess = 40.0;
                
                // Диффузная компонента
                float diffuse = k_diffuse * max(dot(normal, lightVector), 0.0);
                
                // Зеркальная компонента
                float specular = 0.0;
                if (diffuse > 0.0) {
                    specular = k_specular * pow(max(dot(viewVector, reflectVector), 0.0), shininess);
                }
                
                // Суммарная яркость
                float brightness = ambient + diffuse + specular;
                
                // Цвет из текстуры
                vec4 texColor = texture2D(u_texture, v_texCoord);
                
                // Применяем освещение
                gl_FragColor = vec4(texColor.rgb * brightness, texColor.a);
            }
        """.trimIndent()

        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode))
        GLES20.glAttachShader(program, loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode))
        GLES20.glLinkProgram(program)

        // Загружаем текстуру Луны
        textureId = TextureLoader(context).loadTextureFromAsset("moon.jpg")
        if (textureId == 0) {
            textureId = TextureLoader(context).loadTextureFromAsset("moon_detail.jpg")
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        val ratio = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, 45f, ratio, 0.1f, 100f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        GLES20.glUseProgram(program)

        // ВРАЩЕНИЕ ВОКРУГ СВОЕЙ ОСИ
        rotationAngle += 0.8f
        if (rotationAngle > 360f) rotationAngle -= 360f

        // Матрица модели - СНАЧАЛА отодвигаем, ПОТОМ вращаем
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, 0f, -3f)  // Сначала отодвигаем от камеры
        Matrix.rotateM(modelMatrix, 0, rotationAngle, 0f, 1f, 0f)  // Потом вращаем вокруг оси Y

        // Матрица вида (камера смотрит на центр)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 5f, 0f, 0f, 0f, 0f, 1f, 0f)

        // Итоговая матрица MVP
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        // Uniform-переменные
        val mvpHandle = GLES20.glGetUniformLocation(program, "u_MVPMatrix")
        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0)

        val cameraHandle = GLES20.glGetUniformLocation(program, "u_camera")
        GLES20.glUniform3f(cameraHandle, 0f, 0f, 5f)

        val lightHandle = GLES20.glGetUniformLocation(program, "u_lightPosition")
        GLES20.glUniform3f(lightHandle, lightX, lightY, lightZ)

        val textureHandle = GLES20.glGetUniformLocation(program, "u_texture")
        GLES20.glUniform1i(textureHandle, 0)

        // Атрибуты
        val vertexHandle = GLES20.glGetAttribLocation(program, "a_vertex")
        GLES20.glEnableVertexAttribArray(vertexHandle)
        vertexBuffer?.let {
            GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, it)
        }

        val normalHandle = GLES20.glGetAttribLocation(program, "a_normal")
        GLES20.glEnableVertexAttribArray(normalHandle)
        normalBuffer?.let {
            GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 0, it)
        }

        val texCoordHandle = GLES20.glGetAttribLocation(program, "a_texCoord")
        GLES20.glEnableVertexAttribArray(texCoordHandle)
        textureBuffer?.let {
            GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, it)
        }

        // Текстура
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        // Рисуем
        indexBuffer?.let {
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, it)
        }

        // Отключаем атрибуты
        GLES20.glDisableVertexAttribArray(vertexHandle)
        GLES20.glDisableVertexAttribArray(normalHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }
}