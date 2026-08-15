package com.example.lowcheese

import com.example.lowcheese.auth.NaverCookieStore
import com.example.lowcheese.chzzk.ChzzkExtractor
import com.example.lowcheese.download.TransferCoordinator
import com.example.lowcheese.net.buildHttpClient

class AppGraph(app: LowcheeseApplication) {
    val cookies = NaverCookieStore(app)
    val http = buildHttpClient(cookies::cookieHeader)
    val extractor = ChzzkExtractor(http)
    val coordinator = TransferCoordinator(app, http)
}

class LowcheeseApplication : android.app.Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
    }
}
