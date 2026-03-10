package com.example.astronomic

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


class Moon3DActivity : AppCompatActivity() {

    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var renderer: MoonPhongRenderer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        renderer = MoonPhongRenderer(this)

        glSurfaceView = GLSurfaceView(this)
        glSurfaceView.setEGLContextClientVersion(2)
        glSurfaceView.setRenderer(renderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        val composeView = ComposeView(this)
        composeView.setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.7f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🌕 3D МОДЕЛЬ ЛУНЫ",
                                fontSize = 20.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Освещение по модели Фонга",
                                fontSize = 14.sp,
                                color = Color.LightGray
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.7f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text("Модель Фонга:", color = Color.White, fontSize = 16.sp)
                            Text("• Ambient (фоновое): 0.2", color = Color.LightGray)
                            Text("• Diffuse (диффузное): 0.8", color = Color.LightGray)
                            Text("• Specular (зеркальное): 0.5", color = Color.LightGray)
                            Text("• Блеск: 40", color = Color.LightGray)
                        }
                    }
                }
            }
        }

        val layout = FrameLayout(this)
        layout.addView(glSurfaceView)
        layout.addView(composeView)

        setContentView(layout)
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }
}