package com.liubimba.debut.ui.song

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.AudioLines
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music4
import debut.shared.generated.resources.Res
import debut.shared.generated.resources.notes_track_type
import debut.shared.generated.resources.vocal_track_type
import org.jetbrains.compose.resources.StringResource

enum class VocalViewType(
    val titleRes: StringResource,
    val icon: ImageVector,
    val implemented: Boolean,
) {
    VocalTrack(Res.string.vocal_track_type, Lucide.AudioLines, implemented = true),
    Notes(Res.string.notes_track_type, Lucide.Music4, implemented = false),
}
