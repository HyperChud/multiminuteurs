package com.example.multiminuteurs

data class TimerItem(
    val id: Int,
    val label: String,
    val durationMs: Long,
    val remainingMs: Long,       // valid while the timer is not running
    val endAt: Long = 0L,        // end time (wall clock) while running
    val running: Boolean = false,
    val finished: Boolean = false,
    val hue: Float = 0f,         // border hue (0-360)
)

fun TimerItem.remainingAt(now: Long): Long =
    if (running) (endAt - now).coerceIn(0L, durationMs) else remainingMs

val TimerItem.isPaused: Boolean
    get() = !running && !finished && remainingMs > 0 && remainingMs < durationMs

fun TimerItem.displayName(s: Strings): String = label.ifBlank { s.timerN(id) }

/** Rounded up to the next second (classic countdown). */
fun formatDuration(ms: Long): String {
    val total = (ms + 999) / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

/** Includes hundredths when showMs is on. */
fun formatTime(ms: Long, showMs: Boolean): String {
    if (!showMs) return formatDuration(ms)
    val cs = ms / 10
    val h = cs / 360_000
    val m = (cs / 6_000) % 60
    val s = (cs / 100) % 60
    val c = cs % 100
    return if (h > 0) "%d:%02d:%02d.%02d".format(h, m, s, c) else "%02d:%02d.%02d".format(m, s, c)
}
