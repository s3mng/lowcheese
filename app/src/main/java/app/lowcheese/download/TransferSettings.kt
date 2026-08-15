package app.lowcheese.download

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TransferSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _vodRetries = MutableStateFlow(prefs.getInt(KEY_VOD_RETRIES, DEFAULT_RETRIES).coerceIn(0, MAX_RETRIES))
    val vodRetries: StateFlow<Int> = _vodRetries.asStateFlow()

    fun setVodRetries(value: Int) {
        val clamped = value.coerceIn(0, MAX_RETRIES)
        prefs.edit { putInt(KEY_VOD_RETRIES, clamped) }
        _vodRetries.value = clamped
    }

    companion object {
        const val DEFAULT_RETRIES = 2
        const val MAX_RETRIES = 5
        private const val PREFS = "transfer_settings"
        private const val KEY_VOD_RETRIES = "vod_retries"
    }
}
