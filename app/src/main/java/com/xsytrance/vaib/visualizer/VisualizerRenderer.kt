package com.xsytrance.vaib.visualizer

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.graphics.Color as AndroidColor
import android.view.MotionEvent

/**
 * Multi-style OpenGL renderer for the visualizer.
 *
 * Supports [VisualizerStyle.NEBULA], [VisualizerStyle.WAVEFORM], and
 * [VisualizerStyle.PARTICLES]. Switch styles via the constructor.
 *
 * Uniforms shared across all shaders:
 *   uResolution, uTime, uEnergy, uBeat, uBeatAge,
 *   uTouchPos, uTouchActive,
 *   uPrimaryColor, uSecondaryColor
 */
class VisualizerRenderer(
    private var style: VisualizerStyle = VisualizerStyle.NEBULA,
) : GLSurfaceView.Renderer {

    // ── Uniform locations ────────────────────────────────────
    private var program       = 0
    private var aPositionLoc  = 0
    private var uResLoc       = 0
    private var uTimeLoc      = 0
    private var uEnergyLoc    = 0
    private var uBeatLoc      = 0
    private var uBeatAgeLoc   = 0
    private var uTouchPosLoc  = 0
    private var uTouchActiveLoc = 0
    private var uPrimaryLoc   = 0
    private var uSecondaryLoc = 0

    // ── Volatile state (written from main thread, read on GL thread) ──
    @Volatile var energy: Float = 0f
    @Volatile var beat:   Float = 0f
    @Volatile var primaryColorArgb: Int  = AndroidColor.parseColor("#00E5FF")
    @Volatile var secondaryColorArgb: Int = AndroidColor.parseColor("#8B5CF6")

    // Touch state
    @Volatile var touchX: Float = -1f
    @Volatile var touchY: Float = -1f
    @Volatile var touchActive: Boolean = false

    // Surface dimensions (public for touch coordinate normalization)
    var surfaceWidth: Int = 1
        private set
    var surfaceHeight: Int = 1
        private set

    // ── Private state ────────────────────────────────────────
    private var startNs  = 0L

    // Beat tracking (GL thread only — no @Volatile needed)
    private var prevBeat        = 0f
    private var beatTriggerTime = -99f

    private val quadBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(QUAD_VERTS.size * Float.SIZE_BYTES)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .also { it.put(QUAD_VERTS); it.position(0) }

    // Touch event buffer (main thread writes, GL thread reads)
    private var pendingTouch: TouchEvent? = null

    private data class TouchEvent(val x: Float, val y: Float, val active: Boolean)

    // ── Public style setter ──────────────────────────────────

    fun setVisualizerStyle(newStyle: VisualizerStyle) {
        if (newStyle != style) {
            style = newStyle
            program = 0
        }
    }

    /** Forward a touch event from the main thread. Coordinates are in px relative to the surface. */
    fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        pendingTouch = when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE ->
                TouchEvent(event.x / surfaceWidth, 1f - event.y / surfaceHeight, true)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE ->
                TouchEvent(event.x / surfaceWidth, 1f - event.y / surfaceHeight, false)
            else -> null
        }
        return true
    }

    // ── GL lifecycle ──────────────────────────────────────────

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        startNs = System.nanoTime()
        prevBeat = 0f
        beatTriggerTime = -99f
        program = 0
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        surfaceWidth = width
        surfaceHeight = height
    }

    override fun onDrawFrame(gl: GL10?) {
        if (program == 0) {
            compileShaders()
            cacheUniformLocations()
        }

        val elapsed = (System.nanoTime() - startNs) / 1_000_000_000f

        // Detect rising beat edge
        val currentBeat = beat
        val beatAge = elapsed - beatTriggerTime
        if (prevBeat < 0.5f && currentBeat >= 0.5f && beatAge > 0.15f) {
            beatTriggerTime = elapsed
        }
        prevBeat = currentBeat

        // Consume pending touch
        pendingTouch?.let { t ->
            touchX = t.x
            touchY = t.y
            touchActive = t.active
            pendingTouch = null
        }
        // Touch fades out after 1.5s of inactivity
        if (touchActive && elapsed - beatTriggerTime > 1.5f) {
            touchActive = false
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)

        // Shared uniforms
        GLES20.glUniform2f(uResLoc, surfaceWidth.toFloat(), surfaceHeight.toFloat())
        GLES20.glUniform1f(uTimeLoc, elapsed)
        GLES20.glUniform1f(uEnergyLoc, energy)
        GLES20.glUniform1f(uBeatLoc, currentBeat)
        GLES20.glUniform1f(uBeatAgeLoc, beatAge)
        GLES20.glUniform2f(uTouchPosLoc, touchX, touchY)
        GLES20.glUniform1f(uTouchActiveLoc, if (touchActive) 1f else 0f)

        if (uPrimaryLoc >= 0) {
            val pr = AndroidColor.red(primaryColorArgb) / 255f
            val pg = AndroidColor.green(primaryColorArgb) / 255f
            val pb = AndroidColor.blue(primaryColorArgb) / 255f
            GLES20.glUniform3f(uPrimaryLoc, pr, pg, pb)
        }
        if (uSecondaryLoc >= 0) {
            val sr = AndroidColor.red(secondaryColorArgb) / 255f
            val sg = AndroidColor.green(secondaryColorArgb) / 255f
            val sb = AndroidColor.blue(secondaryColorArgb) / 255f
            GLES20.glUniform3f(uSecondaryLoc, sr, sg, sb)
        }

        quadBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aPositionLoc)
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, quadBuffer)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(aPositionLoc)
    }

    // ── Shader compilation ───────────────────────────────────

    private fun compileShaders() {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, VERT_SRC)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragSrcForStyle())
        program = GLES20.glCreateProgram().also { p ->
            GLES20.glAttachShader(p, vs)
            GLES20.glAttachShader(p, fs)
            GLES20.glLinkProgram(p)
            GLES20.glDeleteShader(vs)
            GLES20.glDeleteShader(fs)
        }
    }

    private fun cacheUniformLocations() {
        aPositionLoc     = GLES20.glGetAttribLocation(program, "aPosition")
        uResLoc          = GLES20.glGetUniformLocation(program, "uResolution")
        uTimeLoc         = GLES20.glGetUniformLocation(program, "uTime")
        uEnergyLoc       = GLES20.glGetUniformLocation(program, "uEnergy")
        uBeatLoc         = GLES20.glGetUniformLocation(program, "uBeat")
        uBeatAgeLoc      = GLES20.glGetUniformLocation(program, "uBeatAge")
        uTouchPosLoc     = GLES20.glGetUniformLocation(program, "uTouchPos")
        uTouchActiveLoc  = GLES20.glGetUniformLocation(program, "uTouchActive")
        uPrimaryLoc      = GLES20.glGetUniformLocation(program, "uPrimaryColor")
        uSecondaryLoc    = GLES20.glGetUniformLocation(program, "uSecondaryColor")
    }

    private fun fragSrcForStyle(): String = when (style) {
        VisualizerStyle.NEBULA   -> FRAG_SRC_NEBULA
        VisualizerStyle.WAVEFORM -> FRAG_SRC_WAVEFORM
        VisualizerStyle.PARTICLES -> FRAG_SRC_PARTICLES
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)
        return shader
    }

    // ── Vertex shader (shared) ────────────────────────────────

    companion object {
        private val QUAD_VERTS = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)

        private const val VERT_SRC = """
            attribute vec2 aPosition;
            void main() {
                gl_Position = vec4(aPosition, 0.0, 1.0);
            }
        """.trimIndent()

        // ─── Nebula: concentric rings + beat ripple + bloom + touch ripple ──
        private const val FRAG_SRC_NEBULA = """
            precision mediump float;
            uniform vec2  uResolution;
            uniform float uTime;
            uniform float uEnergy;
            uniform float uBeat;
            uniform float uBeatAge;
            uniform vec2  uTouchPos;
            uniform float uTouchActive;
            uniform vec3  uPrimaryColor;
            uniform vec3  uSecondaryColor;

            void main() {
                vec2 uv = (gl_FragCoord.xy - uResolution * 0.5)
                          / min(uResolution.x, uResolution.y);
                float d     = length(uv);
                float angle = atan(uv.y, uv.x);

                float e = clamp((uEnergy - 0.05) / 0.40, 0.0, 1.0);

                // Polar warp
                float warp = sin(angle * 3.0 + uTime * 0.55)
                           * (0.007 + e * 0.015);
                float dw = d + warp;

                // Breathing base radius
                float breathe = 0.21 + 0.033 * sin(uTime * 0.65);
                float r1 = breathe   + e * 0.12;
                float r2 = r1 * 1.40 + e * 0.07;
                float r3 = r1 * 1.82 + e * 0.05;
                float r4 = r1 * 2.28 + e * 0.03;
                float r5 = r1 * 2.78 + e * 0.018;

                // Rings
                float ring1 = (0.020 + e * 0.010) / (abs(dw - r1) + 0.009);
                float ring2 = (0.012 + e * 0.006) / (abs(dw - r2) + 0.012);
                float ring3 = (0.007 + e * 0.004) / (abs(dw - r3) + 0.016);
                float ring4 = (0.004 + e * 0.002) / (abs(dw - r4) + 0.020);
                float ring5 = (0.002 + e * 0.001) / (abs(dw - r5) + 0.025);

                // Beat ripple
                float beatR    = 0.10 + uBeatAge * 1.75;
                float beatFade = max(0.0, 1.0 - uBeatAge * 2.5);
                float beatRing = beatFade * 0.045 / (abs(dw - beatR) + 0.006);

                float age2      = max(0.0, uBeatAge - 0.10);
                float beatR2    = 0.10 + age2 * 1.75;
                float beatFade2 = max(0.0, 1.0 - age2 * 3.0);
                float beatRing2 = beatFade2 * 0.022 / (abs(dw - beatR2) + 0.009);

                // Touch ripple — rings expand outward from touch point
                float touchDist  = length(uv - uTouchPos);
                float touchRipple = uTouchActive * 0.06 / (abs(touchDist - 0.15 - uTime * 0.15) + 0.008);
                touchRipple *= smoothstep(0.5, 0.0, touchDist - 0.8);

                // Bloom
                float bloom = 0.040 / (d + 0.09)
                            * (0.20 + e * 0.70 + uBeat * 0.45 + uTouchActive * 0.30);

                float total = ring1
                            + ring2 * 0.70
                            + ring3 * 0.45
                            + ring4 * 0.25
                            + ring5 * 0.12
                            + beatRing
                            + beatRing2
                            + touchRipple
                            + bloom;

                total *= (0.40 + e * 0.35 + uBeat * 0.45 + uTouchActive * 0.15);

                // Color: blend primary/secondary with time drift & touch influence
                float drift = 0.5 + 0.5 * sin(uTime * 0.18 + d * 2.0);
                vec3 colour = mix(uPrimaryColor, uSecondaryColor, drift * 0.30 + e * 0.20);
                colour = mix(colour, vec3(1.0), uBeat * 0.35);
                // Touch adds warmth
                colour = mix(colour, vec3(1.0, 0.6, 0.3), uTouchActive * 0.25);

                gl_FragColor = vec4(clamp(colour * total, 0.0, 1.0), 1.0);
            }
        """.trimIndent()

        // ─── Waveform: 3D perspective time-domain + touch interaction ──
        private const val FRAG_SRC_WAVEFORM = """
            precision mediump float;
            uniform vec2  uResolution;
            uniform float uTime;
            uniform float uEnergy;
            uniform float uBeat;
            uniform float uBeatAge;
            uniform vec2  uTouchPos;
            uniform float uTouchActive;
            uniform vec3  uPrimaryColor;
            uniform vec3  uSecondaryColor;

            // Audio waveform shape
            float waveform(float x) {
                return sin(x * 12.0 + uTime * 2.0) * 0.15
                     + sin(x * 24.0 + uTime * 1.5) * 0.08
                     + sin(x * 6.0  + uTime * 0.8) * 0.10;
            }

            void main() {
                vec2 uv = (gl_FragCoord.xy - uResolution * 0.5)
                          / vec2(uResolution.y, uResolution.y);

                float e = clamp((uEnergy - 0.05) / 0.40, 0.0, 1.0);

                // 3D perspective
                float depth = 3.0;
                float perspScale = depth / (depth + uv.x * uv.x * 2.0);
                float perspY = uv.y * perspScale;

                float center = 0.0;
                float wave = waveform(uv.x * 0.5) * perspScale * 0.4 * (0.5 + e * 0.5);

                // Touch displacement — warps the waveform near finger
                float touchDist = length(uv - uTouchPos);
                float touchWave = uTouchActive * 0.15 * exp(-touchDist * 4.0)
                                  * sin(uTime * 6.0 + touchDist * 20.0);
                wave += touchWave;

                // Primary waveform
                float distPrimary = abs(perspY - center - wave);
                float glowPrimary = 0.025 / (distPrimary + 0.008) * perspScale;

                // Secondary mirrored
                float distSecondary = abs(perspY - center + wave * 0.6);
                float glowSecondary = 0.015 / (distSecondary + 0.01) * perspScale;

                // Grid
                float gridX = smoothstep(0.01, 0.0, abs(fract(uv.x * 4.0) - 0.5));
                float gridY = smoothstep(0.01, 0.0, abs(fract(perspY * 8.0) - 0.5));
                float grid = (gridX + gridY) * 0.03 * perspScale;

                // Beat band
                float beatBand = smoothstep(0.15, 0.0, abs(uv.y + wave * 0.3)) * uBeat * 0.3;

                // Touch glow
                float touchGlow = uTouchActive * exp(-touchDist * 8.0) * 0.15;

                vec3 color = uPrimaryColor * glowPrimary
                           + uSecondaryColor * glowSecondary
                           + vec3(0.02) * grid
                           + vec3(1.0) * beatBand
                           + uPrimaryColor * touchGlow;

                float edgeFade = smoothstep(1.8, 1.2, abs(uv.x));
                color *= edgeFade;

                gl_FragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
            }
        """.trimIndent()

        // ─── Particles: GPU particle system + touch interaction ──────────
        private const val FRAG_SRC_PARTICLES = """
            precision mediump float;
            uniform vec2  uResolution;
            uniform float uTime;
            uniform float uEnergy;
            uniform float uBeat;
            uniform float uBeatAge;
            uniform vec2  uTouchPos;
            uniform float uTouchActive;
            uniform vec3  uPrimaryColor;
            uniform vec3  uSecondaryColor;

            float hash(vec2 p) {
                return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
            }

            float hash3(vec2 p) {
                return fract(sin(dot(p + 17.0, vec2(267.1, 181.7))) * 375.9453);
            }

            void main() {
                vec2 uv = gl_FragCoord.xy / uResolution;
                float aspect = uResolution.x / uResolution.y;
                uv.x *= aspect;

                float e = clamp((uEnergy - 0.05) / 0.40, 0.0, 1.0);
                float t = uTime;

                float total = 0.0;
                vec3 totalColor = vec3(0.0);

                // Layer 1: Ambient particles
                for (float i = 0.0; i < 30.0; i++) {
                    vec2 cell = vec2(i, 0.0);
                    float seed = hash(cell + floor(t * 0.05) * 0.1);
                    vec2 pos = vec2(
                        fract(seed + t * (0.02 + e * 0.03)),
                        fract(seed * 1.7 + sin(t * 0.08 + i) * 0.5)
                    );
                    pos.x *= aspect;
                    float dist = length(uv - pos);
                    float size = 0.02 + seed * 0.03 + e * 0.02;
                    float glow = smoothstep(size, 0.0, dist);
                    float hue = seed;
                    vec3 col = mix(uPrimaryColor, uSecondaryColor, hue);
                    totalColor += col * glow * (0.3 + e * 0.4);
                    total += glow;
                }

                // Layer 2: Beat-reactive burst
                if (uBeat > 0.3) {
                    for (float i = 0.0; i < 20.0; i++) {
                        float seed = hash3(vec2(i, uBeatAge * 100.0));
                        vec2 dir = vec2(
                            cos(seed * 6.28) * (0.1 + e * 0.3),
                            sin(seed * 6.28) * (0.1 + e * 0.3)
                        );
                        vec2 pos = vec2(0.5 * aspect, 0.5) + dir * uBeatAge * 2.0;
                        float dist = length(uv - pos);
                        float fade = smoothstep(0.0, 0.8, 1.0 - uBeatAge) * (1.0 - uBeatAge);
                        float glow = smoothstep(0.05, 0.0, dist) * fade * uBeat * 0.5;
                        totalColor += uPrimaryColor * glow;
                        total += glow;
                    }
                }

                // Layer 3: Energy trails
                for (float i = 0.0; i < 8.0; i++) {
                    float phase = (i / 8.0) + t * (0.1 + e * 0.15) + sin(t * 0.5 + i) * 0.3;
                    vec2 pos = vec2(
                        phase * aspect * 1.2 - aspect * 0.1,
                        sin(phase * 3.14159 + t * 0.7) * 0.4 + 0.5
                    );
                    vec2 diff = (uv - pos);
                    float dist = length(diff);
                    float speed = 0.5 + e * 0.8;
                    float trail = smoothstep(0.3, 0.0, abs(dist - t * speed + i * 0.15));
                    trail *= smoothstep(1.5, 0.2, dist);
                    trail *= 0.15 + e * 0.25;
                    float hue = fract(i / 8.0 + t * 0.1);
                    vec3 col = mix(uPrimaryColor, uSecondaryColor, hue);
                    totalColor += col * trail;
                    total += trail;
                }

                // Layer 4: Touch particles — emits at touch point
                if (uTouchActive > 0.5) {
                    for (float i = 0.0; i < 12.0; i++) {
                        float seed = hash(uTouchPos * 100.0 + vec2(i, t));
                        float angle = seed * 6.28;
                        float speed = 0.03 + seed * 0.1;
                        vec2 dir = vec2(cos(angle), sin(angle));
                        vec2 pos = uTouchPos + dir * speed * uBeatAge * 2.5;
                        float dist = length(uv - pos);
                        float fade = smoothstep(0.0, 0.5, 1.0 - uBeatAge * 0.8);
                        float glow = smoothstep(0.04, 0.0, dist) * uTouchActive * fade * 0.8;
                        totalColor += uSecondaryColor * glow;
                        total += glow;
                    }
                }

                // Vignette
                vec2 vUV = (gl_FragCoord.xy - uResolution * 0.5) / uResolution.y;
                float vignette = 1.0 - dot(vUV, vUV) * 1.5;

                vec3 finalColor = totalColor * (1.0 + e * 0.5) * vignette;

                gl_FragColor = vec4(clamp(finalColor, 0.0, 1.0), 1.0);
            }
        """.trimIndent()
    }
}