package yt.dsh.piozalauncher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Text
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import yt.dsh.piozalauncher.R
import yt.dsh.piozalauncher.data.AppUiState
import yt.dsh.piozalauncher.data.DownloadState
import yt.dsh.piozalauncher.data.InstallState

/**
 * Pojedyncza pozycja listy - prosty Chip w standardowym, natywnym stylu Wear OS.
 * Kliknięcie zawsze otwiera ekran szczegółów apki (onItemClick) - pobieranie
 * i otwieranie zostało przeniesione do AppDetailScreen.
 *
 * Ikona wyświetlana jest według priorytetu:
 * 1. Spinner postępu podczas aktywnego pobierania
 * 2. Ikona pobrana z PackageManager (gdy apka jest zainstalowana)
 * 3. iconUrl z katalogu apps.json (ładowany przez Coil)
 * 4. Brak ikony (pusty slot)
 */
@Composable
fun AppListItem(
    appState: AppUiState,
    downloadState: DownloadState,
    onItemClick: () -> Unit
) {
    val app = appState.catalogApp
    val isBusy = downloadState is DownloadState.Downloading

    val iconSlot: (@Composable BoxScope.() -> Unit)? = when {
        isBusy -> {
            // Podczas pobierania: spinner postępu zamiast ikony
            {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            }
        }
        appState.installedIcon != null -> {
            // Apka zainstalowana: ikona systemowa z PackageManager
            {
                Image(
                    painter = rememberAsyncImagePainter(model = appState.installedIcon),
                    contentDescription = app.name,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        app.iconUrl.isNotBlank() -> {
            // Nieinstalowana, ale katalog podaje iconUrl: ładuj przez Coil
            {
                AsyncImage(
                    model = app.iconUrl,
                    contentDescription = app.name,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        else -> null  // Brak ikony - Chip wyświetli tylko etykiety
    }

    Chip(
        // Zablokuj kliknięcie tylko gdy aktywnie pobiera - spinner informuje o stanie
        onClick = if (!isBusy) onItemClick else { {} },
        enabled = !isBusy,
        label = {
            Text(
                text = app.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        secondaryLabel = {
            Text(
                text = secondaryLabelFor(appState, downloadState),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        icon = iconSlot,
        colors = ChipDefaults.primaryChipColors(),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun secondaryLabelFor(appState: AppUiState, downloadState: DownloadState): String {
    return when (downloadState) {
        is DownloadState.Downloading ->
            stringResFormat(R.string.download_progress, downloadState.progressPercent)
        is DownloadState.ReadyToInstall ->
            stringRes(R.string.install_prompt)
        is DownloadState.Failed -> when (downloadState.reason) {
            "permission_required" -> stringRes(R.string.permission_required)
            else -> stringRes(R.string.download_failed)
        }
        DownloadState.Idle -> when (appState.installState) {
            InstallState.NOT_INSTALLED -> stringRes(R.string.status_not_installed)
            InstallState.UPDATE_AVAILABLE -> stringRes(R.string.status_update_available)
            InstallState.UP_TO_DATE -> stringRes(R.string.status_installed)
        }
    }
}

@Composable
private fun stringRes(id: Int): String =
    androidx.compose.ui.res.stringResource(id = id)

@Composable
private fun stringResFormat(id: Int, vararg args: Any): String =
    androidx.compose.ui.res.stringResource(id = id, *args)