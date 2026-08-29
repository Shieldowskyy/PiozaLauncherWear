package yt.dsh.piozalauncher.data

import android.graphics.drawable.Drawable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Struktura pliku apps.json - patrz docs/JAK_DODAC_APKE.md */
@Serializable
data class AppCatalog(
    val schemaVersion: Int = 1,
    val apps: List<CatalogApp> = emptyList()
)

@Serializable
data class CatalogApp(
    val id: String,
    val name: String,
    val shortDescription: String = "",
    val description: String = "",
    val author: String = "",
    val packageName: String,
    val versionName: String = "",
    val versionCode: Long = 0L,
    val apkUrl: String,
    val iconUrl: String = "",
    val repoUrl: String = "",
    val sizeBytes: Long = 0L
)

/** Stan konkretnej apki na zegarku, wyliczany na podstawie CatalogApp + PackageManager. */
enum class InstallState {
    NOT_INSTALLED,
    UPDATE_AVAILABLE,
    UP_TO_DATE
}

data class AppUiState(
    val catalogApp: CatalogApp,
    val installState: InstallState,
    val installedVersionName: String? = null,
    /** Ikona pobrana z PackageManager, gdy apka jest zainstalowana. */
    val installedIcon: Drawable? = null,
    /** Rozmiar pliku APK zainstalowanej apki w bajtach (0 = nieznany / niezainstalowana). */
    val installedSizeBytes: Long = 0L
)

/** Ogólny stan ekranu listy apek. */
sealed interface CatalogUiState {
    data object Loading : CatalogUiState
    data class Error(
        @SerialName("message") val message: String,
                     val details: String? = null
    ) : CatalogUiState
    data class Loaded(val apps: List<AppUiState>) : CatalogUiState
}

/** Stan trwającego pobierania/instalacji pojedynczej apki. */
sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(val progressPercent: Int) : DownloadState
    data object ReadyToInstall : DownloadState
    data class Failed(val reason: String, val details: String? = null) : DownloadState
}
