# HANDOVER — World Barometer (Android) — ŻYWY STAN

> 👉 **ZACZNIJ TUTAJ.** To jest bieżący stan projektu (mały, aktualny). Nie gromadzi historii.
> Limit ~120 linii — gdy rośnie, przenieś rozwiązane/stare do `CHANGELOG.md`.

Cel: natywna apka Android, która TYLKO pobiera publiczny JSON i prezentuje „barometr" świata.
Backend gotowy i nieruszalny bez prośby. Cała aplikacja po angielsku; komentarze w kodzie mogą być PL.

## Lokalna struktura (`WB/`)

| Folder | Rola |
|--------|------|
| `WB/WorldBarometer/` | **Ta apka** (Android Studio → Open ten folder) |
| `WB/barometr/` | Silnik, JSON, GitHub Actions |
| `WB/Tasks/` | Specyfikacje WB-00x, assety (`Tasks/assets/`) |

Ścieżki: Windows `E:\AI\Agenci_SEO\WB\` · kontener `/workspaces/Agenci_SEO/WB/`

## Read order (start sesji)
1. **Ten plik** (cały) — stan + następne kroki + otwarte problemy.
2. **`PROJECT.md`** — stabilna referencja (architektura, stos, decyzje, logika, kontrakt, wersjonowanie).
3. **`CHANGELOG.md`** — TYLKO gdy potrzebujesz historii/„dlaczego".
4. **`../barometr/01_START_TUTAJ.md`** — TYLKO gdy zadanie dotyka backendu.

Protokół dokumentacji: `.cursor/rules/barometr-handover.mdc` (MANUALNA, `@barometr-handover`).

## Bieżąca wersja
- App: **v0.6.0** (versionCode 9, versionName "0.6.0").
- Backend: multi-lens (5 krajów), batched AI call, manifest.json — **WB-008 wdrożone lokalnie**.

## Stan na teraz
- **WB-008 Country lens:** picker w Settings (PL, RO, PT, UA, US), cache per lens, dynamiczny URL, widget + Legal zaktualizowane.
- MVP + Legal + widget trend + branding — gotowe.
- Build w Android Studio u usera — kontener bez Android SDK.
- **Deploy silnika:** przed testem apki u PO — wypchnąć `barometr/` na GitHub (5 plików JSON live).

## Następne kroki (priorytet malejąco)
1. PO: push `barometr/` → workflow_dispatch → weryfikacja 5× HTTP 200 na raw.githubusercontent.com.
2. PO: build v0.6.0 w Android Studio, test zmiany lens → inny score.
3. Tłumaczenia PL/EN UI; `@Preview`; testy jednostkowe core.
4. Publikacja Play (WB-009 privacy URL, Data safety).

## Otwarte problemy
- **Build tylko u usera:** brak Android SDK w kontenerze.
- **`gradle-wrapper.jar` nie commitowany** — AS dogeneruje lub `gradle wrapper`.
- **PAT cron-job.org wygasa** — przy 401 odnowić token (repo `barometr`).
- **JSON multi-lens na GitHub:** lokalnie wygenerowane — wymaga push + workflow przed testem apki u PO.
