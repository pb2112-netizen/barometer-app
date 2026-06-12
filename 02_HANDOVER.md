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
- **App:** v0.6.2 (versionCode 11), branch `master` — **wypushowane do `origin/master`** (GitHub = lokalny). Tag `v0.6.2` **po build u PO**.
- **Silnik:** WB-012 **wypushowany** do `origin/main` (kod scalony z auto-commitami JSON, merge commit).
- **Remote apki:** `origin` → `https://github.com/pb2112-netizen/barometer-app.git` (public).
- **Backend live:** multi-lens — `barometer_{pl,ro,pt,ua,us}.json` + `manifest.json`. **JSON dostanie WB-012 przy najbliższym cyklu silnika** (cron co godzinę, `~:01`).

## Stan na teraz
- **Done (sesja 2026-06-12, v0.6.2):** widget — (a) „Updated"/UI = **czas absolutny** (`RelativeTime.formatAbsolute`, Settings pełna data z rokiem); (b) **fix pustego widgetu dla krajów ≠ PL** — render PO pobraniu (koniec gubienia update'u przez debounce Glance) — **✅ zweryfikowane u PO: widget znów pokazuje poprawne dane**; (c) odświeżanie kraju = hybryda foreground + expedited WorkManager backstop (`SettingsViewModel.setLensId`, `requestLensChangeRefresh`, `RefreshWorker` tryb `KEY_LENS_CHANGE`) — **latencja BEZ zmian, patrz „Problem do następnej sesji"**.
- **Done (sesja 2026-06-12):** WB-012 — `_ensure_event_summaries()`, prompt AI, tryb prosty; apka bez zmian kodu (`EventCard` już renderuje `summary`).
- **Done wcześniej:** MVP, Legal (WB-004), widget trend (WB-002), branding (WB-007), country lens (WB-008), lens visibility (WB-011).
- **WB-011 u PO (częściowo):** chip „Scoring for:” + klikalny kraj OK; widget — pin + nazwa kraju OK.
- **Build:** tylko u PO w Android Studio — kontener bez Android SDK.

## Następne kroki (priorytet ↓)
1. **Cykl silnika** (po pushu WB-012, ~:01) → sprawdź niepuste `top_events[].summary` we wszystkich 5 plikach JSON. Apka v0.6.2 i silnik WB-012 już na GitHubie.
2. **Weryfikacja u PO — WB-012:** pull-to-refresh w apce → rozwinięta karta Top event: akapit opisu **nad** „Sources".
3. **Build u PO → tag `v0.6.2`:** Android Studio → Run → `git push origin --tags`.
4. **Play Store (WB-009+)** + **Privacy URL (WB-010)** przed publikacją.

## Otwarte problemy
- **Latencja odświeżania widgetu po zmianie kraju** — patrz dedykowana sekcja niżej (temat na następną sesję).
- **WB-012 nie zweryfikowane u PO** — czeka push silnika + cykl + refresh apki.
- **Build tylko u usera** — brak Android SDK w kontenerze.
- **`gradle-wrapper.jar` nie w repo** — Android Studio dogeneruje przy sync.
- **PAT cron-job.org wygasa** (backend) — przy 401 odnowić token w repo `barometr`.

## Problem do następnej sesji: LATENCJA odświeżania widgetu (kraj/„Updated")
> Treść (jaki kraj/score) jest już POPRAWNA. Otwarty jest **czas reakcji** widgetu na zmianę kraju.
> NIE zaczynaj od nowa diagnozy — niżej jest cały kontekst.

- **Objaw (S24, One UI, ustawienia domyślne):** po zmianie kraju widget aktualizuje się dopiero gdy
  user **zostaje w apce ~kilkanaście s**. Szybkie wyjście z apki = render się nie dokańcza (proces
  zamrażany), a kolejne przełączenia bywają „kolejkowane".
- **Potwierdzone:** zostanie w apce ~15 s naprawia objaw → to **kwestia czasu na dokończenie renderu**,
  nie błąd treści. Sieć jest szybka (GitHub raw, 304), więc to nie network.
- **Co już zrobiono (v0.6.2, NIE pomogło na latencję):**
  - hybryda w `SettingsViewModel.setLensId`: szybka ścieżka foreground (`refresh()` → 1× `requestUpdate`)
    + **expedited** WorkManager backstop (`RefreshScheduler.requestLensChangeRefresh`, REPLACE,
    `RUN_AS_NON_EXPEDITED_WORK_REQUEST`);
  - 1 render PO pobraniu (usunął gubienie update'u przez debounce Glance — to naprawiło PUSTY widget).
- **Najsilniejsza hipoteza:** One UI/Samsung zamraża/ubija proces apki po wyjściu i **dławi/odracza
  start expedited workera** (limit quota expedited + agresywny „sleeping apps"). `update()` Glance
  (RemoteViews) i tak musi dojść w żywym procesie. To głównie strona telefonu, której kodem do końca
  nie pokonamy.
- **Do sprawdzenia/rozważenia w następnej sesji:**
  1. **Diagnostyka atrybucji:** S24 → Bateria apki = „Bez ograniczeń", wyłącz „sleeping apps”; sprawdź
     czy latencja znika (potwierdzi udział telefonu).
  2. **Logcat** przy zmianie kraju: czy/kiedy startuje `RefreshWorker` (expedited vs odroczony), czy
     proces jest freezowany.
  3. Czy quota expedited się wyczerpuje przy serii przełączeń (stąd „kolejkowanie”).
  4. Ewentualnie: zaakceptować latencję (rzadka akcja) + drobny feedback w UI, zamiast walczyć z One UI.
- **Pliki:** `widget/BarometerWidget*.kt`, `work/RefreshWorker.kt`, `work/RefreshScheduler.kt`,
  `ui/settings/SettingsViewModel.kt`, `xml/barometer_widget_info.xml` (`updatePeriodMillis=0`).

## Szybki git
Apka (`master`) i silnik (`main`) są zsynchronizowane z GitHub. Uwagi na przyszłość:
```bash
# Silnik publikuje JSON co godzinę (GitHub Actions) → przed pushem ZAWSZE:
cd /workspaces/Agenci_SEO/WB/barometr && git fetch origin && git merge origin/main && git push origin main

# Apka:
cd /workspaces/Agenci_SEO/WB/WorldBarometer && git push origin master
# po build u PO: git tag v0.6.2 && git push origin --tags
```
Commit z inline identity → `PROJECT.md` §7. **Nie** commituj z root `Agenci_SEO/`.
