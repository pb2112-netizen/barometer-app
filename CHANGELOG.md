# CHANGELOG — World Barometer

Historia projektu (append-only). Najnowsze na górze. Nowa sesja zwykle czyta tylko 1–2 górne pozycje.
Tu trafia archiwum incydentów i rozwiązanych problemów (skompresowane), żeby `02_HANDOVER.md` był mały.

Format: `## [wersja] — data` + `Added/Changed/Fixed/Docs`. Wersje = tagi git w repo aplikacji.

---

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
