package com.liubimba.debut.ui.addsong

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextOverflow
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Circle
import com.composables.icons.lucide.Loader
import com.composables.icons.lucide.Lucide
import com.liubimba.debut.data.repository.ImportStage
import com.liubimba.debut.ui.theme.DebutTheme
import debut.shared.generated.resources.Res
import debut.shared.generated.resources.add_song_elapsed
import debut.shared.generated.resources.stage_downloading_vocals
import debut.shared.generated.resources.stage_separating
import debut.shared.generated.resources.stage_transcribing
import debut.shared.generated.resources.stage_uploading
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal enum class StageStatus { PENDING, ACTIVE, DONE }

internal fun ImportStage.titleResource(): StringResource = when (this) {
    ImportStage.UPLOADING -> Res.string.stage_uploading
    ImportStage.SEPARATING -> Res.string.stage_separating
    ImportStage.DOWNLOADING_VOCALS -> Res.string.stage_downloading_vocals
    ImportStage.TRANSCRIBING -> Res.string.stage_transcribing
}

internal fun ImportStage.statusAt(current: ImportStage?): StageStatus = when {
    current == null -> StageStatus.DONE
    ordinal < current.ordinal -> StageStatus.DONE
    this == current -> StageStatus.ACTIVE
    else -> StageStatus.PENDING
}

@Composable
fun ImportSummary(filename: String, elapsedSeconds: Int, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = filename,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Text(
            text = stringResource(Res.string.add_song_elapsed, elapsedSeconds),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ImportStageList(current: ImportStage?, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(AddSongDefaults.stageSpacing),
        modifier = modifier.fillMaxWidth(),
    ) {
        for (stage in ImportStage.entries) {
            ImportStageRow(stage = stage, status = stage.statusAt(current))
        }
    }
}

@Composable
private fun ImportStageRow(stage: ImportStage, status: StageStatus) {
    val textColor by animateColorAsState(
        targetValue = when (status) {
            StageStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
            else -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(durationMillis = 150),
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(AddSongDefaults.inlineSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StageBadge(status)
        Text(
            text = stringResource(stage.titleResource()),
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
        )
    }
}

@Composable
private fun StageBadge(status: StageStatus) {
    when (status) {
        StageStatus.DONE -> Icon(
            imageVector = Lucide.Check,
            contentDescription = null,
            tint = DebutTheme.colors.onSignal,
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                )
                .padding(AddSongDefaults.badgePadding)
                .size(AddSongDefaults.badgeIconSize),
        )

        StageStatus.ACTIVE -> {
            val transition = rememberInfiniteTransition()
            val angle by transition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = AddSongDefaults.spinPeriodMillis,
                        easing = LinearEasing,
                    ),
                    repeatMode = RepeatMode.Restart,
                ),
            )
            Icon(
                imageVector = Lucide.Loader,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .border(
                        width = AddSongDefaults.badgeBorderWidth,
                        color = MaterialTheme.colorScheme.outline,
                        shape = CircleShape,
                    )
                    .rotate(angle)
                    .padding(AddSongDefaults.badgePadding)
                    .size(AddSongDefaults.badgeIconSize),
            )
        }

        StageStatus.PENDING -> Icon(
            imageVector = Lucide.Circle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(AddSongDefaults.pendingBadgeSize),
        )
    }
}
