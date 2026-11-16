# Penny — agent guide

Penny is a personal-finance app. This is a monorepo of self-contained components:

- `android/` — native Kotlin app (Jetpack Compose). The product going forward.
  **Open this directory in Android Studio, not the repo root.**
- `backend/` — Python FastAPI + Firebase (Firestore / Cloud Storage).
- `mobile/` — the original Flutter app. **Read-only reference** during the
  native migration; deleted once parity is reached.
- `scripts/`, `.githooks/`, `.github/workflows/` — the verification loop.

## Migration in progress (temporary section — remove when `mobile/` is deleted)

All app work follows `migration/PLAYBOOK.md` (the loop) and tracks parity in
`migration/LEDGER.md`. Session-handoff notes live in gitignored `plan_*` files
at the repo root — read `plan_migration.md` first if it exists.

## Setup (once per clone)

- JDK 17: `brew install openjdk@17`. Gradle 8.13 cannot run on newer JDKs
  (the Android Studio JBR is too new). `scripts/env.sh` picks it up automatically.
- `android/local.properties` with `sdk.dir=/path/to/android-sdk` (gitignored).
- Git hooks install automatically on any Gradle run (`core.hooksPath=.githooks`).
- Emulator: AVD `penny` (API 36). `scripts/e2e.sh` boots it headless if needed.

## Commands

| Purpose | Command |
|---|---|
| Unit tests (fast, no device) | `cd android && ./gradlew test` |
| Build + install + launch on emulator | `scripts/run.sh` |
| E2E on emulator (the suite CI runs) | `scripts/e2e.sh` |
| One e2e class | `scripts/e2e.sh -Pandroid.testInstrumentationRunnerArguments.class=<fqcn>` |
| CI status after pushing | `gh run list` / `gh run watch` |

## Code quality — non-negotiable

- Decision logic lives in `com.penny.app.core` as **pure Kotlin, no Android
  imports**, so it runs in millisecond JVM tests. UI is a thin Compose layer.
- **Tests come with every change.** Logic → JVM tests with edge cases.
  Screens/flows → instrumented e2e in `app/src/androidTest/`. Bug fix → a test
  that fails without the fix.
- Parity ports encode the Flutter behaviour **exactly, quirks included**.
  Fixing an old bug is an explicit ledger decision, never a silent side effect.
- Never make a test pass by weakening it: no deleting tests, loosening
  assertions, `@Ignore`, or longer sleeps. If a test seems wrong, say why and ask.
- Never get around the checks: no `git commit --no-verify`, no editing hooks
  or workflows to turn things green.
- Put a `testTag` on every assertable Compose node.
- No build artifacts in git: `build/`, `*.apk`, `local.properties` are ignored.
  Keep it that way.

## PRs

- Small vertical slices: one coherent behaviour + its tests, reviewable in
  minutes. Open a **draft PR early** so CI runs while you work.
- CI: `android-unit`, `android-e2e` (cold API-36 emulator), `backend-check`,
  aggregated by `ci-ok` — the only required check. Merge only when green and
  the maintainer approves. Red CI on `main` comes before anything else.
- PR body: behaviour changed (with source references), a Tests paragraph, a
  screenshot for user-visible changes, quirks preserved / bugs deferred, and a
  "Needs your eye" section for ask-first areas.

## Ask the maintainer first

- Backend API contract or Firestore data shapes (users' data lives there).
- Auth flows, Firebase config, secrets.
- `applicationId`, signing, manifest permissions.
- CI workflows, hooks, scripts (the loop itself).
- Deleting or rewriting anything under `mobile/` before parity.
