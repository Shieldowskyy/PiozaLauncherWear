package yt.dsh.piozalauncher.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Pobiera plik .apk wskazany w katalogu i otwiera systemowy instalator Androida.
 *
 * WAŻNE OGRANICZENIE SYSTEMOWE (nie da się tego obejść bez roota / uprawnień
 * producenta urządzenia): zwykła aplikacja na Wear OS / Android NIE MOŻE
 * po cichu zainstalować innego pliku .apk w tle. System zawsze pokaże
 * użytkownikowi ekran PackageInstallera z prośbą o potwierdzenie
 * "Zainstaluj" / "Anuluj". Ten launcher robi więc maksimum tego, co możliwe:
 * pobiera plik i automatycznie otwiera ten ekran, żeby użytkownikowi zostało
 * tylko jedno dotknięcie.
 *
 * Wszystkie błędy są logowane pod tagiem "PiozaLauncher" (Logcat) z pełnym
 * stack trace - w razie problemu filtruj Logcat po tym tagu.
 */
class ApkInstaller(private val context: Context) {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
        .connectTimeout(Config.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
    }

    private val downloadsDir: File
        get() = File(context.cacheDir, "apk").apply { mkdirs() }

        /**
         * Pobiera plik APK, zgłaszając postęp przez [onProgress] (0-100).
         * Zwraca lokalny plik gotowy do instalacji albo rzuca IOException
         * z opisowym komunikatem (widocznym też w Logcacie pod tagiem TAG).
         */
        @Throws(IOException::class)
        fun download(app: CatalogApp, onProgress: (Int) -> Unit): File {
            Log.i(TAG, "Rozpoczynam pobieranie '${app.name}' z ${app.apkUrl}")

            if (app.apkUrl.isBlank()) {
                val msg = "apkUrl jest puste dla apki '${app.name}' (id=${app.id}) - sprawdź apps.json"
                Log.e(TAG, msg)
                throw IOException(msg)
            }

            val targetFile = File(downloadsDir, "${app.id}-${app.versionCode}.apk")

            val request = Request.Builder().url(app.apkUrl).get().build()
            val call: Call = client.newCall(request)

            try {
                call.execute().use { response ->
                    Log.i(TAG, "Odpowiedź HTTP ${response.code} dla ${app.apkUrl} (${response.protocol})")

                    if (!response.isSuccessful) {
                        val msg = "Pobieranie nieudane: HTTP ${response.code} dla ${app.apkUrl}"
                        Log.e(TAG, msg)
                        throw IOException(msg)
                    }

                    val contentType = response.header("Content-Type")
                    Log.i(TAG, "Content-Type: $contentType, Content-Length: ${response.body?.contentLength()}")

                    val body = response.body ?: run {
                        val msg = "Pusta odpowiedź (brak body) dla ${app.apkUrl}"
                        Log.e(TAG, msg)
                        throw IOException(msg)
                    }

                    val totalBytes = body.contentLength()
                    var bytesRead = 0L

                    body.byteStream().use { input ->
                        FileOutputStream(targetFile).use { output ->
                            val buffer = ByteArray(8 * 1024)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                bytesRead += read
                                if (totalBytes > 0) {
                                    val percent = ((bytesRead * 100) / totalBytes).toInt()
                                    onProgress(percent.coerceIn(0, 100))
                                }
                            }
                        }
                    }

                    Log.i(TAG, "Pobrano ${bytesRead} bajtów do ${targetFile.absolutePath}")

                    if (bytesRead == 0L) {
                        val msg = "Pobrano 0 bajtów - plik pod ${app.apkUrl} jest pusty albo link jest błędny"
                        Log.e(TAG, msg)
                        targetFile.delete()
                        throw IOException(msg)
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Błąd sieci przy pobieraniu ${app.apkUrl}", e)
                targetFile.delete()
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Nieoczekiwany błąd przy pobieraniu ${app.apkUrl}", e)
                targetFile.delete()
                throw IOException("Nieoczekiwany błąd: ${e.javaClass.simpleName}: ${e.message}", e)
            }

            return targetFile
        }

        /**
         * Otwiera systemowy ekran instalacji dla wskazanego pliku .apk.
         * To jedyny krok, który MUSI potwierdzić użytkownik - system tego wymaga.
         */
        fun launchInstall(apkFile: File) {
            try {
                if (!apkFile.exists() || apkFile.length() == 0L) {
                    val msg = "Plik APK nie istnieje albo jest pusty: ${apkFile.absolutePath}"
                    Log.e(TAG, msg)
                    throw IOException(msg)
                }

                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
                Log.i(TAG, "Uruchamiam instalator dla URI: $uri")

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                if (intent.resolveActivity(context.packageManager) == null) {
                    val msg = "Brak aplikacji obsługującej instalację APK (resolveActivity == null). " +
                    "Na niektórych zegarkach Wear OS PackageInstaller bywa ograniczony/wyłączony."
                    Log.e(TAG, msg)
                    throw IOException(msg)
                }

                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Błąd przy uruchamianiu instalatora dla ${apkFile.absolutePath}", e)
                throw e
            }
        }

        /** Sprawdza, czy aplikacja (launcher) ma zgodę systemu na instalowanie nieznanych apek. */
        fun canRequestPackageInstalls(): Boolean {
            val result = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.packageManager.canRequestPackageInstalls()
            } else {
                true
            }
            Log.i(TAG, "canRequestPackageInstalls() = $result")
            return result
        }

        /** Intent do ekranu ustawień, na którym użytkownik włącza zgodę "Instaluj nieznane aplikacje". */
        fun unknownSourcesSettingsIntent(): Intent {
            return Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }

        companion object {
            private const val TAG = "PiozaLauncher"
        }
}
