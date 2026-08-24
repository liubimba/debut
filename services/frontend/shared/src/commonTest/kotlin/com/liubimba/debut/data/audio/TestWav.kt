package com.liubimba.debut.data.audio

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeIntLe
import kotlinx.io.writeShortLe
import kotlinx.io.writeString

fun writeWav(
    root: Path,
    name: String,
    samples: ShortArray,
    sampleRate: Int = 44100,
    channels: Int = 2,
    extraChunk: Boolean = false,
): Path {
    SystemFileSystem.createDirectories(root)
    val target = Path(root, name)
    val dataBytes = samples.size * 2
    val listBytes = if (extraChunk) 8 + 26 else 0
    SystemFileSystem.sink(target).buffered().use { sink ->
        sink.writeString("RIFF")
        sink.writeIntLe(4 + 24 + listBytes + 8 + dataBytes)
        sink.writeString("WAVE")

        sink.writeString("fmt ")
        sink.writeIntLe(16)
        sink.writeShortLe(1)
        sink.writeShortLe(channels.toShort())
        sink.writeIntLe(sampleRate)
        sink.writeIntLe(sampleRate * channels * 2)
        sink.writeShortLe((channels * 2).toShort())
        sink.writeShortLe(16)

        if (extraChunk) {
            sink.writeString("LIST")
            sink.writeIntLe(26)
            repeat(26) { sink.writeByte(0x20) }
        }

        sink.writeString("data")
        sink.writeIntLe(dataBytes)
        samples.forEach { sink.writeShortLe(it) }
    }
    return target
}

fun purge(target: Path) {
    val metadata = SystemFileSystem.metadataOrNull(target) ?: return
    if (metadata.isDirectory) {
        SystemFileSystem.list(target).forEach { purge(it) }
    }
    SystemFileSystem.delete(target, mustExist = false)
}
