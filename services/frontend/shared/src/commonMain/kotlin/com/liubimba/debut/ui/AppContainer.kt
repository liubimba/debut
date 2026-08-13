package com.liubimba.debut.ui

import com.liubimba.debut.data.api.KtorDebutApi
import com.liubimba.debut.data.repository.SongsRepository

object AppContainer {
    val api = KtorDebutApi()
    val songsRepository = SongsRepository(api)
}
