package yt.dsh.piozalauncher.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Text
import yt.dsh.piozalauncher.R

/**
 * Spinner ładowania w kształcie kręcącej się pizzy.
 *
 * Wykorzystuje res/drawable/ic_pioza_logo.xml obracany w nieskończoność.
 * Gdy podmienisz ic_pioza_logo.xml na docelową grafikę pizzy, animacja
 * (obrót) pozostanie bez zmian - liczy się tylko kształt bazowy.
 *
 * Jeśli wolisz gotowy AnimatedVectorDrawable zamiast animacji Compose,
 * zobacz też res/drawable/avd_pizza_spinner.xml (alternatywna, systemowa
 * wersja tej samej animacji).
 */
@Composable
fun PizzaLoadingSpinner(
    modifier: Modifier = Modifier,
    label: String? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pizza_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pizza_rotation"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_pioza_logo),
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .rotate(rotation)
        )
        if (label != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = label)
        }
    }
}