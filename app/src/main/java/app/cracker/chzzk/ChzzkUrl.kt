package app.cracker.chzzk

import app.cracker.model.JobKind

data class ChzzkTarget(val kind: JobKind, val id: String)

object ChzzkUrl {
    private val live = Regex("""chzzk\.naver\.com/live/([\da-f]+)""", RegexOption.IGNORE_CASE)
    private val video = Regex("""chzzk\.naver\.com/video/(\d+)""", RegexOption.IGNORE_CASE)

    fun parse(raw: String): ChzzkTarget? {
        val text = raw.trim()
        live.find(text)?.let { return ChzzkTarget(JobKind.Live, it.groupValues[1]) }
        video.find(text)?.let { return ChzzkTarget(JobKind.Vod, it.groupValues[1]) }
        return null
    }
}

fun formatClock(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

fun formatDuration(seconds: Int): String {
    if (seconds <= 0) return ""
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return when {
        h > 0 && m > 0 -> "${h}시간 ${m}분"
        h > 0 -> "${h}시간"
        m > 0 -> "${m}분"
        else -> "${seconds}초"
    }
}

fun parseIsoDurationSeconds(value: String): Double {
    val match = Regex(
        """^P(?:(\d+)Y)?(?:(\d+)M)?(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?)?$""",
        RegexOption.IGNORE_CASE,
    ).matchEntire(value.trim()) ?: return 0.0
    val g = match.groupValues
    val hours = g[4].toDoubleOrNull() ?: 0.0
    val minutes = g[5].toDoubleOrNull() ?: 0.0
    val seconds = g[6].toDoubleOrNull() ?: 0.0
    val days = g[3].toDoubleOrNull() ?: 0.0
    return days * 86400 + hours * 3600 + minutes * 60 + seconds
}

fun sanitizeFileName(name: String): String {
    val cleaned = name.replace(Regex("""[\\/:*?"<>|]"""), "_").trim().ifBlank { "chzzk" }
    return cleaned.take(80)
}
