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
- **App:** v0.8.0 (versionCode 15), branch `master`. Tag **po build i teście u PO**.
- **Silnik:** WB-003 + WB-017/018/019 **na `origin/main`** (commit `0d7f108`). Następny cykl GA ~:01 — weryfikacja JSON.
- **Remote apki:** `origin` → `https://github.com/pb2112-netizen/barometer-app.git` (public).
- **Backend live:** multi-lens; po najbliższym cyklu GA — `score_history` w JSON (WB-003).

## Stan na teraz
- **Done (sesja 2026-06-14, WB-003):**
  - Silnik: `score_history` 72h w pamieci + JSON; helpery `_dopisz/_przytnij/_migruj`.
  - Apka: `Sparkline.kt`, dashboard (score `/10`, sparkline, kotwice czasu), widget mini sparkline.
  - Usunięto `TrendArrow`, `ScoreBar`, `ic_trend_*`. Szczegóły → CHANGELOG obu repo.
- **Done wcześniej:** v0.7.0 WB-014/015, silnik WB-013/017/018/019, widget refresh fix, MVP.
- **Build apki:** tylko u PO — kontener bez Android SDK (v0.8.0 **nieskompilowany**).

## Następne kroki (priorytet ↓)
1. **Weryfikacja po cyklu GA** (~:01): `score_history` w `barometer_pl.json`, decay/retoryka bez regresji, punkt bieżący = `global_score`.
2. **Build + test u PO (v0.8.0):** sparkline dashboard (puls ≥3 pkt, stale/offline bez puls),
   widget TopEnd, stary cache bez `score_history`, TalkBack.
3. **Po teście u PO:** `git tag v0.8.0 && git push origin --tags`.
4. **Kalibracja (3–5 dni po push):** rozkład score; kształt sparkline vs rzeczywistość.
5. **Play Store (WB-009+)** + **Privacy URL (WB-010)** — po stabilnym v0.8.0.

## Otwarte problemy
- **v0.8.0 nieskompilowane u PO** — build Android Studio.
- **Sparkline rozgrzewka** — <3 punkty: wykres bez puls; oczekiwane do ~3 cykli GA.
- **Build apki tylko u PO** — brak Android SDK w kontenerze.
- **`gradle-wrapper.jar` nie w repo** — Android Studio dogeneruje przy sync.
- **PAT cron-job.org wygasa** (backend) — przy 401 odnowić token w repo `barometr`.

## Szybki git
```bash
# Silnik — przed pushem ZAWSZE fetch + merge:
cd /workspaces/Agenci_SEO/WB/barometr && git fetch origin && git merge origin/main && git push origin main

# Apka:
cd /workspaces/Agenci_SEO/WB/WorldBarometer && git push origin master
# po build i teście u PO: git tag v0.8.0 && git push origin --tags
```
Commit z inline identity → `PROJECT.md` §7. **Nie** commituj z root `Agenci_SEO/`.
