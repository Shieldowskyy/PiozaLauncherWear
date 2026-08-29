package yt.dsh.piozalauncher.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import yt.dsh.piozalauncher.data.ApkInstaller
import yt.dsh.piozalauncher.data.AppUiState
import yt.dsh.piozalauncher.data.CatalogApp
import yt.dsh.piozalauncher.data.CatalogRepository
import yt.dsh.piozalauncher.data.CatalogUiState
import yt.dsh.piozalauncher.data.DownloadState
import java.io.IOException

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CatalogRepository(application)
    private val installer = ApkInstaller(application)

    private val _catalogState = MutableStateFlow<CatalogUiState>(CatalogUiState.Loading)
    val catalogState: StateFlow<CatalogUiState> = _catalogState.asStateFlow()

    // Stan pobierania per appId, żeby lista mogła pokazywać wiele niezależnych postępów.
    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    // Aktualnie wybrана apka (ekran szczegółów). null = lista apek.
    private val _selectedApp = MutableStateFlow<AppUiState?>(null)
    val selectedApp: StateFlow<AppUiState?> = _selectedApp.asStateFlow()

    // Apka, dla której czekamy na powrót użytkownika z ekranu zgody systemowej -
    // po powrocie do launchera (onResume) sprawdzamy zgodę ponownie i wznawiamy pobieranie.
    private var pendingInstallPermissionApp: CatalogApp? = null

        // Jednorazowe zdarzenia dla UI/Activity (np. "otwórz ten Intent") - SharedFlow,
        // żeby nie odtwarzać tej samej akcji przy rekonfiguracji ekranu.
        private val _events = MutableSharedFlow<LauncherEvent>(extraBufferCapacity = 1)
        val events: SharedFlow<LauncherEvent> = _events

        init {
            refreshCatalog()
        }

        fun refreshCatalog() {
            _catalogState.value = CatalogUiState.Loading
            viewModelScope.launch {
                when (val result = repository.fetchCatalog()) {
                    is CatalogRepository.Result.Success -> {
                        _catalogState.value = CatalogUiState.Loaded(result.apps)
                    }
                    is CatalogRepository.Result.Failure -> {
                        Log.e(TAG, "Błąd pobierania katalogu: ${result.message} / ${result.details}")
                        _catalogState.value = CatalogUiState.Error(result.message, result.details)
                    }
                }
            }
        }

        fun downloadAndInstall(app: CatalogApp) {
            if (!installer.canRequestPackageInstalls()) {
                // Zegarek nie zezwala jeszcze temu launcherowi na instalowanie apek spoza
                // Sklepu Play. To NIE jest błąd sieci - trzeba poprosić o zgodę w Ustawieniach,
                // tak samo jak robi to np. Sklep Play przy pierwszym uruchomieniu.
                Log.w(TAG, "Brak zgody na instalację nieznanych aplikacji dla ${app.name} - otwieram ustawienia")
                pendingInstallPermissionApp = app
                _downloadStates.update {
                    it + (app.id to DownloadState.Failed(
                        reason = "permission_required",
                        details = null
                    ))
                }
                _events.tryEmit(LauncherEvent.OpenUnknownSourcesSettings)
                return
            }

            startDownload(app)
        }

        /**
         * Wywoływane przez Activity po powrocie z ekranu ustawień (onResume) -
         * jeśli użytkownik właśnie włączył zgodę, wznawiamy pobieranie automatycznie
         * zamiast zmuszać go do ponownego dotknięcia "Zainstaluj".
         */
        fun onResumeCheckPendingPermission() {
            val app = pendingInstallPermissionApp ?: return
            if (installer.canRequestPackageInstalls()) {
                Log.i(TAG, "Zgoda na instalację przyznana - wznawiam pobieranie ${app.name}")
                pendingInstallPermissionApp = null
                startDownload(app)
            }
        }

        private fun startDownload(app: CatalogApp) {
            _downloadStates.update { it + (app.id to DownloadState.Downloading(0)) }

            viewModelScope.launch {
                try {
                    val file = withContext(Dispatchers.IO) {
                        installer.download(app) { percent ->
                            _downloadStates.update { current ->
                                current + (app.id to DownloadState.Downloading(percent))
                            }
                        }
                    }
                    _downloadStates.update { it + (app.id to DownloadState.ReadyToInstall) }
                    installer.launchInstall(file)
                } catch (e: IOException) {
                    Log.e(TAG, "Nie udało się pobrać/zainstalować ${app.name}", e)
                    _downloadStates.update {
                        it + (app.id to DownloadState.Failed(
                            reason = "network",
                            details = e.message
                        ))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Nieoczekiwany błąd przy instalacji ${app.name}", e)
                    _downloadStates.update {
                        it + (app.id to DownloadState.Failed(
                            reason = "unknown",
                            details = "${e.javaClass.simpleName}: ${e.message}"
                        ))
                    }
                }
            }
        }

        fun clearDownloadState(appId: String) {
            _downloadStates.update { it - appId }
        }

        /**
         * Otwiera ekran szczegółów dla wybranej apki. Wywołaj z null, żeby wrócić do listy.
         * Po udanym otwarciu zainstalowanej apki resetuje selekcję automatycznie.
         */
        fun selectApp(app: AppUiState?) {
            _selectedApp.value = app
        }

        fun openInstalledApp(packageName: String) {
            val context = getApplication<Application>()
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                // Zamknij ekran szczegółów po odpaleniu apki
                _selectedApp.value = null
            } else {
                Log.w(TAG, "Brak launch intent dla $packageName")
            }
        }

        /**
         * Otwiera systemowy dialog odinstalowania apki (ACTION_DELETE).
         * System zawsze prosi użytkownika o potwierdzenie - nie da się tego ominąć.
         * Po powrocie do launchera (onResume) katalog odnieświ się automatycznie.
         */
        fun uninstallApp(packageName: String) {
            val context = getApplication<Application>()
            val intent = android.content.Intent(android.content.Intent.ACTION_DELETE).apply {
                data = android.net.Uri.parse("package:$packageName")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            // Zamknij ekran szczegółów - powracamy do listy (katalog odświeży się przez onResume)
            _selectedApp.value = null
        }

        fun unknownSourcesSettingsIntent() = installer.unknownSourcesSettingsIntent()

        companion object {
            private const val TAG = "PiozaLauncher"
        }
}

sealed interface LauncherEvent {
    data object OpenUnknownSourcesSettings : LauncherEvent
}
