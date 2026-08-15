package app.lowcheese.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NaverCookieStore(context: Context) {
    private val memory = AtomicReference<Pair<String, String>?>(null)
    private val prefs: SharedPreferences? = runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }.getOrNull()

    private val _loggedIn = MutableStateFlow(false)
    val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    init {
        val saved = prefs?.getString(KEY_AUT, null) to prefs?.getString(KEY_SES, null)
        if (!saved.first.isNullOrBlank() && !saved.second.isNullOrBlank()) {
            memory.set(saved.first!! to saved.second!!)
            _loggedIn.value = true
        }
    }

    fun cookieHeader(): String? {
        val pair = memory.get() ?: return null
        return "NID_AUT=${pair.first}; NID_SES=${pair.second}"
    }

    fun save(aut: String, ses: String) {
        memory.set(aut to ses)
        prefs?.edit {
            putString(KEY_AUT, aut)
            putString(KEY_SES, ses)
        }
        _loggedIn.value = true
    }

    fun importCookieHeader(header: String): Boolean {
        val map = header.split(';')
            .map { it.trim() }
            .filter { it.contains('=') }
            .associate { part ->
                val key = part.substringBefore('=').trim()
                val value = part.substringAfter('=').trim()
                key to value
            }
        val aut = map["NID_AUT"]
        val ses = map["NID_SES"]
        if (aut.isNullOrBlank() || ses.isNullOrBlank()) return false
        save(aut, ses)
        return true
    }

    fun clear() {
        memory.set(null)
        prefs?.edit { clear() }
        _loggedIn.value = false
    }

    companion object {
        const val PREFS_NAME = "naver_cookies"
        private const val KEY_AUT = "nid_aut"
        private const val KEY_SES = "nid_ses"
    }
}
