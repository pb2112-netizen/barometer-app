# CHANGELOG — World Barometer

Historia projektu (append-only). Najnowsze na górze. Nowa sesja zwykle czyta tylko 1–2 górne pozycje.
Tu trafia archiwum incydentów i rozwiązanych problemów (skompresowane), żeby `02_HANDOVER.md` był mały.

Format: `## [wersja] — data` + `Added/Changed/Fixed/Docs`. Wersje = tagi git w repo aplikacji.

---

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
