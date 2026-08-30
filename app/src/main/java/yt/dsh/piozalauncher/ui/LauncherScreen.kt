package yt.dsh.piozalauncher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusable
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import kotlinx.coroutines.launch
import yt.dsh.piozalauncher.BuildConfig
import yt.dsh.piozalauncher.R
import yt.dsh.piozalauncher.data.CatalogUiState
import yt.dsh.piozalauncher.data.DownloadState
import yt.dsh.piozalauncher.ui.components.AppListItem

@Composable
fun LauncherScreen(viewModel: LauncherViewModel) {
    val catalogState by viewModel.catalogState.collectAsState()
    val downloadStates by viewModel.downloadStates.collectAsState()
    val selectedApp by viewModel.selectedApp.collectAsState()
    val listState: ScalingLazyListState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    // Wymagane, by ScalingLazyColumn zaczęła dostawać zdarzenia z pierścienia
    // obrotowego (rotary input) - bez focusu system w ogóle ich nie dostarczy.
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

// --- Nawigacja: jeśli wybrano apkę, pokaż ekran szczegółów ---
    val selected = selectedApp
    if (selected != null) {
        val downloadState = downloadStates[selected.catalogApp.id] ?: DownloadState.Idle
        AppDetailScreen(
            appState = selected,
            downloadState = downloadState,
            onBack = { viewModel.selectApp(null) },
            onOpen = { viewModel.openInstalledApp(selected.catalogApp.packageName) },
            onDownload = { viewModel.downloadAndInstall(selected.catalogApp) },
            // Add the missing onUninstall parameter here:
            onUninstall = { viewModel.uninstallApp(selected.catalogApp.packageName) }
        )
        return
    }

    // --- Lista apek ---
    // Scaffold + TimeText + Vignette + PositionIndicator to standardowy szkielet
    // ekranu w natywnych aplikacjach Google na Wear OS (Ustawienia, Sklep Play itd.).
    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        when (val state = catalogState) {
            is CatalogUiState.Loading -> {
                LoadingContent()
            }

            is CatalogUiState.Error -> {
                ErrorContent(
                    message = errorMessage(state.message),
                    details = state.details,
                    onRetry = { viewModel.refreshCatalog() }
                )
            }

            is CatalogUiState.Loaded -> {
                if (state.apps.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = stringRes(R.string.empty_catalog))
                    }
                } else {
                    ScalingLazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .onRotaryScrollEvent {
                                coroutineScope.launch {
                                    listState.scrollBy(it.verticalScrollPixels)
                                }
                                true
                            }
                            .focusRequester(focusRequester)
                            .focusable(),
                        state = listState,
                        autoCentering = AutoCenteringParams(itemIndex = 0)
                    ) {
                        // --- Nagłówek: nazwa launchera + wersja ---
                        item {
                            ListHeader {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = stringRes(R.string.app_name))
                                    Text(
                                        text = stringResFormat(
                                            R.string.launcher_version,
                                            BuildConfig.VERSION_NAME
                                        ),
                                        style = MaterialTheme.typography.caption3,
                                        color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }

                        // --- Lista apek ---
                        items(state.apps, key = { it.catalogApp.id }) { appState ->
                            val appId = appState.catalogApp.id
                            val downloadState = downloadStates[appId] ?: DownloadState.Idle

                            AppListItem(
                                appState = appState,
                                downloadState = downloadState,
                                onItemClick = { viewModel.selectApp(appState) }
                            )
                        }

                        // --- Przycisk Refresh na dole listy ---
                        item {
                            Chip(
                                onClick = { viewModel.refreshCatalog() },
                                enabled = catalogState !is CatalogUiState.Loading,
                                label = {
                                    Text(
                                        text = stringRes(R.string.action_refresh_short),
                                        maxLines = 1
                                    )
                                },
                                colors = ChipDefaults.secondaryChipColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Natywny, systemowy wskaźnik ładowania - taki sam, jakiego używają
            // aplikacje Google na Wear OS.
            androidx.wear.compose.material.CircularProgressIndicator(
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(text = stringRes(R.string.loading), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ErrorContent(message: String, details: String?, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            Text(text = message, textAlign = TextAlign.Center)
            if (!details.isNullOrBlank()) {
                Text(
                    text = details,
                    style = MaterialTheme.typography.caption3,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
            }
            Button(onClick = onRetry, colors = ButtonDefaults.primaryButtonColors()) {
                Text(text = stringRes(R.string.retry))
            }
        }
    }
}

@Composable
private fun errorMessage(reason: String): String {
    val resId = when (reason) {
        "network" -> R.string.error_network
        "parse" -> R.string.error_catalog
        else -> R.string.error_generic
    }
    return stringRes(resId)
}

@Composable
private fun stringRes(id: Int): String =
    androidx.compose.ui.res.stringResource(id = id)

@Composable
private fun stringResFormat(id: Int, vararg args: Any): String =
    androidx.compose.ui.res.stringResource(id = id, *args)