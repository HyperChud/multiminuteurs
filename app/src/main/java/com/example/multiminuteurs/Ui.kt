package com.example.multiminuteurs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private val Emph = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val NavHeight = 80.dp

@Composable
fun App() {
    val ctx = LocalContext.current
    val timers by TimerStore.timers.collectAsState()
    val settings by SettingsStore.settings.collectAsState()
    val s = remember(settings.lang) { stringsFor(settings) }
    val showMs = settings.showMs

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val anyRunning = timers.any { it.running }

    // Refresh: every frame with hundredths, otherwise 5 times a second
    LaunchedEffect(anyRunning, showMs) {
        now = System.currentTimeMillis()
        while (anyRunning) {
            if (showMs) withFrameMillis { } else delay(200)
            now = System.currentTimeMillis()
        }
    }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TimerItem?>(null) }
    val quip = rememberSaveable { Random.nextInt(5) }

    Box(Modifier.fillMaxSize()) {
        PatternBackground(settings.pattern, Modifier.fillMaxSize())

        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                (fadeIn(tween(320, easing = Emph)) + slideInVertically(tween(450, easing = Emph)) { it / 28 }) togetherWith
                    fadeOut(tween(140))
            },
            label = "tab"
        ) { t ->
            if (t == 0) {
                HomeScreen(
                    timers = timers, now = now, showMs = showMs, s = s, quip = s.quips[quip % s.quips.size],
                    onAdd = { showAdd = true },
                    onEdit = { editing = it },
                )
            } else {
                SettingsScreen(settings = settings, s = s) { new ->
                    SettingsStore.update(ctx) { new }
                    TimerActions.refreshNotifications(ctx)
                }
            }
        }

        BottomNav(tab = tab, onTab = { tab = it }, s = s, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (showAdd) {
        TimerSheet(
            initial = null, s = s,
            onDismiss = { showAdd = false },
            onConfirm = { label, ms -> TimerActions.add(ctx, label, ms) },
            onDelete = null,
        )
    }

    editing?.let { e ->
        TimerSheet(
            initial = e, s = s,
            onDismiss = { editing = null },
            onConfirm = { label, ms -> TimerActions.edit(ctx, e.id, label, ms) },
            onDelete = { TimerActions.delete(ctx, e.id) },
        )
    }
}

// ---------------------------------------------------------------- home

@Composable
private fun HomeScreen(
    timers: List<TimerItem>,
    now: Long,
    showMs: Boolean,
    s: Strings,
    quip: String,
    onAdd: () -> Unit,
    onEdit: (TimerItem) -> Unit,
) {
    val ctx = LocalContext.current
    val pal = LocalPal.current
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = top + 16.dp, bottom = bottom + NavHeight + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "head") { Header(s.appTitle, quip) }
        // the summary card only makes sense with several timers
        if (timers.size >= 2) item(key = "hero") { Hero(timers, now, showMs, s, Modifier.animateItem()) }
        item(key = "actions") {
            val ringing = timers.filter { it.finished }
            val running = timers.filter { it.running }
            val paused = timers.filter { it.isPaused }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TButton(
                    text = s.newShort, icon = Ic.add, onClick = onAdd, big = true,
                    container = pal.primary, content = pal.onPrimary,
                    modifier = Modifier.weight(1.6f)
                )
                if (timers.isNotEmpty()) {
                    val (label, icon, action) = when {
                        ringing.isNotEmpty() -> Triple(s.stopAll, Ic.stop) { ringing.forEach { TimerActions.reset(ctx, it.id) } }
                        running.isNotEmpty() -> Triple(s.pauseAll, Ic.pause) { running.forEach { TimerActions.pause(ctx, it.id) } }
                        paused.isNotEmpty() -> Triple(s.resumeAll, Ic.play) { paused.forEach { TimerActions.start(ctx, it.id) } }
                        else -> Triple(s.startAll, Ic.play) { timers.forEach { TimerActions.start(ctx, it.id) } }
                    }
                    BulkButton(label, icon, action, Modifier.weight(1f))
                }
            }
        }
        if (timers.isNotEmpty()) {
            item(key = "label") { SectionLabel(s.yourTimers, Modifier.padding(top = 4.dp)) }
            items(timers, key = { it.id }) { t ->
                TimerCard(
                    t = t, now = now, showMs = showMs, s = s,
                    onToggle = { TimerActions.toggle(ctx, t.id) },
                    onStart = { TimerActions.start(ctx, t.id) },
                    onReset = { TimerActions.reset(ctx, t.id) },
                    onEdit = { onEdit(t) },
                    modifier = Modifier.animateItem()
                )
            }
            item(key = "hint") {
                Text(
                    s.tapToEdit, style = Type.caption, color = pal.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun Header(title: String, quip: String) {
    val pal = LocalPal.current
    Column(Modifier.padding(start = 6.dp, end = 6.dp, bottom = 6.dp)) {
        AnimatedTitle(title, Type.title, pal.onSurface)
        var shown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { delay(200); shown = true }
        val a by animateFloatAsState(if (shown) 1f else 0f, tween(600, easing = Emph), label = "q")
        Text(
            quip, style = Type.quip, color = pal.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp).graphicsLayer { alpha = a; translationY = (1f - a) * 12f }
        )
    }
}

@Composable
private fun BulkButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier) {
    val pal = LocalPal.current
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val r by animateDpAsState(if (pressed) 40.dp else 28.dp, spring(0.5f, 600f), label = "r")
    val sc by animateFloatAsState(if (pressed) 0.93f else 1f, spring(0.45f, 500f), label = "sc")
    Column(
        modifier
            .height(76.dp)
            .graphicsLayer { scaleX = sc; scaleY = sc }
            .clip(RoundedCornerShape(r))
            .background(pal.secondaryContainer)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = pal.onSecondaryContainer, modifier = Modifier.size(24.dp))
        Text(
            label, style = Type.button.copy(fontSize = 14.sp), color = pal.onSecondaryContainer,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

/** Big card at the top, like Topé's: what's going on right now. */
@Composable
private fun Hero(timers: List<TimerItem>, now: Long, showMs: Boolean, s: Strings, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val pal = LocalPal.current
    val ringing = timers.filter { it.finished }
    val running = timers.filter { it.running }.sortedBy { it.endAt }
    val fg = pal.onPrimaryContainer

    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(36.dp, 36.dp, 36.dp, 12.dp))
            .background(pal.primaryContainer)
    ) {
        Blob(
            fg.copy(alpha = 0.16f),
            Modifier.size(150.dp).align(Alignment.TopEnd).offset(x = 30.dp, y = (-30).dp)
        )
        Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 20.dp)) {
            val wordStyle = fs(44.sp, 700, 114, (-1.2).sp)
            when {
                ringing.isNotEmpty() -> {
                    Text(s.ringing + s.ringingCount(ringing.size), style = Type.label, color = fg.copy(alpha = 0.8f))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        ringing.first().displayName(s), style = wordStyle, color = fg,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(s.finished, style = Type.body, color = fg.copy(alpha = 0.85f))
                }
                running.isNotEmpty() -> {
                    val t = running.first()
                    val rem = t.remainingAt(now)
                    val txt = formatTime(rem, showMs)
                    val size = when {
                        txt.length >= 9 -> 44.sp
                        txt.length >= 7 -> 52.sp
                        else -> 64.sp
                    }
                    Text(s.nextToRing, style = Type.label, color = fg.copy(alpha = 0.8f))
                    TimeText(
                        txt, style = fs(size, 700, 114, (-1.5).sp), color = fg, animate = true,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                    val at = android.text.format.DateFormat.getTimeFormat(ctx).format(java.util.Date(t.endAt))
                    Text(
                        t.displayName(s) + " · " + s.endsAt(at).replaceFirstChar { it.lowercase() } + s.othersRunning(running.size - 1),
                        style = Type.body, color = fg.copy(alpha = 0.85f), maxLines = 2
                    )
                    Spacer(Modifier.height(14.dp))
                    WavyProgress(
                        progress = if (t.durationMs > 0) rem.toFloat() / t.durationMs else 0f,
                        color = pal.primary, track = fg.copy(alpha = 0.14f), moving = true
                    )
                }
                else -> {
                    Text(s.rightNow, style = Type.label, color = fg.copy(alpha = 0.8f))
                    Spacer(Modifier.height(6.dp))
                    Text(s.allQuiet, style = wordStyle, color = fg)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (timers.isEmpty()) s.emptyHint else s.readyCount(timers.size),
                        style = Type.body, color = fg.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerCard(
    t: TimerItem,
    now: Long,
    showMs: Boolean,
    s: Strings,
    onToggle: () -> Unit,
    onStart: () -> Unit,
    onReset: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val pal = LocalPal.current
    val c = remember(t.hue, pal) { timerColors(t.hue, pal) }
    val rem = t.remainingAt(now)
    val progress = if (t.durationMs > 0) (rem.toFloat() / t.durationMs).coerceIn(0f, 1f) else 0f

    // Done: the card breathes in its own color
    val pulse = if (t.finished) {
        rememberInfiniteTransition(label = "pulse")
            .animateFloat(0f, 1f, infiniteRepeatable(tween(700, easing = Emph), RepeatMode.Reverse), label = "pa").value
    } else 0f
    val bg = lerp(c.container, c.accent, pulse * 0.35f)

    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (pressed) 0.97f else 1f, spring(0.5f, 500f), label = "sc")

    val status = when {
        t.finished -> s.finished
        t.running -> s.endsAt(android.text.format.DateFormat.getTimeFormat(ctx).format(java.util.Date(t.endAt)))
        t.isPaused -> s.paused
        else -> s.ready
    }

    Column(
        modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = sc; scaleY = sc }
            .clip(RoundedCornerShape(28.dp))
            .background(bg)
            .clickable(interactionSource = src, indication = null, onClick = onEdit)
            .padding(start = 20.dp, end = 16.dp, top = 18.dp, bottom = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                t.displayName(s), style = Type.label.copy(fontSize = 15.sp), color = c.onContainer.copy(alpha = 0.85f),
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                formatDuration(t.durationMs), style = fs(12.sp, 600, 100), color = c.onContainer,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(c.onContainer.copy(alpha = 0.10f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        TimeText(
            formatTime(rem, showMs),
            style = fs(if (showMs) 44.sp else 54.sp, 700, 114, (-1.2).sp),
            color = c.onContainer,
            animate = true,
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(status, style = Type.rowSub, color = c.onContainer.copy(alpha = 0.8f))

        Spacer(Modifier.height(12.dp))
        WavyProgress(progress = if (t.finished) 0f else progress, color = c.accent, track = c.track, moving = t.running)
        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val (label, icon, action) = when {
                t.finished -> Triple(s.stop, Ic.stop, onReset)
                t.running -> Triple(s.pause, Ic.pause, onToggle)
                t.isPaused -> Triple(s.resume, Ic.play, onToggle)
                else -> Triple(s.start, Ic.play, onToggle)
            }
            TButton(
                text = label, icon = icon, onClick = action, height = 52.dp,
                container = c.accent, content = c.onAccent, modifier = Modifier.weight(1f)
            )
            if (t.finished) {
                IconBtn(Ic.replay, s.restart, onStart, tint = c.onContainer)
            } else if (t.running || t.isPaused) {
                IconBtn(Ic.replay, s.reset, onReset, tint = c.onContainer)
            }
            IconBtn(Ic.edit, s.editTimer, onEdit, tint = c.onContainer)
        }
    }
}

// ---------------------------------------------------------------- bottom bar

@Composable
private fun BottomNav(tab: Int, onTab: (Int) -> Unit, s: Strings, modifier: Modifier = Modifier) {
    val pal = LocalPal.current
    Row(
        modifier
            .fillMaxWidth()
            .background(pal.surfaceLow)
            .navigationBarsPadding()
            .height(NavHeight)
            .padding(horizontal = 32.dp)
    ) {
        listOf(Ic.timer to s.appTitle, Ic.tune to s.settings).forEachIndexed { i, (icon, label) ->
            val on = i == tab
            val w by animateDpAsState(if (on) 64.dp else 24.dp, spring(0.6f, 500f), label = "w")
            val a by animateFloatAsState(if (on) 1f else 0f, tween(200), label = "a")
            val col by animateColorAsState(if (on) pal.onSurface else pal.onSurfaceVariant, tween(250), label = "c")
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTab(i) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(Modifier.size(64.dp, 34.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .width(w)
                            .fillMaxHeight()
                            .graphicsLayer { alpha = a }
                            .clip(CircleShape)
                            .background(pal.secondaryContainer)
                    )
                    Icon(icon, contentDescription = label, tint = col, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.height(4.dp))
                Text(label, style = if (on) Type.navOn else Type.nav, color = col, maxLines = 1)
            }
        }
    }
}

// ---------------------------------------------------------------- settings

@Composable
private fun SettingsScreen(settings: AppSettings, s: Strings, onChange: (AppSettings) -> Unit) {
    val ctx = LocalContext.current
    val pal = LocalPal.current
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val version = remember {
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: ""
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = top + 16.dp, bottom = bottom + NavHeight + 24.dp)
    ) {
        Header(s.settings, s.settingsQuip)

        SectionLabel(s.language)
        Card {
            Seg(
                options = listOf("system" to s.auto, "fr" to "Français", "en" to "English"),
                selected = settings.lang,
                onSelect = { onChange(settings.copy(lang = it)) }
            )
        }

        SectionLabel(s.display)
        SwitchRow(
            title = s.showMs, sub = s.showMsHint, checked = settings.showMs,
            onChange = { onChange(settings.copy(showMs = it)) }, shape = groupShape(0, 1)
        )

        SectionLabel(s.look)
        Card {
            Text(s.theme, style = Type.rowTitle, color = pal.onSurface, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
            Seg(
                options = listOf("auto" to s.auto, "light" to s.light, "dark" to s.dark, "amoled" to s.amoled),
                selected = settings.theme,
                onSelect = { onChange(settings.copy(theme = it)) }
            )
            Text(s.color, style = Type.rowTitle, color = pal.onSurface, modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                APP_HUES.forEach { (key, h) ->
                    Swatch(
                        color = hsl(h, 52f, if (pal.dark) 58f else 46f),
                        on = settings.color == key,
                        ring = pal.onSurface,
                        onClick = { onChange(settings.copy(color = key)) }
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        SwitchRow(
            title = s.pattern, sub = s.patternHint, checked = settings.pattern,
            onChange = { onChange(settings.copy(pattern = it)) }, shape = groupShape(0, 1)
        )

        SectionLabel(s.about)
        Card {
            Text("Multi Minuteurs $version".trim(), style = fs(18.sp, 700, 120), color = pal.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(s.aboutText, style = Type.body, color = pal.onSurfaceVariant)
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    val pal = LocalPal.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(pal.surfaceC)
            .padding(16.dp)
    ) { content() }
}

@Composable
private fun Swatch(color: Color, on: Boolean, ring: Color, onClick: () -> Unit) {
    val sc by animateFloatAsState(if (on) 1f else 0.82f, spring(0.45f, 400f), label = "sc")
    val ringA by animateFloatAsState(if (on) 1f else 0f, tween(200), label = "ra")
    Box(
        Modifier
            .size(52.dp)
            .graphicsLayer { scaleX = sc; scaleY = sc }
            .border(3.dp, ring.copy(alpha = ringA), CircleShape)
            .padding(6.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (on) Icon(Ic.check, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

// ---------------------------------------------------------------- new / edit sheet

private val PRESETS = listOf(1, 3, 5, 10, 15, 20, 30, 60)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerSheet(
    initial: TimerItem?,
    s: Strings,
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val pal = LocalPal.current
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun close(then: () -> Unit) {
        scope.launch { state.hide() }.invokeOnCompletion { then(); onDismiss() }
    }

    val initMs = initial?.durationMs ?: (5 * 60_000L)
    var label by remember { mutableStateOf(initial?.label ?: "") }
    var h by remember { mutableIntStateOf((initMs / 3_600_000L).toInt().coerceAtMost(23)) }
    var m by remember { mutableIntStateOf(((initMs / 60_000L) % 60).toInt()) }
    var sec by remember { mutableIntStateOf(((initMs / 1000L) % 60).toInt()) }
    val total = (h * 3600L + m * 60L + sec) * 1000L

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = pal.surfaceLow,
        contentColor = pal.onSurface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = pal.outlineVariant) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
        ) {
            Text(if (initial == null) s.newTimer else s.editTimer, style = Type.sheetTitle, color = pal.onSurface)
            Spacer(Modifier.height(18.dp))

            Text(s.name, style = Type.label, color = pal.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp, bottom = 6.dp))
            BasicTextField(
                value = label,
                onValueChange = { label = it.take(40) },
                singleLine = true,
                textStyle = fs(18.sp, 500, 100, color = pal.onSurface),
                cursorBrush = SolidColor(pal.primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                decorationBox = { inner ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(pal.surfaceHigh)
                            .padding(horizontal = 18.dp, vertical = 16.dp)
                    ) {
                        if (label.isEmpty()) {
                            Text(s.namePlaceholder, style = fs(18.sp, 420, 100), color = pal.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                        inner()
                    }
                }
            )

            Spacer(Modifier.height(18.dp))
            Text(s.duration, style = Type.label, color = pal.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PRESETS.forEach { min ->
                    val ms = min * 60_000L
                    Chip(
                        text = if (min >= 60) "${min / 60} h" else "$min min",
                        on = total == ms,
                        onClick = { h = min / 60; m = min % 60; sec = 0 }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth()) {
                listOf(s.unitH, s.unitMin, s.unitS).forEach { unit ->
                    Text(unit, style = Type.label, color = pal.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth().height(WheelItemHeight * 5)) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .height(WheelItemHeight + 4.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(pal.surfaceHigh)
                )
                Row(Modifier.fillMaxSize()) {
                    WheelPicker(count = 24, value = h, onChange = { h = it }, modifier = Modifier.weight(1f))
                    WheelPicker(count = 60, value = m, onChange = { m = it }, modifier = Modifier.weight(1f))
                    WheelPicker(count = 60, value = sec, onChange = { sec = it }, modifier = Modifier.weight(1f))
                }
            }

            if (initial != null && total != initial.durationMs && (initial.running || initial.isPaused)) {
                Text(s.editResetNote, style = Type.caption, color = pal.error, modifier = Modifier.padding(start = 6.dp, top = 10.dp))
            }

            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (onDelete != null) {
                    TButton(
                        text = null, icon = Ic.delete, onClick = { close(onDelete) },
                        container = pal.surfaceHigh, content = pal.error, modifier = Modifier.width(56.dp)
                    )
                }
                TButton(
                    text = s.cancel, onClick = { close {} },
                    container = pal.surfaceHigh, content = pal.onSurface, modifier = Modifier.weight(1f)
                )
                TButton(
                    text = if (initial == null) s.start else s.save,
                    onClick = { val l = label.trim(); val ms = total; close { onConfirm(l, ms) } },
                    container = pal.primary, content = pal.onPrimary, enabled = total > 0,
                    modifier = Modifier.weight(1.3f)
                )
            }
        }
    }
}

@Composable
private fun Chip(text: String, on: Boolean, onClick: () -> Unit) {
    val pal = LocalPal.current
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (pressed) 0.9f else 1f, spring(0.45f, 500f), label = "sc")
    val bg by animateColorAsState(if (on) pal.primary else pal.surfaceHigh, tween(220), label = "bg")
    val fg by animateColorAsState(if (on) pal.onPrimary else pal.onSurface, tween(220), label = "fg")
    Text(
        text, style = fs(15.sp, 620, 108), color = fg, maxLines = 1,
        modifier = Modifier
            .graphicsLayer { scaleX = sc; scaleY = sc }
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    )
}
