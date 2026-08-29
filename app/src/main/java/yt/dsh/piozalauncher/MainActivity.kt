package yt.dsh.piozalauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import yt.dsh.piozalauncher.ui.LauncherEvent
import yt.dsh.piozalauncher.ui.LauncherScreen
import yt.dsh.piozalauncher.ui.LauncherViewModel
import yt.dsh.piozalauncher.ui.PizzaSplashScreen
import yt.dsh.piozalauncher.ui.theme.PiozaLauncherTheme
import yt.dsh.piozalauncher.util.StartupSoundPlayer

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash screen systemowy pokazuje logo pizzy (patrz themes.xml) zanim
        // Compose zdąży się narysować - eliminuje "biały błysk" przy starcie.
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Custom dźwięk odpalania launchera. Zobacz res/raw/README.txt, gdzie
        // wrzucić właściwy plik dźwiękowy - do tego czasu wywołanie jest no-opem.
        StartupSoundPlayer.playOnce(applicationContext)

        setContent {
            PiozaLauncherTheme {
                val viewModel: LauncherViewModel = viewModel()
                val lifecycleOwner = LocalLifecycleOwner.current

                // --- Animowany splash z pizzą (1.5 s) ---
                // Compose splash startuje natychmiast po pierwszym draw, niezależnie
                // od tego, kiedy zakończy się pobieranie katalogu z sieci.
                var splashDone by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(1500L)
                    splashDone = true
                }

                // Reaguj na zdarzenie "otwórz ustawienia zgody na instalację nieznanych apek".
                // Kolektor musi być aktywny przez cały czas (splash + launcher).
                LaunchedEffect(Unit) {
                    viewModel.events.collect { event ->
                        when (event) {
                            LauncherEvent.OpenUnknownSourcesSettings -> {
                                startActivity(
                                    viewModel.unknownSourcesSettingsIntent()
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                    }
                }

                // Po powrocie z ekranu ustawień (np. użytkownik włączył zgodę)
                // sprawdź ją ponownie i wznów pobieranie bez dodatkowego dotknięcia.
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.onResumeCheckPendingPermission()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                // Przełącz: splash → lista apek
                if (!splashDone) {
                    PizzaSplashScreen()
                } else {
                    LauncherScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onDestroy() {
        StartupSoundPlayer.release()
        super.onDestroy()
    }
}