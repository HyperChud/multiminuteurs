package com.example.multiminuteurs

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.Violet,
            secondary = Palette.Cyan,
            background = Palette.Bg,
            surface = Color(0xFF14141F),
            onSurface = Color.White,
            onBackground = Color.White,
        ),
        content = content
    )
}

@Composable
fun TimersScreen() {
    val ctx = LocalContext.current
    val timers by TimerStore.timers.collectAsState()
    val settings by SettingsStore.settings.collectAsState()
    val s = remember(settings) { stringsFor(settings) }
    val showMs = settings.showMs

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val anyRunning = timers.any { it.running }

    // Refresh: every frame when showing centiseconds, otherwise 5x/s
    LaunchedEffect(anyRunning, showMs) {
        now = System.currentTimeMillis()
        while (anyRunning) {
            if (showMs) withFrameMillis { } else delay(200)
            now = System.currentTimeMillis()
        }
    }

    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TimerItem?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        LiquidBackground(Modifier.fillMaxSize())

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Header(s, timers.count { it.running }) { showSettings = true }

            if (timers.isEmpty()) {
                EmptyState(s, Modifier.weight(1f).fillMaxWidth())
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(start = 18.dp, top = 10.dp, end = 18.dp, bottom = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    items(timers, key = { it.id }) { t ->
                        TimerCard(
                            t = t,
                            now = now,
                            showMs = showMs,
                            s = s,
                            onToggle = { TimerActions.toggle(ctx, t.id) },
                            onReset = { TimerActions.reset(ctx, t.id) },
                            onEdit = { editing = t },
                            onDelete = { TimerActions.delete(ctx, t.id) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }

        GradientCircleButton(
            onClick = { showAdd = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(24.dp),
            size = 66.dp
        ) {
            Icon(Icons.Filled.Add, contentDescription = s.newTimer, tint = Color.White, modifier = Modifier.size(32.dp))
        }
    }

    if (showAdd) {
        TimerDialog(
            initial = null,
            s = s,
            onDismiss = { showAdd = false },
            onConfirm = { label, ms ->
                TimerActions.add(ctx, label, ms)
                showAdd = false
            }
        )
    }

    editing?.let { e ->
        TimerDialog(
            initial = e,
            s = s,
            onDismiss = { editing = null },
            onConfirm = { label, ms ->
                TimerActions.edit(ctx, e.id, label, ms)
                editing = null
            }
        )
    }

    if (showSettings) {
        SettingsSheet(
            settings = settings,
            s = s,
            onDismiss = { showSettings = false },
            onChange = { new ->
                SettingsStore.update(ctx) { new }
                TimerActions.refreshNotifications(ctx)
            }
        )
    }
}

@Composable
private fun Header(s: Strings, runningCount: Int, onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = s.appTitle,
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Text(text = s.running(runningCount), color = Palette.TextDim, fontSize = 15.sp)
        }
        GlassIconButton(onClick = onSettings) {
            Icon(Icons.Filled.Settings, contentDescription = s.settings, tint = Color.White)
        }
    }
}

@Composable
private fun EmptyState(s: Strings, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .padding(32.dp)
                .glass(RoundedCornerShape(32.dp))
                .padding(horizontal = 36.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = s.emptyTitle, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(text = s.emptyHint, color = Palette.TextDim, fontSize = 15.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun TimerCard(
    t: TimerItem,
    now: Long,
    showMs: Boolean,
    s: Strings,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val rem = t.remainingAt(now)
    val progress = if (t.durationMs > 0) (rem.toFloat() / t.durationMs).coerceIn(0f, 1f) else 0f
    val corner = 30.dp
    val shape = RoundedCornerShape(corner)
    val (c1, c2) = remember(t.hue) { timerColors(t.hue) }

    // Done: full border that pulses
    val pulse = if (t.finished) {
        val tr = rememberInfiniteTransition(label = "pulse")
        tr.animateFloat(
            0.3f, 1f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "pulseA"
        ).value
    } else 1f

    val subtitle = when {
        t.finished -> s.finished
        t.running -> s.endsAt(
            android.text.format.DateFormat.getTimeFormat(ctx).format(java.util.Date(t.endAt))
        )
        t.isPaused -> s.paused
        else -> null
    }

    Column(
        modifier
            .fillMaxWidth()
            .progressBorder(
                progress = if (t.finished) 1f else progress,
                c1 = c1,
                c2 = c2,
                corner = corner,
                intensity = pulse
            )
            .glass(shape, fill = 0.07f, edge = 0.18f)
            .background(
                Brush.linearGradient(listOf(c1.copy(alpha = 0.16f), Color.Transparent, c2.copy(alpha = 0.06f)))
            )
            .padding(horizontal = 22.dp, vertical = 20.dp)
    ) {
        // Name + total duration
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.linearGradient(listOf(c1, c2)))
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = t.displayName(s),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = formatDuration(t.durationMs),
                color = Palette.TextDim,
                fontSize = 12.sp,
                style = Tnum,
                modifier = Modifier
                    .glass(RoundedCornerShape(50), fill = 0.06f, edge = 0.15f)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        // Time remaining
        Text(
            text = formatTime(rem, showMs),
            color = if (t.finished) c1 else Color.White,
            fontSize = if (showMs) 44.sp else 56.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-1).sp,
            style = Tnum,
            maxLines = 1,
            softWrap = false
        )

        Text(
            text = subtitle ?: " ",
            color = if (t.finished) c1 else Palette.TextDim,
            fontSize = 13.sp,
            fontWeight = if (t.finished) FontWeight.SemiBold else FontWeight.Normal
        )

        Spacer(Modifier.height(16.dp))

        // Actions
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GradientCircleButton(
                onClick = onToggle,
                size = 54.dp,
                brush = Brush.linearGradient(listOf(c1, c2))
            ) {
                if (t.running) {
                    PauseGlyph(Color.White)
                } else {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = if (t.isPaused) s.resume else s.start,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            GlassIconButton(onClick = onReset) {
                Icon(Icons.Filled.Refresh, contentDescription = s.reset, tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            GlassIconButton(onClick = onEdit, size = 42.dp) {
                Icon(Icons.Filled.Edit, contentDescription = s.editTimer, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(20.dp))
            }
            GlassIconButton(onClick = onDelete, size = 42.dp) {
                Icon(Icons.Filled.Delete, contentDescription = s.delete, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun TimerDialog(
    initial: TimerItem?,
    s: Strings,
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit,
) {
    val initMs = initial?.durationMs ?: (5 * 60_000L)
    var label by remember { mutableStateOf(initial?.label ?: "") }
    var h by remember { mutableIntStateOf((initMs / 3_600_000L).toInt().coerceAtMost(23)) }
    var m by remember { mutableIntStateOf(((initMs / 60_000L) % 60).toInt()) }
    var sec by remember { mutableIntStateOf(((initMs / 1000L) % 60).toInt()) }
    val total = (h * 3600L + m * 60L + sec) * 1000L
    val panel = RoundedCornerShape(32.dp)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .clip(panel)
                .background(Brush.verticalGradient(listOf(Color(0xF21C1A33), Color(0xF20E1424))))
                .border(
                    1.dp,
                    Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.03f), Color.White.copy(alpha = 0.2f))
                    ),
                    panel
                )
                .padding(22.dp)
        ) {
            Text(
                text = if (initial == null) s.newTimer else s.editTimer,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text(s.name) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                    cursorColor = Palette.Cyan,
                    focusedBorderColor = Palette.Cyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedLabelColor = Palette.Cyan,
                    unfocusedLabelColor = Palette.TextDim,
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth()) {
                listOf(s.unitH, s.unitMin, s.unitS).forEach { unit ->
                    Text(
                        text = unit,
                        color = Palette.TextDim,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))

            Box(Modifier.fillMaxWidth().height(WheelItemHeight * 5)) {
                // Selection band behind the wheels
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .height(WheelItemHeight)
                        .glass(RoundedCornerShape(16.dp), 0.10f, 0.25f)
                )
                Row(Modifier.fillMaxSize()) {
                    WheelPicker(count = 24, initial = h, onChange = { h = it }, modifier = Modifier.weight(1f))
                    WheelPicker(count = 60, initial = m, onChange = { m = it }, modifier = Modifier.weight(1f))
                    WheelPicker(count = 60, initial = sec, onChange = { sec = it }, modifier = Modifier.weight(1f))
                }
            }

            if (initial != null && total != initial.durationMs && (initial.running || initial.isPaused)) {
                Spacer(Modifier.height(10.dp))
                Text(text = s.editResetNote, color = Palette.Pink, fontSize = 12.sp)
            }

            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PillButton(text = s.cancel, onClick = onDismiss, modifier = Modifier.weight(1f))
                PillButton(
                    text = if (initial == null) s.start else s.save,
                    onClick = { onConfirm(label.trim(), total) },
                    modifier = Modifier.weight(1f),
                    primary = true,
                    enabled = total > 0
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: AppSettings,
    s: Strings,
    onDismiss: () -> Unit,
    onChange: (AppSettings) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xF0121220),
        contentColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.5f)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
        ) {
            Text(text = s.settings, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))

            Text(text = s.language, color = Palette.TextDim, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().glass(RoundedCornerShape(50), 0.06f).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("system" to s.system, "fr" to "Français", "en" to "English").forEach { (key, name) ->
                    val selected = settings.lang == key
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .then(if (selected) Modifier.background(Palette.Accent) else Modifier)
                            .clickable { onChange(settings.copy(lang = key)) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name,
                            color = if (selected) Color.White else Palette.TextDim,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .glass(RoundedCornerShape(22.dp), 0.06f)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(text = s.showMs, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(text = s.showMsHint, color = Palette.TextDim, fontSize = 13.sp)
                }
                Switch(
                    checked = settings.showMs,
                    onCheckedChange = { onChange(settings.copy(showMs = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Palette.Violet,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.8f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.12f),
                        uncheckedBorderColor = Color.White.copy(alpha = 0.25f),
                    )
                )
            }
        }
    }
}
