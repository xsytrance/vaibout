package com.xsytrance.vaib.visualizer

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class VisualizerRenderer : GLSurfaceView.Renderer {

    /** Written from the main thread; read on the GL thread every frame. */
    @Volatile var energy: Float = 0f

    private var program = 0
    private var aPositionLoc = 0
    private var uResolutionLoc = 0
    private var uTimeLoc = 0
    private var uEnergyLoc = 0

    private val quadBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(QUAD_VERTS.size * Float.SIZE_BYTES)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .also { it.put(QUAD_VERTS); it.position(0) }

    private var startNs = 0L
    private var surfaceWidth = 1
    private var surfaceHeight = 1

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        startNs = System.nanoTime()

        val vs = compileShader(GLES20.GL_VERTEX_SHADER, VERT_SRC)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAG_SRC)
        program = GLES20.glCreateProgram().also { p ->
            GLES20.glAttachShader(p, vs)
            GLES20.glAttachShader(p, fs)
            GLES20.glLinkProgram(p)
            GLES20.glDeleteShader(vs)
            GLES20.glDeleteShader(fs)
        }

        aPositionLoc   = GLES20.glGetAttribLocation(program, "aPosition")
        uResolutionLoc = GLES20.glGetUniformLocation(program, "uResolution")
        uTimeLoc       = GLES20.glGetUniformLocation(program, "uTime")
        uEnergyLoc     = GLES20.glGetUniformLocation(program, "uEnergy")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        surfaceWidth = width
        surfaceHeight = height
    }

    override fun onDrawFrame(gl: GL10?) {
        val elapsed = (System.nanoTime() - startNs) / 1_000_000_000f

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)

        GLES20.glUniform2f(uResolutionLoc, surfaceWidth.toFloat(), surfaceHeight.toFloat())
        GLES20.glUniform1f(uTimeLoc, elapsed)
        GLES20.glUniform1f(uEnergyLoc, energy)

        quadBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aPositionLoc)
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, quadBuffer)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(aPositionLoc)
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)
        return shader
    }

    companion object {
        // Fullscreen triangle strip: bottom-left → bottom-right → top-left → top-right
        private val QUAD_VERTS = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)

        private val VERT_SRC = """
            attribute vec2 aPosition;
            void main() {
                gl_Position = vec4(aPosition, 0.0, 1.0);
            }
        """.trimIndent()

        private val FRAG_SRC = """
            precision mediump float;
            uniform vec2  uResolution;
            uniform float uTime;
            uniform float uEnergy;

            void main() {
                // Aspect-corrected coords, origin at centre.
                vec2 uv = (gl_FragCoord.xy - uResolution * 0.5)
                          / min(uResolution.x, uResolution.y);
                float d = length(uv);

                // Slowly breathing radii, each kicked outward by audio energy.
                float breathe = 0.28 + 0.04 * sin(uTime * 0.7);
                float r1 = breathe        + uEnergy * 0.12;
                float r2 = r1 * 1.55      + uEnergy * 0.06;
                float r3 = r1 * 2.20      + uEnergy * 0.04;

                // Soft glow rings via inverse distance field.
                float ring1 = 0.018 / (abs(d - r1) + 0.010);
                float ring2 = 0.009 / (abs(d - r2) + 0.014);
                float ring3 = 0.005 / (abs(d - r3) + 0.020);

                // Radial bloom at the centre, intensified by energy.
                float bloom = 0.030 / (d + 0.12) * (0.25 + uEnergy * 0.75);

                float total = ring1 + ring2 * 0.6 + ring3 * 0.35 + bloom;
                total *= (0.55 + uEnergy * 0.45);

                // Colour: slow drift between cyan and violet; energy pushes violet.
                vec3 cyan   = vec3(0.00, 0.898, 1.000);
                vec3 violet = vec3(0.545, 0.361, 0.969);
                float t = 0.5 + 0.5 * sin(uTime * 0.25 + d * 1.8);
                vec3 colour = mix(cyan, violet, t * 0.35 + uEnergy * 0.30);

                // OLED-safe: pure black where total is zero.
                gl_FragColor = vec4(clamp(colour * total, 0.0, 1.0), 1.0);
            }
        """.trimIndent()
    }
}
