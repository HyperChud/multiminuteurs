package com.example.multiminuteurs

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

data class AppSettings(
    val lang: String = "system",   // "system" | "fr" | "en"
    val showMs: Boolean = false,
    val theme: String = "auto",    // "auto" | "light" | "dark" | "amoled"
    val color: String = "violet",  // key of APP_HUES
    val pattern: Boolean = true,   // faint background pattern
)

/** App accent colors (hue in degrees). */
val APP_HUES = linkedMapOf(
    "violet" to 272f,
    "vert" to 140f,
    "orange" to 28f,
    "bleu" to 212f,
    "rose" to 335f,
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
        val d = AppSettings()
        _settings.value = AppSettings(
            lang = p.getString("lang", d.lang) ?: d.lang,
            showMs = p.getBoolean("showMs", d.showMs),
            theme = p.getString("theme", d.theme) ?: d.theme,
            color = (p.getString("color", d.color) ?: d.color).takeIf { it in APP_HUES } ?: d.color,
            pattern = p.getBoolean("pattern", d.pattern),
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
            .putString("theme", n.theme)
            .putString("color", n.color)
            .putBoolean("pattern", n.pattern)
            .apply()
    }
}

class Strings(val fr: Boolean) {
    private fun t(f: String, e: String) = if (fr) f else e

    val appTitle = t("Minuteurs", "Timers")
    val newTimer = t("Nouveau minuteur", "New timer")
    val newShort = t("Nouveau", "New")
    val editTimer = t("Modifier le minuteur", "Edit timer")
    val name = t("Nom", "Name")
    val namePlaceholder = t("Pâtes, lessive, sieste…", "Pasta, laundry, nap…")
    val start = t("Lancer", "Start")
    val save = t("Enregistrer", "Save")
    val cancel = t("Annuler", "Cancel")
    val delete = t("Supprimer", "Delete")
    val finished = t("Terminé !", "Done!")
    val settings = t("Réglages", "Settings")
    val language = t("Langue", "Language")
    val auto = t("Auto", "Auto")
    val showMs = t("Centièmes de seconde", "Hundredths of a second")
    val showMsHint = t("Pour les chronos au cordeau", "For down-to-the-wire timing")
    val stop = t("Arrêter", "Stop")
    val pause = t("Pause", "Pause")
    val resume = t("Reprendre", "Resume")
    val restart = t("Relancer", "Restart")
    val reset = t("Remettre à zéro", "Reset")
    val paused = t("En pause", "Paused")
    val ready = t("Prêt à partir", "Ready to go")
    val duration = t("Durée", "Duration")
    val channelAlarm = t("Minuteurs terminés", "Finished timers")
    val channelOngoing = t("Minuteurs en cours", "Running timers")
    val unitH = "h"
    val unitMin = "min"
    val unitS = "s"
    val editResetNote = t(
        "Changer la durée remet le minuteur à zéro.",
        "Changing the duration resets the timer."
    )

    // Home
    val yourTimers = t("Tes minuteurs", "Your timers")
    val nextToRing = t("Prochain à sonner", "Next to ring")
    val ringing = t("Ça sonne", "Ringing")
    val rightNow = t("En ce moment", "Right now")
    val allQuiet = t("Calme plat", "All quiet")
    val emptyHint = t("Crée ton premier minuteur juste en dessous.", "Make your first timer just below.")
    val pauseAll = t("Tout figer", "Freeze all")
    val resumeAll = t("Tout reprendre", "Resume all")
    val startAll = t("Tout lancer", "Start all")
    val stopAll = t("Tout couper", "Silence all")
    val tapToEdit = t("Touche une carte pour la modifier", "Tap a card to edit it")

    // Settings
    val settingsQuip = t("Réglé comme une horloge.", "Set like clockwork.")
    val display = t("Affichage", "Display")
    val look = t("Apparence", "Look")
    val theme = t("Thème", "Theme")
    val light = t("Clair", "Light")
    val dark = t("Sombre", "Dark")
    val amoled = "AMOLED"
    val color = t("Couleur", "Colour")
    val pattern = t("Motif de fond", "Background pattern")
    val patternHint = t("Des chronos et des sabliers en filigrane", "Faint stopwatches and hourglasses in the back")
    val about = t("À propos", "About")
    val aboutText = t(
        "Aucune pub, aucun pistage, aucun accès réseau. Tout reste sur ton téléphone.",
        "No ads, no tracking, no network access. Everything stays on your phone."
    )

    /** Shown under the title, one picked per launch. */
    val quips = if (fr) listOf(
        "Tout vient à point à qui sait minuter.",
        "Plusieurs fers au feu, zéro pâtes trop cuites.",
        "Ici, on ne compte pas. On décompte.",
        "Chaque seconde compte. Nous aussi.",
        "Toujours à l'heure, même quand toi non.",
    ) else listOf(
        "Good things come to those who time.",
        "Many irons in the fire, zero soggy pasta.",
        "We don't count. We count down.",
        "Every second counts. So do we.",
        "Always on time, even when you're not.",
    )

    fun timerN(n: Int) = t("Minuteur $n", "Timer $n")
    fun finishedTitle(label: String) = t("$label terminé", "$label done")
    fun endsAt(time: String) = t("Fin à $time", "Ends at $time")
    fun running(n: Int) = if (n == 0) t("Aucun en cours", "None running") else t("$n en cours", "$n running")
    fun othersRunning(n: Int) = when {
        n <= 0 -> ""
        fr -> if (n == 1) " · 1 autre en cours" else " · $n autres en cours"
        else -> if (n == 1) " · 1 more running" else " · $n more running"
    }
    fun readyCount(n: Int) = when {
        fr -> if (n == 1) "1 minuteur prêt à partir." else "$n minuteurs prêts à partir."
        else -> if (n == 1) "1 timer ready to go." else "$n timers ready to go."
    }
    fun ringingCount(n: Int) = when {
        n <= 1 -> ""
        fr -> " · $n sonnent"
        else -> " · $n ringing"
    }
}

fun stringsFor(s: AppSettings): Strings = Strings(
    when (s.lang) {
        "fr" -> true
        "en" -> false
        else -> Locale.getDefault().language == "fr"
    }
)

fun stringsFor(ctx: Context): Strings = stringsFor(SettingsStore.current(ctx))
