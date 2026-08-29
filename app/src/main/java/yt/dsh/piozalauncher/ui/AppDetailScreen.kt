package yt.dsh.piozalauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import yt.dsh.piozalauncher.R
import yt.dsh.piozalauncher.data.AppUiState
import yt.dsh.piozalauncher.data.DownloadState
import yt.dsh.piozalauncher.data.InstallState

/**
 * Ekran szczegółów konkretnej apki z katalogu. Wyświetla dane z apps.json:
 * ikonę, nazwę, autora, wersję, opis, rozmiar APK - oraz przyciski akcji
 * (Otwórz / Zainstaluj / Aktualizuj / Odinstaluj). Przycisk Back → lista.
 */
@Composable
fun AppDetailScreen(
    appState: AppUiState,
    downloadState: DownloadState,
    onBack: () -> Unit,
    onOpen: () -> Unit,
    onDownload: () -> Unit,
    onUninstall: () -> Unit
) {
    // Obsługa przycisku Back (fizyczny przycisk na Wear OS lub gest systemowy).
    BackHandler(onBack = onBack)

    val app = appState.catalogApp
    val listState = rememberScalingLazyListState()
    val isBusy = downloadState is DownloadState.Downloading
    val isInstalled = appState.installState != InstallState.NOT_INSTALLED

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            autoCentering = AutoCenteringParams(itemIndex = 0),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            // --- Ikona aplikacji ---
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        appState.installedIcon != null -> {
                            Image(
                                painter = rememberAsyncImagePainter(model = appState.installedIcon),
                                contentDescription = app.name,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        app.iconUrl.isNotBlank() -> {
                            AsyncImage(
                                model = app.iconUrl,
                                contentDescription = app.name,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }
            }

            // --- Nazwa apki ---
            item {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.title3,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // --- Autor ---
            if (app.author.isNotBlank()) {
                item {
                    Text(
                        text = stringResFormat(R.string.author_label, app.author),
                        style = MaterialTheme.typography.caption2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // --- Wersje ---
            item {
                val versionText = buildString {
                    if (app.versionName.isNotBlank()) {
                        append(stringResFormat(R.string.detail_catalog_version, app.versionName))
                    }
                    if (!appState.installedVersionName.isNullOrBlank()) {
                        if (isNotEmpty()) append("\n")
                        append(stringResFormat(R.string.detail_installed_version, appState.installedVersionName))
                    }
                }
                if (versionText.isNotBlank()) {
                    Text(
                        text = versionText,
                        style = MaterialTheme.typography.caption3,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // --- Opis ---
            item {
                val description = when {
                    app.description.isNotBlank() -> app.description
                    app.shortDescription.isNotBlank() -> app.shortDescription
                    else -> stringRes(R.string.detail_no_description)
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.body2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
            }

            // --- Rozmiar APK ---
            // Pierwszeństwo: rozmiar zainstalowanego pliku (dokładny) > rozmiar z katalogu (szacunkowy)
            item {
                val sizeText = when {
                    appState.installedSizeBytes > 0 ->
                        formatSize(appState.installedSizeBytes)
                    app.sizeBytes > 0 ->
                        "~" + formatSize(app.sizeBytes)  // tyldą sygnalizujemy szacunek
                    else -> null
                }
                if (sizeText != null) {
                    Text(
                        text = stringResFormat(R.string.detail_size_mb, sizeText),
                        style = MaterialTheme.typography.caption3,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // --- Postęp pobierania (gdy trwa) ---
            if (isBusy) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        val percent = (downloadState as? DownloadState.Downloading)?.progressPercent ?: 0
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "$percent%",
                            style = MaterialTheme.typography.caption3
                        )
                    }
                }
            }

            // --- Komunikat o błędzie ---
            if (downloadState is DownloadState.Failed) {
                item {
                    val msg = when (downloadState.reason) {
                        "permission_required" -> stringRes(R.string.permission_required)
                        else -> stringRes(R.string.download_failed)
                    }
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.caption2,
                        color = MaterialTheme.colors.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // --- Główny przycisk akcji (Otwórz / Zainstaluj / Aktualizuj) ---
            if (!isBusy) {
                item {
                    when (appState.installState) {
                        InstallState.UP_TO_DATE -> {
                            Button(
                                onClick = onOpen,
                                colors = ButtonDefaults.primaryButtonColors(),
                                modifier = Modifier.fillMaxWidth(0.75f)
                            ) {
                                Text(text = stringRes(R.string.action_open))
                            }
                        }
                        InstallState.UPDATE_AVAILABLE -> {
                            Button(
                                onClick = onDownload,
                                colors = ButtonDefaults.primaryButtonColors(),
                                modifier = Modifier.fillMaxWidth(0.75f)
                            ) {
                                Text(text = stringRes(R.string.action_update))
                            }
                        }
                        InstallState.NOT_INSTALLED -> {
                            Button(
                                onClick = onDownload,
                                colors = ButtonDefaults.primaryButtonColors(),
                                modifier = Modifier.fillMaxWidth(0.75f)
                            ) {
                                Text(text = stringRes(R.string.action_install))
                            }
                        }
                    }
                }
            }

            // --- Przycisk Odinstaluj (tylko gdy zainstalowana, nie podczas pobierania) ---
            if (isInstalled && !isBusy) {
                item {
                    Button(
                        onClick = onUninstall,
                        colors = ButtonDefaults.secondaryButtonColors(),
                        modifier = Modifier.fillMaxWidth(0.75f)
                    ) {
                        Text(text = stringRes(R.string.action_uninstall))
                    }
                }
            }

            // --- Przycisk Wróć ---
            item {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.secondaryButtonColors(),
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .padding(bottom = 8.dp)
                ) {
                    Text(text = stringRes(R.string.action_cancel))
                }
            }
        }
    }
}

/** Formatuje bajty jako czytelny string: "X.X MB" lub "X KB". */
private fun formatSize(bytes: Long): String {
    return when {
        bytes >= 1_000_000L -> "%.1f MB".format(bytes / 1_000_000f)
        bytes >= 1_000L     -> "${bytes / 1_000} KB"
        else                -> "$bytes B"
    }
}

@Composable
private fun stringRes(id: Int): String =
    androidx.compose.ui.res.stringResource(id = id)

@Composable
private fun stringResFormat(id: Int, vararg args: Any): String =
    androidx.compose.ui.res.stringResource(id = id, *args)
