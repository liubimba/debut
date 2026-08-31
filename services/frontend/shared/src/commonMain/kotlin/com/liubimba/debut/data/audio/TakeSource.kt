package com.liubimba.debut.data.audio

class TakeSource(
    private val take: FrameSource,
    private val startFrame: Long,
    songFrames: Long,
    channels: Int
) : FrameSource {
    override val format: WavFormat = WavFormat(
        sampleRate = take.format.sampleRate,
        channels = channels,
        bitsPerSample = take.format.bitsPerSample,
        frameCount = songFrames
    )

    private val endFrame = startFrame + take.format.frameCount
    private var scratch = FloatArray(0)
    private var position = 0L

    override fun seekTo(frame: Long) {
        position = frame.coerceIn(0, format.frameCount)
        take.seekTo((position - startFrame).coerceIn(0, take.format.frameCount))
    }

    override fun readFrames(destination: FloatArray, frames: Int): Int {
        val capacity = destination.size / format.channels
        val available = minOf(
            frames.toLong(),
            capacity.toLong(),
            format.frameCount - position
        ).toInt()
        if (available <= 0) {
            return 0
        }

        val produced = when {
            position + available <= startFrame -> silence(destination, available)
            position < startFrame -> silence(destination, (startFrame - position).toInt())
            position >= endFrame -> silence(destination, available)
            else -> takeFrames(destination, minOf(available.toLong(), endFrame - position).toInt())
        }
        position += produced
        return produced
    }

    override fun close() = take.close()

    private fun silence(destination: FloatArray, frames: Int): Int {
        destination.fill(0f, 0, frames * format.channels)
        return frames
    }

    private fun takeFrames(destination: FloatArray, frames: Int): Int {
        val samples = frames * take.format.channels
        if (scratch.size < samples) {
            scratch = FloatArray(samples)
        }
        val read = take.readFrames(scratch, frames)
        if (read <= 0) {
            return silence(destination, frames)
        }
        if (take.format.channels == format.channels) {
            scratch.copyInto(destination, 0, 0, read * format.channels)
            return read
        }
        for (frame in 0 until read) {
            val value = scratch[frame * take.format.channels]
            for (channel in 0 until format.channels) {
                destination[frame * format.channels + channel] = value
            }
        }
        return read
    }
}
