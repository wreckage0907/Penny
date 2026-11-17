# Migration ledger

Status of the Flutter → native Kotlin port. Update after every chunk.
Statuses: `not started` → `in progress` → `ported (tests green)` → `verified (e2e + visual)` → `merged`.

## Feature parity

| Feature | Flutter source (`mobile/lib/`) | Native status | Chunk / PR | Notes |
|---|---|---|---|---|
| Budget split + investment projection math | `Pages/expenseTracker/income_analysis.dart:120-155` | verified (e2e + visual) | 001 | Quirks preserved: 7 rates / 4 used (incl. duplicate 0.10 dead tail); iterative compounding (raw-bits locked); unguarded inputs (neg/NaN/Inf/±0.0/underflow tested); 21 JVM tests + 1 e2e |
| Analysis screen (real data, charts) | `Pages/expenseTracker/income_analysis.dart` | not started | — | Needs expense/income API slice first; charts via Compose |
| Expense/income API client + DTOs | `Pages/expenseTracker/expense_service.dart` (pure Dart, 164 lines) | not started | — | Best next slice; mirror backend quirks exactly |
| Expense dashboard (CRUD, pie, month/year) | `Pages/expenseTracker/expense_dashboard.dart` | not started | — | Aggregation logic at :163-189 is pure; extract first |
| Practice MCQ models + grading | `Pages/practice/mcqpage.dart:12-46,99-114,245-263` | not started | — | Backend ignores `num_question`, always returns 10 |
| Course module list + sorting | `Pages/coursePage/course_page.dart` | not started | — | Numeric-prefix sort; capitalize extension |
| Lesson markdown + TTS cleaner | `Pages/coursePage/lesson_page.dart:53-66` | not started | — | `cleanMarkdownText` is pure string logic |
| Auth (login/signup/onboarding) | `Services/auth.dart`, `Pages/login/*`, `Pages/home/onboarding_page.dart` | not started | — | Firebase Auth; username→email lookup via `GET /onboarding/{username}` |
| Home (greeting, news carousel, grid) | `Pages/home/home.dart` | not started | — | News is a static bundled JSON (`assets/finance.json`), not live |
| Chatbot (Gemini) | `Pages/chatbot/chatbot_page.dart` | not started | — | Flutter calls Gemini directly with a bundled key — security decision needed before porting |
| Stock emulator | `Pages/stocks/stock_profile.dart` | not started | — | Trading is local-only in Flutter; backend buy/sell endpoints are broken |
| Profile settings / edit profile | `Pages/home/profile_settings.dart`, `edit_profile.dart` | not started | — | Image upload via `POST /prof` multipart |

## Backend quirks to respect (or explicitly decide to fix)

- `PUT /income/{user}/{year}/{month}` takes `amount` as a **query param**; the Flutter app sends JSON (→ 422). Flutter never uses it; `POST /income` **adds to** existing income.
- `/stocks/buy` and `/stocks/sell` raise KeyError (`finances['user']` doesn't exist). Flutter trades locally only.
- `/generate_questions` ignores `num_question`, always 10.
- No auth on any endpoint; `user_id` trusted from URL. `POST /set-custom-claims` is unauthenticated.
- `google-generativeai` + gemini-1.5 models are retired; backend needs a model migration before chatbot/MCQ slices can work end-to-end.
- Root `render.yaml` is misconfigured; `backend/render.yaml` is the real one.
- **Int-income TypeError (slice 002 decision, preserve by default):** Dart assigns
  JSON income (dynamic) to a `double` field; an integer payload (backend returns
  int `0` for months with no income entry, `expenseTracker.py:229,236`) throws
  TypeError, caught at `income_analysis.dart:110-117` → income 0.0 **and expenses
  wiped** → "No data available" even when expenses exist. A Kotlin DTO coercing
  `0 → 0.0` would visibly diverge (pies rendered). Decide explicitly in 002.
- **Dart mutable state:** `savings`/`years`/`annualReturns` are instance fields;
  a projection taken before income load is 6 × 0.0. Relevant to initial-frame UI.
- **Money formatting divergence (deferred):** `Money.format` (Java `%.2f`,
  Locale.US) matches Dart `toStringAsFixed(2)` for all realistic amounts and
  exact binary ties, but diverges at |x| ≥ 1e21 (Dart → exponential). Unreachable
  for incomes; revisit only if a slice renders such values.

## Chunk log

### 001 — Migration scaffolding + first parity slice (budget math)
- Branch: `migration/kotlin` → PR to `main`.
- Added: `android/` Gradle project (Compose, JDK 17, AGP 8.13.0), pure core
  `BudgetSplit` + `InvestmentProjection` + `Money`, 14 JVM characterisation
  tests, 1 instrumented e2e test, minimal Analysis screen, scripts (`run.sh`,
  `e2e.sh`), pre-commit hook, CI (`changes` → unit/e2e/backend → `ci-ok`),
  this playbook + ledger.
- `Money.format` (contributed in parallel during session 1): pure formatting
  core (`toStringAsFixed(2)` stand-in, Locale.US) replacing the screen-local
  helper; 3 tests — slice goldens, exact-binary-tie rounding, negative zero.
  **Deliberate divergence, recorded here:** Dart `toStringAsFixed(2)` switches
  to exponential notation at |value| >= 1e21; Java `%f` prints full digits.
  Unreachable for income/expense amounts — revisit only if a slice ever
  renders such values.
- Parity source: `income_analysis.dart:120-155`, read verbatim; goldens
  hand-derived; float op order preserved (exact-equality test).
- Adversarial review (subagent) + independent golden recomputation: port faithful;
  suite strengthened to 21 JVM tests — raw-bits op-order lock at MF year 2
  (11236.0 vs pow's 11236.000000000002; reviewer's suggested Stocks-year-2 point
  was verified NON-discriminating and rejected), exact literals (16105.1 /
  17623.416832 / 6666.666 do not round-trip), verbatim rate/type lists,
  negative/NaN/Inf/±0.0/underflow, headless split→projection chain, `Money.format`
  extracted to core with tests. testTags added to header texts.
- Verified: `./gradlew test assembleDebug` green; e2e 1/1 on emulator penny
  (API 36); install+launch+screenshot (`migration/shots/slice001.png`).
- Infra audit (subagent) pre-first-CI: blocker = missing `pull-requests: read`
  for dorny/paths-filter on PR events; warnings = invalid `build-root-directory`
  input on setup-gradle@v5, paths-filter v3 (node20) → v4, unbounded emulator
  boot waits in e2e.sh, arm64-only JAVA_HOME in env.sh, backend-check on 3.12
  vs Dockerfile 3.10, wrong e2e artifact paths, no unit-job timeout. Wrapper jar
  was stock 9.7.0 → regenerated to official 8.13 (sha 81a82aae…). `.gitignore`
  gained `.idea/ *.iml captures/ *.jks *.keystore`. Loop fixes pending maintainer
  OK (ask-first area).
- Open decisions: none blocking. Chatbot key handling and backend model
  migration flagged above for later slices.
- Coordination incident (2026-11-16): a second agent session wrote commits
  4afe490/49e1833/c13bcb2 concurrently with the orchestrating session;
  maintainer paused it. Its final commit converged with the maintainer's
  decisions (action bumps kept, shots stay in git, ledger reconciled).
  Lesson recorded in playbook: one writer per worktree.
- Verification: local unit + e2e green at 14 tests; CI run 43916459458 all
  green on the first run (android-unit 2m47s, android-e2e 4m34s on a cold
  API-36 emulator, backend-check, ci-ok). Suite then strengthened to 21 JVM
  tests by adversarial review. Run 43917491925 (hardening commit 6a222c4)
  failed on infra only: setup-gradle `dependency-graph: false` is not a valid
  enum (must be `disabled`) — ci-ok correctly blocked. Fixed in follow-up
  commit, which also bumps action majors (checkout v7, setup-java v6,
  setup-python v7). Run 43918765909 on the hardened conventional-history tip
  a123fda: all five jobs green (cold API-36 e2e included); PR #10 marked ready
  for maintainer review — merging is the maintainer's gate. Slice 002 must
  decide the int-income quirk (above) and needs INTERNET permission (ask-first).
