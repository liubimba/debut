package com.liubimba.debut.ui.addsong

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.liubimba.debut.data.repository.ImportedSong
import com.liubimba.debut.ui.components.UnderlineButton
import com.liubimba.debut.ui.theme.PillShape
import debut.shared.generated.resources.Res
import debut.shared.generated.resources.add_song_add_another
import debut.shared.generated.resources.add_song_sing_it
import org.jetbrains.compose.resources.stringResource

@Composable
fun AddSongDone(
    song: ImportedSong,
    filename: String,
    elapsedSeconds: Int,
    onSing: () -> Unit,
    onAddAnother: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AddSongCard(modifier) {
        ImportSummary(filename = filename, elapsedSeconds = elapsedSeconds)

        ImportStageList(current = null)

        Row(
            horizontalArrangement = Arrangement.spacedBy(AddSongDefaults.stageSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onSing,
                modifier = Modifier.defaultMinSize(minHeight = AddSongDefaults.minTouchTarget),
                shape = PillShape,
                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AddSongDefaults.tightSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Lucide.Music,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(AddSongDefaults.inlineIconSize),
                    )
                    Text(
                        text = stringResource(Res.string.add_song_sing_it),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            UnderlineButton(
                text = stringResource(Res.string.add_song_add_another),
                onClick = onAddAnother,
                minTouchTarget = AddSongDefaults.minTouchTarget,
            )
        }
    }
}
