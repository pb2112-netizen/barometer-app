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
- **App:** v0.6.3 (versionCode 12), branch `master`. Tag `v0.6.3` **po build u PO**.
- **Silnik:** WB-012 **wypushowany** do `origin/main` (kod scalony z auto-commitami JSON, merge commit).
- **Remote apki:** `origin` → `https://github.com/pb2112-netizen/barometer-app.git` (public).
- **Backend live:** multi-lens — `barometer_{pl,ro,pt,ua,us}.json` + `manifest.json`. **JSON dostanie WB-012 przy najbliższym cyklu silnika** (cron co godzinę, `~:01`).

## Stan na teraz
- **Done (sesja 2026-06-12, v0.6.3):** **fix martwego odświeżania widgetu** — prawdziwa przyczyna:
  `provideGlance` czytał dane PRZED `provideContent`; żywa sesja Glance przy `update()` tylko
  rekomponuje (nie re-runuje `provideGlance`) → renderowała zamrożone wartości. Teraz treść
  reaktywna wewnątrz kompozycji (`observe()`+`collectAsState`, `scan` trzyma ostatni niepusty
  snapshot). Hipoteza One UI z v0.6.2 obalona (objaw też na emulatorze Pixel 7).
  Szczegóły → `CHANGELOG.md` [v0.6.3]. **Czeka na weryfikację u PO.**
- **Done (sesja 2026-06-12, v0.6.2):** widget — czas absolutny („Updated"/Settings); fix pustego
  widgetu dla krajów ≠ PL (render PO pobraniu) — ✅ u PO; hybryda foreground + expedited backstop.
- **Done (sesja 2026-06-12):** WB-012 — `_ensure_event_summaries()`, prompt AI, tryb prosty; apka bez zmian kodu (`EventCard` już renderuje `summary`).
- **Done wcześniej:** MVP, Legal (WB-004), widget trend (WB-002), branding (WB-007), country lens (WB-008), lens visibility (WB-011).
- **Build:** tylko u PO w Android Studio — kontener bez Android SDK.

## Następne kroki (priorytet ↓)
1. **Build u PO (v0.6.3) → test widgetu:** zmiana kraju w apce → widget (kraj + „Updated") powinien
   przełączyć się w ~1–2 s przy żywej apce; po wyjściu z apki — najpóźniej po starcie workera.
   Test też na emulatorze Pixel 7. Po OK: `git tag v0.6.3 && git push origin --tags`.
2. **Weryfikacja u PO — WB-012:** pull-to-refresh w apce → rozwinięta karta Top event: akapit opisu **nad** „Sources"; sprawdź niepuste `top_events[].summary` w 5 plikach JSON (silnik wypushowany, cykl ~:01).
3. **Play Store (WB-009+)** + **Privacy URL (WB-010)** przed publikacją.

## Otwarte problemy
- **v0.6.3 (fix widgetu) nie zweryfikowane u PO** — wymaga builda.
- **WB-012 nie zweryfikowane u PO** — czeka cykl silnika + refresh apki.
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
# po build u PO: git tag v0.6.3 && git push origin --tags
```
Commit z inline identity → `PROJECT.md` §7. **Nie** commituj z root `Agenci_SEO/`.
