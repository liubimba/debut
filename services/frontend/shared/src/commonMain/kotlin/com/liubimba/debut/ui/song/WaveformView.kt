package com.liubimba.debut.ui.song

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import com.liubimba.debut.data.audio.Waveform
import com.liubimba.debut.ui.theme.DebutTheme

private data class WaveformColors(
    val played: Color,
    val remaining: Color,
    val marker: Color,
    val outsideAlpha: Float
)

private fun DrawScope.drawBars(
    bars: FloatArray,
    step: Float,
    barWidth: Float,
    area: ClosedFloatingPointRange<Float>,
    edge: Float,
    minBarHeightFraction: Float,
    colors: WaveformColors
) {
    val middle = size.height / 2f
    bars.forEachIndexed { index, peak ->
        val x = index * step
        val center = (x + barWidth / 2f) / size.width
        val height = peak.coerceAtLeast(minBarHeightFraction) * size.height
        val color = if (x <= edge) colors.played else colors.remaining
        drawRect(
            color = if (center in area) color else color.copy(alpha = colors.outsideAlpha),
            topLeft = Offset(x, middle - height / 2f),
            size = Size(barWidth, height),
        )
    }
}

private fun DrawScope.drawMarker(x: Float, width: Float, color: Color) {
    drawRect(
        color = color,
        topLeft = Offset(x, 0f),
        size = Size(width, size.height)
    )
}

private fun DrawScope.drawGrip(x: Float, width: Float, height: Float, color: Color) {
    drawRoundRect(
        color = color,
        topLeft = Offset(x - width / 2f, size.height / 2f - height / 2f),
        size = Size(width, height),
        cornerRadius = CornerRadius(width / 2f),
    )
}


@Composable
fun WaveformView(
    waveform: Waveform,
    progress: () -> Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    barWidth: Dp = SongScreenDefaults.barWidth,
    barGap: Dp = SongScreenDefaults.barGap,
    startArea: Float,
    endArea: Float,
    onSelectedArea: (Float, Float) -> Unit,
    minBarHeightFraction: Float = SongScreenDefaults.minBarHeightFraction,
) {
    val colors = WaveformColors(
        played = DebutTheme.colors.noteActive,
        remaining = DebutTheme.colors.note,
        marker = MaterialTheme.colorScheme.primary,
        outsideAlpha = SongScreenDefaults.outsideAreaAlpha,
    )

    BoxWithConstraints(
        modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        val columns = (maxWidth / (barWidth + barGap)).toInt().coerceAtLeast(1)
        val bars = remember(waveform, columns) { waveform.resampleTo(columns) }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(bars) {
                    detectTapGestures { offset ->
                        onSeek((offset.x / size.width).coerceIn(0f, 1f))
                    }
                }
                .pointerInput(Unit) {
                    var anchor = 0f

                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            anchor = offset.x / size.width
                        },
                        onHorizontalDrag = { change, _ ->
                            val offset = change.position.x / size.width
                            change.consume()
                            onSelectedArea(
                                minOf(anchor, offset),
                                maxOf(anchor, offset)
                            )
                        },
                    )
                }
        ) {
            if (bars.isEmpty()) {
                return@Canvas
            }
            val step = size.width / bars.size
            val markerWidth = step * (barWidth.toPx() / (barWidth + barGap).toPx())
            val edge = size.width * progress()
            val gripWidth = markerWidth * SongScreenDefaults.gripWidthFactor
            val gripHeight = SongScreenDefaults.gripHeight.toPx()

            drawBars(
                bars = bars,
                step = step,
                barWidth = markerWidth,
                area = startArea..endArea,
                edge = edge,
                minBarHeightFraction = minBarHeightFraction,
                colors = colors,
            )

            drawMarker(edge, markerWidth, colors.marker)

            if (startArea > 0f || endArea < 1f) {
                listOf(startArea, endArea).forEach { edgeFraction ->
                    val gripX = edgeFraction * size.width
                    drawMarker(gripX, markerWidth, colors.marker)
                    drawGrip(gripX, gripWidth, gripHeight, colors.marker)
                }
            }
        }
    }
}
