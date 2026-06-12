# CHANGELOG — World Barometer

Historia projektu (append-only). Najnowsze na górze. Nowa sesja zwykle czyta tylko 1–2 górne pozycje.
Tu trafia archiwum incydentów i rozwiązanych problemów (skompresowane), żeby `02_HANDOVER.md` był mały.

Format: `## [wersja] — data` + `Added/Changed/Fixed/Docs`. Wersje = tagi git w repo aplikacji.

---

## [v0.6.4] — 2026-06-12 — Widget: render bezpośredni przez AppWidgetManager (omija sesje Glance)
- Fixed (próba 3, po niepowodzeniu v0.6.3 u PO): widget nadal nie reagował na zmianę kraju
  mimo poprawnego (książkowego) wzorca reaktywnego. Wniosek: zawodzi DOSTARCZENIE renderu —
  `GlanceAppWidget.update()` przy żywej/zombie sesji tylko zgłasza event do SessionWorkera,
  a te bywają gubione (znany problem biblioteki: update'y w oknie życia sesji ~45 s ignorowane).
- Changed: `BarometerWidgetUpdater` — **omija menedżera sesji Glance**: `runComposition()`
  (Glance 1.1, @ExperimentalGlanceApi; obecność API zweryfikowana w binarce 1.1.0) renderuje
  pełny `provideGlance` do RemoteViews, wypychanych BEZPOŚREDNIO przez
  `AppWidgetManager.updateAppWidget()` — deterministyczna ścieżka klasycznych widgetów.
  Timeout 10 s → fallback na klasyczne `update()`. Mutex (runComposition nie wspiera
  równoległych wywołań dla tego samego ID). Jedyna akcja widgetu (`actionStartActivity`)
  to PendingIntent — działa bez sesji.
- Added: logi diagnostyczne tag **`WB-Widget`** (updater, `RefreshWorker`, `setLensId`) —
  filtr w logcat pokazuje całą ścieżkę zmiany kraju.
- Note: limit Androida (~30 min) dotyczy tylko pasywnego `updatePeriodMillis`; event-driven
  update'y (akcja usera, worker) są dozwolone i natychmiastowe — oczekiwane UX jest osiągalne.
- Verified: **✅ PO (2026-06-12, S24 + emulator Pixel 7)** — widget reaguje na zmianę kraju;
  problem odświeżania ZAMKNIĘTY po 3 podejściach (v0.6.2 hipoteza One UI — błędna;
  v0.6.3 treść reaktywna — poprawna, ale niewystarczająca; v0.6.4 bezpośredni render — skuteczny).

## [v0.6.3] — 2026-06-12 — Widget: reaktywna treść (fix martwego odświeżania)
- Fixed: **widget nie odświeżał się po zmianie kraju** (czasem dopiero po ~10 s, czasem wcale,
  niezależnie od urządzenia — S24 i emulator Pixel 7). PRAWDZIWA przyczyna (poprzednie hipotezy
  One UI/expedited błędne): `provideGlance` czytał snapshot/lens JEDNORAZOWO przed `provideContent`.
  Glance trzyma żywą sesję kompozycji per widget; `update()` na żywej sesji tylko REKOMPONUJE
  (nie uruchamia ponownie `provideGlance`), więc renderował wartości zamrożone w domknięciu.
  Działało tylko gdy sesja zdążyła wygasnąć (timeout) — stąd losowość i „działa po cyklu silnika".
- Changed: `BarometerWidget.provideGlance` — treść czytana WEWNĄTRZ kompozycji:
  `repository.observe()` + `settings.lensId` przez `collectAsState`; `scan` trzyma ostatni
  niepusty snapshot (kraj+dane przełączają się atomowo po zapisie nowego cache — zachowany
  fix „pustego widgetu" z v0.6.2). Kraj w badge = lens FAKTYCZNIE pokazywanych danych.
- Changed: `BarometerWidgetUpdater` — tylko `update()` per instancja (restart martwej sesji);
  usunięto zapis stanu Glance (`updateAppWidgetState`) i plik `BarometerWidgetState.kt` —
  stan Glance niepotrzebny, źródłem prawdy jest DataStore.
- Unchanged: hybryda foreground + expedited backstop z v0.6.2 (nadal potrzebna: worker
  przeżywa wyjście z apki i restartuje martwą sesję po pobraniu).
- Fixed: kontrast nazwy kraju w chipie „Scoring for:" w light mode — `primary` (calmTeal
  `0xFF8FB8B2`) ginął na jasnym tle chipa; nowy `BrandPalette.calmTealDeep` (`0xFF3E6B64`)
  tylko dla light (dark bez zmian, globalny `primary` nieruszony — używa go Settings).

## [v0.6.2] — 2026-06-12 — Widget: czas absolutny + niezawodne odświeżanie kraju
- Fixed: „Updated" w widgecie nie kłamie po długim braku przerysowania. Czas WZGLĘDNY
  („5 min ago") zamrażał się między cyklami workera (~60 min), bo widget Glance nie
  przerysowuje się sam (`updatePeriodMillis=0`). Teraz czas ABSOLUTNY lokalny.
- Changed: `RelativeTime.formatAbsolute()` — dzisiaj „HH:mm", inny dzień „d MMM, HH:mm"
  (stare/offline dane nie wyglądają na świeże). Usunięto względny `format()` (źródło regresji).
- Changed: spójnie absolutny czas także na dashboardzie (`MainScreen`) i w Settings (`SettingsViewModel`).
- Changed: Settings „Last data update" — pełna data z rokiem (`formatAbsoluteFull` → „12 Jun 2026, 14:05").
- Fixed: **kraj w widgecie nie odświeżał się po zmianie lensu** (pokazywał stary kraj aż do
  cyklu silnika). Przyczyna: render Glance był zlecany ze zwykłej korutyny UI, którą One UI
  (Samsung) zamrażał po wyjściu z apki, zanim render doszedł.
- Fixed: **puste wyniki w widgecie dla krajów ≠ PL** (`—/10`, „Stable", choć w apce dane OK).
  Przyczyna: render „kraj od razu" leciał PRZED pobraniem (cache nowego lensu pusty), a drugi
  render z danymi był gubiony przez debounce Glance (~1–2 s). Naprawa: **jeden render PO pobraniu**.
- Added: `RefreshScheduler.requestLensChangeRefresh()` — **expedited** one-off WorkManager
  (REPLACE, `RUN_AS_NON_EXPEDITED_WORK_REQUEST`) jako backstop przeżywający wyjście z apki;
  `RefreshWorker` tryb `KEY_LENS_CHANGE` (render PO refreshu) + `getForegroundInfo()`
  wymagane przez expedited na API < 31 (cichy kanał `barometer_updates`, IMPORTANCE_MIN).
- Changed: `SettingsViewModel.setLensId` — hybryda: szybka ścieżka w foregroundzie
  (refresh → 1 render) dla natychmiastowości, gdy user zostaje w apce, + WorkManager jako
  backstop, gdy wyjdzie od razu. Usunięto kruchy `requestUpdate + delay(2500) + requestUpdate`
  oraz `GLANCE_SECOND_UPDATE_DELAY_MS`.

## [backend WB-012] — 2026-06-12 — Event summary restore (apka bez zmian kodu)
- Fixed (silnik `barometr/`): `top_events[].summary` — regresja po multi-lens; fallback + tryb prosty.
- Docs: weryfikacja u PO po cyklu silnika — rozwinięta karta już renderuje `summary` (`MainScreen.kt`).

## [v0.6.1] — 2026-06-11 — Country lens visibility (WB-011)
- Added: klikalny chip **Scoring for {Country}** nad score na dashboardzie (`CountryLensChip.kt`);
  tap → Settings (bez słowa „Lens” w UI głównym).
- Changed: widget Glance — kraj w lewym dolnym rogu (pin + nazwa); timestamp oddzielony od summary; rozszerzony `contentDescription` (TalkBack).
- Changed: dashboard — tylko nazwa kraju jest klikalna; prefix „Scoring for:” jako zwykły tekst.
- Changed: `legal_strings.xml` — privacy: country lens w local storage + opis pliku per lens w Permissions.
- Changed: `HomeViewModel` obserwuje `lensId` — chip widoczny także przy pierwszym ładowaniu.
- Changed: próby przyspieszenia widgetu po zmianie kraju — `BarometerWidgetUpdater`, Glance state, `currentSnapshot()` (fix **niesprawdzony u PO**; temat odłożony → `02_HANDOVER.md`).

## [proces] — 2026-06-11 — GitHub remote (barometer-app)
- Docs: repo apki publiczne na GitHub — `pb2112-netizen/barometer-app`; `origin` skonfigurowany,
  push branch `master` + tagi git (`v0.1.0` … `v0.6.0`). Handover i `PROJECT.md` §7 zaktualizowane.

## [v0.6.0] — 2026-06-11 — Country lens (WB-008)
- Added: **Country lens** picker w Settings (Poland, Romania, Portugal, Ukraine, United States).
- Added: `LensCatalog`, `lens_id` w `SettingsStore`, cache per lens w `BarometerStore`.
- Changed: `BarometerApi` — dynamiczny URL `barometer_{lens}.json`; `BarometerRepository` reaguje na zmianę lens.
- Changed: Legal/About — usunięto „planned”, dodano listę krajów i info o pliku per lens.
- Changed: Home — dyskretna linia „Lens: …” pod summary.
- Verified: PO — zmiana lens, widget, offline cache; silnik multi-lens live na GitHub.

## [proces] — 2026-06-11 — Monorepo lokalne `WB/`
- Docs: `WorldBarometer`, `barometr`, `Tasks` przeniesione do `WB/`; `WB/README.md`, zaktualizowane
  ścieżki w handover, `PROJECT.md`, regule `.cursor/rules/barometr-handover.mdc`. Git remote bez zmian.

## [v0.5.1] — 2026-06-11 — Launcher icon fix (pixel-perfect PNG)
- Fixed: adaptive icon foreground = wycięty bitmap z `world_barometer_logo_calm_water_v1.png`
  (zamiast uproszczonego wektora); tło `#FDFAF4` dopasowane do PNG.
- Changed: assety Play Store wygenerowane z tego samego wycięcia.

## [v0.5.0] — 2026-06-11 — Branding (calm water logo)
- Changed: adaptive launcher icon — ciepłe tło kremowe + wektor fal wody (opcja C).
- Changed: cieplejszy `NeutralPalette`, `BrandPalette`, `primary` = calm teal (Settings switch/slider).
- Changed: disclaimer i karty eventów — zaokrąglenie 16 dp, ciepły disclaimer (light).
- Added: `design/source_logo_calm_water_v1.png`, `design/play-store/ic_launcher_play_512.png`,
  `feature_graphic_1024x500.png`.
- Changed: ikona powiadomienia (`ic_stat_barometer`) — uproszczona fala.
- Unchanged: kolory poziomów alertu (`LevelPalette`) i gradienty widgetu.

## [v0.4.1] — 2026-06-11 — Widget trend icon
- Added: biała ikona trendu (↑/↓/→) w widgecie Glance; `ic_trend_rising`,
  `ic_trend_falling`, `ic_trend_stable`; źródło `Snapshot.trend` (bez dodatkowej sieci).
- Changed: ikona trendu — prawy górny róg widgetu, rozmiar 34 dp (jak cyfra score).

## [v0.4.0] — 2026-06-11 — Legal & About screen
- Added: ekran **Legal & About** (`ui/legal/LegalAboutScreen.kt`) — About, Privacy, MIT license,
  Content &amp; sources, Contact, wersja apki; wejście z Settings.
- Added: `LICENSE` (MIT), `legal_strings.xml`, `core/LegalLinks.kt`, `core/OpenExternal.kt`.
- Changed: Settings — tylko ustawienia funkcjonalne + link do Legal (usunięto pełną politykę z Settings).
- Changed: nawigacja `HOME → SETTINGS → LEGAL`; `buildConfig` dla stopki wersji.
- Docs: README — badge MIT + sekcja licencji.

## [proces] — 2026-06-11 — System handoveru
- Docs: rozdzielono dokumentację na trzy role: `02_HANDOVER.md` (żywy stan, mały),
  `PROJECT.md` (stabilna referencja), `CHANGELOG.md` (historia). Protokół aktualizacji
  jako reguła `.cursor/rules/handover.mdc` (always-apply). Cel: stały, niski koszt kontekstu
  na start kolejnej sesji niezależnie od wieku projektu.

## [v0.3.0] — 2026-06-11 — Dopasowanie odświeżania do backendu
- Changed: `RefreshScheduler` interwał WorkManager 15 → 60 min; polityka `KEEP` → `UPDATE`
  (przy aktualizacji appki nowy interwał podmienia stary plan bez gubienia harmonogramu).
- Changed: `BarometerRepository.STALE_AFTER_MILLIS` 45 → 90 min (baner „out of date" nie świeci
  fałszywie między godzinnymi aktualizacjami).
- Changed (backend): cron `*/30` → `17 * * * *`; utwardzony krok publikacji
  (`commit -> git pull --rebase -> push`, odporny na non-fast-forward); wyciszony warning Node20
  (`FORCE_JAVASCRIPT_ACTIONS_TO_NODE24`).
- Chore: `.gitignore` += `.kotlin/` (pliki sesji kompilatora).

## [v0.2.1] — 2026-06-10 — Fix widgetu
- Fixed: tap w widget przez jawny `Intent` (naprawa błędu kompilacji `intent`).

## [v0.2.0] — 2026-06-10 — Poprawki bugów z testów
- Added: polityka prywatności (Settings), disclaimer (dashboard), atrybucja źródeł (po rozwinięciu eventu).
- Changed: sekcja „Top events" i karty zwijalne (domyślnie zwinięte); cała aplikacja po angielsku
  (UI, disclaimer, privacy, ustawienia, czas, powiadomienia); pasek skali z oznaczeniami 1 i 10.
- Changed: widget — last update + ocena „X/10" + krótki komentarz; tap otwiera aplikację
  (usunięto wcześniejsze „tap = odświeżenie", kod był w `RefreshWidgetAction`).
- Added: manualne odświeżanie (pull-to-refresh + przycisk Refresh w app barze).

## [v0.1.0] — 2026-06-10 — Baseline MVP
- Added: pełny MVP (5 kroków) — dashboard, widget Glance, WorkManager + powiadomienia, ustawienia,
  warstwa sieci + model danych, cache offline, hardening bezpieczeństwa.

---

## Archiwum incydentów (backend)

### 2026-06-10/11 — Silnik nie odświeżał się automatycznie
- **Objaw:** dane nie aktualizowały się mimo crona; ręczny „Re-run jobs" padał z
  `! [rejected] main -> main (fetch first)`.
- **Przyczyna 1 (push):** „Re-run jobs" odtwarza STARY commit → `git push` odrzucony
  (non-fast-forward), bo `main` poszło do przodu. Silnik (krok 5) był zielony — błąd tylko w publikacji.
  → Fix: utwardzony krok publikacji `commit -> git pull --rebase -> push origin HEAD:<branch>`.
- **Przyczyna 2 (cron, główna):** wbudowany `schedule` GitHuba w tym repo NIE odpalał się
  (przez ~10h 0 runów z `schedule`, mimo `state: active` i poprawnej składni — GitHub po cichu gubi
  zaplanowane runy świeżych repo; nie tworzy nawet wpisu w historii). Niemożliwe do naprawy w pliku.
  → Fix: **zewnętrzny trigger cron-job.org** woła co godzinę
  `POST /repos/pb2112-netizen/barometr/actions/workflows/barometr.yml/dispatches {"ref":"main"}`
  z fine-grained PAT (scope Actions: read/write, tylko to repo; token żyje TYLKO w cron-job.org).
  Potwierdzone: runy `workflow_dispatch` zielone, świeży JSON. `schedule: "17 * * * *"` zostaje jako backup.
- **Do pilnowania:** wygaśnięcie PAT (przy 401 w cron-job.org odnowić token).
