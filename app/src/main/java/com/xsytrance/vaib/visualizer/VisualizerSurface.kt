package com.xsytrance.vaib.visualizer

import android.graphics.Color as AndroidColor
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Wrapper that hosts an OpenGL ES surface for the visualizer.
 *
 * Delegates rendering to the appropriate [VisualizerRenderer] based on [style].
 * Lifecycle-aware — pauses when the composable leaves the foreground.
 *
 * Touch interaction is forwarded to the renderer, which uses it to
 * drive ripple/displacement effects in the shader.
 */
@Composable
fun VisualizerSurface(
    modifier: Modifier = Modifier,
    style: VisualizerStyle = VisualizerStyle.NEBULA,
    energy: Float = 0f,
    beat: Float = 0f,
    primaryColor: Color = Color(0xFF00E5FF),
    secondaryColor: Color = Color(0xFF8B5CF6),
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val holder = remember { GLViewHolder() }

    // ── Convert Compose Color → ARGB int ─────────────────
    fun toArgb(c: Color): Int {
        val r = (c.red * 255).toInt()
        val g = (c.green * 255).toInt()
        val b = (c.blue * 255).toInt()
        val a = (c.alpha * 255).toInt()
        return AndroidColor.argb(a, r, g, b)
    }

    // ── Build / update the GLSurfaceView ──────────────────
    AndroidView(
        modifier = modifier
            .pointerInteropFilter { event ->
                holder.renderer?.onTouchEvent(event)
                true
            },
        factory = { context ->
            val renderer = VisualizerRenderer(style).apply {
                primaryColorArgb   = toArgb(primaryColor)
                secondaryColorArgb = toArgb(secondaryColor)
            }
            holder.renderer = renderer
            GLSurfaceView(context).apply {
                setEGLContextClientVersion(2)
                setRenderer(renderer)
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                holder.view = this
            }
        },
        update = { _ ->
            holder.renderer?.apply {
                this.energy = energy
                this.beat   = beat
                setVisualizerStyle(style)
            }
        },
    )

    // ── Lifecycle glue ─────────────────────────────────────
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> holder.view?.onResume()
                Lifecycle.Event.ON_PAUSE  -> holder.view?.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            holder.view?.onPause()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

/** Holds a reference to the GLSurfaceView + its renderer so they survive recomposition. */
private class GLViewHolder {
    var view: GLSurfaceView? = null
    var renderer: VisualizerRenderer? = null
}