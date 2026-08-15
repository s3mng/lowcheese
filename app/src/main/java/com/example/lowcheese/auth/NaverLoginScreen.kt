package com.example.lowcheese.auth

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

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
            .statusBarsPadding(),
    ) {
        IconButton(onClick = { onDone(false) }) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "닫기")
        }
        Text(
            "네이버 로그인",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        Text(
            "쿠키는 이 기기에만 암호화해서 둡니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        if (progress in 1..99) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
            )
        }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                CookieManager.getInstance().setAcceptCookie(true)
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
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
