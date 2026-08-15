package app.cracker.download

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.content.edit
import androidx.core.net.toUri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DownloadLocationStore(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _uri = MutableStateFlow(prefs.getString(KEY_URI, null)?.toUri())
    val uri: StateFlow<Uri?> = _uri.asStateFlow()

    fun set(uri: Uri) {
        prefs.edit { putString(KEY_URI, uri.toString()) }
        _uri.value = uri
    }

    fun clear() {
        val current = _uri.value
        if (current != null) {
            runCatching {
                app.contentResolver.releasePersistableUriPermission(
                    current,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
        }
        prefs.edit { remove(KEY_URI) }
        _uri.value = null
    }

    fun label(): String {
        val current = _uri.value ?: return DEFAULT_LABEL
        val documentId = runCatching { DocumentsContract.getTreeDocumentId(current) }.getOrNull()
        return documentId
            ?.replaceFirst("primary:", "")
            ?.replace(':', '/')
            ?.ifBlank { DEFAULT_LABEL }
            ?: DEFAULT_LABEL
    }

    companion object {
        const val DEFAULT_LABEL = "Movies/cracker"
        private const val PREFS = "download_location"
        private const val KEY_URI = "tree_uri"
    }
}
