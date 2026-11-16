# Penny migration playbook — Flutter → native Kotlin

**Temporary document.** This folder (`migration/`) is the working memory of the
migration. When parity is reached and `mobile/` is deleted, delete this folder
too. It deliberately is NOT `AGENTS.md`: the migrated repo is a normal app repo
afterwards.

References that shaped this loop:
- `rubberpencils/moly` — the verification loop (pure decision core, tests with
  every change, hooks, emulator scripts, CI gates, draft PRs early).
- Shopify, "Native is now the future of mobile at Shopify" (Sep 2026) —
  greenfield rebuild with the old app as reference; small ordered checkpoints
  gated by tests + visual review + adversarial review + human approval;
  business logic decoupled from UI so it runs headlessly (fast agent feedback).

## Strategy

- **Greenfield, not brownfield.** `android/` is a fresh native app. `mobile/`
  (Flutter) stays untouched as the *specification* until parity, then is
  deleted (tag `flutter-final` first). No Flutter code is converted
  mechanically; behaviour is ported and proven with tests.
- **Parity is differential testing.** For every slice, characterisation tests
  encode the *exact* Dart behaviour — quirks included (e.g. 7 return rates of
  which only 4 are used). Fixing a Flutter bug is a separate, explicit decision
  recorded in the ledger, never a silent side effect of a port.
- **Headless-first.** Decision logic lives in `com.penny.app.core` as pure
  Kotlin with no Android types, tested on the JVM in milliseconds. UI is a thin
  Compose layer. Emulator e2e is for flows, not for logic.
- **Vertical slices.** One coherent behaviour per chunk: pure logic + tests →
  wiring → screen + e2e. Small enough to review in minutes.

## Layout (monorepo, self-contained components)

```
android/    Kotlin app, own Gradle root (open THIS dir in Android Studio)
backend/    FastAPI, unchanged for now
mobile/     Flutter reference (read-only during migration; deleted at the end)
api/        (future) openapi.json + shared JSON fixtures for all clients
ios/        (future) native Swift peer — nothing to build now
scripts/    env.sh, run.sh, e2e.sh
.githooks/  pre-commit (auto-installed by android/settings.gradle.kts)
migration/  this playbook + LEDGER.md
```

## Frozen decisions (change only with the maintainer's OK)

- `applicationId` / namespace: `com.penny.app` (the Flutter app was
  `com.example.mobile`; no real users, so a clean id was chosen at the start).
- Stack: Gradle 8.13, AGP 8.13.0, Kotlin 2.0.21, JDK 17, compileSdk 36,
  minSdk 26, targetSdk 36, Compose (BOM 2024.12.01), JUnit 4.
  Mirrors moly's proven stack; JDK 17 because Gradle 8.13 cannot run on the
  Android Studio JBR (25). CI uses Temurin 17 — keep local identical.
- Firestore data shapes and backend endpoints are parity surfaces: the native
  app must speak the existing API exactly (see backend quirks in LEDGER.md).

## The loop (per chunk)

### 0. Setup — once per clone
```sh
brew install openjdk@17                      # JDK 17 (keg-only path is fine)
echo "sdk.dir=$HOME/Library/Android/sdk" > android/local.properties
git config core.hooksPath .githooks          # also automatic on any Gradle run
sdkmanager "system-images;android-36;google_apis;arm64-v8a"   # if no AVD yet
avdmanager create avd -n penny -k "system-images;android-36;google_apis;arm64-v8a" -d pixel_7
```

### 1. Pick the chunk
Take the next item from `LEDGER.md` (or add one). Pure logic before screens;
screens in dependency order (auth → home → features). A chunk is one coherent
behaviour + its tests, reviewable in minutes — not a whole feature area.

### 2. Characterise (write the tests FIRST)
- Read the exact Dart source. Note every quirk: unguarded inputs, off-by-one
  lists, ignored parameters, float op order, error fallbacks.
- Write JVM tests in `android/app/src/test/` with hand-derived goldens. For
  bigger logic, optionally generate goldens by running the extracted pure Dart
  with a standalone Dart SDK (`brew install dart-sdk`) — never by guessing.
- Bug-fix tests must fail without the fix. Parity tests must encode the Dart
  behaviour even when it looks wrong; record suspected bugs in the ledger.
- Goldens suggested by anyone (including reviewer subagents) are independently
  recomputed before encoding — a reviewer's "discriminating point" for the
  op-order lock turned out to have identical iterative/pow bits; the verified
  one (MF 6% year 2) went in instead. Tolerances looser than the drift being
  guarded (e.g. 1e-6 vs ~1e-12 pow difference) are not characterisation tests.

### 3. Port
- Pure logic → `com.penny.app.core`, no Android imports.
- UI → Compose screens with `testTag`s on every assertable node.
- API calls → match the existing backend contract exactly (including its
  quirks, e.g. `PUT /income` takes `amount` as a query param).

### 4. Verify (the inner loop)
```sh
cd android && ./gradlew test          # milliseconds; do this constantly
scripts/run.sh                        # unit tests + assembleDebug + install + launch
scripts/e2e.sh                        # instrumented suite on the emulator (same as CI)
```
- Every new/changed screen or flow gets an e2e test in `app/src/androidTest/`.
- User-visible change: actually look at it on the emulator. Capture a
  screenshot for the PR (`adb exec-out screencap -p > shot.png`).
- Single e2e class:
  `scripts/e2e.sh -Pandroid.testInstrumentationRunnerArguments.class=com.penny.app.AnalysisE2eTest`

### 5. Review gates (Helix-style, from Shopify)
Before a chunk is committed as done:
1. Tests prove the behaviour (unit + e2e as applicable).
2. Visual review: screenshot matches the Flutter screen's intent.
3. Adversarial review: a fresh reviewer (subagent or human) tries to break the
   slice — wrong inputs, edge cases, parity drift. Findings are fixed or
   recorded, and the lesson goes back into this playbook.
4. Human nod: the maintainer reviews and merges the PR.

### 6. Commit → PR → CI
- The pre-commit hook runs `./gradlew test` (plus a backend syntax check when
  `backend/` is staged). Never bypass it.
- Push the branch and **open a draft PR early** so CI runs while you work.
- CI: `android-unit` (test + assembleDebug), `android-e2e` (cold API-36
  emulator, connectedDebugAndroidTest), `backend-check`, aggregated by `ci-ok`
  (the only required check). Watch with `gh run list` / `gh run watch`.
- PR body: what behaviour was ported (with Dart file:line references), Tests
  paragraph, screenshot, any quirks preserved or bugs deferred, and a
  "Needs your eye" section for ask-first areas.
- Merge only when CI is green and the maintainer approves. Red CI on `main`
  comes before anything else.

### 7. Record
Update `LEDGER.md`: chunk status, parity notes, discovered bugs, and any
playbook improvements. The loop should get faster every chunk.
Also refresh the gitignored `plan_migration.md` at the repo root — it is the
session-handoff dump (state, resume checklist, environment facts, next chunks)
so a fresh session can start the next turn of the loop immediately.

## Done means

1. Pre-commit hook passes.
2. `scripts/e2e.sh` passes for any app/test change.
3. The new behaviour has its tests (parity goldens for logic, e2e for flows).
4. User-visible changes were tried on the emulator, with a screenshot in the PR.
5. CI is green after pushing; the PR is reviewed and merged by the maintainer.
6. The ledger reflects reality.

## Never

- Never make a test pass by weakening it: no deleting tests, loosening
  assertions, `@Ignore`, or longer sleeps to hide a problem. If a test seems
  wrong, say why and ask.
- Never get around the checks: no `--no-verify`, no editing hooks/workflows to
  turn things green.
- Never silently "fix" Flutter behaviour during a port — parity first, fixes
  as explicit ledger entries.
- Never commit build artifacts (`build/`, `*.apk`, `local.properties`).
- Never run two writer sessions in the same worktree: parallel agents produce
  conflicting commits and ledger claims contradicted by observation
  (incident 2026-11-16). Pause the other session or use separate worktrees.
- Verify CI action inputs against the action's schema or error output, not
  reviewer prose: `dependency-graph: false` is invalid on setup-gradle@v5
  (`disabled` is the off value) — it cost one red CI run.

## Ask the maintainer first

- Backend API contract or Firestore data shapes (users' data lives there).
- Auth flows / Firebase config / secrets.
- `applicationId`, signing, manifest permissions.
- CI workflows, hooks, scripts (the loop itself).
- Deleting or rewriting anything under `mobile/` before parity.

## Future

- `api/`: generate `openapi.json` from FastAPI, share JSON fixtures between
  pytest and Kotlin tests, fail CI on spec drift. Hand-written DTOs until
  endpoint count justifies a generator.
- Backend tests (pytest + TestClient) and real `backend-check` CI job.
- iOS: add `ios/` as a peer; the shared contract in `api/` keeps it cheap.
  KMP only if duplicated logic ever dominates — native per platform is the plan.
- Agent-addressable device CLI (Shopify-style) if emulator feedback becomes
  the bottleneck; the pure core already gives headless millisecond feedback.
