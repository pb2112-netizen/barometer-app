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
- **App:** v0.6.1 (versionCode 10), tag `v0.6.1` (do utworzenia po build u PO), branch `master`.
- **Remote:** `origin` → `https://github.com/pb2112-netizen/barometer-app.git` (public).
- **Backend:** multi-lens live — `barometer_{pl,ro,pt,ua,us}.json` + `manifest.json`.

## Stan na teraz
- **Done:** MVP, Legal (WB-004), widget trend (WB-002), branding (WB-007), country lens (WB-008), lens visibility (WB-011), **event summary restore (WB-012 — silnik)**.
- **WB-011 u PO (częściowo):** chip „Scoring for:” + klikalny kraj OK; widget — pin + nazwa kraju w rogu OK.
- **WB-008 zweryfikowane u PO:** picker Settings, zmiana lens → dashboard OK; widget **wolno / nie reaguje** na zmianę kraju (patrz otwarte problemy).
- **Silnik:** JSON na GitHubie HTTP 200; cron przez cron-job.org → `workflow_dispatch`.
- **Repo apki:** git w `WorldBarometer/`, remote skonfigurowany, `master` + tagi na GitHubie.
- **Build:** tylko u usera w Android Studio — kontener bez Android SDK.

## Następne kroki (priorytet ↓)
1. **Play Store (WB-009+)** — listing EN, Data safety, assety w `design/play-store/`.
2. **Privacy URL publiczny (WB-010)** — wymagany przed publikacją Store.
3. Po build u PO: commit + `git push` + tag `v0.6.1`.
4. **WB-012 weryfikacja u PO** — po cyklu silnika: rozwinięta karta Top event pokazuje opis nad Sources (apka bez zmian kodu).
5. Tłumaczenia PL/EN UI; `@Preview`; testy jednostkowe core.

**Do zaadresowania później (nie blokują Store na razie):**
- **Widget — szybkie odświeżenie po zmianie kraju** (WB-011 follow-up): po wyborze lens w Settings widget
  aktualizuje się z opóźnieniem lub wcale; dashboard reaguje od razu. Próbowane: `currentSnapshot()`,
  `BarometerWidgetUpdater`, Glance Widget State (`PreferencesGlanceStateDefinition`), podwójne `update()` z delay —
  **bez potwierdzonego fix u PO**; dalsze debugowanie wstrzymane. Podejrzenie: limit sesji Glance + launcher/OEM.
  Pliki: `BarometerWidget.kt`, `BarometerWidgetUpdater.kt`, `BarometerWidgetState.kt`, `SettingsViewModel.setLensId`.

## Otwarte problemy
- **Widget nie nadąża za zmianą kraju** — patrz „Do zaadresowania później” wyżej; model telefonu/launcher do ustalenia przy kolejnej sesji.
- **Build tylko u usera** — brak Android SDK w kontenerze deweloperskim.
- **`gradle-wrapper.jar` nie w repo** — Android Studio dogeneruje przy sync lub `gradle wrapper`.
- **PAT cron-job.org wygasa** (backend) — przy 401 odnowić token w repo `barometr`.

## Szybki git (apka)
```bash
cd /workspaces/Agenci_SEO/WB/WorldBarometer   # lub E:\AI\Agenci_SEO\WB\WorldBarometer
git status && git push && git push origin --tags   # tagi tylko przy nowej wersji
```
Commit z inline identity → `PROJECT.md` §7. **Nie** commituj z root `Agenci_SEO/` — to inny folder.
