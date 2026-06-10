# HANDOVER — World Barometer (Android)

Dokument przekazania dla nowej sesji/modelu. Czytaj na starcie, zanim zaczniesz kodować.
Cel projektu: natywna apka Android, która TYLKO pobiera gotowy publiczny JSON i prezentuje
„barometr" sytuacji światowej. Backend jest gotowy i nieruszalny.

---

## 1. Gdzie co jest

- **Aplikacja Android (ten projekt):** `/workspaces/Agenci_SEO/WorldBarometer/` — osobne repo git.
- **Backend (silnik, NIE ruszać bez prośby):** `/workspaces/Agenci_SEO/barometr/` — Python + GitHub Actions.
- **Kontrakt danych i decyzje:** `barometr/START_TUTAJ.md`, `barometr/SPEC_MVP.md`, `barometr/makiety/paleta.json`. **Przeczytaj je.**
- **Endpoint danych (jedyny):** `https://raw.githubusercontent.com/pb2112-netizen/barometr/main/barometer.json`
- Aplikacja NIE zna żadnego klucza API. Czyta tylko publiczny plik.

## 2. Stos i decyzje techniczne (trzymaj się ich)

- Kotlin + Jetpack Compose (Material3), single-Activity, **minSdk 26 / compile+target 35**.
- Sieć: **OkHttp + kotlinx.serialization** (świadomie BEZ Retrofit). Cache HTTP (ETag/304) w OkHttp.
- **DataStore Preferences** (2 magazyny): cache wyniku + ustawienia.
- **WorkManager** (cykl 15 min, constraints sieć+bateria).
- **Glance** (widget). **DI: prosty `ServiceLocator`** (bez Hilt).
- Build: Gradle KTS + version catalog `gradle/libs.versions.toml` (AGP 8.5.2, Kotlin 2.0.20, Compose BOM 2024.09.02).
- Pakiet: `com.worldbarometer.app`.

## 3. Architektura (mapa plików)

```
core/         Level (mapowanie label/score), LevelPalette+NeutralPalette (z paleta.json),
              RelativeTime (czas wzgl., EN), ContentSafety (sanityzacja niezaufanego JSON)
data/model/   BarometerData, TopEvent (@Serializable, 1:1 z barometer.json)
data/remote/  BarometerApi (OkHttp, limit 256 KB)
data/local/   BarometerStore (cache wyniku), SettingsStore (próg, on/off, stan powiadomień)
data/repo/    BarometerRepository (refresh()+observe(), Snapshot{level,trend,isStale 45min})
di/           ServiceLocator (repository, settingsStore; init w BarometerApp)
ui/home/      MainScreen (dashboard), HomeViewModel
ui/settings/  SettingsScreen, SettingsViewModel
ui/theme/     BarometerTheme (light/dark)
widget/       BarometerWidget (Glance), BarometerWidgetReceiver
work/         RefreshWorker (pobiera w tle, update widget, logika powiadomień),
              RefreshScheduler (periodic+one-off), Notifier (kanał+alert)
```

## 4. Logika kluczowa (żeby nie odkrywać od nowa)

- **Poziomy/kolory:** `Level.resolve(level_label, score)` — priorytet etykiety z JSON, fallback ze score.
  Zakresy: Stable<3, Low<5, Elevated<7, High<9, Critical≥9. Kolory/gradienty = `paleta.json`.
- **Powiadomienie (RefreshWorker):** wyślij gdy `score ≥ próg` ORAZ wzrost względem poprzedniego
  odczytu ORAZ minęło ≥ 3 h od ostatniego (pierwszy odczyt nigdy nie alarmuje). Próg z SettingsStore (domyślnie 5.0).
- **Pull-to-refresh:** throttling 60 s (HomeViewModel.MANUAL_THROTTLE_MS); przy throttlingu pokazujemy krótki feedback bez sieci.
- **Stale:** dane > 45 min = baner „Data may be out of date". Offline = ostatni cache + baner.
- **Sanityzacja:** `BarometerData.sanitized()` (clamp 1–10, max 3 eventy, czyszczenie znaków sterujących,
  limity długości) stosowana w repo przy sieci i cache. UI renderuje tylko Text (zero WebView/HTML).
- **Bezpieczeństwo:** `network_security_config.xml` wymusza TLS (bez cleartext), bez cert-pinningu (świadomie).

## 5. Wersjonowanie (git, lokalne, bez remote)

- Repo git w `WorldBarometer/`. Brak globalnej tożsamości git → commituj z inline:
  `git -c user.name="World Barometer Dev" -c user.email="dev@worldbarometer.local" commit ...`
  (NIE modyfikuj globalnego git config).
- Tagi: **`v0.1.0`** = baseline MVP, **`v0.2.0`** = poprawki bugów (bieżący `main`).
- Wersja w `app/build.gradle.kts`: versionCode 2, versionName "0.2.0". Przy kolejnych zmianach podbijaj.
- Powrót do punktu: `git checkout v0.1.0` / `git checkout main`.

## 6. Stan na teraz (po v0.2.0)

Zrobione: cały MVP (5 kroków) + hardening + polityka prywatności (Settings) + disclaimer (dashboard)
+ atrybucja źródeł (po rozwinięciu eventu). Poprawione bugi z testów:
- Sekcja „Top events" zwijalna (domyślnie zwinięta) + karty zwijalne (domyślnie zwinięte).
- Manualne odświeżanie (pull-to-refresh + przycisk Refresh w app barze).
- Cała aplikacja po angielsku (UI, disclaimer, privacy, ustawienia, czas, powiadomienia).
- Pasek skali z oznaczeniami 1 (min) i 10 (max).
- Widget: last update + ocena „X/10" (10 jak potęga) + krótki komentarz + tap otwiera aplikację.

## 7. WAŻNE / nieрozwiązane

- **NIE skompilowano w tym środowisku** (brak Android SDK + brak dostępu do pobierania zależności w kontenerze).
  Pierwszy realny build/sync robi użytkownik w Android Studio. Możliwe drobne korekty wersji bibliotek.
- **gradle-wrapper.jar nie jest commitowany** (binarny) — Android Studio dogeneruje przy otwarciu, lub `gradle wrapper`.
- **Backend cron:** workflow „Barometr update" działa POPRAWNIE logicznie (1 udany run, AI, poprawny JSON),
  ale **harmonogram `*/30` jeszcze nie odpalał automatycznie** (tylko ręczny `workflow_dispatch`).
  Do obserwacji; ew. nudge commitem lub zewnętrzny trigger przez `workflow_dispatch` API.
  W workflow jest też nieszkodliwy warning deprecacji Node20 (fix: `env: FORCE_JAVASCRIPT_ACTIONS_TO_NODE24: "true"`).

## 8. Jak uruchomić (skrót dla usera-amatora)

Android Studio → Open → folder `WorldBarometer` → poczekaj na Gradle sync (pierwszy raz długo) →
Device Manager (emulator Pixel, API 34/35) lub telefon z debugowaniem USB → ▶ Run.

## 9. Sensowne następne kroki (poza MVP, do uzgodnienia)

- Tłumaczenia PL/EN przez `res/values/strings.xml` (teraz teksty są wpisane w kodzie po EN — do ekstrakcji).
- `@Preview` dla ekranów (podgląd UI bez emulatora).
- Testy jednostkowe: `Level.resolve`, `ContentSafety.sanitized`, logika powiadomień, `RelativeTime`.
- Ikona launchera (obecnie prosty wektor) + grafiki do Google Play.
- Publikacja: konto Play Console, podpis (Play App Signing), polityka prywatności jako URL (hosting),
  Data safety, ocena treści. (Patrz wcześniejsze ustalenia w rozmowie.)
- Ew. ponowne włączenie „tap widgetu = odświeżenie" jako osobna akcja, jeśli będzie potrzeba (kod był w RefreshWidgetAction, usunięty w v0.2.0).
