# Security policy

## Supported versions

RenewalRadar is pre-1.0. Security fixes are applied to the latest
release only; older releases do not receive backports.

| Version | Supported |
| ------- | --------- |
| 0.1.x   | Yes       |
| < 0.1   | No        |

## Reporting a vulnerability

Please report vulnerabilities **privately** — do not open a public
issue for a security problem.

Use GitHub's private vulnerability reporting for this repository:
**Security → Advisories → Report a vulnerability** on
[github.com/DangTruong96/renewal-radar](https://github.com/DangTruong96/renewal-radar/security/advisories).

Please include:

- the RenewalRadar version and your Android version / device model,
- a description of the issue and its impact,
- steps to reproduce, if you can share them.

You can expect an acknowledgement within 7 days. Confirmed issues are
fixed in a patch release, and the reporter is credited in the release
notes unless they prefer to stay anonymous.

## Scope notes

RenewalRadar is designed so that most classic app vulnerabilities have
very little to attach to:

- **No network surface.** The app declares no `INTERNET` permission,
  has no server, no accounts, and no deep links beyond its launcher
  activity. There is nothing to phish and nothing to breach remotely.
- **Data at rest** is the user's own subscription list in a Room
  database inside the app's private storage, protected by Android's
  per-app sandboxing. The app stores **no payment credentials** — only
  names, amounts, dates, and an optional cancellation-page URL.
- **JSON import** parses untrusted text with `org.json`. A malformed
  or hostile import file should fail cleanly (the importer validates
  and returns an error rather than partially applying); a file that
  crashes the importer or corrupts the library *is* in scope for a
  report.
- **Notifications** contain the subscription name and price the user
  typed in. On a locked phone, Android may show them on the lock
  screen per the device's own notification settings — that is a
  device-level setting, not an app leak.
