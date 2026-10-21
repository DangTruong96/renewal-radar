## What this changes

<!-- One or two sentences. Link the issue it closes, if any. -->

## The renewal it protects (behavior changes only)

<!-- Date/reminder behavior changes exist because a real renewal
     pattern was handled wrong. Describe it: cycle, anchor date,
     and what the app did before this change. -->

## Checklist

- [ ] No `INTERNET` permission, account system, analytics, or
      phoning-home SDK added — the app stays fully offline
- [ ] Domain-layer changes (`RenewalCalculator`, `BillingCycle`,
      `Money`) are covered by deterministic unit tests with fixed
      dates, and `gradle testDebugUnitTest` passes
- [ ] Dependency versions changed only in `gradle/libs.versions.toml`
- [ ] Database schema changes include a migration or an explicit
      MAJOR-version note
- [ ] README updated if user-visible behavior changed
- [ ] CHANGELOG entry added under the current version
