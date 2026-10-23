# RenewalRadar

**Never pay for a subscription you meant to cancel.**

An offline subscription and free-trial renewal reminder for Android.
You enter the subscriptions and trials you want to keep an eye on;
RenewalRadar watches the calendar for you and reminds you *before* the
charge lands — while there is still time to cancel.

No account. No ads. No analytics. The app does not even declare the
`INTERNET` permission, so it *cannot* send your data anywhere: everything
lives in a local database on your phone.

## Why this exists

Subscription creep is not a budgeting failure; it is a dark pattern that
works exactly as designed. The evidence that people pay for services they
meant to cancel is not anecdotal:

- Consumer surveys by BestMoney and Self Financial have repeatedly found
  that **roughly 69–70% of subscribers have been charged for a
  subscription or free-trial conversion they had forgotten about**.
- The UK government has estimated that consumers spend **about
  £1.6 billion a year on unwanted subscriptions** — subscriptions they
  no longer use, or never meant to keep past a trial.
- Regulators have started treating this as a market failure rather than
  a personal one: the UK's Digital Markets, Competition and Consumers
  Act introduces mandatory renewal reminders for subscription
  contracts, and the US FTC's "click-to-cancel" rulemaking targeted the
  same asymmetry — signing up is one tap, remembering to cancel is on
  you.

The commercial answer to this problem is a subscription-tracking app
that asks for an account and, often, read access to your bank feed. That
trades one problem for another. RenewalRadar is the simpler trade: you
type in the handful of subscriptions you actually care about, and the
phone you already own does the remembering — locally, with nothing to
leak.

## Features

- **Renewal countdowns.** Every subscription shows its next renewal
  date and a days-left chip that turns red at 3 days or fewer.
- **Free-trial mode.** Trials track their conversion date instead of a
  renewal date, are badged in the list, and get extra reminders at
  3 days, 1 day, and the day the trial converts to paid.
- **You choose the lead time.** Per-subscription reminder at 1, 3, 7, or
  14 days before the charge.
- **A daily offline check.** A WorkManager job runs once a day and
  posts notifications for anything due — it survives reboots on its
  own, with no boot receiver and no server.
- **The real monthly cost.** The dashboard converts every billing cycle
  (weekly, monthly, quarterly, yearly) to a monthly equivalent and a
  yearly total, grouped per currency. Currencies are never mixed: with
  no network there is no honest exchange rate, so USD and JPY totals
  are shown separately instead of being silently added together.
- **Zero-decimal currencies done right.** JPY, KRW, and VND have no
  minor unit; amounts are stored and entered as whole units, driven by
  `java.util.Currency` — not a hard-coded "divide by 100".
- **Cancellation link on file.** Store each service's cancellation page
  URL with the subscription, so it is one tap away when the reminder
  fires.
- **Backup that respects the design.** The whole library serializes to
  a documented JSON format (export/import in the view model), so your
  data is never locked in and a backup only ever moves when *you*
  initiate it — the app never transmits anything by itself.

## Requirements

To build:

- Android Studio Hedgehog (2023.1.1) or newer, **or** a plain JDK 17 +
  Android SDK 34 toolchain
- Gradle 8.7 (the version pinned in `gradle/wrapper/gradle-wrapper.properties`; CI uses this exact version)

To run:

- A phone or emulator on **Android 8.0 (API 26)** or newer

Stack: Kotlin 2.0.20, Jetpack Compose + Material 3, Room 2.6.1,
WorkManager 2.9.0 — all pinned in `gradle/libs.versions.toml`.

## Install

Clone and open in Android Studio:

```bash
git clone https://github.com/DangTruong96/renewal-radar
```

…then press **Run**. Or build from the command line:

```bash
cd renewal-radar
gradle assembleDebug
# APK lands in app/build/outputs/apk/debug/app-debug.apk
```

Install that APK on a device with USB debugging enabled:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

Releases are tagged (`vX.Y.Z`); CI builds a debug APK artifact on every
push, so you can also grab one from the repository's Actions tab.

## Usage

1. **Add a subscription.** Tap **+**, enter the name and the price
   *as charged* (e.g. `12.99`, or `1500` for a ¥1,500 plan), pick the
   currency and billing cycle, and set the next renewal date — or any
   past charge date: RenewalRadar steps the cycle forward to the real
   next renewal.
2. **…or track a free trial.** Flip **"This is a free trial"** and set
   the date the trial ends. The card switches to counting down to the
   conversion date, and you will be reminded 3 days before, 1 day
   before, and on the day.
3. **Pick your lead time.** Choose how many days before the charge you
   want the reminder: 1, 3, 7, or 14.
4. **Watch the dashboard.** The top card totals your monthly and yearly
   spend per currency; the **Coming up** list shows everything due in
   the next 30 days, soonest first.
5. **Back up occasionally.** Before switching phones, export the
   library to JSON; importing it restores everything on the new device.

## Privacy

This is the section most subscription apps cannot honestly write:

- **No `INTERNET` permission** is declared in `AndroidManifest.xml`.
  Verify it yourself — it is a one-line check, and it is a hard
  guarantee, not a policy promise: without the permission, Android will
  not let the app open a network connection at all.
- **No account, no ads, no analytics, no third-party SDKs** that phone
  home. The dependency list is AndroidX and Kotlin only.
- **All data stays on the device** in a Room database. Uninstalling the
  app deletes it.
- **Export is user-initiated.** JSON leaves the phone only when you
  explicitly export and share it.

## Known limitations

- **A renewal reminder fires on exactly one day.** The daily worker
  notifies when a renewal is exactly `remindDaysBefore` days away, so
  if the phone is off (or the worker is deferred by the OS) on that
  day, the reminder for that cycle is skipped rather than delivered
  late. Fixing this properly means remembering that a cycle was
  already reminded — a persisted "last reminded" marker per
  subscription — instead of firing repeatedly on every day inside the
  window. That is a deliberate design change (schema + worker), tracked
  here rather than patched over.
- **Trial reminders are the exception:** a pending trial notifies at
  3, 1 and 0 days before it converts, so a missed day still leaves the
  later countdown notifications.

## Roadmap

- **v0.2** — notification deep link straight to the subscription's edit
  screen (and its cancellation URL).
- **v0.2** — a custom reminder time of day, and a quiet "digest" mode
  that batches everything due this week into one notification.
- **v0.3** — yearly-cost report per category, computed locally from the
  same monthly-equivalent math the dashboard uses.
- **v0.3** — optional encrypted export (passphrase-protected JSON), for
  people who store backups in cloud drives.

Nothing on this list is promised on a date; items move when a real
renewal someone nearly missed promotes them.

## Maintainer & governance

RenewalRadar is maintained by **DangTruong96**, currently the sole
maintainer. That means, concretely:

- **Decisions.** The maintainer makes final calls on features, defaults,
  and releases. Two rules are standing and not up for silent revision:
  the app never gains the `INTERNET` permission or any account system,
  and reminder math stays pure `java.time` in the domain layer where it
  is unit-tested.
- **Issues & PRs.** Bug reports and feature requests are triaged by the
  maintainer. Feature requests that need a server, an account, or
  network access will be declined with an explanation — that is the
  product, not an oversight.
- **Releases.** Semantic versioning (`MAJOR.MINOR.PATCH`): breaking
  changes to the database schema (without migration) or the JSON export
  format bump MAJOR; new features bump MINOR; fixes bump PATCH. Every
  release is a git tag (`vX.Y.Z`) with a matching
  [CHANGELOG.md](CHANGELOG.md) entry.
- **Conduct & security.** Participation is covered by the
  [Code of Conduct](CODE_OF_CONDUCT.md); vulnerabilities are reported
  privately as described in [SECURITY.md](SECURITY.md).
- **Self-audit.** [EVALUATION.md](EVALUATION.md) records an honest
  checklist of this repository against common open-source project
  criteria, including the items that are not done yet.

If the project ever gains regular co-maintainers, this section will be
updated to name them and to describe how decisions move from one
person's call to a shared process.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Short version: keep the app
offline, keep the renewal math pure and tested, and every notification
behavior change needs a date-arithmetic test behind it.

## License

MIT — see [LICENSE](LICENSE).
