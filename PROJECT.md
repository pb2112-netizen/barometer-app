# PROJECT — World Barometer (Android)

Stabilna referencja projektu. Zmienia się RZADKO (architektura, stos, decyzje, kontrakt danych).
Bieżący stan i następne kroki → `02_HANDOVER.md`. Historia → `CHANGELOG.md`.

## 1. Cel i granice

- Natywna apka Android, która **TYLKO pobiera gotowy publiczny JSON** i prezentuje „barometr"
  sytuacji światowej. Aplikacja nie liczy nic sama, nie zna żadnego klucza API.
- **Backend (silnik) jest gotowy i nieruszalny bez wyraźnej prośby.**
- Cała aplikacja po **angielsku** (UI, teksty). Komentarze w kodzie mogą być po polsku.

## 2. Gdzie co jest

- **Aplikacja Android (ten projekt):** `/workspaces/Agenci_SEO/WorldBarometer/` — osobne repo git.
- **Backend (silnik):** `/workspaces/Agenci_SEO/barometr/` — Python + GitHub Actions. Osobne repo
  (remote: `github.com/pb2112-netizen/barometr`). Kontrakt/decyzje backendu: `barometr/01_START_TUTAJ.md`,
  `barometr/SPEC_MVP.md`, `barometr/makiety/paleta.json` — czytaj TYLKO gdy zadanie dotyka backendu/danych.
- **Endpoint danych (jedyny):** `https://raw.githubusercontent.com/pb2112-netizen/barometr/main/barometer.json`

## 3. Stos i decyzje techniczne (trzymaj się ich)

- Kotlin + Jetpack Compose (Material3), single-Activity, **minSdk 26 / compile+target 35**.
- Sieć: **OkHttp + kotlinx.serialization** (świadomie BEZ Retrofit). Cache HTTP (ETag/304) w OkHttp.
- **DataStore Preferences** (2 magazyny): cache wyniku + ustawienia.
- **WorkManager** (cykliczne odświeżanie + one-off). **Glance** (widget). **DI: prosty `ServiceLocator`** (bez Hilt).
- Build: Gradle KTS + version catalog `gradle/libs.versions.toml` (AGP 8.5.2, Kotlin 2.0.20, Compose BOM 2024.09.02).
- Pakiet: `com.worldbarometer.app`.
- **Bezpieczeństwo:** `network_security_config.xml` wymusza TLS (bez cleartext), bez cert-pinningu (świadomie).
  Limit pobrania 256 KB. UI renderuje wyłącznie `Text` (zero WebView/HTML). Uprawnienia: sieć + `POST_NOTIFICATIONS`.

## 4. Architektura (mapa plików)

```
core/         Level (mapowanie label/score), LevelPalette+NeutralPalette (z paleta.json),
              RelativeTime (czas wzgl., EN), ContentSafety (sanityzacja niezaufanego JSON)
data/model/   BarometerData, TopEvent (@Serializable, 1:1 z barometer.json)
data/remote/  BarometerApi (OkHttp, limit 256 KB)
data/local/   BarometerStore (cache wyniku), SettingsStore (próg, on/off, stan powiadomień)
data/repo/    BarometerRepository (refresh()+observe(), Snapshot{level,trend,isStale})
di/           ServiceLocator (repository, settingsStore; init w BarometerApp)
ui/home/      MainScreen (dashboard), HomeViewModel
ui/settings/  SettingsScreen, SettingsViewModel
ui/theme/     BarometerTheme (light/dark)
widget/       BarometerWidget (Glance), BarometerWidgetReceiver
work/         RefreshWorker (pobiera w tle, update widget, logika powiadomień),
              RefreshScheduler (periodic+one-off), Notifier (kanał+alert)
```

## 5. Logika kluczowa (żeby nie odkrywać od nowa)

- **Poziomy/kolory:** `Level.resolve(level_label, score)` — priorytet etykiety z JSON, fallback ze score.
  Zakresy: Stable<3, Low<5, Elevated<7, High<9, Critical≥9. Kolory/gradienty = `paleta.json`.
- **Odświeżanie:** WorkManager periodic **60 min** (`RefreshScheduler`, polityka `UPDATE`),
  constraints sieć+bateria. Plus one-off (pull-to-refresh / przyszłe tapy). Wyzwalacze ręczne
  (przycisk Refresh, pull-to-refresh) → `HomeViewModel.refresh(manual=true)`; otwarcie ekranu też odświeża.
- **Throttling ręczny:** 60 s (`HomeViewModel.MANUAL_THROTTLE_MS`); przy throttlingu krótki feedback bez sieci.
- **Stale:** dane > **90 min** = baner „Data may be out of date" (`BarometerRepository.STALE_AFTER_MILLIS`,
  liczone od ostatniego udanego pobrania). Offline = ostatni cache + baner; ekran nigdy nie pustoszeje.
- **Powiadomienie (RefreshWorker):** wyślij gdy `score ≥ próg` ORAZ wzrost względem poprzedniego odczytu
  ORAZ minęło ≥ 3 h od ostatniego (pierwszy odczyt nigdy nie alarmuje). Próg z SettingsStore (domyślnie 5.0).
- **Sanityzacja:** `BarometerData.sanitized()` (clamp 1–10, max 3 eventy, czyszczenie znaków sterujących,
  limity długości) stosowana w repo przy sieci i cache.
- **Widget vs przycisk:** przycisk Refresh w appce aktualizuje dane+ekran, ale NIE przerysowuje widgetu
  (to robi `RefreshWorker`). Tap w widget (od v0.2.0) tylko otwiera aplikację.

## 6. Kontrakt danych (skrót; pełny w `barometr/01_START_TUTAJ.md`)

Pola `barometer.json` używane przez UI: `global_score` (1.0–10.0, wielka liczba + kolor),
`level_label` (Stable/Low/Elevated/High/Critical), `trend` (rising/falling/stable), `short_summary`,
`top_events[]` (`title`, `summary`, `score`, `sources`), `updated_at` (ISO UTC). Język treści: angielski.

## 7. Wersjonowanie (git, lokalne, bez remote)

- Repo git w `WorldBarometer/`. Brak globalnej tożsamości git → commituj z inline:
  `git -c user.name="World Barometer Dev" -c user.email="dev@worldbarometer.local" commit ...`
  (NIE modyfikuj globalnego git config).
- Po paczce poprawek: podbij `versionCode`/`versionName` w `app/build.gradle.kts`, commit, tag (np. `v0.4.0`).
- Powrót do punktu: `git checkout <tag>` / `git checkout main`. Historia wersji → `CHANGELOG.md`.

## 8. Backend — model współpracy (skrót)

- Silnik liczy ocenę ~co godzinę i publikuje `barometer.json`. Apka tylko konsumuje wynik
  (nie potrafi i NIE ma uruchamiać workflow — brak sekretów w telefonie, decyzja bezpieczeństwa).
- Automatyczne uruchamianie silnika realizuje **zewnętrzny trigger** (cron-job.org → `workflow_dispatch`),
  bo wbudowany `schedule` GitHuba nie odpalał się. Szczegóły/incydent → `CHANGELOG.md`.

## 9. Jak uruchomić (skrót dla usera-amatora)

Android Studio → Open → folder `WorldBarometer` → poczekaj na Gradle sync (pierwszy raz długo) →
Device Manager (emulator Pixel, API 34/35) lub telefon z debugowaniem USB → ▶ Run.
Uwaga: kontener deweloperski NIE kompiluje (brak Android SDK) — build robi użytkownik w Android Studio.
