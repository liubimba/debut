package com.liubimba.debut.ui.song

import com.liubimba.debut.data.entity.StemType
import debut.shared.generated.resources.Res
import debut.shared.generated.resources.track_instrumental
import debut.shared.generated.resources.track_vocalist
import org.jetbrains.compose.resources.StringResource

enum class TrackGroup(val titleRes: StringResource, val stems: List<StemType>) {
    Instrumental(
        Res.string.track_instrumental,
        listOf(StemType.BASS, StemType.DRUMS, StemType.OTHER),
    ),
    Vocalist(
        Res.string.track_vocalist,
        listOf(StemType.VOCALS),
    ),
}
