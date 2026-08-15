package com.example.lowcheese.download

import android.content.Context
import android.content.Intent
import com.example.lowcheese.chzzk.formatClock
import com.example.lowcheese.model.DownloadJob
import com.example.lowcheese.model.JobKind
import com.example.lowcheese.model.JobStatus
import com.example.lowcheese.model.QualityOption
import com.example.lowcheese.model.StreamProtocol
import com.example.lowcheese.model.VideoMeta
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient

data class TransferRequest(
    val job: DownloadJob,
    val quality: QualityOption,
    val title: String,
)

class TransferCoordinator(
    private val context: Context,
    http: OkHttpClient,
) {
    private val transfer = MediaTransfer(http)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val pause = ConcurrentHashMap<String, AtomicBoolean>()
    private val cancel = ConcurrentHashMap<String, AtomicBoolean>()
    private val queue = ConcurrentHashMap<String, TransferRequest>()
    private val _jobs = MutableStateFlow<List<DownloadJob>>(emptyList())
    val jobs: StateFlow<List<DownloadJob>> = _jobs.asStateFlow()
    private var loop: Job? = null

    fun enqueue(meta: VideoMeta, quality: QualityOption): String {
        val id = UUID.randomUUID().toString()
        val job = DownloadJob(
            id = id,
            kind = meta.kind,
            title = meta.title,
            channel = meta.channel,
            quality = quality.label,
            status = JobStatus.Queued,
            isAdult = meta.isAdult,
        )
        queue[id] = TransferRequest(job, quality, meta.title)
        pause[id] = AtomicBoolean(false)
        cancel[id] = AtomicBoolean(false)
        upsert(job)
        startService()
        return id
    }

    fun cancel(id: String) {
        cancel[id]?.set(true)
        pause[id]?.set(false)
        val job = _jobs.value.firstOrNull { it.id == id } ?: return
        val status = if (job.kind == JobKind.Live) JobStatus.Stopped else JobStatus.Failed
        upsert(job.copy(status = status, error = if (job.kind == JobKind.Vod) "취소됨" else null))
    }

    fun togglePause(id: String) {
        val job = _jobs.value.firstOrNull { it.id == id } ?: return
        if (job.kind != JobKind.Vod) return
        when (job.status) {
            JobStatus.Running -> {
                pause[id]?.set(true)
                upsert(job.copy(status = JobStatus.Paused))
            }
            JobStatus.Paused -> {
                pause[id]?.set(false)
                upsert(job.copy(status = JobStatus.Running))
                startService()
            }
            else -> Unit
        }
    }

    fun startLoop() {
        if (loop?.isActive == true) return
        loop = scope.launch {
            mutex.withLock {
                while (true) {
                    val next = _jobs.value.firstOrNull {
                        it.status == JobStatus.Queued || it.status == JobStatus.Running || it.status == JobStatus.Paused
                    } ?: break
                    if (cancel[next.id]?.get() == true) continue
                    runCatching { process(next.id) }
                        .onFailure { error ->
                            upsert(
                                (_jobs.value.firstOrNull { it.id == next.id } ?: next).copy(
                                    status = JobStatus.Failed,
                                    error = error.message ?: "실패",
                                ),
                            )
                        }
                }
            }
            loop = null
            context.stopService(Intent(context, TransferService::class.java))
        }
    }

    private suspend fun process(id: String) {
        val request = queue[id] ?: return
        if (cancel[id]?.get() == true) return
        upsert(request.job.copy(status = JobStatus.Running, progress = 0f))
        val live = request.job.kind == JobKind.Live
        val extension = when {
            live -> "ts"
            request.quality.protocol == StreamProtocol.Hls -> "ts"
            else -> "mp4"
        }
        val staging = StagingFile(context, id, extension)
        val startedAt = System.currentTimeMillis()
        val ticker = if (live) {
            scope.launch {
                while (!cancel[id]!!.get() && _jobs.value.any { it.id == id && it.status == JobStatus.Running }) {
                    upsertElapsed(id, startedAt)
                    delay(1000)
                }
            }
        } else {
            null
        }
        try {
            if (live) {
                transfer.recordLive(
                    mediaPlaylistUrl = request.quality.mediaUrl,
                    output = staging.outputStream(),
                    onBytes = {},
                    isCancelled = { cancel[id]?.get() == true },
                )
            } else {
                transfer.downloadVod(
                    quality = request.quality,
                    output = staging.file,
                    onProgress = { value ->
                        val current = _jobs.value.firstOrNull { it.id == id } ?: return@downloadVod
                        upsert(current.copy(progress = value, status = if (pause[id]?.get() == true) JobStatus.Paused else JobStatus.Running))
                    },
                    isPaused = { pause[id]?.get() == true },
                    isCancelled = { cancel[id]?.get() == true },
                )
            }
            ticker?.cancel()
            val current = _jobs.value.firstOrNull { it.id == id } ?: request.job
            if (cancel[id]?.get() == true) {
                if (live && staging.file.exists() && staging.file.length() > 0) {
                    staging.publish(request.title, mime(extension))
                    upsert(current.copy(status = JobStatus.Stopped, elapsedLabel = formatClock(System.currentTimeMillis() - startedAt)))
                } else {
                    staging.delete()
                    upsert(current.copy(status = if (live) JobStatus.Stopped else JobStatus.Failed, error = if (live) null else "취소됨"))
                }
                return
            }
            val mime = mime(extension)
            staging.publish(request.title, mime)
            upsert(
                (_jobs.value.firstOrNull { it.id == id } ?: request.job).copy(
                    status = JobStatus.Completed,
                    progress = 1f,
                    elapsedLabel = if (live) formatClock(System.currentTimeMillis() - startedAt) else null,
                ),
            )
        } catch (error: Exception) {
            staging.delete()
            throw error
        } finally {
            ticker?.cancel()
            queue.remove(id)
        }
    }

    private fun mime(extension: String): String = when (extension) {
        "mp4" -> "video/mp4"
        else -> "video/mp2t"
    }

    private fun upsertElapsed(id: String, startedAt: Long) {
        val current = _jobs.value.firstOrNull { it.id == id } ?: return
        if (current.status != JobStatus.Running) return
        upsert(current.copy(elapsedLabel = formatClock(System.currentTimeMillis() - startedAt)))
    }

    private fun upsert(job: DownloadJob) {
        _jobs.update { list ->
            val without = list.filterNot { it.id == job.id }
            listOf(job) + without
        }
    }

    private fun startService() {
        context.startForegroundService(Intent(context, TransferService::class.java))
    }
}
