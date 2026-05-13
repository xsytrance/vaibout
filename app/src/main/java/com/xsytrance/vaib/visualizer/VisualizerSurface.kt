package com.xsytrance.vaib.visualizer

import android.opengl.GLSurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

private class GLViewHolder { var view: GLSurfaceView? = null }

@Composable
fun VisualizerSurface(modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val holder = remember { GLViewHolder() }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            GLSurfaceView(context).apply {
                setEGLContextClientVersion(2)
                setRenderer(VisualizerRenderer())
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                holder.view = this
            }
        },
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> holder.view?.onResume()
                Lifecycle.Event.ON_PAUSE -> holder.view?.onPause()
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
