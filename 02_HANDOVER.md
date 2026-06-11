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
- App: **v0.6.0** (versionCode 9), tag `v0.6.0`, commit lokalny `e98836e`.
- Backend: multi-lens live na GitHub (`barometer_{pl,ro,pt,ua,us}.json` + `manifest.json`).

## Stan na teraz
- **WB-008 Country lens — zweryfikowane u PO:** picker Settings, zmiana lens → inny score/treść, widget OK.
- MVP + Legal (WB-004) + widget trend (WB-002) + branding (WB-007) + country lens (WB-008) — **gotowe**.
- Silnik: push na `github.com/pb2112-netizen/barometr`, pliki JSON HTTP 200.
- Build w Android Studio u usera — kontener bez Android SDK.
- Repo apki: **lokalne git** (`WorldBarometer/.git`), bez remote (PROJECT.md §7).

## Następne kroki (priorytet malejąco)
1. **Publikacja Play Store** — listing EN, Data safety, assety w `design/play-store/` (WB-009+).
2. **Privacy URL** publiczny przed Store (WB-010).
3. Tłumaczenia PL/EN UI; `@Preview`; testy jednostkowe core.
4. Opcjonalnie: remote git dla `WorldBarometer/` (backup na GitHub).

## Otwarte problemy
- **Build tylko u usera:** brak Android SDK w kontenerze.
- **`gradle-wrapper.jar` nie commitowany** — AS dogeneruje lub `gradle wrapper`.
- **PAT cron-job.org wygasa** — przy 401 odnowić token (repo `barometr`).
