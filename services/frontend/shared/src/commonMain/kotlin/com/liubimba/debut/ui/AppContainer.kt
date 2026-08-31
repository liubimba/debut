package com.liubimba.debut.ui

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.liubimba.debut.data.api.KtorDebutApi
import com.liubimba.debut.data.repository.SongsRepository
import com.liubimba.debut.data.storage.SongsStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.io.files.Path

class AppContainer(path: String, debug: Boolean = true) : AutoCloseable {
    init {
        Logger.setMinSeverity(if (debug) Severity.Debug else Severity.Warn)
    }

    private val log = Logger.withTag(TAG)

    val microphoneGranted = MutableStateFlow(false)

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val api = KtorDebutApi()

    val songsStorage: SongsStorage = SongsStorage(Path(path))
    val songsRepository: SongsRepository =
        SongsRepository(api, pollIntervalMillis = 1_000, localStorage = songsStorage, appScope)

    init {
        log.i { "container built, storage root $path, debug $debug" }
    }

    override fun close() {
        log.i { "closing container" }
        api.close()
        appScope.cancel()
    }

    private companion object {
        const val TAG = "AppContainer"
    }
}
