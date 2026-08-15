package com.example.lowcheese.download

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.example.lowcheese.chzzk.sanitizeFileName
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

    fun publish(title: String, mime: String): Boolean {
        val name = "${sanitizeFileName(title)}.${file.extension}"
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
    }
}
