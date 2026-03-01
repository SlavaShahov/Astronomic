package com.example.astronomic

import android.opengl.GLES20
import android.util.Log

object ShaderManager {

    var program = 0
        private set

    fun initShaders() {
        program = GLES20.glCreateProgram()

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, Shaders.vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, Shaders.fragmentShaderCode)

        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            Log.e("ShaderManager", "Program linking failed")
            GLES20.glDeleteProgram(program)
            program = 0
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            Log.e("ShaderManager", "Shader compilation failed")
            GLES20.glDeleteShader(shader)
            return 0
        }

        return shader
    }

    fun useProgram() {
        GLES20.glUseProgram(program)
    }
}