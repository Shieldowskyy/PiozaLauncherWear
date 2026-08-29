package yt.dsh.piozalauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import yt.dsh.piozalauncher.ui.components.PizzaLoadingSpinner

/**
 * Własny splash screen w Compose - kręcąca się pizza przez 1.5 s.
 * Wyświetlany w MainActivity.kt zaraz po `installSplashScreen()`,
 * zanim zostanie pokazany właściwy LauncherScreen.
 *
 * Długość trwania (1.5 s) i opóźnienie sterowane są w MainActivity.
 */
@Composable
fun PizzaSplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PizzaLoadingSpinner(
                modifier = Modifier,
                label = null
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Pioza Launcher",
                style = MaterialTheme.typography.title3,
                textAlign = TextAlign.Center
            )
        }
    }
}
