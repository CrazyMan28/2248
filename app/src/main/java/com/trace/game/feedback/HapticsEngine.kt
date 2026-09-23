package com.trace.game.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import com.trace.game.domain.FeedbackEvent

class HapticsEngine(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        mgr.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    @Volatile
    var enabled: Boolean = true

    private var lastPathTickMs: Long = 0L

    fun uiClick(view: View?) {
        if (!enabled) return
        view?.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    fun onFeedback(events: List<FeedbackEvent>) {
        if (!enabled || vibrator == null || !vibrator.hasVibrator()) return
        for (e in events) {
            when (e) {
                is FeedbackEvent.PathUpdated -> pathTick()
                FeedbackEvent.Rejected -> pulse(12, 80)
                is FeedbackEvent.MergeCommitted -> mergePulse()
                is FeedbackEvent.Combo -> comboPulse(e.pathLen)
                is FeedbackEvent.LevelUp -> celebrate()
                is FeedbackEvent.ShatterUsed, is FeedbackEvent.AlignUsed -> powerPulse()
                FeedbackEvent.NoMoves -> pulse(25, 60)
                is FeedbackEvent.Spawned -> { /* silent */ }
            }
        }
    }

    private fun pathTick() {
        val now = System.currentTimeMillis()
        if (now - lastPathTickMs < 18) return
        lastPathTickMs = now
        composeOrOneShot(
            prim = VibrationEffect.Composition.PRIMITIVE_CLICK,
            scale = 0.45f,
            fallbackMs = 10,
            fallbackAmp = 50,
        )
    }

    private fun mergePulse() {
        composeOrOneShot(
            prim = VibrationEffect.Composition.PRIMITIVE_THUD,
            scale = 0.7f,
            fallbackMs = 30,
            fallbackAmp = 120,
        )
    }

    private fun comboPulse(pathLen: Int) {
        val scale = (0.75f + (pathLen / 20f)).coerceAtMost(1f)
        composeOrOneShot(
            prim = VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
            scale = scale,
            fallbackMs = 40,
            fallbackAmp = 180,
        )
    }

    private fun celebrate() {
        composeOrOneShot(
            prim = VibrationEffect.Composition.PRIMITIVE_SLOW_RISE,
            scale = 1f,
            fallbackMs = 60,
            fallbackAmp = 220,
        )
    }

    private fun powerPulse() {
        composeOrOneShot(
            prim = VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
            scale = 0.75f,
            fallbackMs = 35,
            fallbackAmp = 160,
        )
    }

    private fun pulse(ms: Long, amp: Int) {
        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(ms, amp.coerceIn(1, 255)))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(ms)
        }
    }

    private fun composeOrOneShot(prim: Int, scale: Float, fallbackMs: Long, fallbackAmp: Int) {
        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(prim, scale)
                    .compose()
                v.vibrate(effect)
                return
            } catch (_: Throwable) {
                // fall through
            }
        }
        pulse(fallbackMs, fallbackAmp)
    }
}
