package com.example.multiminuteurs

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_ID, -1)
        if (id < 0) return
        when (intent.action) {
            ACTION_FIRE -> TimerActions.onFired(ctx, id)
            ACTION_STOP -> TimerActions.reset(ctx, id)
            ACTION_PAUSE -> TimerActions.pause(ctx, id)
            ACTION_RESUME -> TimerActions.start(ctx, id)
        }
    }

    companion object {
        const val ACTION_FIRE = "com.example.multiminuteurs.FIRE"
        const val ACTION_STOP = "com.example.multiminuteurs.STOP"
        const val ACTION_PAUSE = "com.example.multiminuteurs.PAUSE"
        const val ACTION_RESUME = "com.example.multiminuteurs.RESUME"
        const val EXTRA_ID = "timer_id"
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            TimerActions.rescheduleAll(ctx)
        }
    }
}
