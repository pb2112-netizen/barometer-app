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
- **App:** v0.6.0 (versionCode 9), tag `v0.6.0`, HEAD `7e8ee56`, branch `master`.
- **Remote:** `origin` → `https://github.com/pb2112-netizen/barometer-app.git` (public).
- **Backend:** multi-lens live — `barometer_{pl,ro,pt,ua,us}.json` + `manifest.json`.

## Stan na teraz
- **Done:** MVP, Legal (WB-004), widget trend (WB-002), branding (WB-007), country lens (WB-008).
- **WB-008 zweryfikowane u PO:** picker Settings, zmiana lens → inny score/treść, widget OK.
- **Silnik:** JSON na GitHubie HTTP 200; cron przez cron-job.org → `workflow_dispatch`.
- **Repo apki:** git w `WorldBarometer/`, remote skonfigurowany, `master` + tagi na GitHubie.
- **Build:** tylko u usera w Android Studio — kontener bez Android SDK.

## Następne kroki (priorytet ↓)
1. **Play Store (WB-009+)** — listing EN, Data safety, assety w `design/play-store/`.
2. **Privacy URL publiczny (WB-010)** — wymagany przed publikacją Store.
3. Tłumaczenia PL/EN UI; `@Preview`; testy jednostkowe core.
4. Po zmianach docs: commit + `git push` (+ tag przy nowej wersji apki).

## Otwarte problemy
- **Build tylko u usera** — brak Android SDK w kontenerze deweloperskim.
- **`gradle-wrapper.jar` nie w repo** — Android Studio dogeneruje przy sync lub `gradle wrapper`.
- **PAT cron-job.org wygasa** (backend) — przy 401 odnowić token w repo `barometr`.

## Szybki git (apka)
```bash
cd /workspaces/Agenci_SEO/WB/WorldBarometer   # lub E:\AI\Agenci_SEO\WB\WorldBarometer
git status && git push && git push origin --tags   # tagi tylko przy nowej wersji
```
Commit z inline identity → `PROJECT.md` §7. **Nie** commituj z root `Agenci_SEO/` — to inny folder.
