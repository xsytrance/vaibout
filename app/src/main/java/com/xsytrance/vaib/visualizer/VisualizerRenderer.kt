package com.xsytrance.vaib.visualizer

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class VisualizerRenderer : GLSurfaceView.Renderer {

    /** Both written from the main thread, read on the GL thread each frame. */
    @Volatile var energy: Float = 0f
    @Volatile var beat:   Float = 0f

    private var program      = 0
    private var aPositionLoc = 0
    private var uResLoc      = 0
    private var uTimeLoc     = 0
    private var uEnergyLoc   = 0
    private var uBeatLoc     = 0

    private val quadBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(QUAD_VERTS.size * Float.SIZE_BYTES)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .also { it.put(QUAD_VERTS); it.position(0) }

    private var startNs     = 0L
    private var surfaceW    = 1
    private var surfaceH    = 1

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        startNs = System.nanoTime()

        val vs = compileShader(GLES20.GL_VERTEX_SHADER,   VERT_SRC)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAG_SRC)
        program = GLES20.glCreateProgram().also { p ->
            GLES20.glAttachShader(p, vs)
            GLES20.glAttachShader(p, fs)
            GLES20.glLinkProgram(p)
            GLES20.glDeleteShader(vs)
            GLES20.glDeleteShader(fs)
        }

        aPositionLoc = GLES20.glGetAttribLocation(program,  "aPosition")
        uResLoc      = GLES20.glGetUniformLocation(program, "uResolution")
        uTimeLoc     = GLES20.glGetUniformLocation(program, "uTime")
        uEnergyLoc   = GLES20.glGetUniformLocation(program, "uEnergy")
        uBeatLoc     = GLES20.glGetUniformLocation(program, "uBeat")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        surfaceW = width
        surfaceH = height
    }

    override fun onDrawFrame(gl: GL10?) {
        val elapsed = (System.nanoTime() - startNs) / 1_000_000_000f

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)

        GLES20.glUniform2f(uResLoc,    surfaceW.toFloat(), surfaceH.toFloat())
        GLES20.glUniform1f(uTimeLoc,   elapsed)
        GLES20.glUniform1f(uEnergyLoc, energy)
        GLES20.glUniform1f(uBeatLoc,   beat)

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
        private val QUAD_VERTS = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)

        private val VERT_SRC = """
            attribute vec2 aPosition;
            void main() {
                gl_Position = vec4(aPosition, 0.0, 1.0);
            }
        """.trimIndent()

        //  Solo Dreamscape — neon pulse with beat reactivity.
        //
        //  5 breathing rings with polar warp for dimension,
        //  a beat-born ripple that expands and fades,
        //  a central bloom scaled by energy and transient,
        //  cyan-to-violet drift, white-hot flash on beat.
        private val FRAG_SRC = """
            precision mediump float;
            uniform vec2  uResolution;
            uniform float uTime;
            uniform float uEnergy;
            uniform float uBeat;

            void main() {
                // Aspect-corrected coords, origin at centre.
                vec2 uv = (gl_FragCoord.xy - uResolution * 0.5)
                          / min(uResolution.x, uResolution.y);
                float d     = length(uv);
                float angle = atan(uv.y, uv.x);

                // Polar warp: sinusoidal ripple around the ring boundary.
                // Intensity grows with audio energy.
                float warp = sin(angle * 3.0 + uTime * 0.55)
                             * (0.007 + uEnergy * 0.013);
                float dw = d + warp;

                // Slowly breathing base radius, pushed out by energy.
                float breathe = 0.21 + 0.033 * sin(uTime * 0.65);
                float r1 = breathe        + uEnergy * 0.10;
                float r2 = r1 * 1.40      + uEnergy * 0.06;
                float r3 = r1 * 1.82      + uEnergy * 0.04;
                float r4 = r1 * 2.28      + uEnergy * 0.025;
                float r5 = r1 * 2.78      + uEnergy * 0.015;

                // Soft inverse-distance-field glows, inner rings brighter.
                float ring1 = 0.020 / (abs(dw - r1) + 0.009);
                float ring2 = 0.012 / (abs(dw - r2) + 0.012);
                float ring3 = 0.007 / (abs(dw - r3) + 0.016);
                float ring4 = 0.004 / (abs(dw - r4) + 0.020);
                float ring5 = 0.002 / (abs(dw - r5) + 0.025);

                // Beat ripple: born at centre radius, expands and fades.
                float beatR    = 0.10 + (1.0 - uBeat) * 0.70;
                float beatRing = uBeat * 0.038 / (abs(dw - beatR) + 0.007);

                // Central bloom: scales with both energy and beat.
                float bloom = 0.040 / (d + 0.09)
                              * (0.20 + uEnergy * 0.80 + uBeat * 0.55);

                float total = ring1
                            + ring2 * 0.70
                            + ring3 * 0.45
                            + ring4 * 0.25
                            + ring5 * 0.12
                            + beatRing
                            + bloom;

                // Energy scales overall brightness; beat adds a flash.
                total *= (0.45 + uEnergy * 0.55 + uBeat * 0.55);

                // Colour: cyan identity, slow violet drift, white-hot on beat.
                vec3 cyan   = vec3(0.000, 0.898, 1.000);
                vec3 violet = vec3(0.545, 0.361, 0.969);
                vec3 white  = vec3(1.000, 1.000, 1.000);
                float drift = 0.5 + 0.5 * sin(uTime * 0.18 + d * 2.0);
                vec3 colour = mix(cyan, violet,
                                  drift * 0.30 + uEnergy * 0.25);
                colour = mix(colour, white, uBeat * 0.38);

                // OLED-safe: true black wherever total is zero.
                gl_FragColor = vec4(clamp(colour * total, 0.0, 1.0), 1.0);
            }
        """.trimIndent()
    }
}
