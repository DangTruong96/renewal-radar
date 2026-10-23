# Changelog

All notable changes to this project are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/).

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
