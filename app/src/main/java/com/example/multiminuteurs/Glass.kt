package com.example.multiminuteurs

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

object Palette {
    val Bg = Color(0xFF07070D)
    val Violet = Color(0xFF7C4DFF)
    val Cyan = Color(0xFF00E5FF)
    val Pink = Color(0xFFFF4FA3)
    val Blue = Color(0xFF2979FF)
    val TextDim = Color.White.copy(alpha = 0.6f)
    val Accent: Brush = Brush.linearGradient(listOf(Cyan, Violet))
}

/** Monospaced digits so the counter doesn't jitter. */
val Tnum = TextStyle(fontFeatureSettings = "tnum")

/** Animated background: slowly drifting color blobs. */
@Composable
fun LiquidBackground(modifier: Modifier = Modifier) {
    val tr = rememberInfiniteTransition(label = "bg")
    val a by tr.animateFloat(
        0f, 1f, infiniteRepeatable(tween(17000, easing = LinearEasing), RepeatMode.Reverse), label = "a"
    )
    val b by tr.animateFloat(
        0f, 1f, infiniteRepeatable(tween(23000, easing = LinearEasing), RepeatMode.Reverse), label = "b"
    )
    Canvas(modifier.background(Palette.Bg)) {
        val w = size.width
        val h = size.height
        blob(Palette.Violet, Offset(w * (0.15f + 0.6f * a), h * (0.10f + 0.25f * b)), w * 0.95f)
        blob(Palette.Cyan, Offset(w * (0.95f - 0.6f * b), h * (0.45f + 0.20f * a)), w * 0.85f)
        blob(Palette.Pink, Offset(w * (0.25f + 0.5f * b), h * (0.92f - 0.25f * a)), w * 0.90f)
        blob(Palette.Blue, Offset(w * (0.80f - 0.4f * a), h * (0.80f - 0.50f * b)), w * 0.60f)
    }
}

private fun DrawScope.blob(color: Color, c: Offset, r: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            listOf(color.copy(alpha = 0.45f), color.copy(alpha = 0.12f), Color.Transparent),
            center = c, radius = r
        ),
        radius = r,
        center = c
    )
}

/** Glass effect: translucent gradient fill + bright edge. */
fun Modifier.glass(shape: Shape, fill: Float = 0.10f, edge: Float = 0.35f): Modifier = this
    .clip(shape)
    .background(
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = fill + 0.06f), Color.White.copy(alpha = fill * 0.35f))
        )
    )
    .border(
        1.dp,
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = edge),
                Color.White.copy(alpha = 0.03f),
                Color.White.copy(alpha = edge * 0.6f)
            )
        ),
        shape
    )

@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier.size(size).glass(CircleShape, 0.12f).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
fun GradientCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    brush: Brush = Palette.Accent,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(brush)
            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(50)
    val base = modifier.alpha(if (enabled) 1f else 0.4f)
    val styled = if (primary) base.clip(shape).background(Palette.Accent) else base.glass(shape, 0.10f)
    Box(
        styled
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
fun PauseGlyph(color: Color, glyphSize: Dp = 18.dp) {
    Canvas(Modifier.size(glyphSize)) {
        val bw = size.width * 0.3f
        val r = CornerRadius(bw / 3f, bw / 3f)
        drawRoundRect(color = color, topLeft = Offset(size.width * 0.1f, 0f), size = Size(bw, size.height), cornerRadius = r)
        drawRoundRect(color = color, topLeft = Offset(size.width * 0.6f, 0f), size = Size(bw, size.height), cornerRadius = r)
    }
}

val WheelItemHeight = 44.dp
private const val WHEEL_VISIBLE = 5

/** Looping swipe wheel (… 58, 59, 00, 01 …) with snapping and haptic feedback. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    count: Int,
    initial: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val loops = 400
    val startIndex = (loops / 2) * count + initial - WHEEL_VISIBLE / 2
    val state = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val fling = rememberSnapFlingBehavior(state)
    val haptic = LocalHapticFeedback.current
    val itemPx = with(LocalDensity.current) { WheelItemHeight.toPx() }

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
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                currentOnChange(it)
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
            val a = when (dist) { 0 -> 1f; 1 -> 0.5f; 2 -> 0.2f; else -> 0.1f }
            val sc = when (dist) { 0 -> 1.15f; 1 -> 0.92f; else -> 0.8f }
            Box(
                Modifier.height(WheelItemHeight).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "%02d".format(i % count),
                    color = Color.White.copy(alpha = a),
                    fontSize = 26.sp,
                    fontWeight = if (dist == 0) FontWeight.SemiBold else FontWeight.Normal,
                    style = Tnum,
                    modifier = Modifier.graphicsLayer { scaleX = sc; scaleY = sc }
                )
            }
        }
    }
}

/** A timer's two colors, derived from its hue. */
fun timerColors(hue: Float): Pair<Color, Color> =
    Color.hsv(hue % 360f, 0.70f, 1f) to Color.hsv((hue + 45f) % 360f, 0.75f, 1f)

/**
 * Colored border around the whole card, used as a progress bar:
 * starts at top center and empties clockwise. Soft glow underneath.
 */
fun Modifier.progressBorder(
    progress: Float,
    c1: Color,
    c2: Color,
    corner: Dp,
    intensity: Float = 1f,
): Modifier = this.drawWithCache {
    val sw = 2.5.dp.toPx()
    val inset = sw / 2f
    val l = inset
    val t = inset
    val r = size.width - inset
    val b = size.height - inset
    val rad = (corner.toPx() - inset).coerceAtLeast(0f)
    val d = rad * 2f

    // Clockwise path starting at top center
    val path = Path().apply {
        moveTo((l + r) / 2f, t)
        lineTo(r - rad, t)
        arcTo(Rect(r - d, t, r, t + d), -90f, 90f, false)
        lineTo(r, b - rad)
        arcTo(Rect(r - d, b - d, r, b), 0f, 90f, false)
        lineTo(l + rad, b)
        arcTo(Rect(l, b - d, l + d, b), 90f, 90f, false)
        lineTo(l, t + rad)
        arcTo(Rect(l, t, l + d, t + d), 180f, 90f, false)
        close()
    }
    val measure = PathMeasure().apply { setPath(path, false) }
    val length = measure.length
    val brush = Brush.linearGradient(listOf(c1, c2), start = Offset.Zero, end = Offset(size.width, size.height))
    val segment = Path()
    val p = progress.coerceIn(0f, 1f)

    onDrawBehind {
        // Subtle track
        drawPath(path, color = c1.copy(alpha = 0.16f), style = Stroke(width = 1.dp.toPx()))
        if (p > 0f) {
            segment.reset()
            if (p >= 0.999f) segment.addPath(path) else measure.getSegment(0f, length * p, segment, true)
            // Glow
            drawPath(segment, brush = brush, alpha = 0.22f * intensity, style = Stroke(width = sw * 4f, cap = StrokeCap.Round))
            // Main stroke
            drawPath(segment, brush = brush, alpha = intensity, style = Stroke(width = sw, cap = StrokeCap.Round))
        }
    }
}
