package app.cracker.download

import android.content.Context
import androidx.core.content.edit
import app.cracker.model.DownloadJob
import app.cracker.model.JobKind
import app.cracker.model.JobStatus
import org.json.JSONArray
import org.json.JSONObject

class JobHistoryStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): List<DownloadJob> = runCatching {
        val raw = prefs.getString(KEY_JOBS, null) ?: return emptyList()
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                decode(array.getJSONObject(index))?.let(::add)
            }
        }
    }.getOrDefault(emptyList())

    fun save(jobs: List<DownloadJob>) {
        val array = JSONArray()
        jobs.take(MAX).forEach { array.put(encode(it)) }
        prefs.edit { putString(KEY_JOBS, array.toString()) }
    }

    private fun encode(job: DownloadJob): JSONObject = JSONObject().apply {
        put("id", job.id)
        put("kind", job.kind.name)
        put("title", job.title)
        put("channel", job.channel)
        put("quality", job.quality)
        put("status", job.status.name)
        put("progress", job.progress.toDouble())
        put("elapsedLabel", job.elapsedLabel ?: JSONObject.NULL)
        put("isAdult", job.isAdult)
        put("error", job.error ?: JSONObject.NULL)
        put("attempt", job.attempt)
        put("maxAttempts", job.maxAttempts)
    }

    private fun decode(json: JSONObject): DownloadJob? = runCatching {
        DownloadJob(
            id = json.getString("id"),
            kind = JobKind.valueOf(json.getString("kind")),
            title = json.getString("title"),
            channel = json.getString("channel"),
            quality = json.getString("quality"),
            status = JobStatus.valueOf(json.getString("status")),
            progress = json.optDouble("progress", 0.0).toFloat(),
            elapsedLabel = json.nullableString("elapsedLabel"),
            isAdult = json.optBoolean("isAdult"),
            error = json.nullableString("error"),
            attempt = json.optInt("attempt", 1),
            maxAttempts = json.optInt("maxAttempts", 1),
        )
    }.getOrNull()

    companion object {
        const val MAX = 80
        private const val PREFS = "job_history"
        private const val KEY_JOBS = "jobs"
    }
}

private fun JSONObject.nullableString(key: String): String? =
    if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotBlank() } else null
