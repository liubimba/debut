package com.liubimba.debut.ui.addsong

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import com.liubimba.debut.data.repository.ImportStage
import com.liubimba.debut.ui.components.UnderlineButton
import com.liubimba.debut.ui.theme.PillShape
import debut.shared.generated.resources.Res
import debut.shared.generated.resources.add_song_cancel
import org.jetbrains.compose.resources.stringResource

@Composable
fun AddSongInProgress(
    stage: ImportStage,
    filename: String,
    elapsedSeconds: Int,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AddSongCard(modifier) {
        ImportSummary(filename = filename, elapsedSeconds = elapsedSeconds)

        ImportStageList(current = stage)

        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().clip(PillShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
        )

        UnderlineButton(
            text = stringResource(Res.string.add_song_cancel),
            onClick = onCancel,
            minTouchTarget = AddSongDefaults.minTouchTarget,
        )
    }
}
