package yt.dsh.piozalauncher.data

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Pobiera plik apps.json (katalog aplikacji) z sieci i zestawia go ze stanem
 * apek już zainstalowanych na zegarku, żeby wyliczyć InstallState dla każdej pozycji.
 */
class CatalogRepository(private val context: Context) {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(Config.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(Config.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    sealed interface Result {
        data class Success(val apps: List<AppUiState>) : Result
        data class Failure(val message: String, val details: String? = null) : Result
    }

    suspend fun fetchCatalog(): Result = withContext(Dispatchers.IO) {
        val body = try {
            downloadText(Config.CATALOG_URL)
        } catch (e: IOException) {
            return@withContext Result.Failure("network", e.message)
        } catch (e: Exception) {
            return@withContext Result.Failure("network", e.message)
        }

        val catalog = try {
            json.decodeFromString(AppCatalog.serializer(), body)
        } catch (e: Exception) {
            // Błąd parsowania JSON - najczęściej link wskazuje na stronę HTML
            // (np. github.com/.../blob/... zamiast raw.githubusercontent.com/...)
            // albo plik apps.json ma literówkę składniową.
            return@withContext Result.Failure("parse", e.message)
        }

        val appStates = catalog.apps.map { app -> toUiState(app) }
        Result.Success(appStates)
    }

    private fun downloadText(url: String): String {
        if (url.contains("<TWOJ_USER>") || url.contains("<TWOJE_REPO>")) {
            // CATALOG_URL w Config.kt nie został jeszcze podmieniony na prawdziwy adres.
            throw IOException("CATALOG_URL nie został skonfigurowany (patrz Config.kt)")
        }

        val request = Request.Builder().url(url).get().build()
        val call: Call = client.newCall(request)
        call.execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} dla $url")
            }
            return response.body?.string() ?: throw IOException("Pusta odpowiedź z $url")
        }
    }

    private fun toUiState(app: CatalogApp): AppUiState {
        val installedInfo = getInstalledPackageInfo(app.packageName)
        val installState = when {
            installedInfo == null -> InstallState.NOT_INSTALLED
            app.versionCode > installedInfo.longVersionCodeCompat -> InstallState.UPDATE_AVAILABLE
            else -> InstallState.UP_TO_DATE
        }
        // Pobieramy ikonę z PackageManager tylko gdy apka jest zainstalowana.
        // loadIcon() nigdy nie rzuca - zwraca domyślną ikonę Androida, jeśli apka
        // nie zdefiniowała własnej.
        val icon = installedInfo?.applicationInfo?.loadIcon(context.packageManager)
        // Rozmiar pliku APK zainstalowanej apki (sourceDir = główny plik .apk).
        val installedSize = installedInfo?.applicationInfo?.sourceDir
            ?.let { java.io.File(it).length() }
            ?: 0L
        return AppUiState(
            catalogApp = app,
            installState = installState,
            installedVersionName = installedInfo?.versionName,
            installedIcon = icon,
            installedSizeBytes = installedSize
        )
    }

    private fun getInstalledPackageInfo(packageName: String) =
        try {
            context.packageManager.getPackageInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
}

private val android.content.pm.PackageInfo.longVersionCodeCompat: Long
    get() = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        longVersionCode
    } else {
        @Suppress("DEPRECATION")
        versionCode.toLong()
    }