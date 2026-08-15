package app.lowcheese.chzzk

import okhttp3.HttpUrl.Companion.toHttpUrl

data class HlsVariant(
    val uri: String,
    val bandwidth: Int,
    val width: Int?,
    val height: Int?,
)

data class HlsSegment(
    val uri: String,
    val durationSec: Double,
)

data class HlsMediaPlaylist(
    val targetDurationSec: Double,
    val mediaSequence: Long,
    val ended: Boolean,
    val mapUri: String?,
    val segments: List<HlsSegment>,
)

object HlsParser {
    fun parseMaster(body: String, playlistUrl: String): List<HlsVariant> {
        val base = playlistUrl.toHttpUrl()
        val lines = body.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        val variants = mutableListOf<HlsVariant>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (line.startsWith("#EXT-X-STREAM-INF", ignoreCase = true)) {
                val attrs = attributes(line.substringAfter(":"))
                val next = lines.getOrNull(i + 1)
                if (next != null && !next.startsWith("#")) {
                    variants += HlsVariant(
                        uri = resolve(base, next),
                        bandwidth = attrs["BANDWIDTH"]?.toIntOrNull() ?: 0,
                        width = attrs["RESOLUTION"]?.substringBefore("x")?.toIntOrNull(),
                        height = attrs["RESOLUTION"]?.substringAfter("x")?.toIntOrNull(),
                    )
                    i++
                }
            }
            i++
        }
        return variants.sortedByDescending { it.height ?: it.bandwidth }
    }

    fun parseMedia(body: String, playlistUrl: String): HlsMediaPlaylist {
        val base = playlistUrl.toHttpUrl()
        var target = 6.0
        var sequence = 0L
        var ended = false
        var mapUri: String? = null
        var duration = 0.0
        val segments = mutableListOf<HlsSegment>()
        for (raw in body.lineSequence()) {
            val line = raw.trim()
            when {
                line.startsWith("#EXT-X-TARGETDURATION", ignoreCase = true) ->
                    target = line.substringAfter(":").toDoubleOrNull() ?: target
                line.startsWith("#EXT-X-MEDIA-SEQUENCE", ignoreCase = true) ->
                    sequence = line.substringAfter(":").toLongOrNull() ?: sequence
                line.startsWith("#EXT-X-MAP", ignoreCase = true) -> {
                    val uri = attributes(line.substringAfter(":") )["URI"]?.trim('"')
                    if (uri != null) mapUri = resolve(base, uri)
                }
                line.startsWith("#EXTINF", ignoreCase = true) ->
                    duration = line.substringAfter(":").substringBefore(",").toDoubleOrNull() ?: 0.0
                line.startsWith("#EXT-X-ENDLIST", ignoreCase = true) -> ended = true
                line.isNotEmpty() && !line.startsWith("#") -> {
                    segments += HlsSegment(resolve(base, line), duration)
                    duration = 0.0
                }
            }
        }
        return HlsMediaPlaylist(target, sequence, ended, mapUri, segments)
    }

    fun isMaster(body: String): Boolean =
        body.lineSequence().any { it.startsWith("#EXT-X-STREAM-INF", ignoreCase = true) }

    private fun resolve(base: okhttp3.HttpUrl, ref: String): String =
        base.resolve(ref)?.toString() ?: ref

    private fun attributes(raw: String): Map<String, String> {
        val out = linkedMapOf<String, String>()
        val regex = Regex("""([A-Z0-9-]+)=("(?:\\.|[^"])*"|[^,]*)""", RegexOption.IGNORE_CASE)
        for (match in regex.findAll(raw)) {
            out[match.groupValues[1].uppercase()] = match.groupValues[2].trim('"')
        }
        return out
    }
}
