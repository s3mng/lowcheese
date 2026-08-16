package app.cracker.download

import android.content.Context
import android.content.Intent
import app.cracker.chzzk.formatClock
import app.cracker.model.DownloadJob
import app.cracker.model.JobKind
import app.cracker.model.JobStatus
import app.cracker.model.QualityOption
import app.cracker.model.StreamProtocol
import app.cracker.model.VideoMeta
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
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
    private val locationStore: DownloadLocationStore,
    private val settings: TransferSettings,
) {
    private val transfer = MediaTransfer(http)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val pause = ConcurrentHashMap<String, AtomicBoolean>()
    private val cancel = ConcurrentHashMap<String, AtomicBoolean>()
    private val discarded = CopyOnWriteArraySet<String>()
    private val queue = ConcurrentHashMap<String, TransferRequest>()
    private val history = JobHistoryStore(context)
    private val notifier = TransferNotifier(context)
    private val _jobs = MutableStateFlow(restore(history.load()))
    val jobs: StateFlow<List<DownloadJob>> = _jobs.asStateFlow()
    private var loop: Job? = null

    init {
        history.save(_jobs.value)
        StagingFile.sweep(context)
    }

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
        val status = if (job.kind == JobKind.Live) JobStatus.Stopped else JobStatus.Cancelled
        upsert(job.copy(status = status, error = null))
    }

    fun remove(id: String) {
        discarded.add(id)
        cancel[id]?.set(true)
        pause[id]?.set(false)
        queue.remove(id)
        StagingFile.deleteFor(context, id)
        _jobs.update { jobs -> jobs.filterNot { it.id == id } }
        persist()
    }

    fun clearAll() {
        _jobs.value.forEach { job ->
            discarded.add(job.id)
            cancel[job.id]?.set(true)
            pause[job.id]?.set(false)
        }
        queue.clear()
        StagingFile.sweep(context)
        _jobs.value = emptyList()
        persist()
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
                    if (cancel[next.id]?.get() == true) {
                        StagingFile.deleteFor(context, next.id)
                        upsert(
                            next.copy(
                                status = if (next.kind == JobKind.Live) JobStatus.Stopped else JobStatus.Cancelled,
                                error = null,
                            ),
                        )
                        continue
                    }
                    runCatching { process(next.id) }
                        .onFailure { error ->
                            val current = _jobs.value.firstOrNull { it.id == next.id } ?: next
                            if (cancel[next.id]?.get() == true) {
                                upsert(
                                    current.copy(
                                        status = if (current.kind == JobKind.Live) JobStatus.Stopped else JobStatus.Cancelled,
                                        error = null,
                                    ),
                                )
                            } else {
                                upsert(
                                    current.copy(
                                        status = JobStatus.Failed,
                                        error = error.message ?: "실패",
                                    ),
                                )
                            }
                        }
                }
            }
            loop = null
            StagingFile.sweep(context)
            context.stopService(Intent(context, TransferService::class.java))
        }
    }

    private fun cancelled(id: String): Boolean = cancel[id]?.get() == true

    private suspend fun process(id: String) {
        val request = queue[id] ?: return
        if (cancelled(id)) {
            StagingFile.deleteFor(context, id)
            return
        }
        val live = request.job.kind == JobKind.Live
        val extension = when {
            live -> "ts"
            request.quality.protocol == StreamProtocol.Hls -> "ts"
            else -> "mp4"
        }
        val maxAttempts = if (live) 1 else settings.vodRetries.value.coerceIn(0, TransferSettings.MAX_RETRIES) + 1
        upsert(
            currentJob(id, request.job).copy(
                status = JobStatus.Running,
                progress = 0f,
                attempt = 1,
                maxAttempts = maxAttempts,
                error = null,
            ),
        )
        var lastError: Exception? = null
        try {
            for (attempt in 1..maxAttempts) {
                if (cancelled(id)) {
                    StagingFile.deleteFor(context, id)
                    markCancelled(id, request.job, live)
                    return
                }
                if (attempt > 1) {
                    upsert(
                        currentJob(id, request.job).copy(
                            status = JobStatus.Running,
                            progress = 0f,
                            attempt = attempt,
                            maxAttempts = maxAttempts,
                            error = null,
                        ),
                    )
                    delay((1_000L * attempt).coerceAtMost(5_000L))
                    if (cancelled(id)) {
                        StagingFile.deleteFor(context, id)
                        markCancelled(id, request.job, live)
                        return
                    }
                }
                val staging = StagingFile(context, id, extension)
                val startedAt = System.currentTimeMillis()
                val ticker = if (live) {
                    scope.launch {
                        while (!cancelled(id) && _jobs.value.any { it.id == id && it.status == JobStatus.Running }) {
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
                            isCancelled = { cancelled(id) },
                        )
                    } else {
                        transfer.downloadVod(
                            quality = request.quality,
                            output = staging.file,
                            onProgress = { value ->
                                val current = _jobs.value.firstOrNull { it.id == id } ?: return@downloadVod
                                upsert(
                                    current.copy(
                                        progress = value,
                                        status = if (pause[id]?.get() == true) JobStatus.Paused else JobStatus.Running,
                                    ),
                                )
                            },
                            isPaused = { pause[id]?.get() == true },
                            isCancelled = { cancelled(id) },
                        )
                    }
                    ticker?.cancel()
                    val current = currentJob(id, request.job)
                    if (cancelled(id)) {
                        if (live && staging.file.exists() && staging.file.length() > 0) {
                            staging.publish(request.title, mime(extension), locationStore.uri.value)
                            val stopped = current.copy(
                                status = JobStatus.Stopped,
                                elapsedLabel = formatClock(System.currentTimeMillis() - startedAt),
                            )
                            upsert(stopped)
                            notifier.notifyFinished(stopped)
                        } else {
                            staging.delete()
                            markCancelled(id, request.job, live)
                        }
                        return
                    }
                    staging.publish(request.title, mime(extension), locationStore.uri.value)
                    val finished = currentJob(id, request.job).copy(
                        status = JobStatus.Completed,
                        progress = 1f,
                        elapsedLabel = if (live) formatClock(System.currentTimeMillis() - startedAt) else null,
                    )
                    upsert(finished)
                    notifier.notifyFinished(finished)
                    return
                } catch (error: Exception) {
                    staging.delete()
                    if (error is CancellationException) throw error
                    if (cancelled(id)) {
                        markCancelled(id, request.job, live)
                        return
                    }
                    lastError = error
                    if (attempt == maxAttempts) throw error
                } finally {
                    ticker?.cancel()
                }
            }
            throw lastError ?: IllegalStateException("실패")
        } finally {
            queue.remove(id)
            discarded.remove(id)
        }
    }

    private fun currentJob(id: String, fallback: DownloadJob): DownloadJob =
        _jobs.value.firstOrNull { it.id == id } ?: fallback

    private fun markCancelled(id: String, fallback: DownloadJob, live: Boolean) {
        upsert(
            currentJob(id, fallback).copy(
                status = if (live) JobStatus.Stopped else JobStatus.Cancelled,
                error = null,
            ),
        )
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

    private fun restore(jobs: List<DownloadJob>): List<DownloadJob> =
        jobs.map { job ->
            when (job.status) {
                JobStatus.Queued, JobStatus.Running, JobStatus.Paused ->
                    job.copy(status = JobStatus.Failed, error = "앱이 종료되어 중단됐어요")
                else -> job
            }
        }

    private fun persist() {
        history.save(_jobs.value)
    }

    private fun upsert(job: DownloadJob) {
        if (job.id in discarded) return
        val previous = _jobs.value.firstOrNull { it.id == job.id }
        _jobs.update { list ->
            val without = list.filterNot { it.id == job.id }
            (listOf(job) + without).take(JobHistoryStore.MAX)
        }
        if (previous == null || previous.status != job.status) persist()
    }

    private fun startService() {
        context.startForegroundService(Intent(context, TransferService::class.java))
    }
}
