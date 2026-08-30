# Pioza Launcher (Wear OS)

Natywny launcher aplikacji na zegarki Wear OS (Kotlin + Jetpack Compose for
Wear), inspirowany [Pioza Launcherem](https://github.com/Shieldowskyy/PiozaLauncher)
(desktopowy launcher gier w Unreal Engine) - pobiera i aktualizuje aplikacje
Wear OS z listy zdefiniowanej w pliku `apps.json`, m.in.
[DSHStatusWear](https://github.com/Shieldowskyy/DSHStatusWear).


<img width="454" height="454" alt="obraz" src="https://github.com/user-attachments/assets/7db4733f-617a-4b0a-96cd-754b8d539cb7" />


## Ważna uwaga na start

Oryginalny **Pioza Launcher jest napisany w Unreal Engine 5** i działa na
Windows/Linux (desktop) — Unreal Engine fizycznie nie uruchomi się na
zegarku z Wear OS. Ten projekt to więc **nowy, natywny launcher napisany od
zera w Kotlinie/Compose** (tak jak DSHStatusWear), zachowujący identyczną
*ideę* Pioza Launchera: katalog apek, pobieranie, aktualizowanie, branding
pizzy, dźwięk startowy, spinner ładowania w kształcie pizzy.

## Czego launcher NIE może zrobić (ograniczenie systemowe Androida)

Instalacja pliku `.apk` spoza Sklepu Play **zawsze wymaga potwierdzenia przez
użytkownika** na systemowym ekranie instalatora - żadna zwykła aplikacja
(bez roota / bez bycia producentem urządzenia) nie może zainstalować innej
apki po cichu w tle. Ten launcher robi więc maksimum możliwego: pobiera plik
i automatycznie otwiera ekran instalacji, więc użytkownikowi zostaje tylko
jedno dotknięcie "Zainstaluj".

## Struktura projektu

```
app/src/main/java/yt/dsh/piozalauncher/
├── MainActivity.kt                 # punkt wejścia, splash + dźwięk startowy
├── PiozaApp.kt                     # klasa Application
├── data/
│   ├── Config.kt                   # <-- TU wpisujesz adres swojego apps.json
│   ├── Models.kt                   # modele katalogu + stanu UI
│   ├── CatalogRepository.kt        # pobieranie i parsowanie apps.json
│   └── ApkInstaller.kt             # pobieranie .apk + systemowy instalator
├── ui/
│   ├── LauncherViewModel.kt        # stan ekranu, logika pobierania
│   ├── LauncherScreen.kt           # główny ekran (lista apek)
│   ├── components/
│   │   ├── AppListItem.kt          # karta pojedynczej apki
│   │   └── PizzaLoadingSpinner.kt  # kręcąca się pizza (spinner ładowania)
│   └── theme/                      # kolory i motyw Wear Compose
└── util/
    └── StartupSoundPlayer.kt       # custom dźwięk odpalania
```

Poza kodem aplikacji:

- **`apps.json`** (w korzeniu repo) - katalog aplikacji do pobrania. To ten
  plik hostujesz u siebie i edytujesz, żeby dodawać/aktualizować pozycje.
  Zobacz **`docs/JAK_DODAC_APKE.md`** po pełną instrukcję.

## Pierwsze uruchomienie / konfiguracja

1. **Wystaw `apps.json` w internecie.** Najprościej: wrzuć ten plik do
   swojego repozytorium na GitHub i skopiuj jego "raw" link
   (`https://raw.githubusercontent.com/<user>/<repo>/main/apps.json`).
2. Wklej ten link w `app/src/main/java/yt/dsh/piozalauncher/data/Config.kt`,
   stała `CATALOG_URL`.
3. Otwórz projekt w **Android Studio** (Koala lub nowszy), poczekaj na
   synchronizację Gradle.
4. Podłącz zegarek z Wear OS 3+ (min. API 30) przez ADB przez Wi-Fi (patrz
   instrukcja w README DSHStatusWear - identyczny proces) albo użyj
   emulatora Wear OS.
5. Uruchom **Run ▶** - aplikacja "Pioza Launcher" zainstaluje się na
   zegarku.

## Branding - miejsca na Twoje własne zasoby (placeholdery)

Zgodnie z ustaleniami, poniższe elementy są na razie **placeholderami** -
podmień je na docelowe pliki:

| Element | Gdzie | Jak podmienić |
|---|---|---|
| Ikona pizzy (logo/splash) | `res/drawable/ic_pioza_logo.xml` | Podmień na własny wektor, albo w Android Studio: PPM na `res` → New → Image Asset → wygeneruj `ic_launcher` z własnego PNG/SVG pizzy. |
| Ikona aplikacji (launcher icon) | `res/mipmap-anydpi-v26/ic_launcher*.xml` | To samo co wyżej - Image Asset Studio nadpisze te pliki automatycznie. |
| Spinner ładowania (kręcąca się pizza) | `ui/components/PizzaLoadingSpinner.kt` + `res/drawable/avd_pizza_spinner.xml` | Animacja obrotu jest już gotowa i działa na dowolnym kształcie z `ic_pioza_logo.xml` - wystarczy podmienić tę jedną grafikę bazową, spinner automatycznie użyje nowego kształtu. |

## Jak dodawać/aktualizować aplikacje w katalogu

Zobacz **`docs/JAK_DODAC_APKE.md`** - to jest ten "oddzielny plik", w którym
łatwo dopisujesz linki do apek i ich opisy, bez dotykania kodu launchera.

## Licencja

Kod tego launchera jest na licencji MIT. DSHStatusWear i PiozaLauncher pozostają własnością ich
autora i mają własne licencje - ten projekt jedynie pobiera i instaluje ich
oficjalne pliki APK z GitHub Releases, nie zawiera ich kodu.

## Screenshoty

<img width="454" height="454" alt="obraz" src="https://github.com/user-attachments/assets/2c8bec12-9cfc-4cf2-9ea1-35ee5d4ebdc4" />
<img width="454" height="454" alt="obraz" src="https://github.com/user-attachments/assets/d9b3aec4-026e-4a78-aa57-bbb119e6f698" />
<img width="454" height="454" alt="obraz" src="https://github.com/user-attachments/assets/804cab7c-4ca6-4fba-9571-0a08f0ad39af" />
