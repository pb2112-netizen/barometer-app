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
- **App:** v0.8.0 (versionCode 15), branch `master`, commit `fbf979d` na `origin/master`.
  Build + test u PO ✅ (sparkline dashboard + widget). Tag `v0.8.0` — **do założenia**.
- **Silnik:** WB-003 + WB-017/018/019 na `origin/main` (commit `0d7f108`). `score_history` w JSON live.
- **Remote apki:** `origin` → `https://github.com/pb2112-netizen/barometer-app.git` (public).
- **Backend live:** multi-lens + `score_history` 72h (WB-003).

## Stan na teraz
- **Done (sesja 2026-06-14, WB-003 — zamknięte):**
  - Silnik: push `0d7f108`; `score_history` w pamieci + `barometer_{lens}.json`.
  - Apka: push `fbf979d`; sparkline dashboard/widget; fix kompilacji (`coerceIn`, `isSystemInDarkTheme`).
  - AGP 8.13.2 / Gradle 8.13 — build Android Studio OK u PO.
- **Done wcześniej:** v0.7.0 WB-014/015, silnik WB-013/017/018/019, widget refresh, MVP.
- **Build apki:** tylko u PO — kontener bez Android SDK.

## Następne kroki (priorytet ↓)
1. **Tag release:** `git tag v0.8.0 && git push origin --tags` (repo apki).
2. **Kalibracja (3–5 dni):** rozkład score ~90% cykli 1–3; kształt sparkline vs decay WB-017.
3. **Play Store (WB-009+)** + **Privacy URL (WB-010)** — po stabilnym v0.8.0 w produkcji.
4. **Opcjonalnie:** pełna regresja JSON po GA (decay UA, cap retoryki ≤3.0) — jeśli nie sprawdzono ręcznie.

## Otwarte problemy
- **Tag v0.8.0 nie założony** — punkt powrotu (decyzja PO).
- **Sparkline rozgrzewka** — <3 punkty w historii: wykres bez puls; wypełnia się co cykl GA (~1 h).
- **Build apki tylko u PO** — brak Android SDK w kontenerze.
- **`gradle-wrapper.jar` nie w repo** — Android Studio dogeneruje przy sync.
- **PAT cron-job.org wygasa** (backend) — przy 401 odnowić token w repo `barometr`.

## Szybki git
```bash
# Silnik — przed pushem ZAWSZE fetch + merge:
cd /workspaces/Agenci_SEO/WB/barometr && git fetch origin && git merge origin/main && git push origin main

# Apka:
cd /workspaces/Agenci_SEO/WB/WorldBarometer && git push origin master
# tag po decyzji PO:
git tag v0.8.0 && git push origin --tags
```
Commit z inline identity → `PROJECT.md` §7. **Nie** commituj z root `Agenci_SEO/`.
