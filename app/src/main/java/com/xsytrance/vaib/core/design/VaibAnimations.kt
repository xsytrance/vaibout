package com.xsytrance.vaib.core.design

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*

/**
 * Haptic feedback helper — wraps Vibrator API with safe fallbacks.
 */
object HapticHelper {

    private fun getVibrator(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun lightTap(context: Context) {
        val v = getVibrator(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(10)
        }
    }

    fun mediumTap(context: Context) {
        val v = getVibrator(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(20)
        }
    }

    fun heavyTap(context: Context) {
        val v = getVibrator(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(40)
        }
    }
}

// ── Standard animation specs ─────────────────────────────────────

object VaibAnimations {
    /** Standard card/content entry animation */
    @Composable
    fun cardEnter(): EnterTransition = fadeIn(
        animationSpec = tween(MotionTokens.Standard, easing = MotionTokens.FastOutSlowIn)
    ) + slideInVertically(
        animationSpec = tween(MotionTokens.Standard, easing = MotionTokens.FastOutSlowIn),
        initialOffsetY = { it / 4 },
    )

    /** Standard exit animation */
    @Composable
    fun cardExit(): ExitTransition = fadeOut(
        animationSpec = tween(MotionTokens.Quick, easing = MotionTokens.FastOutLinearIn)
    ) + slideOutVertically(
        animationSpec = tween(MotionTokens.Quick, easing = MotionTokens.FastOutLinearIn),
        targetOffsetY = { -it / 4 },
    )

    /** Screen transition — enter from right */
    @Composable
    fun screenEnter(): EnterTransition = fadeIn(
        animationSpec = tween(MotionTokens.Standard)
    ) + slideInHorizontally(
        animationSpec = tween(MotionTokens.Standard, easing = MotionTokens.FastOutSlowIn),
        initialOffsetX = { it / 3 },
    )

    /** Screen transition — exit to left */
    @Composable
    fun screenExit(): ExitTransition = fadeOut(
        animationSpec = tween(MotionTokens.Quick)
    ) + slideOutHorizontally(
        animationSpec = tween(MotionTokens.Quick, easing = MotionTokens.FastOutLinearIn),
        targetOffsetX = { -it / 3 },
    )

    /** Scale + fade for buttons */
    @Composable
    fun buttonPress(): Float {
        val animatable = remember { Animatable(1f) }
        LaunchedEffect(Unit) { /* triggered externally */ }
        return animatable.value
    }

    /** Staggered list item entry */
    @Composable
    fun staggeredEnter(index: Int, staggerMs: Int = 50): EnterTransition =
        fadeIn(
            animationSpec = tween(
                durationMillis = MotionTokens.Standard,
                delayMillis = index * staggerMs,
                easing = MotionTokens.FastOutSlowIn,
            )
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = MotionTokens.Standard,
                delayMillis = index * staggerMs,
                easing = MotionTokens.FastOutSlowIn,
            ),
            initialOffsetY = { it / 6 },
        )
}
