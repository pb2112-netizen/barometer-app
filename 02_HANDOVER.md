# HANDOVER — World Barometer (Android) — ŻYWY STAN

> 👉 **ZACZNIJ TUTAJ.** Bieżący stan (mały, aktualny). Bez historii — limit ~120 linii;
> nadmiar → `CHANGELOG.md`.

**Cel:** natywna apka Android — TYLKO publiczny JSON → „barometr" świata. UI po angielsku;
komentarze w kodzie mogą być PL. Backend gotowy i **nieruszalny bez prośby**.

## Struktura `WB/`

| Folder | Rola | GitHub |
|--------|------|--------|
| `WB/WorldBarometer/` | **Ta apka** (Android Studio → Open ten folder) | [barometer-app](https://github.com/pb2112-netizen/barometer-app) |
| `WB/barometr/` | Silnik, JSON, GitHub Actions | [barometr](https://github.com/pb2112-netizen/barometr) |
| `WB/Tasks/` | Specyfikacje WB-00x, assety | — |

Ścieżki: Windows `E:\AI\Agenci_SEO\WB\` · kontener `/workspaces/Agenci_SEO/WB/`

## Read order (start sesji)
1. **Ten plik** (cały).
2. **`PROJECT.md`** — architektura, stos, decyzje, logika, kontrakt, wersjonowanie.
3. **`CHANGELOG.md`** — tylko gdy potrzebujesz historii / „dlaczego".
4. **`../barometr/01_START_TUTAJ.md`** — tylko gdy zadanie dotyka backendu.

Protokół docs: `.cursor/rules/barometr-handover.mdc` (MANUALNA, `@barometr-handover`).

## Bieżąca wersja
- **App:** v0.7.0 (versionCode 14), branch `master`. Tag `v0.7.0` **po build i teście u PO**.
- **Silnik:** WB-013 **wypushowany** do `origin/main` (commit `WB-013: importance scale + sentiment axis`).
- **Remote apki:** `origin` → `https://github.com/pb2112-netizen/barometer-app.git` (public).
- **Backend live:** multi-lens — `barometer_{pl,ro,pt,ua,us}.json` + `manifest.json`.
  **JSON dostanie `tone`/`sentiment` (WB-013) przy najbliższym cyklu silnika** (cron co godzinę, `~:01`).

## Stan na teraz
- **Done (sesja 2026-06-12, WB-013/014/015 — na polecenie PO wszystkie trzy naraz):**
  - **WB-013 (silnik):** skala istotności (8–10 także dla pozytywnych przełomów), `sentiment`
    per event (walidacja + fallback neutral), deterministyczny `tone` per lens (`_wylicz_tone`,
    konflikt ≤ 0.5 → neutral), tryb prosty z `SLOWA_POZYTYWNE` (ceasefire → positive, waga bez
    zmian), cisza/decay → neutral, `level_label` legacy bez zmian, zero dodatkowych calli AI.
    Logika przetestowana lokalnie (asercje OK).
  - **WB-014 (dashboard):** `Tone` enum (default NEUTRAL), pola `tone`/`sentiment` w modelu,
    `LevelPalette` = jedno źródło prawdy (pasmo × ton → etykieta/kolor/opis a11y), etykiety
    lokalnie (level_label z JSON ignorowany), pas spokoju <5 brand teal, badge eventów per
    (score × sentiment), trend per ton, a11y z tonem, zero nowych ikon. Szczegóły → `CHANGELOG.md`.
  - **WB-015 (widget):** 11 gradientów (2 spokoju + 9 sygnałowych), `backgroundFor(level, tone)`,
    etykieta ze wspólnego słownika, contentDescription z tonem przy score ≥ 5; stare tła usunięte.
- **Uwaga — kalibracja WB-013 (spec §6) pominięta przed WB-014/015 decyzją PO** (zadania zlecone
  razem). Bezpieczne: apka traktuje brak/zły `tone` jako NEUTRAL. Rozkład score (~90% cykli 1–3)
  i trafność sentymentu obserwować przez 3–5 dni po wdrożeniu.
- **Done (sesja 2026-06-12, v0.6.4):** widget odświeża się poprawnie — ✅ u PO (render przez
  `runComposition()` + RemoteViews wprost do `AppWidgetManager`). Historia → `CHANGELOG.md`.
- **Done wcześniej:** MVP, Legal (WB-004), widget trend (WB-002), branding (WB-007), country lens (WB-008), lens visibility (WB-011), WB-012 (summaries).
- **Build:** tylko u PO w Android Studio — kontener bez Android SDK (kod v0.7.0 **nieskompilowany**).

## Następne kroki (priorytet ↓)
1. **Build + test u PO (v0.7.0):** Android Studio → Run. Testy wizualne WB-015 T1–T6
   (kontrast białego tekstu na 11 tłach, „Breakthrough" na najmniejszym widgecie 110dp,
   stary cache → NEUTRAL bez crasha, zmiana kraju, tap/refresh) + dashboard: kolory/etykiety
   wg (pasmo × ton), TalkBack z tonem.
2. **Weryfikacja JSON po cyklu silnika (~:01):** 5 × `barometer_*.json` ma top-level `tone`
   i `sentiment` per event (+ niepuste summary — regresja WB-012).
3. **Po teście u PO:** `git tag v0.7.0 && git push origin --tags`; opcjonalnie punkt powrotu
   sprzed przebudowy kolorów: `git tag v0.6.4 50da461 && git push origin --tags`.
4. **Kalibracja WB-013 (3–5 dni):** rozkład score per lens nadal ~90% cykli 1–3; wyrywkowo
   trafność `sentiment` (zawieszenia broni, negocjacje, sankcje). Rozjazd → poprawki tylko w prompcie.
5. **Play Store (WB-009+)** + **Privacy URL (WB-010)** — screenshoty dopiero po v0.7.0.

## Otwarte problemy
- **v0.7.0 nieskompilowane i niezweryfikowane u PO** — build tylko u PO; testy T1–T6 czekają.
- **WB-013 bez fazy kalibracji** — obserwować rozkład score i sentyment 3–5 dni (patrz wyżej).
- **Tag v0.6.4 nie założony** — punkt powrotu sprzed v0.7.0 (decyzja PO).
- **Build tylko u usera** — brak Android SDK w kontenerze.
- **`gradle-wrapper.jar` nie w repo** — Android Studio dogeneruje przy sync.
- **PAT cron-job.org wygasa** (backend) — przy 401 odnowić token w repo `barometr`.

## Szybki git
Apka (`master`) i silnik (`main`) są zsynchronizowane z GitHub. Uwagi na przyszłość:
```bash
# Silnik publikuje JSON co godzinę (GitHub Actions) → przed pushem ZAWSZE:
cd /workspaces/Agenci_SEO/WB/barometr && git fetch origin && git merge origin/main && git push origin main

# Apka:
cd /workspaces/Agenci_SEO/WB/WorldBarometer && git push origin master
# po build i teście u PO: git tag v0.7.0 && git push origin --tags
```
Commit z inline identity → `PROJECT.md` §7. **Nie** commituj z root `Agenci_SEO/`.
