package com.xsytrance.vaib.core.performance

import android.util.Log
import androidx.compose.runtime.*
import kotlinx.coroutines.*

/**
 * Performance monitoring utilities for vAIb.
 * Tracks frame times, memory usage, and provides adaptive quality scaling.
 */
object PerformanceMonitor {

    private const val TAG: String = "VaibPerf"

    // ── Frame timing ──────────────────────────────────────────
    private var frameCount = 0L
    private var lastFpsTime = 0L
    private var currentFps = 60f

    /**
     * Call once per frame from the visualizer's onDrawFrame.
     * Returns current FPS (smoothed).
     */
    fun tickFrame(): Float {
        frameCount++
        val now = System.nanoTime()
        if (lastFpsTime == 0L) lastFpsTime = now
        val elapsed = (now - lastFpsTime) / 1_000_000_000f
        if (elapsed >= 1.0f) {
            currentFps = frameCount / elapsed
            frameCount = 0
            lastFpsTime = now
        }
        return currentFps
    }

    val fps: Float get() = currentFps

    // ── Adaptive quality ──────────────────────────────────────

    /** Quality level based on current FPS. */
    val qualityLevel: QualityLevel
        get() = when {
            currentFps >= 55f -> QualityLevel.HIGH
            currentFps >= 35f -> QualityLevel.MEDIUM
            else -> QualityLevel.LOW
        }

    enum class QualityLevel(val particleCount: Int, val gridDensity: Int) {
        HIGH(particleCount = 30, gridDensity = 60),
        MEDIUM(particleCount = 20, gridDensity = 40),
        LOW(particleCount = 12, gridDensity = 24),
    }

    // ── Memory monitoring ─────────────────────────────────────

    fun logMemory(tag: String = "") {
        val rt = Runtime.getRuntime()
        val used = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024
        val max = rt.maxMemory() / 1024 / 1024
        Log.d(TAG, "Memory${if (tag.isNotBlank()) " [$tag]" else ""}: ${used}MB / ${max}MB")
    }

    // ── Battery optimization hints ────────────────────────────

    /**
     * Returns true if the visualizer should reduce quality to save battery.
     * Call periodically (e.g., every 30s) to adapt.
     */
    fun shouldReduceQuality(isCharging: Boolean, batteryPct: Int): Boolean {
        if (isCharging) return false
        return batteryPct < 20
    }
}
