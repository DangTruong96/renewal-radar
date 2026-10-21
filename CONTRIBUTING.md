# Contributing

Thanks for taking a look. RenewalRadar is a small, deliberately
offline Android app — please help keep it that way.

## Ground rules

- **No network, ever.** A PR that adds the `INTERNET` permission, an
  account system, analytics, or any third-party SDK that phones home
  will be declined, however useful the feature. That constraint *is*
  the product.
- **Reminder math stays pure.** Date and money logic lives in
  `domain/` as pure Kotlin on `java.time` / `java.util.Currency`, with
  no Android imports, so it stays unit-testable on the JVM.
- **Every behavior change needs a test.** If you touch
  `RenewalCalculator`, `BillingCycle`, or `Money`, add or update the
  deterministic unit tests in `app/src/test/` (fixed `LocalDate`s only —
  never `LocalDate.now()` in a test).
- **Dates are ISO strings in the database.** The schema stores
  `yyyy-MM-dd` strings and parses them in the domain layer. Do not add
  Room type converters without a very good reason.

## Development

```bash
git clone https://github.com/DangTruong96/renewal-radar
cd renewal-radar
gradle testDebugUnitTest   # JVM unit tests for the domain layer
gradle assembleDebug       # debug APK in app/build/outputs/apk/debug/
```

- JDK 17, Android SDK 34. Versions are pinned in
  `gradle/libs.versions.toml` — bump them in the catalog, not inline.
- Keep the single-activity, state-based navigation; do not add a
  navigation library for a two-screen app.

## Reporting bugs

Open an issue with: your phone model and Android version, the
subscription you entered (name can be fake — cycle, anchor date, trial
setting, and reminder lead are what matter), what reminder you expected,
and what actually happened. Date bugs are almost always reproducible
from the anchor + cycle + today triple.
