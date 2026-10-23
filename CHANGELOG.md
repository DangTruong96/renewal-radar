# Changelog

All notable changes to this project are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/).

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
