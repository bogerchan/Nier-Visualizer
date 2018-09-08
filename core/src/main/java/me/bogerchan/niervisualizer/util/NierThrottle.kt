package me.bogerchan.niervisualizer.util

import android.os.SystemClock

/**
 * Created by Boger Chan on 2018/9/8.
 */
class NierThrottle(private val throttleValue: Int) {
    private var lastThrottleTime = 0L

    fun process(): Boolean {
        val curTime = SystemClock.elapsedRealtime()
        val allow: Boolean
        allow = if (lastThrottleTime == 0L) true else (curTime - lastThrottleTime) > throttleValue
        if (allow) {
            lastThrottleTime = curTime
        }
        return allow
    }
}