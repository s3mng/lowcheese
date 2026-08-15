package app.lowcheese.download

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import app.lowcheese.chzzk.sanitizeFileName
import java.io.File
import java.io.FileOutputStream

class StagingFile(
    private val context: Context,
    jobId: String,
    extension: String,
) {
    val file: File = File(context.cacheDir, "lowcheese-$jobId.$extension")

    init {
        file.parentFile?.mkdirs()
        if (file.exists()) file.delete()
        file.createNewFile()
    }

    fun outputStream(): FileOutputStream = FileOutputStream(file, true)

    fun publish(title: String, mime: String, treeUri: Uri?): Boolean {
        val name = "${sanitizeFileName(title)}.${file.extension}"
        if (treeUri != null && publishToTree(treeUri, name, mime)) {
            file.delete()
            return true
        }
        return publishToMovies(name, mime)
    }

    private fun publishToTree(treeUri: Uri, name: String, mime: String): Boolean {
        val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return false
        if (!tree.canWrite()) return false
        tree.findFile(name)?.delete()
        val created = tree.createFile(mime, name) ?: return false
        context.contentResolver.openOutputStream(created.uri)?.use { dest ->
            file.inputStream().use { it.copyTo(dest) }
        } ?: return false
        return true
    }

    private fun publishToMovies(name: String, mime: String): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, mime)
            put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/lowcheese")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: return false
        context.contentResolver.openOutputStream(uri)?.use { dest ->
            file.inputStream().use { it.copyTo(dest) }
        } ?: return false
        values.clear()
        values.put(MediaStore.Video.Media.IS_PENDING, 0)
        context.contentResolver.update(uri, values, null, null)
        file.delete()
        return true
    }

    fun delete() {
        file.delete()
        File(file.parent, "${file.nameWithoutExtension}.video.m4s").delete()
        File(file.parent, "${file.nameWithoutExtension}.audio.m4s").delete()
    }

    companion object {
        private const val PREFIX = "lowcheese-"

        fun deleteFor(context: Context, jobId: String) {
            context.cacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("$PREFIX$jobId")) file.delete()
            }
        }

        fun sweep(context: Context, keepIds: Collection<String> = emptySet()) {
            context.cacheDir.listFiles()?.forEach { file ->
                if (!file.name.startsWith(PREFIX)) return@forEach
                val rest = file.name.removePrefix(PREFIX)
                if (keepIds.none { rest.startsWith(it) }) file.delete()
            }
        }
    }
}
