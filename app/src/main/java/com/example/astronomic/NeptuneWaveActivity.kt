package com.example.astronomic

import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Bundle
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sqrt

class NeptuneWaveActivity : AppCompatActivity() {

    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var renderer: NeptuneWaveRenderer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        renderer = NeptuneWaveRenderer(this)

        glSurfaceView = GLSurfaceView(this)
        glSurfaceView.setEGLContextClientVersion(2)
        glSurfaceView.setRenderer(renderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        glSurfaceView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val hit = screenPointToSphere(event)
                if (hit != null) {
                    renderer.addSplash(hit[0], hit[1], hit[2])
                }
            }
            true
        }

        val composeView = ComposeView(this)
        composeView.setContent {
            MaterialTheme {
                Box(modifier = Modifier.fillMaxSize()) {
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
                            Text("🌊 НЕПТУН", fontSize = 20.sp, color = Color.White)
                            Text(
                                "Нажми на планету - пойдут волны",
                                fontSize = 14.sp,
                                color = Color.LightGray
                            )
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

    private fun screenPointToSphere(event: MotionEvent): FloatArray? {

        val x = (2f * event.x) / glSurfaceView.width - 1f
        val y = 1f - (2f * event.y) / glSurfaceView.height

        val near = floatArrayOf(x, y, -1f, 1f)
        val far = floatArrayOf(x, y, 1f, 1f)

        val mvp = FloatArray(16)
        val inverted = FloatArray(16)

        Matrix.multiplyMM(
            mvp, 0,
            renderer.projectionMatrix, 0,
            renderer.viewMatrix, 0
        )

        Matrix.invertM(inverted, 0, mvp, 0)

        val nearRes = FloatArray(4)
        val farRes = FloatArray(4)

        Matrix.multiplyMV(nearRes, 0, inverted, 0, near, 0)
        Matrix.multiplyMV(farRes, 0, inverted, 0, far, 0)

        for (i in 0..2) {
            nearRes[i] /= nearRes[3]
            farRes[i] /= farRes[3]
        }

        val dirX = farRes[0] - nearRes[0]
        val dirY = farRes[1] - nearRes[1]
        val dirZ = farRes[2] - nearRes[2]

        val len = sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ)

        val rayX = dirX / len
        val rayY = dirY / len
        val rayZ = dirZ / len

        val originZ = 5f

        val a = 1f
        val b = 2f * originZ * rayZ
        val c = originZ * originZ - 1f

        val discriminant = b * b - 4f * a * c
        if (discriminant < 0f) return null

        val t = (-b - sqrt(discriminant)) / (2f * a)

        val hitX = rayX * t
        val hitY = rayY * t
        val hitZ = originZ + rayZ * t

        val hitLen = sqrt(hitX * hitX + hitY * hitY + hitZ * hitZ)

        return floatArrayOf(
            hitX / hitLen,
            hitY / hitLen,
            hitZ / hitLen
        )
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