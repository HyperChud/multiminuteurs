package com.example.multiminuteurs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.using
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ---------------------------------------------------------------- icons (Material, 24 dp)

object Ic {
    private fun v(d: String) = ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .addPath(addPathNodes(d), fill = SolidColor(Color.Black))
        .build()

    val timer by lazy { v("M15,1H9v2h6V1zM11,14h2V8h-2V14zM19.03,7.39l1.42,-1.42c-0.43,-0.51 -0.9,-0.99 -1.41,-1.41l-1.42,1.42C16.07,4.74 14.12,4 12,4c-4.97,0 -9,4.03 -9,9s4.02,9 9,9s9,-4.03 9,-9C21,10.88 20.26,8.93 19.03,7.39zM12,20c-3.87,0 -7,-3.13 -7,-7s3.13,-7 7,-7s7,3.13 7,7S15.87,20 12,20z") }
    val tune by lazy { v("M3,17v2h6v-2H3zM3,5v2h10V5H3zM13,21v-2h8v-2h-8v-2h-2v6H13zM7,9v2H3v2h4v2h2V9H7zM21,13v-2H11v2H21zM15,9h2V7h4V5h-4V3h-2V9z") }
    val pause by lazy { v("M6,19h4V5H6v14zM14,5v14h4V5h-4z") }
    val play by lazy { v("M8,5v14l11,-7z") }
    val add by lazy { v("M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z") }
    val replay by lazy { v("M12,5V1L7,6l5,5V7c3.31,0 6,2.69 6,6s-2.69,6 -6,6 -6,-2.69 -6,-6H4c0,4.42 3.58,8 8,8s8,-3.58 8,-8 -3.58,-8 -8,-8z") }
    val edit by lazy { v("M3,17.25V21h3.75L17.81,9.94l-3.75,-3.75L3,17.25zM20.71,7.04c0.39,-0.39 0.39,-1.02 0,-1.41l-2.34,-2.34c-0.39,-0.39 -1.02,-0.39 -1.41,0l-1.83,1.83 3.75,3.75 1.83,-1.83z") }
    val delete by lazy { v("M6,19c0,1.1 0.9,2 2,2h8c1.1,0 2,-0.9 2,-2V7H6v12zM19,4h-3.5l-1,-1h-5l-1,1H5v2h14V4z") }
    val stop by lazy { v("M6,6h12v12H6z") }
    val check by lazy { v("M9,16.17L4.83,12l-1.42,1.42L9,19 21,7l-1.41,-1.41z") }
}

// ---------------------------------------------------------------- buttons (they squish when pressed, like in Topé)

private val squishSpring = spring<Float>(dampingRatio = 0.45f, stiffness = 500f)

@Composable
fun TButton(
    text: String?,
    onClick: () -> Unit,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    big: Boolean = false,
    enabled: Boolean = true,
    height: Dp = if (big) 76.dp else 56.dp,
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val rest = if (big) 28.dp else height / 2
    val r by animateDpAsState(
        if (pressed) (if (big) 40.dp else 18.dp) else rest,
        spring(dampingRatio = 0.5f, stiffness = 600f), label = "r"
    )
    val sx by animateFloatAsState(if (pressed) (if (big) 0.95f else 0.93f) else 1f, squishSpring, label = "sx")
    val sy by animateFloatAsState(if (pressed) 0.9f else 1f, squishSpring, label = "sy")
    val bg by animateColorAsState(container, tween(300), label = "bg")
    val fg by animateColorAsState(content, tween(300), label = "fg")
    Row(
        modifier
            .height(height)
            .graphicsLayer { scaleX = sx; scaleY = sy; alpha = if (enabled) 1f else 0.4f }
            .clip(RoundedCornerShape(r))
            .background(bg)
            .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = if (text == null) 0.dp else 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = text, tint = fg, modifier = Modifier.size(if (big) 28.dp else 24.dp))
            if (text != null) Spacer(Modifier.width(10.dp))
        }
        if (text != null) {
            Text(
                text, style = if (big) Type.buttonBig else Type.button, color = fg,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun IconBtn(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    tint: Color,
    modifier: Modifier = Modifier,
    container: Color = Color.Transparent,
    size: Dp = 48.dp,
) {
    val pal = LocalPal.current
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (pressed) 0.85f else 1f, squishSpring, label = "sc")
    val bg by animateColorAsState(
        if (pressed && container == Color.Transparent) pal.onSurface.copy(alpha = 0.10f) else container,
        tween(200), label = "bg"
    )
    Box(
        modifier
            .size(size)
            .graphicsLayer { scaleX = sc; scaleY = sc }
            .clip(CircleShape)
            .background(bg)
            .clickable(interactionSource = src, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(24.dp))
    }
}

// ---------------------------------------------------------------- segmented control (the pill slides)

@Composable
fun <T> Seg(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pal = LocalPal.current
    val idx = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(CircleShape)
            .background(pal.surfaceHigh)
            .padding(5.dp)
    ) {
        val w = maxWidth / options.size
        val x by animateDpAsState(w * idx, spring(dampingRatio = 0.72f, stiffness = 420f), label = "x")
        Box(
            Modifier
                .offset(x = x)
                .width(w)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(pal.primary)
        )
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, (k, label) ->
                val c by animateColorAsState(if (i == idx) pal.onPrimary else pal.onSurfaceVariant, tween(250), label = "c")
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(k) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, style = Type.button.copy(fontSize = 15.sp), color = c, maxLines = 1)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- lists and settings rows

/** Rows stuck together: big corners on the outside, small ones in between (like Topé). */
fun groupShape(i: Int, n: Int): RoundedCornerShape {
    val big = 22.dp
    val small = 8.dp
    return when {
        n == 1 -> RoundedCornerShape(big)
        i == 0 -> RoundedCornerShape(big, big, small, small)
        i == n - 1 -> RoundedCornerShape(small, small, big, big)
        else -> RoundedCornerShape(small)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    val pal = LocalPal.current
    Text(
        text.uppercase(),
        style = Type.section,
        color = pal.onSurfaceVariant,
        modifier = modifier.padding(start = 10.dp, top = 20.dp, bottom = 10.dp)
    )
}

@Composable
fun SwitchRow(
    title: String,
    sub: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
) {
    val pal = LocalPal.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(pal.surfaceC)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onChange(!checked) }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.rowTitle, color = pal.onSurface)
            if (sub != null) Text(sub, style = Type.rowSub, color = pal.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = pal.onPrimary,
                checkedTrackColor = pal.primary,
                checkedBorderColor = pal.primary,
                uncheckedThumbColor = pal.outline,
                uncheckedTrackColor = pal.surfaceHighest,
                uncheckedBorderColor = pal.outline,
            )
        )
    }
}

// ---------------------------------------------------------------- background pattern (drifts slowly, like Topé's)

private class Motif(val path: Path, val x: Float, val y: Float, val angle: Float)

private fun stopwatch(r: Float) = Path().apply {
    addOval(Rect(Offset.Zero, r))
    moveTo(0f, 0f); lineTo(r * 0.38f, -r * 0.42f)
    moveTo(0f, -r); lineTo(0f, -r * 1.3f)
    moveTo(-r * 0.26f, -r * 1.3f); lineTo(r * 0.26f, -r * 1.3f)
    moveTo(r * 0.72f, -r * 0.72f); lineTo(r * 0.9f, -r * 0.9f)
}

private fun hourglass(h: Float) = Path().apply {
    val w = h * 0.62f
    moveTo(-w, -h); lineTo(w, -h)
    moveTo(-w, h); lineTo(w, h)
    moveTo(-w * 0.8f, -h); quadraticTo(-w * 0.8f, -h * 0.3f, -w * 0.12f, 0f); quadraticTo(-w * 0.8f, h * 0.3f, -w * 0.8f, h)
    moveTo(w * 0.8f, -h); quadraticTo(w * 0.8f, -h * 0.3f, w * 0.12f, 0f); quadraticTo(w * 0.8f, h * 0.3f, w * 0.8f, h)
    moveTo(-w * 0.42f, h * 0.72f); lineTo(w * 0.42f, h * 0.72f)
}

private fun alarmClock(r: Float) = Path().apply {
    addOval(Rect(Offset.Zero, r))
    addArc(Rect(Offset(-r * 0.78f, -r * 0.86f), r * 0.42f), 190f, 150f)
    addArc(Rect(Offset(r * 0.78f, -r * 0.86f), r * 0.42f), 200f, 150f)
    moveTo(-r * 0.62f, r * 0.8f); lineTo(-r * 0.9f, r * 1.15f)
    moveTo(r * 0.62f, r * 0.8f); lineTo(r * 0.9f, r * 1.15f)
    moveTo(0f, -r * 0.55f); lineTo(0f, 0f); lineTo(r * 0.4f, 0f)
}

@Composable
fun PatternBackground(enabled: Boolean, modifier: Modifier = Modifier) {
    val pal = LocalPal.current
    Box(modifier.background(pal.surface)) {
        if (enabled) {
            val tr = rememberInfiniteTransition(label = "pat")
            val drift = tr.animateFloat(0f, 1f, infiniteRepeatable(tween(80_000, easing = LinearEasing)), label = "drift")
            val color = pal.onSurface.copy(alpha = if (pal.dark) 0.075f else 0.07f)
            Spacer(
                Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val s = 260.dp.toPx()
                        val u = 1.dp.toPx()
                        val motifs = listOf(
                            Motif(stopwatch(24 * u), 0.22f * s, 0.20f * s, -14f),
                            Motif(hourglass(22 * u), 0.74f * s, 0.30f * s, 18f),
                            Motif(alarmClock(20 * u), 0.40f * s, 0.70f * s, 9f),
                            Motif(stopwatch(15 * u), 0.88f * s, 0.80f * s, 24f),
                            Motif(hourglass(13 * u), 0.08f * s, 0.62f * s, -28f),
                        )
                        val stroke = Stroke(width = 2.2f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        onDrawBehind {
                            val off = drift.value * s
                            val cols = (size.width / s).toInt() + 1
                            val rows = (size.height / s).toInt() + 1
                            for (i in -1..cols) for (j in -1..rows) {
                                translate(i * s + off, j * s + off) {
                                    motifs.forEach { m ->
                                        withTransform({
                                            translate(m.x, m.y)
                                            rotate(m.angle, Offset.Zero)
                                        }) { drawPath(m.path, color, style = stroke) }
                                    }
                                }
                            }
                        }
                    }
            )
        }
    }
}

// ---------------------------------------------------------------- wavy progress bar (Material 3 Expressive)

@Composable
fun WavyProgress(
    progress: Float,
    color: Color,
    track: Color,
    moving: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
) {
    val phase = if (moving) {
        rememberInfiniteTransition(label = "wave")
            .animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "ph")
    } else null
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), spring(dampingRatio = 1f, stiffness = 120f), label = "p")
    // the wave flattens out when stopped
    val amp by animateFloatAsState(if (moving) 1f else 0f, spring(dampingRatio = 0.8f, stiffness = 200f), label = "amp")
    val path = remember { Path() }
    Canvas(modifier.fillMaxWidth().height(height)) {
        val sw = 5.dp.toPx()
        val a = 3.2.dp.toPx() * amp
        val wl = 28.dp.toPx()
        val gap = 6.dp.toPx()
        val cy = size.height / 2f
        val start = sw / 2f
        val end = size.width - sw / 2f
        val x1 = start + (end - start) * p
        val ph = phase?.value ?: 0f

        if (p < 0.999f) {
            val ts = (x1 + gap + sw).coerceAtMost(end)
            if (ts < end) drawLine(track, Offset(ts, cy), Offset(end, cy), sw, StrokeCap.Round)
            drawCircle(color, sw * 0.42f, Offset(end, cy))
        }
        if (p > 0.001f) {
            path.reset()
            var x = start
            val step = 2f
            // fade the amplitude in over the first wavelength so the start stays clean
            fun y(x: Float): Float {
                val ramp = ((x - start) / wl).coerceIn(0f, 1f)
                return cy + a * ramp * sin((2.0 * PI * ((x - start) / wl - ph)).toFloat())
            }
            path.moveTo(x, y(x))
            while (x < x1) {
                x = (x + step).coerceAtMost(x1)
                path.lineTo(x, y(x))
            }
            drawPath(path, color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

// ---------------------------------------------------------------- "cookie" blob that spins slowly (hero card)

@Composable
fun Blob(color: Color, modifier: Modifier = Modifier) {
    val rot = rememberInfiniteTransition(label = "blob")
        .animateFloat(0f, 360f, infiniteRepeatable(tween(26_000, easing = LinearEasing)), label = "rot")
    val path = remember { Path() }
    Canvas(modifier) {
        val r = size.minDimension / 2f
        val c = Offset(size.width / 2f, size.height / 2f)
        path.reset()
        val n = 180
        for (i in 0..n) {
            val t = i / n.toFloat() * 2f * PI.toFloat()
            val rr = r * (0.86f + 0.14f * cos(9f * t))
            val px = c.x + rr * cos(t)
            val py = c.y + rr * sin(t)
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        rotate(rot.value, c) { drawPath(path, color) }
    }
}

// ---------------------------------------------------------------- title: letters pop in one by one

@Composable
fun AnimatedTitle(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    Row(modifier) {
        text.forEachIndexed { i, ch ->
            key(text, i) {
                val a = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    delay(i * 24L)
                    a.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 260f))
                }
                Text(
                    ch.toString(), style = style, color = color,
                    modifier = Modifier.graphicsLayer {
                        val v = a.value
                        alpha = (v * 1.8f).coerceIn(0f, 1f)
                        translationY = (1f - v) * size.height * 0.4f
                        scaleX = 0.55f + 0.45f * v
                        scaleY = 0.8f + 0.2f * v
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                )
            }
        }
    }
}

// ---------------------------------------------------------------- countdown: fixed-width digits that roll

/**
 * Fredoka has proportional digits: each digit gets a fixed-width slot so the counter doesn't wobble.
 * When [animate] is on, a changing digit rolls in from above. Hundredths never animate.
 */
@Composable
fun TimeText(text: String, style: TextStyle, color: Color, animate: Boolean, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val digitW = remember(style, density) {
        with(density) { "0123456789".maxOf { measurer.measure(it.toString(), style).size.width }.toDp() }
    }
    val dot = text.indexOf('.')
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        text.forEachIndexed { i, ch ->
            // keyed from the right so the seconds keep their identity when the hours disappear
            key(text.length - i) {
                if (ch.isDigit()) {
                    Box(Modifier.width(digitW).clipToBounds(), contentAlignment = Alignment.Center) {
                        if (animate && (dot < 0 || i < dot)) {
                            AnimatedContent(
                                targetState = ch,
                                transitionSpec = {
                                    (slideInVertically(spring(dampingRatio = 0.75f, stiffness = 520f)) { -it / 2 } + fadeIn(tween(140))) togetherWith
                                        (slideOutVertically(spring(dampingRatio = 1f, stiffness = 520f)) { it / 2 } + fadeOut(tween(110))) using
                                        SizeTransform(clip = false)
                                },
                                label = "digit"
                            ) { c -> Text(c.toString(), style = style, color = color, maxLines = 1) }
                        } else {
                            Text(ch.toString(), style = style, color = color, maxLines = 1)
                        }
                    }
                } else {
                    Text(ch.toString(), style = style, color = color, maxLines = 1)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- picker wheel

val WheelItemHeight = 46.dp
private const val WHEEL_VISIBLE = 5

/** Looping wheel (… 58, 59, 00, 01 …) that snaps, buzzes, and follows [value] when it's changed from outside. */
@Composable
fun WheelPicker(
    count: Int,
    value: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pal = LocalPal.current
    val loops = 400
    val state = rememberLazyListState(initialFirstVisibleItemIndex = (loops / 2) * count + value - WHEEL_VISIBLE / 2)
    val fling = rememberSnapFlingBehavior(state)
    val haptic = LocalHapticFeedback.current
    val itemPx = with(LocalDensity.current) { WheelItemHeight.toPx() }
    var programmatic by remember { mutableStateOf(false) }

    val centerIndex by remember(itemPx) {
        derivedStateOf {
            state.firstVisibleItemIndex + WHEEL_VISIBLE / 2 +
                if (state.firstVisibleItemScrollOffset > itemPx / 2) 1 else 0
        }
    }
    val currentOnChange by rememberUpdatedState(onChange)

    LaunchedEffect(state) {
        snapshotFlow { centerIndex % count }
            .distinctUntilChanged()
            .drop(1)
            .collect {
                if (!programmatic) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    currentOnChange(it)
                }
            }
    }

    // value changed from outside (quick-pick chips): spin to it the short way round
    LaunchedEffect(value) {
        val cur = centerIndex % count
        if (cur != value && !state.isScrollInProgress) {
            var diff = value - cur
            if (diff > count / 2) diff -= count
            if (diff < -count / 2) diff += count
            programmatic = true
            try {
                state.animateScrollToItem(centerIndex + diff - WHEEL_VISIBLE / 2)
            } finally {
                programmatic = false
            }
        }
    }

    LazyColumn(
        state = state,
        flingBehavior = fling,
        modifier = modifier.height(WheelItemHeight * WHEEL_VISIBLE),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(count * loops) { i ->
            val dist = kotlin.math.abs(i - centerIndex)
            val a = when (dist) { 0 -> 1f; 1 -> 0.55f; 2 -> 0.22f; else -> 0.1f }
            val sc = when (dist) { 0 -> 1.12f; 1 -> 0.92f; else -> 0.8f }
            Box(Modifier.height(WheelItemHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "%02d".format(i % count),
                    style = if (dist == 0) WheelOn else WheelOff,
                    color = pal.onSurface.copy(alpha = a),
                    modifier = Modifier.graphicsLayer { scaleX = sc; scaleY = sc }
                )
            }
        }
    }
}

private val WheelOn = fs(28.sp, 650, 112)
private val WheelOff = fs(28.sp, 450, 100)
