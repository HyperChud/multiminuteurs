package com.example.multiminuteurs

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlin.math.abs
import kotlin.random.Random

object TimerActions {

    fun add(ctx: Context, label: String, durationMs: Long) {
        var newId = 0
        TimerStore.update(ctx) { list ->
            newId = (list.maxOfOrNull { it.id } ?: 0) + 1
            list + TimerItem(
                id = newId, label = label, durationMs = durationMs, remainingMs = durationMs,
                hue = pickHue(list.map { it.hue })
            )
        }
        start(ctx, newId)
    }

    /** Random hue, avoiding ones too close to existing timers. */
    private fun pickHue(existing: List<Float>): Float {
        var best = Random.nextFloat() * 360f
        repeat(12) {
            val h = Random.nextFloat() * 360f
            val ok = existing.all { e -> val d = abs(e - h) % 360f; minOf(d, 360f - d) >= 35f }
            if (ok) return h
            best = h
        }
        return best
    }

    fun toggle(ctx: Context, id: Int) {
        TimerStore.init(ctx)
        val t = TimerStore.timers.value.find { it.id == id } ?: return
        if (t.running) pause(ctx, id) else start(ctx, id)
    }

    fun start(ctx: Context, id: Int) {
        val now = System.currentTimeMillis()
        var started: TimerItem? = null
        TimerStore.update(ctx) { list ->
            list.map { t ->
                if (t.id == id && !t.running) {
                    val rem = if (t.finished || t.remainingMs <= 0) t.durationMs else t.remainingMs
                    t.copy(running = true, finished = false, endAt = now + rem, remainingMs = rem)
                        .also { started = it }
                } else t
            }
        }
        Notifs.cancelAlarm(ctx, id)
        started?.let {
            Alarms.schedule(ctx, it)
            Notifs.syncOngoing(ctx, it)
        }
    }

    fun pause(ctx: Context, id: Int) {
        val now = System.currentTimeMillis()
        Alarms.cancel(ctx, id)
        var paused: TimerItem? = null
        TimerStore.update(ctx) { list ->
            list.map { t ->
                if (t.id == id && t.running)
                    t.copy(running = false, remainingMs = (t.endAt - now).coerceAtLeast(0L))
                        .also { paused = it }
                else t
            }
        }
        paused?.let { Notifs.syncOngoing(ctx, it) }
    }

    fun reset(ctx: Context, id: Int) {
        Alarms.cancel(ctx, id)
        Notifs.cancelAlarm(ctx, id)
        Notifs.cancelOngoing(ctx, id)
        TimerStore.update(ctx) { list ->
            list.map { t ->
                if (t.id == id) t.copy(running = false, finished = false, remainingMs = t.durationMs)
                else t
            }
        }
    }

    fun delete(ctx: Context, id: Int) {
        Alarms.cancel(ctx, id)
        Notifs.cancelAlarm(ctx, id)
        Notifs.cancelOngoing(ctx, id)
        TimerStore.update(ctx) { list -> list.filter { it.id != id } }
    }

    /** Name only: state is kept. Duration changed: the timer is reset and stopped. */
    fun edit(ctx: Context, id: Int, label: String, newDurationMs: Long) {
        var edited: TimerItem? = null
        TimerStore.update(ctx) { list ->
            list.map { t ->
                if (t.id != id) t
                else (
                    if (newDurationMs == t.durationMs) t.copy(label = label)
                    else t.copy(
                        label = label, durationMs = newDurationMs, remainingMs = newDurationMs,
                        running = false, finished = false, endAt = 0L
                    )
                ).also { edited = it }
            }
        }
        edited?.let { e ->
            if (!e.running) {
                Alarms.cancel(ctx, id)
                Notifs.cancelAlarm(ctx, id)
            }
            Notifs.syncOngoing(ctx, e)
        }
    }

    fun onFired(ctx: Context, id: Int) {
        var fired: TimerItem? = null
        TimerStore.update(ctx) { list ->
            list.map { t ->
                if (t.id == id && t.running)
                    t.copy(running = false, finished = true, remainingMs = 0L).also { fired = it }
                else t
            }
        }
        fired?.let {
            Notifs.cancelOngoing(ctx, it.id)
            Notifs.showFinished(ctx, it)
        }
    }

    /** Reschedules alarms + notifications (app launch, reboot, update). */
    fun rescheduleAll(ctx: Context) {
        TimerStore.init(ctx)
        val now = System.currentTimeMillis()
        TimerStore.timers.value.forEach { t ->
            when {
                t.running && t.endAt <= now -> onFired(ctx, t.id)
                t.running -> {
                    Alarms.schedule(ctx, t)
                    Notifs.syncOngoing(ctx, t)
                }
                t.isPaused -> Notifs.syncOngoing(ctx, t)
            }
        }
    }

    /** After a language or notification-permission change. */
    fun refreshNotifications(ctx: Context) {
        Notifs.ensureChannels(ctx)
        TimerStore.init(ctx)
        TimerStore.timers.value.forEach { Notifs.syncOngoing(ctx, it) }
    }
}

object Alarms {
    private fun pendingIntent(ctx: Context, id: Int): PendingIntent {
        val intent = Intent(ctx, TimerReceiver::class.java)
            .setAction(TimerReceiver.ACTION_FIRE)
            .putExtra(TimerReceiver.EXTRA_ID, id)
        return PendingIntent.getBroadcast(
            ctx, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun schedule(ctx: Context, t: TimerItem) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(ctx, t.id)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (canExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.endAt, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.endAt, pi)
        }
    }

    fun cancel(ctx: Context, id: Int) {
        ctx.getSystemService(AlarmManager::class.java).cancel(pendingIntent(ctx, id))
    }
}
