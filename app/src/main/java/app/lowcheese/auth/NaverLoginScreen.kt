package app.lowcheese.auth

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.lowcheese.ui.theme.Cheddar
import app.lowcheese.ui.theme.InkOnPaper
import app.lowcheese.ui.theme.MutedOnPaper
import app.lowcheese.ui.theme.PaperElevated

private const val LOGIN_URL =
    "https://nid.naver.com/nidlogin.login?url=https://chzzk.naver.com/"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NaverLoginScreen(
    store: NaverCookieStore,
    onDone: (Boolean) -> Unit,
) {
    var progress by remember { mutableIntStateOf(0) }
    Column(
        Modifier
            .fillMaxSize()
            .background(PaperElevated),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onDone(false) }) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "닫기",
                    tint = InkOnPaper,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "네이버 로그인",
                    style = MaterialTheme.typography.titleMedium,
                    color = InkOnPaper,
                )
                Text(
                    "쿠키는 안전하게 로컬에만 보관됩니다",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedOnPaper,
                )
            }
        }
        if (progress in 1..99) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = Cheddar,
                trackColor = PaperElevated,
            )
        }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                CookieManager.getInstance().setAcceptCookie(true)
                WebView(context).apply {
                    setBackgroundColor(AndroidColor.WHITE)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        settings.isAlgorithmicDarkeningAllowed = false
                    }
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        private val finished = java.util.concurrent.atomic.AtomicBoolean(false)

                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean = false

                        override fun onPageFinished(view: WebView, url: String) {
                            val captured = captureCookies(store)
                            if (captured && url.contains("chzzk.naver.com") && finished.compareAndSet(false, true)) {
                                view.post { onDone(true) }
                            }
                        }
                    }
                    loadUrl(LOGIN_URL)
                }
            },
        )
    }
}

private fun captureCookies(store: NaverCookieStore): Boolean {
    val manager = CookieManager.getInstance()
    val blobs = listOf(
        "https://nid.naver.com",
        "https://www.naver.com",
        "https://chzzk.naver.com",
    ).mapNotNull { manager.getCookie(it) }
    return blobs.any { store.importCookieHeader(it) }
}
