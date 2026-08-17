package com.liubimba.debut.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Settings
import debut.shared.generated.resources.Res
import debut.shared.generated.resources.nav_add_song
import debut.shared.generated.resources.nav_library
import debut.shared.generated.resources.nav_settings
import org.jetbrains.compose.resources.StringResource

enum class Destination(
    val titleRes: StringResource,
    val icon: ImageVector,
    val route: Any,
    val pinnedToEnd: Boolean = false,
) {
    Library(Res.string.nav_library, Lucide.Music, LibraryGraph),
    Settings(Res.string.nav_settings, Lucide.Settings, SettingsRoute),
    AddSong(Res.string.nav_add_song, Lucide.Plus, AddSongRoute, pinnedToEnd = true),
}
