package com.example.multiminuteurs

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

object TimerStore {
    private const val PREFS = "timers"
    private const val KEY = "list"

    private val _timers = MutableStateFlow<List<TimerItem>>(emptyList())
    val timers: StateFlow<List<TimerItem>> = _timers
    private var loaded = false

    @Synchronized
    fun init(ctx: Context) {
        if (loaded) return
        _timers.value = load(ctx.applicationContext)
        loaded = true
    }

    @Synchronized
    fun update(ctx: Context, transform: (List<TimerItem>) -> List<TimerItem>) {
        init(ctx)
        val updated = transform(_timers.value)
        _timers.value = updated
        save(ctx.applicationContext, updated)
    }

    private fun load(ctx: Context): List<TimerItem> {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                TimerItem(
                    id = o.getInt("id"),
                    label = o.optString("label", ""),
                    durationMs = o.getLong("duration"),
                    remainingMs = o.getLong("remaining"),
                    endAt = o.optLong("endAt", 0L),
                    running = o.optBoolean("running", false),
                    finished = o.optBoolean("finished", false),
                    hue = o.optDouble("hue", (o.getInt("id") * 137.508) % 360.0).toFloat(),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun save(ctx: Context, list: List<TimerItem>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("label", it.label)
                    .put("duration", it.durationMs)
                    .put("remaining", it.remainingMs)
                    .put("endAt", it.endAt)
                    .put("running", it.running)
                    .put("finished", it.finished)
                    .put("hue", it.hue.toDouble())
            )
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).commit()
    }
}
