package com.trace.game

import android.app.Application
import com.trace.game.data.UserPrefsRepository
import com.trace.game.feedback.HapticsEngine
import com.trace.game.feedback.SfxEngine

class TraceApp : Application() {
    lateinit var prefs: UserPrefsRepository
        private set
    lateinit var haptics: HapticsEngine
        private set
    lateinit var sfx: SfxEngine
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = UserPrefsRepository(this)
        haptics = HapticsEngine(this)
        sfx = SfxEngine(this)
    }
}
