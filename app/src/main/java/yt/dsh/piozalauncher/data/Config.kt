package yt.dsh.piozalauncher.data

/**
 * Konfiguracja launchera.
 *
 * CATALOG_URL - adres pliku apps.json (katalog aplikacji). Podmień na swój,
 * najwygodniej "raw" link do pliku w repozytorium na GitHub, np.:
 *
 *   https://raw.githubusercontent.com/<user>/<repo>/main/apps.json
 *
 * Zobacz plik apps.json w korzeniu tego projektu (przykładowy katalog z wpisem
 * dla DSHStatusWear) oraz docs/JAK_DODAC_APKE.md z instrukcją edycji.
 */
object Config {

    // TODO: podmień na docelowy adres swojego pliku apps.json
    const val CATALOG_URL: String =
        "https://gist.githubusercontent.com/Shieldowskyy/85e33b883e4dbc590caaeebcc6ebdf62/raw/11a7703fdaae5ab80e6036b24489412353d76f47/gistfile1.txt"

    /** Co ile ms automatycznie odświeżać katalog w tle (0 = tylko ręcznie / przy starcie). */
    const val AUTO_REFRESH_INTERVAL_MS: Long = 5 * 60 * 1000L

    /** Limit czasu na pojedyncze żądanie sieciowe. */
    const val NETWORK_TIMEOUT_SECONDS: Long = 15L
}
