package com.trace.game.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.trace.game.R
import com.trace.game.domain.FeedbackEvent

class SfxEngine(context: Context) {
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(10)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val ids = mutableMapOf<String, Int>()

    @Volatile var volume: Float = 0.85f
    @Volatile var muted: Boolean = false

    private var lastPathTickMs: Long = 0L

    init {
        val app = context.applicationContext
        ids["path"] = pool.load(app, R.raw.sfx_path_tick, 1)
        ids["deny"] = pool.load(app, R.raw.sfx_path_deny, 1)
        ids["merge"] = pool.load(app, R.raw.sfx_merge, 1)
        ids["cascade"] = pool.load(app, R.raw.sfx_cascade_step, 1)
        ids["combo5"] = pool.load(app, R.raw.sfx_combo_x5, 1)
        ids["combo10"] = pool.load(app, R.raw.sfx_combo_x10, 1)
        ids["goal"] = pool.load(app, R.raw.sfx_goal, 1)
        ids["power"] = pool.load(app, R.raw.sfx_powerup, 1)
        ids["gem"] = pool.load(app, R.raw.sfx_gem, 1)
        ids["ui"] = pool.load(app, R.raw.sfx_ui_tap, 1)
        ids["fail"] = pool.load(app, R.raw.sfx_soft_fail, 1)
    }

    fun release() {
        pool.release()
    }

    fun playUi() = play("ui")

    fun onFeedback(events: List<FeedbackEvent>) {
        for (e in events) {
            when (e) {
                is FeedbackEvent.PathUpdated -> {
                    if (e.path.isNotEmpty()) playPathTick()
                }
                FeedbackEvent.Rejected -> play("deny")
                is FeedbackEvent.MergeCommitted -> play("merge")
                is FeedbackEvent.Combo -> {
                    play(if (e.pathLen >= 10) "combo10" else "combo5")
                    play("gem")
                }
                is FeedbackEvent.LevelUp -> {
                    play("goal")
                    play("gem")
                }
                is FeedbackEvent.ShatterUsed, is FeedbackEvent.AlignUsed -> play("power")
                is FeedbackEvent.MilestoneGems -> play("gem")
                FeedbackEvent.NoMoves -> play("fail")
                is FeedbackEvent.Spawned -> { /* silent */ }
            }
        }
    }

    private fun playPathTick() {
        val now = System.currentTimeMillis()
        if (now - lastPathTickMs < 18) return
        lastPathTickMs = now
        play("path")
    }

    private fun play(key: String) {
        if (muted) return
        val id = ids[key] ?: return
        val v = volume.coerceIn(0f, 1f)
        if (v <= 0f) return
        pool.play(id, v, v, 1, 0, 1f)
    }
}
