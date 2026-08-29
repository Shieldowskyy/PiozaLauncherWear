# Jak dodać nową aplikację do Pioza Launcher (Wear OS)

Cały katalog aplikacji to jeden plik: **`apps.json`**.
Launcher pobiera go z internetu przy każdym uruchomieniu / odświeżeniu, więc
wystarczy edytować ten plik w swoim repo (commit + push) - nie trzeba
przebudowywać ani publikować nowej wersji launchera.

## 1. Gdzie hostować `apps.json`

Najprościej: trzymaj go w repo na GitHubie (np. w tym samym repo co launcher,
albo w osobnym) i użyj linku w formacie "raw":

```
https://raw.githubusercontent.com/<user>/<repo>/<branch>/apps.json
```

Ten adres wpisujesz raz w kodzie launchera, w pliku:
`app/src/main/java/yt/dsh/piozalauncher/data/Config.kt` -> stała `CATALOG_URL`.

## 2. Jak dodać nową pozycję

Otwórz `apps.json` i dopisz nowy obiekt do tablicy `"apps"`, np.:

```json
{
  "id": "moja-nowa-apka",
  "name": "Moja Nowa Apka",
  "shortDescription": "Krótki opis, jedna linijka",
  "description": "Dłuższy opis - co robi aplikacja, dla kogo jest, jakie ma funkcje.",
  "author": "TwojNick",
  "packageName": "com.przyklad.mojaapka",
  "versionName": "1.2.0",
  "versionCode": 5,
  "apkUrl": "https://github.com/TwojNick/MojaApka/releases/latest/download/mojaapka.apk",
  "iconUrl": "",
  "repoUrl": "https://github.com/TwojNick/MojaApka",
  "sizeBytes": 4200000
}
```

Nie zapomnij przecinka po poprzednim `}`, jeśli dodajesz kolejny wpis na końcu listy.

### Opis pól

| Pole | Wymagane | Opis |
|---|---|---|
| `id` | tak | Unikalny identyfikator wpisu w katalogu (dowolny tekst, bez spacji). |
| `name` | tak | Nazwa wyświetlana w launcherze. |
| `shortDescription` | nie | Krótki podpis pod nazwą na liście. |
| `description` | nie | Pełny opis na ekranie szczegółów apki. |
| `author` | nie | Autor / twórca. |
| `packageName` | tak | Android package name apki (np. `yt.dsh.statuswear`) - używane do sprawdzania, czy apka jest już zainstalowana i w jakiej wersji. |
| `versionName` | nie | Wersja "po ludzku", np. `1.0`. |
| `versionCode` | tak | Liczba całkowita. **Podbijaj przy każdym nowym wydaniu** - launcher po tym wykrywa dostępną aktualizację. |
| `apkUrl` | tak | Bezpośredni link do pliku `.apk`. Najwygodniej: link `.../releases/latest/download/plik.apk` z GitHub Releases - zawsze wskazuje na najnowsze wydanie. |
| `iconUrl` | nie | Link do obrazka ikony (PNG/WebP). Puste = użyta zostanie domyślna ikona. |
| `repoUrl` | nie | Link do repo/strony projektu (przycisk "Więcej informacji"). |
| `sizeBytes` | nie | Przybliżony rozmiar pliku w bajtach, do wyświetlenia użytkownikowi przed pobraniem. |

## 3. Aktualizacja istniejącej apki

Po prostu zmień `versionName`, `versionCode` (koniecznie wyższy niż poprzednio)
i `apkUrl` (jeśli się zmienił) w istniejącym wpisie. Launcher przy następnym
odświeżeniu listy pokaże tę pozycję jako "Aktualizacja dostępna".

## 4. Usuwanie apki z katalogu

Usuń cały obiekt `{ ... }` odpowiadający tej apce z tablicy `apps`. Zainstalowana
już apka nie zostanie automatycznie odinstalowana z zegarka - po prostu zniknie
z listy do pobrania/aktualizacji w launcherze.

## Uwaga o instalacji

Android (Wear OS) z powodów bezpieczeństwa **zawsze wymaga potwierdzenia przez
użytkownika** przy instalacji pliku APK spoza sklepu Play - launcher nie może
tego zrobić cicho w tle. Po pobraniu pliku otworzy się systemowy ekran instalatora,
na którym trzeba dotknąć "Zainstaluj".
