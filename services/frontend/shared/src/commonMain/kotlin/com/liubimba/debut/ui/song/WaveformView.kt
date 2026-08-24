package com.liubimba.debut.ui.song

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import com.liubimba.debut.data.audio.Waveform
import com.liubimba.debut.ui.theme.DebutTheme

@Composable
fun WaveformView(
    waveform: Waveform,
    progress: () -> Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    barWidth: Dp = SongScreenDefaults.barWidth,
    barGap: Dp = SongScreenDefaults.barGap,
    minBarHeightFraction: Float = SongScreenDefaults.minBarHeightFraction,
) {
    val playedColor = DebutTheme.colors.noteActive
    val remainingColor = DebutTheme.colors.note
    val progressColor = MaterialTheme.colorScheme.primary

    BoxWithConstraints(modifier) {
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
        ) {
            if (bars.isEmpty()) {
                return@Canvas
            }
            val step = size.width / bars.size
            val width = step * (barWidth.toPx() / (barWidth + barGap).toPx())
            val middle = size.height / 2f
            val edge = size.width * progress()

            bars.forEachIndexed { index, peak ->
                val x = index * step
                val height = peak.coerceAtLeast(minBarHeightFraction) * size.height
                drawRect(
                    color = if (x <= edge) playedColor else remainingColor,
                    topLeft = Offset(x, middle - height / 2f),
                    size = Size(width, height)
                )
            }
            val hOffset = 6f

            drawRect(
                color = progressColor,
                topLeft = Offset(x = edge, y = -hOffset),
                size = Size(width, size.height + hOffset * 2)
            )

        }
    }
}
