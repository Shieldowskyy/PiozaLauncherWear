package yt.dsh.piozalauncher.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer

/**
 * Odtwarza custom dźwięk przy starcie launchera.
 *
 * Wrzuć plik dźwiękowy do res/raw/ pod nazwą "startup_sound"
 * (np. startup_sound.ogg lub startup_sound.mp3) - patrz res/raw/README.txt.
 * Dopóki plik nie istnieje, odtwarzanie jest bezpiecznie pomijane (bez crasha).
 */
object StartupSoundPlayer {

    private var player: MediaPlayer? = null

    fun playOnce(context: Context) {
        val resId = context.resources.getIdentifier(
            RAW_RES_NAME, "raw", context.packageName
        )
        if (resId == 0) {
            // Plik dźwiękowy jeszcze nie został dodany do res/raw - pomiń bez błędu.
            return
        }

        release()
        try {
            player = MediaPlayer.create(context, resId)?.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setOnCompletionListener { mp -> mp.release(); player = null }
                setOnErrorListener { mp, _, _ -> mp.release(); player = null; true }
                start()
            }
        } catch (_: Exception) {
            // Uszkodzony/niewspierany plik dźwiękowy - nie wywalaj startu aplikacji.
            release()
        }
    }

    fun release() {
        player?.let {
            try {
                if (it.isPlaying) it.stop()
                it.release()
            } catch (_: IllegalStateException) {
                // MediaPlayer był już w nieprawidłowym stanie - nic nie robimy.
            }
        }
        player = null
    }

    private const val RAW_RES_NAME = "startup_sound"
}