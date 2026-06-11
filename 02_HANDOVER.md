# HANDOVER — World Barometer (Android) — ŻYWY STAN

> 👉 **ZACZNIJ TUTAJ.** To jest bieżący stan projektu (mały, aktualny). Nie gromadzi historii.
> Limit ~120 linii — gdy rośnie, przenieś rozwiązane/stare do `CHANGELOG.md`.

Cel: natywna apka Android, która TYLKO pobiera publiczny JSON i prezentuje „barometr" świata.
Backend gotowy i nieruszalny bez prośby. Cała aplikacja po angielsku; komentarze w kodzie mogą być PL.

## Read order (start sesji)
1. **Ten plik** (cały) — stan + następne kroki + otwarte problemy.
2. **`PROJECT.md`** — stabilna referencja (architektura, stos, decyzje, logika, kontrakt, wersjonowanie).
3. **`CHANGELOG.md`** — TYLKO gdy potrzebujesz historii/„dlaczego".
4. **`barometr/01_START_TUTAJ.md`** (+ `SPEC_MVP.md`, `makiety/paleta.json`) — TYLKO gdy zadanie dotyka backendu/danych.

Protokół utrzymania tych dokumentów to reguła workspace `.cursor/rules/barometr-handover.mdc`
(MANUALNA) — **przywoływana wzmianką `@barometr-handover`** na starcie rozmowy. Bez przywołania
sesja jest domyślnie czysta (zero śladu, protokół się nie ładuje).

## Bieżąca wersja
- App: **v0.3.0** (versionCode 4, versionName "0.3.0"), tag `v0.3.0` = bieżący `main`.
- Backend: silnik liczy ~co godzinę; uruchamiany zewnętrznym triggerem (cron-job.org), działa.

## Stan na teraz
- MVP (5 kroków) + hardening + privacy + disclaimer + atrybucja źródeł — **gotowe**.
- Odświeżanie dopasowane do backendu: WorkManager 60 min (`UPDATE`), próg stale 90 min.
- Backend stabilny: cron `17 * * * *` (+ zewnętrzny trigger jako realny napęd), push utwardzony.
- Aplikacja **nieskompilowana w kontenerze** (brak Android SDK) — build robi user w Android Studio.

## Następne kroki (priorytet malejąco; do uzgodnienia)
1. Tłumaczenia PL/EN przez `res/values/strings.xml` (teraz teksty EN wpisane w kodzie — do ekstrakcji).
2. `@Preview` dla ekranów (podgląd UI bez emulatora).
3. Testy jednostkowe: `Level.resolve`, `ContentSafety.sanitized`, logika powiadomień, `RelativeTime`.
4. Ikona launchera (obecnie prosty wektor) + grafiki do Google Play.
5. Publikacja: Play Console, podpis (Play App Signing), polityka prywatności jako URL, Data safety, ocena treści.
6. Ew. „tap widgetu = odświeżenie" jako osobna akcja (kod był w `RefreshWidgetAction`, usunięty w v0.2.0).

## Otwarte problemy (tylko NIEROZWIĄZANE)
- **Build tylko u usera:** kontener nie ma Android SDK ani dostępu do zależności — możliwe drobne korekty wersji bibliotek przy pierwszym sync.
- **`gradle-wrapper.jar` nie jest commitowany** (binarny) — Android Studio dogeneruje przy otwarciu, lub `gradle wrapper`.
- **PAT do triggera backendu wygasa** (cron-job.org) — przy 401 w logach odnowić token (scope Actions: read/write, tylko repo `barometr`).
