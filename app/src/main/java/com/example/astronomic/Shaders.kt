package com.example.astronomic

object Shaders {

    // Вершинный шейдер - передает текстурные координаты
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

    // Фрагментный шейдер - принимает текстурные координаты
    val fragmentShaderCode = """
        precision mediump float;
        varying vec2 textureCoordinate;
        uniform sampler2D inputImageTexture;
        uniform vec4 vColor;
        uniform bool useTexture;
        
        void main() {
            if (useTexture) {
                gl_FragColor = texture2D(inputImageTexture, textureCoordinate);
            } else {
                gl_FragColor = vColor;
            }
        }
    """.trimIndent()
}