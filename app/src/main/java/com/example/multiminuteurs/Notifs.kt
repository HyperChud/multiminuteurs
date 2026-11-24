package com.example.multiminuteurs

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.Date

object Notifs {
    private const val CH_ALARM = "timer_alarm_v1"
    private const val CH_ONGOING = "timer_ongoing_v1"
    private const val ONGOING_BASE = 100_000

    /** Creates the channels, or updates their names if the language changed. */
    fun ensureChannels(ctx: Context) {
        val s = stringsFor(ctx)
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val alarm = NotificationChannel(CH_ALARM, s.channelAlarm, NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(
                sound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 600, 400, 600)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val ongoing = NotificationChannel(CH_ONGOING, s.channelOngoing, NotificationManager.IMPORTANCE_LOW).apply {
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannels(listOf(alarm, ongoing))
    }

    private fun canNotify(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun action(ctx: Context, action: String, id: Int): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, id,
            Intent(ctx, TimerReceiver::class.java)
                .setAction(action)
                .putExtra(TimerReceiver.EXTRA_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun open(ctx: Context, id: Int): PendingIntent =
        PendingIntent.getActivity(
            ctx, id,
            Intent(ctx, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    @SuppressLint("MissingPermission")
    fun showFinished(ctx: Context, t: TimerItem) {
        ensureChannels(ctx)
        if (!canNotify(ctx)) return
        val s = stringsFor(ctx)
        val stop = action(ctx, TimerReceiver.ACTION_STOP, t.id)
        val notif = NotificationCompat.Builder(ctx, CH_ALARM)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(s.finishedTitle(t.displayName(s)))
            .setContentText("${s.duration} : ${formatDuration(t.durationMs)}")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(open(ctx, t.id))
            .setDeleteIntent(stop)
            .setAutoCancel(true)
            .addAction(0, s.stop, stop)
            .build()
        notif.flags = notif.flags or Notification.FLAG_INSISTENT // rings in a loop
        NotificationManagerCompat.from(ctx).notify(t.id, notif)
    }

    /** Ongoing notification: countdown while the timer runs, "Paused" otherwise. */
    @SuppressLint("MissingPermission")
    fun syncOngoing(ctx: Context, t: TimerItem) {
        if (!(t.running || t.isPaused)) {
            cancelOngoing(ctx, t.id)
            return
        }
        if (!canNotify(ctx)) return
        ensureChannels(ctx)
        val s = stringsFor(ctx)
        val b = NotificationCompat.Builder(ctx, CH_ONGOING)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(t.displayName(s))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(open(ctx, t.id))

        if (t.running) {
            val time = DateFormat.getTimeFormat(ctx).format(Date(t.endAt))
            b.setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(t.endAt)
                .setShowWhen(true)
                .setContentText(s.endsAt(time))
                .addAction(0, s.pause, action(ctx, TimerReceiver.ACTION_PAUSE, t.id))
        } else {
            b.setShowWhen(false)
                .setContentText("${s.paused} · ${formatDuration(t.remainingMs)}")
                .addAction(0, s.resume, action(ctx, TimerReceiver.ACTION_RESUME, t.id))
                .addAction(0, s.reset, action(ctx, TimerReceiver.ACTION_STOP, t.id))
        }
        NotificationManagerCompat.from(ctx).notify(ONGOING_BASE + t.id, b.build())
    }

    fun cancelAlarm(ctx: Context, id: Int) = NotificationManagerCompat.from(ctx).cancel(id)
    fun cancelOngoing(ctx: Context, id: Int) = NotificationManagerCompat.from(ctx).cancel(ONGOING_BASE + id)
}
