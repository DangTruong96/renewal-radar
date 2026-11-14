# Changelog

All notable changes to this project are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/).

## [0.1.5] - 2026-11-14

### Fixed

- Trials now get their chosen lead-time reminder. The daily worker's
  trial branch only ever fired the 3/1/0 countdown, so a trial with a
  7- or 14-day lead got no reminder on its lead day at all — even
  though the editor shows the 1/3/7/14 chips for trials and the README
  calls the countdown "extra" reminders. A pending trial now notifies
  on its lead day as well as at 3, 1, and 0 days before it converts
  (a lead of 3 or 1 still produces exactly one notification that day).
- Weekly plans with a 7- or 14-day lead never got any reminder. The
  worker fired only when days-until-renewal exactly equalled the lead,
  but a weekly plan's days-until is always 0..6, so 7 could never
  match — a weekly subscription with a 14-day lead was silently
  unreminded forever. A renewal now fires on the closest day the cycle
  can actually reach (`min(lead, cycleLength - 1)`): for weekly + 7/14
  days that is 6 days out, once per cycle. Leads the cycle can reach
  still fire on exactly the lead day.
- JSON import accepted entries that could never work. Any
  structurally-parseable entry replaced the whole library, so a
  garbage anchor date made a subscription invisible to reminders and
  "Coming up" while its amount still counted in the dashboard totals,
  a typo'd cycle (`YEARLYY`) silently fell back to MONTHLY (12x the
  monthly total for a yearly plan), and a negative amount put negative
  spend on the dashboard. Import now validates each entry (non-blank
  name, non-negative amount, real ISO 4217 currency, known cycle,
  parseable anchor/trial-end dates, non-negative lead) and rejects the
  whole document — leaving the existing library untouched — on any bad
  entry.

### Added

- `Round4ReminderTest` (25 tests): day-by-day simulations of the
  reminder decision for trials and all four cycles, pinning the fixed
  behaviour and that every previously-working lead/cycle combination
  still fires on exactly its lead day; plus import-validation cases.
  The reminder decision itself is now a pure function,
  `RenewalCalculator.reminderDue`, which the worker calls — the tests
  exercise the exact function that ships. The JVM suite is now
  83 tests.

## [0.1.4] - 2026-11-02

### Fixed

- Grouped decimal-comma amounts were stored ~1000x too small.
  `Money.parseToMinor` treated every dot as a decimal separator, so
  "1.299,50" (the natural grouped format in dot-grouping locales)
  was saved as €1.30 instead of €1,299.50. When both separators are
  present, the last one is now the decimal separator ("1,299.50" US
  grouping keeps working exactly as before).
- Amounts grouped with a non-breaking space were rejected outright.
  French-locale formatting groups thousands with U+00A0 (or narrow
  U+202F), so a pasted or retyped French-formatted price failed with
  a generic "valid price" error; both are now stripped as grouping
  separators like a plain space.
- Scientific-notation input could freeze the app. "1e100000000"
  (12 characters, reachable by pasting into the price field) stalled
  the caller for 30+ seconds while BigDecimal scaling materialised
  a hundred-million-digit number — and the edit form parses on the
  main thread, so that was an ANR. Price fields now accept plain
  decimal notation only; "1e3" and huge exponents return null.

### Added

- `Round3MoneyTest` (4 tests): dot-grouped decimal-comma parsing
  (including multi-group "1.299.500,50"), US grouped input
  unchanged, NBSP/narrow-NBSP grouping, and scientific-notation
  rejection. The JVM suite is now 58 tests.

## [0.1.3] - 2026-10-23

### Fixed

- Amounts typed with a decimal comma were stored 100x too large.
  The edit form feeds raw user text to `Money.parseToMinor`, which
  stripped every comma as if it were a grouping separator — so
  "12,99" (the natural format in decimal-comma locales, and what
  decimal keyboards there produce) was saved as 1299.00 instead of
  12.99. A single comma followed by 1-2 digits with no dot present
  is now read as a decimal separator; grouping commas keep working
  ("1,299" USD, "1,500" JPY), as does "1,299.50".

### Added

- `Round2MoneyTest` (5 tests): a locale x currency matrix proving
  `Money.format` carries the exact value in US/UK/German/French/
  Japanese locales, zero-decimal currencies never gain a fraction,
  decimal-comma parsing, and overflow rejection.
- `Round2DomainTest` (11 tests): `upcoming` window boundaries (an
  item exactly at today + withinDays is in; one day past is out),
  sort stability for equal dates, subscriptions with unparseable
  stored dates mixed among valid ones, `BillingCycle.fromName`
  fallback pinning, and `monthlyEquivalentMinor` rounding across all
  cycles. The JVM suite is now 54 tests.

### Documentation

- The renewal reminder's exact-day behaviour (a reminder is skipped
  if the device is off on the one matching day) is now documented
  under "Known limitations" in the README, with the reason the fix
  needs a persisted already-reminded marker.

## [0.1.2] - 2026-10-23

### Fixed

- A free trial whose end date had passed was treated as a trial
  forever: `RenewalCalculator.effectiveDate` kept returning the past
  trial-end date, so the converted plan vanished from the upcoming
  list, its row kept showing the stale "Trial ends …" date with a
  negative day count, it never received renewal reminders, and its
  cost never appeared in the dashboard totals. A trial that has ended
  is now treated as what it is — a paid plan whose first charge landed
  on the trial-end date: the effective date is the next renewal stepped
  from that date, reminders and totals treat it as a normal
  subscription, and the list shows its renewal date. New
  `RenewalCalculator.isTrialPending` is the single source of truth for
  "still trialling", used by the calculator, the reminder worker, the
  home list and the totals.

### Added

- `TrialConversionTest` (7 tests): expired monthly/weekly trials roll
  to the next paid renewal, a trial ending today still reports its end
  date, converted trials appear in `upcoming`, and `isTrialPending`
  boundary behaviour. The JVM suite is now 38 tests.

## [0.1.1] - 2026-10-23

### Fixed

- `Money.format` could render zero-decimal currencies with spurious
  decimals — e.g. a ¥1,500 plan shown as "¥1,500.00" in a US locale.
  `NumberFormat.setCurrency()` does not reset the fraction digits the
  formatter inherited from the locale's own currency, so the digits are
  now pinned explicitly to the amount currency's real fraction digits.

### Added

- Unit tests for the `Money` domain object (16 tests), which previously
  had no coverage — they caught the formatting bug above. Coverage:
  currency fraction digits (including zero-decimal JPY/KRW/VND and the
  unknown-code fallback), parsing to minor units (grouping commas,
  whitespace, HALF_UP rounding, rejection of negative and malformed
  input), plain-string rendering, parse/format round-trip, and
  locale-pinned display formatting. The JVM suite is now 31 tests.

## [0.1.0] - 2026-10-22

First public release. An offline subscription and free-trial renewal
reminder for Android 8.0+.

### Added

- Subscription library (Room): name, price in minor units, currency,
  billing cycle (weekly / monthly / quarterly / yearly), anchor date,
  per-item reminder lead (1 / 3 / 7 / 14 days), category, notes, and an
  optional cancellation-page URL.
- Free-trial tracking: trials count down to their conversion date
  instead of a renewal date and are badged in the list.
- Renewal math in a pure-Kotlin domain layer (`java.time` only):
  next-renewal stepping from an anchor date (forward or back),
  days-until, monthly equivalents per cycle, and effective-date
  resolution (trial end wins over renewal).
- Dashboard with monthly and yearly totals grouped per currency —
  currencies are never summed together. Zero-decimal currencies (JPY,
  KRW, VND) handled via `java.util.Currency` fraction digits.
- Daily reminder via WorkManager (survives reboots, no boot receiver):
  renewals notify at the chosen lead time; trials notify at 3 days,
  1 day, and on conversion day. `POST_NOTIFICATIONS` requested at
  runtime on Android 13+.
- JSON export/import of the full library (in the view model), so data
  survives a phone switch without any cloud.
- Privacy by construction: **no `INTERNET` permission**, no account,
  no ads, no analytics; all data in an on-device Room database.
- 15 deterministic JVM unit tests for the renewal calculator.
- GitHub Actions CI building the debug APK and running the unit tests
  (JDK 17, Gradle 8.7).
- Community and governance files: CODE_OF_CONDUCT.md (Contributor
  Covenant 2.1, short form), SECURITY.md, issue templates (bug report,
  feature request), a pull-request template, and a "Maintainer &
  governance" section in the README.
