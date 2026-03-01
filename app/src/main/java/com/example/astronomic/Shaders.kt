package com.example.astronomic

object Shaders {

    // Вершинный шейдер
    val vertexShaderCode = """
        attribute vec4 vPosition;
        attribute vec2 inputTextureCoordinate;
        varying vec2 textureCoordinate;
        uniform mat4 uMVPMatrix;
        
        void main() {
            gl_Position = uMVPMatrix * vPosition;
            textureCoordinate = inputTextureCoordinate;
        }
    """.trimIndent()

    // Фрагментный шейдер
    val fragmentShaderCode = """
        precision mediump float;
        varying vec2 textureCoordinate;
        uniform sampler2D inputImageTexture;
        
        void main() {
            gl_FragColor = texture2D(inputImageTexture, textureCoordinate);
        }
    """.trimIndent()
}