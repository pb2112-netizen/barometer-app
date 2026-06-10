# World Barometer (Android)

Natywna aplikacja Android (Kotlin + Jetpack Compose), która pobiera gotowy plik JSON
z publicznego URL i prezentuje globalny „barometr" sytuacji. **Aplikacja nie zna żadnego
klucza API** — czyta wyłącznie publiczny plik wygenerowany przez backend.

Źródło danych:
```
https://raw.githubusercontent.com/pb2112-netizen/barometr/main/barometer.json
```

## Stos technologiczny
- Kotlin + Jetpack Compose (Material3)
- OkHttp + kotlinx.serialization (sieć, cache HTTP ETag/304)
- DataStore Preferences (cache offline + ustawienia)
- WorkManager (cykliczne odświeżanie w tle)
- Glance (widget na pulpit)

## Wymagania
- Android Studio (Ladybug lub nowszy), JDK 17
- minSdk 26, target/compileSdk 35

## Uruchomienie
1. Otwórz folder `WorldBarometer/` w Android Studio (generuje `gradle-wrapper.jar`
   oraz `local.properties` z `sdk.dir`).
2. Alternatywnie z CLI: `gradle wrapper` (raz), potem `./gradlew assembleDebug`.
3. Zainstaluj na urządzeniu/emulatorze: `./gradlew installDebug`.

## Struktura
- `core/`  — mapowanie poziomu i paleta kolorów (wspólne dla UI i widgetu)
- `data/`  — model (`model/`), sieć (`remote/`), repozytorium (`repo/`)
- `di/`    — `ServiceLocator` (lekkie DI bez Hilt)
- `ui/`    — ekran główny, ustawienia, theme, ViewModel
- `widget/`— widget Glance
- `work/`  — WorkManager + powiadomienia

## Bezpieczeństwo (hardening)
- **TLS wymuszony**: `usesCleartextTraffic=false` + `network_security_config.xml` (brak HTTP, brak wyłączania weryfikacji certyfikatu).
- **Dane niezaufane**: każdy `barometer.json` jest sanityzowany przed wyświetleniem (`core/ContentSafety.kt`) — clamp ocen do 1.0–10.0, limit 3 eventów, usuwanie znaków sterujących, przycinanie długości.
- **Limit payloadu**: odpowiedź czytana z sufitem 256 KB (ochrona przed DoS).
- **Brak sekretów / minimalne uprawnienia**: aplikacja nie zna żadnego klucza API; tylko `INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`.
- **Brak wykonywalnej treści**: render wyłącznie przez `Text` (zero WebView/HTML/auto-linkify).
- **Łańcuch dostaw**: wersje zależności przypięte w `gradle/libs.versions.toml`; zalecany audyt + Gradle dependency verification.
- **Transparentność (wymogi Google Play)**: w aplikacji jest polityka prywatności (zakładka Ustawienia), disclaimer na ekranie głównym oraz atrybucja źródeł (po rozwinięciu eventu).

## Status MVP
- [x] Fundament projektu (Gradle, manifest, uprawnienia)
- [x] Krok 1: sieć + model danych + mapowanie poziomu/palety
- [x] Krok 2: ekran główny + hardening bezpieczeństwa
- [x] Krok 3: widget Glance (gradient poziomu, liczba, tap = odświeżenie)
- [x] Krok 4: WorkManager (cykliczny 15 min + one-off, constraints sieć/bateria) + powiadomienia (próg + wzrost + ≥3 h)
- [x] Krok 5: ustawienia (DataStore) — suwak progu, przełącznik powiadomień, info o ostatniej aktualizacji
