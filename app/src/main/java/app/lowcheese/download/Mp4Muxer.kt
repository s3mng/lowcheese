package app.lowcheese.download

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

object Mp4Muxer {
    fun mux(video: File, audio: File?, output: File) {
        val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val videoExtractor = MediaExtractor().apply { setDataSource(video.absolutePath) }
        val videoTrack = selectTrack(videoExtractor, "video/")
        val videoIndex = muxer.addTrack(videoExtractor.getTrackFormat(videoTrack))
        var audioExtractor: MediaExtractor? = null
        var audioIndex = -1
        if (audio != null && audio.length() > 0) {
            audioExtractor = MediaExtractor().apply { setDataSource(audio.absolutePath) }
            val audioTrack = selectTrack(audioExtractor, "audio/")
            audioIndex = muxer.addTrack(audioExtractor.getTrackFormat(audioTrack))
        }
        muxer.start()
        copyTrack(videoExtractor, videoTrack, muxer, videoIndex)
        if (audioExtractor != null) {
            copyTrack(audioExtractor, selectTrack(audioExtractor, "audio/"), muxer, audioIndex)
        }
        muxer.stop()
        muxer.release()
        videoExtractor.release()
        audioExtractor?.release()
    }

    private fun selectTrack(extractor: MediaExtractor, prefix: String): Int {
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString("mime").orEmpty()
            if (mime.startsWith(prefix)) return i
        }
        error("트랙을 찾지 못했어요")
    }

    private fun copyTrack(
        extractor: MediaExtractor,
        track: Int,
        muxer: MediaMuxer,
        muxerTrack: Int,
    ) {
        extractor.selectTrack(track)
        val buffer = ByteBuffer.allocate(1024 * 1024)
        val info = MediaCodec.BufferInfo()
        while (true) {
            val size = extractor.readSampleData(buffer, 0)
            if (size < 0) break
            info.offset = 0
            info.size = size
            info.presentationTimeUs = extractor.sampleTime.coerceAtLeast(0)
            info.flags = extractor.sampleFlags
            muxer.writeSampleData(muxerTrack, buffer, info)
            extractor.advance()
        }
        extractor.unselectTrack(track)
    }
}
