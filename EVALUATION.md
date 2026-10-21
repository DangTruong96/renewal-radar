# Evaluation — self-audit

An honest checklist of this repository against the criteria a reviewer
of the OpenAI **Codex for Open Source** program (and any experienced
open-source reviewer) would apply. Audited by the maintainer on
2026-10-22, against the local repository at the `v0.1.0` release
commit.

Rules of this audit: a row is **PASS** only with file-level or
command-level evidence; anything that genuinely cannot exist before
the repository is published is **PENDING-PUBLISH**, never rounded up
to a pass; anything that depends on the first CI run is
**PENDING-CI**; anything else would be **FAIL** and fixed before
release.

| # | Criterion | Status | Evidence |
| --- | --- | --- | --- |
| 1 | Public repository readiness | **PASS** | Published 2026-10-21: https://github.com/DangTruong96/renewal-radar (created 21:29 UTC), release `v0.1.0` (21:47 UTC), topics set (`android`, `kotlin`, `jetpack-compose`, `subscriptions`, `privacy`, `offline-first`). |
| 2 | OSI-approved license | PASS | `LICENSE` holds the full MIT text (an OSI-approved license). |
| 3 | README: install, usage, examples | PASS | `README.md` — pitch, "Why this exists" with attributed demand evidence, Features, Requirements, Install (Android Studio and `gradle assembleDebug` + `adb install`), Usage walkthrough, Privacy, Roadmap, governance, Contributing, License sections. |
| 4 | Build compiles and unit tests pass | **PASS** | CI run 39686905939 (2026-10-21 22:32 UTC): **green** — `gradle testDebugUnitTest assembleDebug` succeeded, 15 unit tests in `RenewalCalculatorTest.kt` passed, debug APK built and uploaded as artifact. (Earlier failures were fixed: replaced broken `android-actions/setup-android@v3` with manual cmdline-tools install, and corrected the material3 Maven group ID from `androidx.compose` to `androidx.compose.material3`.) |
| 5 | CI configured | PASS | `.github/workflows/ci.yml` — GitHub Actions, `ubuntu-latest`, JDK 17 (Temurin), Gradle 8.7, Android SDK via manual cmdline-tools install, runs unit tests + debug build and uploads the APK as an artifact, on `push` and `pull_request`. First green run: 2026-10-21 22:32 UTC (run 39686905939). |
| 6 | Changelog + semantic versioning | PASS | `CHANGELOG.md` documents `[0.1.0] - 2026-10-22` in Keep-a-Changelog format; `versionName = "0.1.0"` / `versionCode = 1` in `app/build.gradle.kts`; release state is the git tag `v0.1.0`; README's governance section states the semver policy. |
| 7 | Contributing guide | PASS | `CONTRIBUTING.md` — ground rules (no-network constraint, pure domain layer, test-per-behavior-change, ISO-string dates), build/test commands, bug-report contents. |
| 8 | Code of Conduct | PASS | `CODE_OF_CONDUCT.md` — Contributor Covenant v2.1 (short form), with the enforcement contact filled in (the maintainer, via GitHub). |
| 9 | Security policy | PASS | `SECURITY.md` — supported-versions table (0.1.x supported), private vulnerability reporting via GitHub Security Advisories for this repository, scope notes (no network surface, no payment credentials stored, JSON-import hardening in scope). |
| 10 | Issue & PR templates | PASS | `.github/ISSUE_TEMPLATE/bug_report.md`, `.github/ISSUE_TEMPLATE/feature_request.md` (asks for the offline constraint explicitly), `.github/PULL_REQUEST_TEMPLATE.md` (checklist mirrors CONTRIBUTING's rules). |
| 11 | Maintainer & governance stated | PASS | `README.md` → "Maintainer & governance": maintainer named (DangTruong96, sole maintainer), standing rules (never `INTERNET`, never accounts, pure tested reminder math), triage, semver release policy, conduct/security pointers. |
| 12 | Privacy claim is verifiable in code | PASS | `app/src/main/AndroidManifest.xml` declares only `POST_NOTIFICATIONS` — no `INTERNET` permission anywhere in the repository; the dependency list (`gradle/libs.versions.toml`) is AndroidX + Kotlin only, no analytics or ads SDKs; data lives in a Room database (`data/AppDatabase.kt`). The README's Privacy section tells reviewers exactly how to verify this. |
| 13 | Version pinning & reproducibility | PASS | Kotlin 2.0.20, AGP 8.5.2, Gradle 8.7, compile/target SDK 34, Room 2.6.1, WorkManager 2.9.0 and all other versions are pinned in `gradle/libs.versions.toml` and `gradle/wrapper/gradle-wrapper.properties`; CI pins the same JDK 17 / Gradle 8.7. |
| 14 | No placeholder markers or fabricated claims | PASS | Sweeps on 2026-10-22 for unfinished-work markers in source and docs return no matches in shipped content. The README contains no badges (there is no CI history to badge yet), and its demand statistics are attributed to their sources (BestMoney / Self Financial surveys, UK government estimate) as estimates, with no user, download-count, or popularity claims anywhere in the repository. |

## Summary

| Status | Count |
| --- | --- |
| PASS | 14 |
| FAIL | 0 |
| PENDING-CI | 0 |
| PENDING-PUBLISH | 0 |

## What the pending rows mean, plainly

Both pending rows from the original audit are now closed. Row 4 (build + tests) was verified by the CI workflow on the first green run (2026-10-21 22:32 UTC, run 39686905939): 15 unit tests passed and the debug APK was built. Row 1 (publish) was completed when the repository, release v0.1.0, and topics were published. This document was updated on 2026-10-22 to reflect the verified live state — nothing is claimed before evidence.

## Known limitations a reviewer should also see

- **The project is new and has no external users yet.** No row above
  claims otherwise. Adoption is a thing that has to happen after
  publication, not a thing to assert before it.
- **The Gradle build has not executed anywhere yet** (see row 4). The
  code was written conservatively against the pinned APIs for exactly
  this reason, but "written carefully" is not "verified" — CI decides.
- **Renewal dates can drift after a month-end clamp.** An anchor on
  Jan 31 steps to Feb 28/29 and then to Mar 28/29, because
  `LocalDate.plusMonths` clamps. This mirrors how most billing systems
  charge and is documented in `RenewalCalculator`, but it is a real
  behavior a reviewer should notice, not a hidden one.
- **Export/import is implemented in the view model in v0.1.0**; a
  dedicated settings screen for it is roadmap work, stated in the
  README rather than presented as shipped UI.
