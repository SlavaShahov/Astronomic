package com.example.astronomic

import android.content.Intent
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class OpenGLActivity : AppCompatActivity() {

    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var renderer: PlanetRenderer

    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false

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
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Кнопки внизу
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Влево (предыдущий объект)
                        Button(
                            onClick = {
                                renderer.prevObject()
                            }
                        ) {
                            Text("←")
                        }

                        // Информация о выбранном объекте
                        Button(
                            onClick = {
                                val objectName = renderer.getSelectedObjectName()
                                val intent = Intent(this@OpenGLActivity, PlanetInfoActivity::class.java)
                                intent.putExtra(PlanetInfoActivity.EXTRA_PLANET_NAME, objectName)
                                startActivity(intent)
                            }
                        ) {
                            Text("ℹ️")
                        }

                        // Вправо (следующий объект)
                        Button(
                            onClick = {
                                renderer.nextObject()
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