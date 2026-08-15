package app.lowcheese

import app.lowcheese.auth.NaverCookieStore
import app.lowcheese.chzzk.ChzzkExtractor
import app.lowcheese.download.DownloadLocationStore
import app.lowcheese.download.TransferCoordinator
import app.lowcheese.download.TransferSettings
import app.lowcheese.net.buildHttpClient

class AppGraph(app: LowcheeseApplication) {
    val cookies = NaverCookieStore(app)
    val http = buildHttpClient(cookies::cookieHeader)
    val extractor = ChzzkExtractor(http)
    val downloadLocation = DownloadLocationStore(app)
    val transferSettings = TransferSettings(app)
    val coordinator = TransferCoordinator(app, http, downloadLocation, transferSettings)
}

class LowcheeseApplication : android.app.Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
    }
}
