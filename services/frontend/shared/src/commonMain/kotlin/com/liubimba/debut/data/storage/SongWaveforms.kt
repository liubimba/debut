package com.liubimba.debut.data.storage

import com.liubimba.debut.data.audio.WavReader
import com.liubimba.debut.data.audio.Waveform
import com.liubimba.debut.data.entity.StemType
import com.liubimba.debut.data.entity.TakeMetadata
import kotlinx.coroutines.withContext

suspend fun SongsStorage.presentStems(songId: String): List<StemType> =
    withContext(ioDispatcher) {
        StemType.entries.filter { it != StemType.RECORDING && stems.has(songId, it) }
    }

suspend fun SongsStorage.waveformOf(songId: String, type: StemType): Waveform {
    waveforms.get(songId, type)?.let { return it }
    val computed = withContext(ioDispatcher) {
        WavReader(stems.pathOf(songId, type)).use { Waveform.of(it) }
    }
    waveforms.save(songId, type, computed)
    return computed
}

suspend fun SongsStorage.waveformOf(
    songId: String,
    take: TakeMetadata,
    songFrames: Long
): Waveform = withContext(ioDispatcher) {
    WavReader(takes.pathOf(songId, take.id)).use {
        Waveform.of(it).placedAt(take.startFrame, songFrames)
    }
}

suspend fun SongsStorage.lastTake(songId: String): TakeMetadata? =
    takes.readAll(songId).values.maxByOrNull { it.startFrame }
