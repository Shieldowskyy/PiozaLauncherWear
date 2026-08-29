package yt.dsh.piozalauncher.ui.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme

/**
 * Motyw launchera - celowo używa domyślnej palety androidx.wear.compose.material
 * (ta sama, której używają natywne aplikacje Google na Wear OS: Ustawienia,
 * Sklep Play, Zegarek). Czarne tło, niebieski akcent Material, brak
 * dodatkowego brandingu w kolorach - spójnie z resztą systemu.
 */
@Composable
fun PiozaLauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        content = content
    )
}
