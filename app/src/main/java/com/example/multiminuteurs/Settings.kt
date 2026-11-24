package com.example.multiminuteurs

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

data class AppSettings(
    val lang: String = "system",   // "system" | "fr" | "en"
    val showMs: Boolean = false,
)

object SettingsStore {
    private const val PREFS = "settings"
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings
    private var loaded = false

    @Synchronized
    fun init(ctx: Context) {
        if (loaded) return
        val p = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _settings.value = AppSettings(
            lang = p.getString("lang", "system") ?: "system",
            showMs = p.getBoolean("showMs", false),
        )
        loaded = true
    }

    fun current(ctx: Context): AppSettings {
        init(ctx)
        return _settings.value
    }

    @Synchronized
    fun update(ctx: Context, transform: (AppSettings) -> AppSettings) {
        init(ctx)
        val n = transform(_settings.value)
        _settings.value = n
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("lang", n.lang)
            .putBoolean("showMs", n.showMs)
            .apply()
    }
}

class Strings(private val fr: Boolean) {
    private fun t(f: String, e: String) = if (fr) f else e

    val appTitle = t("Minuteurs", "Timers")
    val emptyTitle = t("Aucun minuteur", "No timers yet")
    val emptyHint = t("Appuie sur + pour en créer un", "Tap + to create one")
    val newTimer = t("Nouveau minuteur", "New timer")
    val editTimer = t("Modifier le minuteur", "Edit timer")
    val name = t("Nom (optionnel)", "Name (optional)")
    val start = t("Lancer", "Start")
    val save = t("Enregistrer", "Save")
    val cancel = t("Annuler", "Cancel")
    val delete = t("Supprimer", "Delete")
    val finished = t("Terminé !", "Done!")
    val settings = t("Réglages", "Settings")
    val language = t("Langue", "Language")
    val system = t("Système", "System")
    val showMs = t("Millisecondes", "Milliseconds")
    val showMsHint = t("Affiche les centièmes de seconde", "Shows hundredths of a second")
    val stop = t("Arrêter", "Stop")
    val pause = t("Pause", "Pause")
    val resume = t("Reprendre", "Resume")
    val reset = t("Réinitialiser", "Reset")
    val paused = t("En pause", "Paused")
    val duration = t("Durée", "Duration")
    val channelAlarm = t("Minuteurs terminés", "Finished timers")
    val channelOngoing = t("Minuteurs en cours", "Running timers")
    val unitH = "h"
    val unitMin = "min"
    val unitS = "s"
    val editResetNote = t(
        "Changer la durée réinitialise le minuteur",
        "Changing the duration resets the timer"
    )

    fun timerN(n: Int) = t("Minuteur $n", "Timer $n")
    fun finishedTitle(label: String) = t("$label terminé", "$label done")
    fun endsAt(time: String) = t("Fin à $time", "Ends at $time")
    fun running(n: Int) = if (n == 0) t("Aucun en cours", "None running") else t("$n en cours", "$n running")
}

fun stringsFor(s: AppSettings): Strings = Strings(
    when (s.lang) {
        "fr" -> true
        "en" -> false
        else -> Locale.getDefault().language == "fr"
    }
)

fun stringsFor(ctx: Context): Strings = stringsFor(SettingsStore.current(ctx))
