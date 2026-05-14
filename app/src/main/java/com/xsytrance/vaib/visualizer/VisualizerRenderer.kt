package com.xsytrance.vaib.visualizer

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class VisualizerRenderer : GLSurfaceView.Renderer {

    /**
     * Written from the main thread via VisualizerSurface.update().
     * Read on the GL thread inside onDrawFrame().
     * @Volatile ensures cross-thread visibility without locking.
     */
    @Volatile var energy: Float = 0f
    @Volatile var beat:   Float = 0f   // 0..1, Kotlin-decaying at ~10 Hz

    private var program      = 0
    private var aPositionLoc = 0
    private var uResLoc      = 0
    private var uTimeLoc     = 0
    private var uEnergyLoc   = 0
    private var uBeatLoc     = 0
    private var uBeatAgeLoc  = 0

    private val quadBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(QUAD_VERTS.size * Float.SIZE_BYTES)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .also { it.put(QUAD_VERTS); it.position(0) }

    private var startNs  = 0L
    private var surfaceW = 1
    private var surfaceH = 1

    // GL-thread-only beat tracking — no @Volatile needed.
    private var prevBeat        = 0f
    private var beatTriggerTime = -99f  // large negative → beatAge huge → ring invisible

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        startNs         = System.nanoTime()
        prevBeat        = 0f
        beatTriggerTime = -99f

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
        uBeatAgeLoc  = GLES20.glGetUniformLocation(program, "uBeatAge")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        surfaceW = width
        surfaceH = height
    }

    override fun onDrawFrame(gl: GL10?) {
        val elapsed = (System.nanoTime() - startNs) / 1_000_000_000f

        // Detect rising edge of beat on the GL thread.
        // This is the only place beatTriggerTime is written, so no locking needed.
        val currentBeat = beat
        val beatAge = elapsed - beatTriggerTime  // seconds since last beat

        // Gate: don't retrigger until current ripple has expanded at least 150 ms.
        if (prevBeat < 0.5f && currentBeat >= 0.5f && beatAge > 0.15f) {
            beatTriggerTime = elapsed
        }
        prevBeat = currentBeat

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)

        GLES20.glUniform2f(uResLoc,     surfaceW.toFloat(), surfaceH.toFloat())
        GLES20.glUniform1f(uTimeLoc,    elapsed)
        GLES20.glUniform1f(uEnergyLoc,  energy)
        GLES20.glUniform1f(uBeatLoc,    currentBeat)
        GLES20.glUniform1f(uBeatAgeLoc, beatAge)

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

        //  Solo Dreamscape — audio-reactive neon pulse.
        //
        //  uEnergy  : raw RMS energy 0..1 from analyzer (~0.25–0.47 on device).
        //  uBeat    : intensity 0..1 from Kotlin, decays at ~10 Hz.
        //  uBeatAge : seconds since last beat trigger, GL-thread 60 fps.
        //
        //  Energy remap e: maps real 0.05–0.45 band to full 0–1 visual range
        //  so typical music drives the full dynamic range of the visualizer.
        //
        //  Beat ring: r 0.10→0.80 in ~400 ms. Echo follows 100 ms behind.
        //  Burst: sharp centre flash at beat moment, fades in ~150 ms.
        private val FRAG_SRC = """
            precision mediump float;
            uniform vec2  uResolution;
            uniform float uTime;
            uniform float uEnergy;
            uniform float uBeat;
            uniform float uBeatAge;

            void main() {
                // Aspect-corrected coords, origin at centre.
                vec2 uv = (gl_FragCoord.xy - uResolution * 0.5)
                          / min(uResolution.x, uResolution.y);
                float d     = length(uv);
                float angle = atan(uv.y, uv.x);

                // Remap raw energy to the observed device band (0.05–0.45 → 0–1).
                // Ensures typical music drives full visual intensity range.
                float e = clamp((uEnergy - 0.05) / 0.40, 0.0, 1.0);

                // Polar warp: organic ripple on rings, dual-frequency with energy.
                float warp = sin(angle * 3.0 + uTime * 0.55) * (0.007 + e * 0.025)
                           + sin(angle * 6.0 - uTime * 0.33) * e * 0.008;
                float dw = d + warp;

                // Breathing base radius; remapped energy pushes rings outward.
                float breathe = 0.21 + 0.033 * sin(uTime * 0.65);
                float r1 = breathe   + e * 0.16;
                float r2 = r1 * 1.40 + e * 0.10;
                float r3 = r1 * 1.82 + e * 0.07;
                float r4 = r1 * 2.28 + e * 0.04;
                float r5 = r1 * 2.78 + e * 0.025;

                // Glow rings — numerator scales with e for visible brightness jump.
                float ring1 = (0.022 + e * 0.040) / (abs(dw - r1) + 0.008);
                float ring2 = (0.014 + e * 0.022) / (abs(dw - r2) + 0.011);
                float ring3 = (0.008 + e * 0.012) / (abs(dw - r3) + 0.014);
                float ring4 = (0.005 + e * 0.006) / (abs(dw - r4) + 0.018);
                float ring5 = (0.003 + e * 0.003) / (abs(dw - r5) + 0.022);

                // Primary beat ripple: smooth 60 fps expansion via uBeatAge.
                float beatR    = 0.10 + uBeatAge * 1.75;
                float beatFade = max(0.0, 1.0 - uBeatAge * 2.5);
                float beatRing = beatFade * 0.090 / (abs(dw - beatR) + 0.005);

                // Echo ring: 100 ms behind, dimmer, faster fade.
                float age2      = max(0.0, uBeatAge - 0.10);
                float beatR2    = 0.10 + age2 * 1.75;
                float beatFade2 = max(0.0, 1.0 - age2 * 3.0);
                float beatRing2 = beatFade2 * 0.045 / (abs(dw - beatR2) + 0.008);

                // Central bloom: energy drives ambient glow from centre outward.
                float bloom = 0.060 / (d + 0.07)
                            * (0.12 + e * 1.40 + uBeat * 1.00);

                float baseTotal = ring1
                                + ring2 * 0.70
                                + ring3 * 0.45
                                + ring4 * 0.25
                                + ring5 * 0.12
                                + beatRing
                                + beatRing2
                                + bloom;

                // Energy + beat drive overall brightness.
                baseTotal *= (0.50 + e * 1.30 + uBeat * 0.90);

                // Beat burst: sharp star-like centre flash, fades in ~150 ms.
                float burst = max(0.0, 1.0 - uBeatAge * 6.5) * 0.18 / (d + 0.045);

                float total = baseTotal + burst;

                // Colour: cyan identity, slow violet drift, white-hot on beat.
                vec3 cyan   = vec3(0.000, 0.898, 1.000);
                vec3 violet = vec3(0.545, 0.361, 0.969);
                vec3 white  = vec3(1.000, 1.000, 1.000);
                float drift = 0.5 + 0.5 * sin(uTime * 0.18 + d * 2.0);
                vec3 colour = mix(cyan, violet, drift * 0.25 + e * 0.30);
                // Layered white flash: uBeat slow decay + sharp uBeatAge spike.
                float beatWhite = uBeat * 0.55 + max(0.0, 1.0 - uBeatAge * 4.0) * 0.30;
                colour = mix(colour, white, clamp(beatWhite, 0.0, 0.85));

                // OLED-safe: true black wherever total is zero.
                gl_FragColor = vec4(clamp(colour * total, 0.0, 1.0), 1.0);
            }
        """.trimIndent()
    }
}
