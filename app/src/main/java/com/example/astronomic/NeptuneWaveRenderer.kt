package com.example.astronomic

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

class NeptuneWaveRenderer(private val context: Context) : GLSurfaceView.Renderer {

    inner class Point {
        var origX = 0f
        var origY = 0f
        var origZ = 0f
        var x = 0f
        var y = 0f
        var z = 0f
        var height = 0f
        var velocity = 0f
        var u = 0f
        var v = 0f
    }

    val viewMatrix = FloatArray(16)
    val projectionMatrix = FloatArray(16)

    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private val segments = 64
    private val DAMPING = 0.985f
    private val STIFFNESS = 0.08f
    private val SPREAD = 0.25f

    private lateinit var points: Array<Array<Point>>
    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var texBuffer: FloatBuffer
    private lateinit var indexBuffer: ShortBuffer
    private var indexCount = 0

    private var program = 0
    private var textureId = 0
    private var rotationAngle = 0f

    init { createSphere() }

    private fun createSphere() {

        points = Array(segments + 1) { Array(segments + 1) { Point() } }

        val vertices = mutableListOf<Float>()
        val tex = mutableListOf<Float>()
        val indices = mutableListOf<Short>()

        for (i in 0..segments) {
            val phi = PI * i / segments
            for (j in 0..segments) {

                val theta = 2 * PI * j / segments
                val x = (sin(phi) * cos(theta)).toFloat()
                val y = cos(phi).toFloat()
                val z = (sin(phi) * sin(theta)).toFloat()

                val p = points[i][j]
                p.origX = x
                p.origY = y
                p.origZ = z
                p.x = x
                p.y = y
                p.z = z
                p.u = j.toFloat() / segments
                p.v = i.toFloat() / segments

                vertices.add(x); vertices.add(y); vertices.add(z)
                tex.add(p.u); tex.add(p.v)
            }
        }

        for (i in 0 until segments) {
            for (j in 0 until segments) {
                val p1 = (i * (segments + 1) + j).toShort()
                val p2 = (i * (segments + 1) + j + 1).toShort()
                val p3 = ((i + 1) * (segments + 1) + j).toShort()
                val p4 = ((i + 1) * (segments + 1) + j + 1).toShort()
                indices.addAll(listOf(p1,p2,p3,p2,p4,p3))
            }
        }

        indexCount = indices.size

        vertexBuffer = floatBuffer(vertices.toFloatArray())
        texBuffer = floatBuffer(tex.toFloatArray())
        indexBuffer = shortBuffer(indices.toShortArray())
    }

    private fun floatBuffer(arr: FloatArray) =
        ByteBuffer.allocateDirect(arr.size * 4).order(ByteOrder.nativeOrder())
            .asFloatBuffer().put(arr).apply { position(0) }

    private fun shortBuffer(arr: ShortArray) =
        ByteBuffer.allocateDirect(arr.size * 2).order(ByteOrder.nativeOrder())
            .asShortBuffer().put(arr).apply { position(0) }

    fun addSplash(x: Float, y: Float, z: Float) {

        var minDist = Float.MAX_VALUE
        var mi = 0
        var mj = 0

        for (i in 0..segments) {
            for (j in 0..segments) {
                val p = points[i][j]
                val dx = p.origX - x
                val dy = p.origY - y
                val dz = p.origZ - z
                val d = dx*dx+dy*dy+dz*dz
                if (d < minDist) {
                    minDist = d; mi = i; mj = j
                }
            }
        }

        val radius = 0.25f

        for (i in max(0,mi-6)..min(segments,mi+6)) {
            for (j in max(0,mj-6)..min(segments,mj+6)) {
                val p = points[i][j]
                val dx = p.origX - points[mi][mj].origX
                val dy = p.origY - points[mi][mj].origY
                val dz = p.origZ - points[mi][mj].origZ
                val dist = sqrt(dx*dx+dy*dy+dz*dz)
                if (dist < radius) {
                    p.velocity += 0.2f * (1f - dist/radius)
                }
            }
        }
    }

    private fun physics() {

        for (i in 0..segments)
            for (j in 0..segments) {
                val p = points[i][j]
                p.velocity += -STIFFNESS * p.height
                p.velocity *= DAMPING
            }

        for (i in 1 until segments)
            for (j in 1 until segments) {
                val p = points[i][j]
                val avg = (
                        points[i-1][j].height +
                                points[i+1][j].height +
                                points[i][j-1].height +
                                points[i][j+1].height) * 0.25f
                p.velocity += (avg - p.height) * SPREAD
            }

        for (i in 0..segments)
            for (j in 0..segments) {
                val p = points[i][j]
                p.height += p.velocity
                val r = 1f + p.height
                p.x = p.origX * r
                p.y = p.origY * r
                p.z = p.origZ * r
            }

        val vertices = mutableListOf<Float>()
        for (i in 0..segments)
            for (j in 0..segments) {
                val p = points[i][j]
                vertices.addAll(listOf(p.x,p.y,p.z))
            }

        vertexBuffer = floatBuffer(vertices.toFloatArray())
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f,0f,0f,1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        val vs = """
            attribute vec4 aPosition;
            attribute vec2 aTex;
            varying vec2 vTex;
            uniform mat4 uMVP;
            void main(){
                vTex=aTex;
                gl_Position=uMVP*aPosition;
            }
        """

        val fs = """
            precision mediump float;
            varying vec2 vTex;
            uniform sampler2D uTexture;
            void main(){
                gl_FragColor=texture2D(uTexture,vTex);
            }
        """

        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, load(GLES20.GL_VERTEX_SHADER,vs))
        GLES20.glAttachShader(program, load(GLES20.GL_FRAGMENT_SHADER,fs))
        GLES20.glLinkProgram(program)

        textureId = TextureLoader(context).loadTextureFromAsset("neptune.jpg")
    }

    private fun load(type:Int,code:String):Int{
        val s=GLES20.glCreateShader(type)
        GLES20.glShaderSource(s,code)
        GLES20.glCompileShader(s)
        return s
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
        GLES20.glViewport(0,0,w,h)
        Matrix.perspectiveM(projectionMatrix,0,45f,w.toFloat()/h,0.1f,100f)
    }

    override fun onDrawFrame(gl: GL10?) {

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        physics()

        GLES20.glUseProgram(program)

        rotationAngle+=0.5f

        Matrix.setIdentityM(modelMatrix,0)
        Matrix.translateM(modelMatrix,0,0f,0f,-3f)
        Matrix.rotateM(modelMatrix,0,rotationAngle,0f,1f,0f)

        Matrix.setLookAtM(viewMatrix,0,
            0f,0f,5f,
            0f,0f,0f,
            0f,1f,0f)

        Matrix.multiplyMM(mvpMatrix,0,viewMatrix,0,modelMatrix,0)
        Matrix.multiplyMM(mvpMatrix,0,projectionMatrix,0,mvpMatrix,0)

        GLES20.glUniformMatrix4fv(
            GLES20.glGetUniformLocation(program,"uMVP"),
            1,false,mvpMatrix,0)

        val pos=GLES20.glGetAttribLocation(program,"aPosition")
        val tex=GLES20.glGetAttribLocation(program,"aTex")

        GLES20.glEnableVertexAttribArray(pos)
        GLES20.glVertexAttribPointer(pos,3,GLES20.GL_FLOAT,false,0,vertexBuffer)

        GLES20.glEnableVertexAttribArray(tex)
        GLES20.glVertexAttribPointer(tex,2,GLES20.GL_FLOAT,false,0,texBuffer)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textureId)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES,indexCount,
            GLES20.GL_UNSIGNED_SHORT,indexBuffer)

        GLES20.glDisableVertexAttribArray(pos)
        GLES20.glDisableVertexAttribArray(tex)
    }
}