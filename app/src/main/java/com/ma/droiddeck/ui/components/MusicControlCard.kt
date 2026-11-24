package com.ma.droiddeck.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ma.droiddeck.data.model.MediaPlaybackState
import com.ma.droiddeck.ui.theme.OledBorder
import com.ma.droiddeck.ui.theme.PureBlack
import com.ma.droiddeck.ui.theme.TextSecondary
import java.util.Locale

/**
 * Compact OLED media card on a pure #000000 surface with dimmed monochrome artwork,
 * track metadata, slim interactive seekbar with timestamps, transport buttons,
 * and a compact integrated Media Volume control row.
 */
@Composable
fun MusicControlCard(
    media: MediaPlaybackState,
    livePositionMs: Long,
    mediaVolume: Int,
    mediaMaxVolume: Int,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSetMediaVolume: (Int) -> Unit,
    onAdjustMediaVolume: (Int) -> Unit,
    onToggleMediaMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = PureBlack,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, OledBorder)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = PureBlack,
                    border = BorderStroke(1.dp, OledBorder)
                ) {
                    val artwork = media.artwork
                    if (artwork != null && media.isActive) {
                        val grayScale = ColorMatrix().apply { setToSaturation(0.15f) }
                        Image(
                            bitmap = artwork.asImageBitmap(),
                            contentDescription = "Album Art",
                            contentScale = ContentScale.Crop,
                            colorFilter = ColorFilter.colorMatrix(grayScale),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .alpha(0.78f)
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (media.isActive) media.title else "No Active Media",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (media.isActive) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            TextSecondary
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subtitle = when {
                        !media.isActive -> "Start music in any player"
                        media.artist.isNotBlank() -> media.artist
                        else -> media.album
                    }
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Compact Seekbar & Timestamps
            if (media.isActive && media.durationMs > 0L) {
                Spacer(modifier = Modifier.height(6.dp))
                var isScrubbing by remember { mutableStateOf(false) }
                var scrubPositionMs by remember { mutableFloatStateOf(0f) }

                val displayedPosition = if (isScrubbing) {
                    scrubPositionMs.toLong()
                } else {
                    livePositionMs
                }

                CompactSeekbar(
                    value = displayedPosition.toFloat(),
                    valueRange = 0f..media.durationMs.toFloat(),
                    onValueChange = { pos ->
                        isScrubbing = true
                        scrubPositionMs = pos
                    },
                    onValueChangeFinished = {
                        onSeekTo(scrubPositionMs.toLong())
                        isScrubbing = false
                    },
                    activeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    thumbColor = MaterialTheme.colorScheme.primary,
                    inactiveColor = OledBorder,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(displayedPosition),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = formatDuration(media.durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Compact Transport Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrevious,
                    enabled = media.isActive,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = if (media.isActive) MaterialTheme.colorScheme.onSurface else TextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (media.isActive) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    } else {
                        PureBlack
                    },
                    border = BorderStroke(
                        1.dp,
                        if (media.isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else OledBorder
                    )
                ) {
                    IconButton(
                        onClick = onPlayPause,
                        enabled = media.isActive,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = if (media.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (media.isPlaying) "Pause" else "Play",
                            tint = if (media.isActive) MaterialTheme.colorScheme.primary else TextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onNext,
                    enabled = media.isActive,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next Track",
                        tint = if (media.isActive) MaterialTheme.colorScheme.onSurface else TextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Compact Integrated Media Volume Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onToggleMediaMute,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (mediaVolume == 0) {
                            Icons.AutoMirrored.Rounded.VolumeOff
                        } else {
                            Icons.AutoMirrored.Rounded.VolumeUp
                        },
                        contentDescription = "Toggle Media Mute",
                        tint = if (mediaVolume == 0) TextSecondary else MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = { onAdjustMediaVolume(-1) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Remove,
                        contentDescription = "Decrease Media Volume",
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                CompactSeekbar(
                    value = mediaVolume.toFloat(),
                    valueRange = 0f..mediaMaxVolume.coerceAtLeast(1).toFloat(),
                    onValueChange = { value -> onSetMediaVolume(value.toInt()) },
                    onValueChangeFinished = {},
                    activeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    thumbColor = MaterialTheme.colorScheme.primary,
                    inactiveColor = OledBorder,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { onAdjustMediaVolume(1) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Increase Media Volume",
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactSeekbar(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    activeColor: Color,
    thumbColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier
) {
    val min = valueRange.start
    val max = valueRange.endInclusive.coerceAtLeast(min + 0.001f)
    val fraction = ((value - min) / (max - min)).coerceIn(0f, 1f)

    Canvas(
        modifier = modifier
            .height(22.dp)
            .pointerInput(min, max) {
                detectTapGestures { offset ->
                    val frac = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                    onValueChange(min + frac * (max - min))
                    onValueChangeFinished()
                }
            }
            .pointerInput(min, max) {
                detectHorizontalDragGestures(
                    onDragEnd = { onValueChangeFinished() },
                    onDragCancel = { onValueChangeFinished() },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val frac = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        onValueChange(min + frac * (max - min))
                    }
                )
            }
    ) {
        val centerY = size.height / 2f
        val thumbRadius = 5.dp.toPx()
        val trackStart = thumbRadius
        val trackEnd = (size.width - thumbRadius).coerceAtLeast(trackStart)
        val thumbX = trackStart + fraction * (trackEnd - trackStart)
        val strokeWidth = 3.dp.toPx()

        drawLine(
            color = inactiveColor,
            start = Offset(trackStart, centerY),
            end = Offset(trackEnd, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = activeColor,
            start = Offset(trackStart, centerY),
            end = Offset(thumbX, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = thumbColor,
            radius = thumbRadius,
            center = Offset(thumbX, centerY)
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
