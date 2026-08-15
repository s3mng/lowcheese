package app.cracker

import app.cracker.auth.NaverCookieStore
import app.cracker.chzzk.ChzzkExtractor
import app.cracker.download.DownloadLocationStore
import app.cracker.download.TransferCoordinator
import app.cracker.download.TransferSettings
import app.cracker.net.buildHttpClient

class AppGraph(app: CrackerApplication) {
    val cookies = NaverCookieStore(app)
    val http = buildHttpClient(cookies::cookieHeader)
    val extractor = ChzzkExtractor(http)
    val downloadLocation = DownloadLocationStore(app)
    val transferSettings = TransferSettings(app)
    val coordinator = TransferCoordinator(app, http, downloadLocation, transferSettings)
}

class CrackerApplication : android.app.Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
    }
}
