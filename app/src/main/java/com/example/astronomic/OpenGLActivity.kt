package com.example.astronomic

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel

class OpenGLActivity : AppCompatActivity() {

    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var renderer: PlanetRenderer

    // Для обработки жестов
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    // Выбранная планета
    private var selectedPlanetIndex = 0  // 0 = Солнце

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        renderer = PlanetRenderer(this)

        glSurfaceView = GLSurfaceView(this)
        glSurfaceView.setEGLContextClientVersion(2)
        glSurfaceView.setRenderer(renderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        setupTouchListeners()
        val mainLayout = FrameLayout(this)
        mainLayout.addView(glSurfaceView)

        val composeView = ComposeView(this)
        composeView.setContent {
            MaterialTheme {
                // Кнопки поверх
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Кнопка ВЛЕВО
                        Button(
                            onClick = {
                                selectedPlanetIndex = (selectedPlanetIndex - 1 + 9) % 9
                                renderer.selectPlanet(selectedPlanetIndex)
                            }
                        ) {
                            Text("←")
                        }

                        // Кнопка ИНФОРМАЦИЯ
                        Button(
                            onClick = {
                                renderer.showPlanetInfo(selectedPlanetIndex)
                            }
                        ) {
                            Text("ℹ️")
                        }

                        // Кнопка ВПРАВО
                        Button(
                            onClick = {
                                selectedPlanetIndex = (selectedPlanetIndex + 1) % 9
                                renderer.selectPlanet(selectedPlanetIndex)
                            }
                        ) {
                            Text("→")
                        }
                    }
                }
            }
        }

        mainLayout.addView(composeView)

        setContentView(mainLayout)
    }

    private fun setupTouchListeners() {
        glSurfaceView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.x
                    lastTouchY = event.y
                    isDragging = true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isDragging && event.pointerCount == 1) {
                        val dx = event.x - lastTouchX
                        val dy = event.y - lastTouchY
                        renderer.handleDrag(dx, dy)
                        lastTouchX = event.x
                        lastTouchY = event.y
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDragging = false
                }
            }
            true
        }

        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                renderer.handlePinch(detector.scaleFactor)
                return true
            }
        })
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