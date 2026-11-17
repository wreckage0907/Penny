# Migration ledger

Status of the Flutter → native Kotlin port. Update after every chunk.
Statuses: `not started` → `in progress` → `ported (tests green)` → `verified (e2e + visual)` → `merged`.

## Feature parity

| Feature | Flutter source (`mobile/lib/`) | Native status | Chunk / PR | Notes |
|---|---|---|---|---|
| Budget split + investment projection math | `Pages/expenseTracker/income_analysis.dart:120-155` | merged | 001 / PR #10 (c758ebf) | Quirks preserved: 7 rates / 4 used (incl. duplicate 0.10 dead tail); iterative compounding (raw-bits locked); unguarded inputs (neg/NaN/Inf/±0.0/underflow tested); 21 JVM tests + 1 e2e |
| Analysis screen (real data, charts) | `Pages/expenseTracker/income_analysis.dart` | not started | — | Needs expense/income API slice first; charts via Compose |
| Expense/income API client + DTOs | `Pages/expenseTracker/expense_service.dart` (pure Dart, 164 lines) | merged | 002 / PR #11 (3d30b01) | Backend quirks mirrored exactly; 45 new JVM tests (URL/JSON/MockWebServer contract); decisions in quirks section |
| Expense dashboard (CRUD, pie, month/year) | `Pages/expenseTracker/expense_dashboard.dart` | logic verified (screen + CRUD not started) | 005 | `ExpenseAggregation` (categories, subtotal-then-accumulate total, partial-total-on-throw, income int quirk, bar flex) + `ExpenseDashboardText`; carry-forwards in 005 log |
| Practice MCQ models + per-question reveal (NO scoring exists in Flutter) | `Pages/practice/mcqpage.dart:12-46,99-114,245-263` | merged | 003 / PR #12 (14dff35) | "Grading" = reveal-only; score/results would be NEW behaviour (separate ledger row if ever wanted). Backend ignores `num_question` (never worked: old param was plural) |
| Course module list + sorting | `Pages/coursePage/course_page.dart` | logic merged (fetch + screen not started) | 004 / PR #13 (34d8503) | `CourseText`: filter-empty + numeric-prefix sort, `capitalize`, display names, lesson title/file name; Dart string semantics in `DartText`. Screen carry-forwards in 004 log |
| Lesson markdown + TTS cleaner | `Pages/coursePage/lesson_page.dart:53-66` | merged | 004 / PR #13 (34d8503) | `LessonText.cleanMarkdownText`; 112 Dart-executed goldens; Dart `\s`/`.`/multiline-`^`/trim sets reproduced exactly |
| Auth (login/signup/onboarding) | `Services/auth.dart`, `Pages/login/*`, `Pages/home/onboarding_page.dart` | not started | — | Firebase Auth; username→email lookup via `GET /onboarding/{username}` |
| Home (greeting, news carousel, grid) | `Pages/home/home.dart` | not started | — | News is a static bundled JSON (`assets/finance.json`), not live |
| Chatbot (Gemini) | `Pages/chatbot/chatbot_page.dart` | not started | — | Flutter calls Gemini directly with a bundled key — security decision needed before porting |
| Stock emulator | `Pages/stocks/stock_profile.dart` | not started | — | Trading is local-only in Flutter; backend buy/sell endpoints are broken |
| Profile settings / edit profile | `Pages/home/profile_settings.dart`, `edit_profile.dart` | not started | — | Image upload via `POST /prof` multipart |

## Backend quirks to respect (or explicitly decide to fix)

- `PUT /income/{user}/{year}/{month}` takes `amount` as a **query param**; the Flutter app sends JSON (→ 422). Flutter never uses it; `POST /income` **adds to** existing income. **Verified in-process (002):** PUT also ADDS (never sets) and 400s ("404: Finance document not found.") when the doc is absent; all handler 404s are re-raised as **400** `{"detail":"404: …"}` by blanket excepts; `?year=0`/`?month=` treated as absent (truthiness); month-without-year ignored on both GETs; DELETE by name removes ALL duplicates and returns a **lying 200** for unknown names, cascading empty category→month→year keys; POST into a month lacking `categories` leaks KeyError as 400 `{"detail":"'categories'"}`; unfiltered GET /income breaks (400) if `finances` holds livestock's `"user"` sibling; responses compact UTF-8 JSON, no charset; key order = Firestore order (do not sort).
- `/stocks/buy` and `/stocks/sell` raise KeyError (`finances['user']` doesn't exist). Flutter trades locally only.
- `/generate_questions` ignores `num_question`, always 10. **Verified (003):** param was removed in 8a4a27f and even before that the signature was plural `num_questions`, so the client's singular param NEVER worked; "10" is prompt intent, not enforced — `[]` → 200 with zero questions; option count and `ans` format unvalidated (lowercase/full-text `ans` → no option ever green); invalid chapter → **400** `{"detail":"Invalid chapter"}` (not 422); any parse/generation failure → **500** `{"detail":"Failed to generate questions"}` (narrow fence stripping: bare ``` or CRLF or preamble → 500); top-level key is the chapter DISPLAY name (client ignores it, takes values.first); keys `question_1..N` 1-based unpadded; **import-time `generate_content("Test")` re-raises → retired model/key kills the WHOLE backend boot**, not just MCQ.
- No auth on any endpoint; `user_id` trusted from URL. `POST /set-custom-claims` is unauthenticated.
- `google-generativeai` + gemini-1.5 models are retired; backend needs a model migration before chatbot/MCQ slices can work end-to-end.
- Root `render.yaml` is misconfigured; `backend/render.yaml` is the real one.
- **Int-income TypeError (DECIDED 2026-11-17: PRESERVE):** Dart assigns
  JSON income (dynamic) to a `double` field; an integer payload (backend returns
  int `0` for months with no income entry, `expenseTracker.py:229,236`) throws
  TypeError, caught at `income_analysis.dart:110-117` → income 0.0 **and expenses
  wiped** → "No data available" even when expenses exist. A Kotlin DTO coercing
  `0 → 0.0` would visibly diverge (pies rendered). Maintainer chose PRESERVE:
  002's DTO keeps income as raw JSON (no coercion); an int income reproduces
  the TypeError→catch path (income 0, expenses wiped) in the analysis slice.
  A fix remains possible as its own explicit chunk.
  **Blast radius is per-screen** (verified): analysis loads income+expenses in
  ONE try → both wiped; dashboard loads income in its OWN try
  (`expense_dashboard.dart:127-144`) → only income zeroed, pies stay.
  Trigger: year key exists in `finances` AND requested month absent (or stored
  int). Absent year → `{}` → `?? 0.0`, no throw. Stored `0.0` → double, no throw.
  **Screen slices MUST discriminate via primitive.content/longOrNull, never
  JsonPrimitive.double** (parses "0"→0.0 silently, quirk would vanish).
- **002 decisions (maintainer, 2026-11-17):** INTERNET manifest permission
  approved; int-income quirk preserved (above); **UTF-8 body decoding FIX**
  (Dart http 1.2.2 latin1 fallback mangles non-ASCII on read and writes the
  mangled name back on edit — verified live: "Café"→"CafÃ©"; explicit
  divergence); **OkHttp default timeouts kept** (Dart hangs forever; explicit
  divergence). Orchestrator: OkHttp + kotlinx.serialization + MockWebServer;
  `retryOnConnectionFailure(false)` = parity (Dart never retries; a retried
  POST /expenses would duplicate, backend appends).
- **002 carry-forwards for screen slices:** snackbars render Dart `$e` =
  "Exception: Failed to …: <body>" (DOUBLED prefix, expense_dashboard.dart:397,
  428,633) — build snackbar text accordingly; month must be initialised to the
  English name BEFORE the first request (field initialisers "01"/"11" are dead
  code, overwritten in initState); dashboard reads amounts `as num` (ints OK)
  while analysis `as double` (ints throw) — per-screen extraction semantics.
- **Accepted tolerance divergences (002, unreachable via backend):** double
  spelling outside [1e-3,1e7) (Java 1.0E7 vs Dart 10000000.0 / 1e-7; same IEEE
  value, Pydantic identical); kotlinx lexer accepts NaN/Infinity literals where
  Dart jsonDecode throws (backend 500s on non-finite before serialising);
  DELETE sends Content-Length: 0 (Dart none) — cosmetic.
- **003 carry-forwards for the practice screen slice:** AppBar title is
  hard-coded "Chapter 1" for every chapter (mcqpage.dart:155) — parity default
  reproduce, fix only as explicit decision; zero-question 200 payload crashes
  Flutter at `values.elementAt(0)` (RangeError during build) — screen slice
  must decide crash-equivalent error state vs "no questions" (maintainer call);
  option strings ALREADY carry "A) " prefixes from the backend — never
  double-prefix in Compose; out-of-range chapter index → null slug → Dart
  TypeError in initState (core returns null, screen reproduces failure);
  same-frame double tap last-wins (no confirm step); error screen has no retry;
  transport-error inner text diverges (Dart ClientException vs IOException),
  wrap structure preserved; malformed-JSON 200 inner text diverges likewise
  (Dart FormatException vs kotlinx JsonDecodingException), single wrap kept.
  Practice slice must encode Dart 3.14 RangeError text for the zero-question
  crash: "RangeError (index): Index out of range: index should be less than N: N".
- **Dart mutable state:** `savings`/`years`/`annualReturns` are instance fields;
  a projection taken before income load is 6 × 0.0. Relevant to initial-frame UI.
- **Money formatting (CORRECTED in 005):** the 001 claim that Java `%.2f`
  "matches Dart `toStringAsFixed(2)` for all realistic amounts" was WRONG —
  `%.2f` rounds the shortest decimal repr, Dart rounds the exact binary value
  (1.005 → Dart "1.00", `%.2f` "1.01"; also 2.675, 0.015, 100.005, 999999.995,
  1e15+0.125). Never visible in 001 (its goldens are not near-ties). 005 swaps
  in exact `BigDecimal(double)` HALF_UP + Dart's "-0.00"/NaN/Infinity spelling;
  42 Dart-emitted goldens, on JVM and ART. Remaining divergence (deferred):
  |x| ≥ 1e21 (Dart → exponential). Unreachable for amounts.

- **004 accepted divergences (text utils, unreachable for maintainer-controlled
  ASCII module slugs):** (1) case mapping — Dart VM tables are older Unicode:
  749 code points (JDK 17; more on ART/ICU) where Dart leaves the char unchanged
  but `Character.toUpperCase/LowerCase` map it (Georgian Mtavruli, Cyrillic
  U+0524-052F, IPA letters…); never the reverse; ASCII/Latin-1 identical —
  verified by exhaustive comparison over all code points. (2) sort tie order for
  >33 modules — Dart `List.sort` is insertion sort (stable) up to 33 elements and
  dual-pivot quicksort (unstable) beyond; Kotlin `sortedBy` is stable. Input order
  is itself non-deterministic: backend `/subfolder` returns a Python `set` of
  str (hash-randomised per process; no PYTHONHASHSEED set), so no reproducible
  behaviour is lost.
- **004 carry-forwards for the course/lesson screen slices:** `/subfolder`
  order is random per backend process (set) and can include `""` (a blob named
  exactly `content/` → parts[1] == "") — the filter matters; `List<String>.from` throws on
  non-string titles → caught → EMPTY list, spinner off, no error UI;
  `getModuleNames` throws RangeError for an empty `_` part (`1_a__b`, trailing
  `_`) inside itemBuilder → Flutter error widget for that tile; skeleton shows 5
  "Module Name" tiles while loading; learning_page has 8 hard-coded lesson nodes
  regardless of content; `backgroundImages[moduleName]` only knows 3 display
  names — any other module passes null into `DecorationImage(image:)` (dynamic
  → TypeError at build; exact text unverified); lesson fetch failures (non-200,
  null content) render the literal "Failed to load content" as markdown and TTS
  reads it; `late String content` → tapping the speaker before load throws
  LateInitializationError (swallowed); `FutureBuilder(future: fetchFile())` is
  created in build, so every setState (incl. toggling TTS) RE-FETCHES the lesson
  and flashes the loader; TTS is en-US, pitch 0.8, cleaned text only.

- **005 carry-forwards for the dashboard screen slice:** month/year change
  fires `_loadExpenseData` + `_loadIncomeData` with NO cancellation — responses
  can land out of order, last-arriving wins (stale month shown); a failed
  expenses fetch keeps the PREVIOUS month's total in the "Expenses $x" text
  while the pie says "No data"; a malformed payload shows "No data" with the
  PARTIAL total; `-0.0` income renders "$-0.00"; add-income sets the displayed
  income to the ENTERED amount although `POST /income` ADDS (display ≠ backend
  until reload) and the call is not awaited (errors silently lost); income bar
  `Flexible(flex: 0)` when income is 0 (green bar collapses), negative amounts
  give negative flex (Flutter behaviour unverified); delete removes ALL same
  name+amount entries locally then reloads; empty category list still gets a
  legend dot, a 0-value pie slice and a "$0.00" tile; icon lookup is exact-case
  (8 names) else tags; snackbar texts per 002 carry-forwards.

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

### 002 — Expense/income API client + DTOs
- Branch `migration/002-expense-api` → PR #11. CI run 44084579687 green
  (android-unit, android-e2e, changes; backend-check legitimately skipped —
  ci-ok skip path now live-verified). PR #11 ready for maintainer review.
- Ported: `ExpenseUrls` (pure URL builders), `ExpenseDtos` (request-only,
  snake_case update body), `ExpenseApiException` (verbatim message format),
  `ExpenseService` (OkHttp, suspend, raw JsonObject GETs — no coercion).
  Stack: OkHttp 4.12.0 + kotlinx-serialization-json 1.7.3 + coroutines 1.9.0,
  MockWebServer for JVM contract tests; serialization plugin 2.0.21.
- Tests: 66 JVM total (21 from 001 + 12 URL + 6 JSON + 27 contract); e2e 1/1
  unchanged (no UI in slice). Goldens verified by EXECUTING Dart 3.14
  (brew dart-sdk now installed): Uri.replace/encodeQueryComponent semantics,
  jsonEncode double spelling, http 1.2.2 wire charset + latin1 fallback.
  Backend contract verified in-process by subagent (real router, stubbed
  Firestore, two FastAPI eras) — canned fixtures F01-F30 in review notes.
- Adversarial review caught one real drift: getIncome query must use
  encodeQueryComponent (space→+, &→%26, empty→bare key), NOT raw interpolation
  — fixed + goldens. Also: retryOnConnectionFailure(false) parity; read()
  status-before-decode tests; escaping/#/year-0/baseUrl goldens; test-smell fix
  (explicit tuples). Reviewer's "Response.body non-nullable" claim was WRONG
  (Kotlin API nullable) — compiler caught it; lesson: verify reviewer claims.
- Decisions: see quirks section (INTERNET approved; int-income PRESERVE;
  UTF-8 FIX; OkHttp timeouts kept; retries off = parity).
- Open: none blocking. Next: 003 MCQ models + grading.

### 003 — MCQ models + per-question reveal + fetch
- Branch `migration/003-mcq-grading` → PR #12. CI run 44091674562 green on
  3969dd8; PR #12 ready for maintainer review.
- Premise corrected: Flutter has NO scoring/results/threshold anywhere in the
  file's history — "grading" is the reveal colouring table only. Ported scope
  (a): models + verdict + progress/labels + transitions + slug table + fetch.
  A score feature would be NEW behaviour (own ledger row if ever requested).
- Ported: `McqModels` (decode-only, Dart cast-error texts reproduced verbatim —
  executed on Dart 3.14; first-value chapter, insertion order, duplicate-key
  last-wins), `McqState` (one-shot select, next no-op at last, letters
  charCode 65+i unbounded, progress double incl Infinity@0, labels),
  `McqChapters` (slug table, null out of range), `McqApiException` + dartRender
  (doubled "Exception: " wrap), `McqService` (GET num_question=5, exactly-200,
  non-object → Invalid response format, catch-all rewrap, retries off).
- Tests: 108 JVM total (66 prior + 18 models + 11 state + 13 contract);
  e2e 1/1 unchanged (no UI in slice).
  Backend fixtures verified in-process by subagent (real questionGen router,
  stubbed genai): 400 invalid chapter, 500 generic, empty-200, lowercase ans,
  raw UTF-8, fence-parse 500 matrix.
- Decisions: insertion order preserved (NO numeric sort — reviewer suggestion
  rejected as divergence); ans byte-exact (no normalisation); decode-only.
- Adversarial review: two real low-severity drifts in dartKind, both verified
  in Dart then fixed — JsonObject as cast SOURCE prints '_Map<String, dynamic>'
  (underscore), and exponent-form integers (1e2) are double in Dart (kotlinx
  longOrNull misclassified; now String.toLongOrNull). +8 regression guards.
- Open: practice-screen decisions queued in carry-forwards (AppBar title,
  zero-question crash policy). Next: 004 text utils.

### 004 — Course/lesson text utils (Dart string semantics)
- Branch `migration/004-text-utils` → PR (draft until CI green).
- Ported: `LessonText.cleanMarkdownText` (lesson_page.dart:53-66),
  `CourseText` — `capitalize` (course_page.dart:11-15), `sortModules` +
  `moduleSortKey` (:42-49), `moduleDisplayName` (:63-66), `lessonTitle` /
  `lessonFileName` (learning_page.dart:29-30) — and `DartText`, the Dart
  runtime string semantics they need.
- Characterised by EXECUTING Dart 3.14, exhaustively where it matters: per-BMP
  code unit tables for RegExp `\s`, `.`, multiline `^`, `trim`, `int.tryParse`
  padding; all-code-point case-mapping comparison vs JDK 17. Golden tables were
  EMITTED by Dart as Kotlin literals (no hand-typed goldens).
- Naive Kotlin would have drifted in 7 ways, each now locked by a test that a
  mutation run confirmed fails without the fix: Java `\s` is ASCII-only (Dart
  includes NBSP/U+3000/BOM…); Dart multiline `^` fires between `\r` and `\n`
  (`- a\r\n- b` → `a\rb`); Java `.` stops at U+0085, Dart's doesn't
  (`(a\u0085b)` → ``); Dart trim strips U+0085/BOM but not U+001C-1F (Kotlin
  is the reverse); `uppercase()` maps `ß`→`SS` and applies final sigma, Dart
  doesn't; `int.tryParse` accepts `0x` hex (64-bit wrap: `0xFFFFFFFFFFFFFFFF` →
  -1) and trims, while `toLongOrNull` accepts non-ASCII digits (`٣`). 12/12
  mutations killed.
- Tests: JVM +16 (DartTextTest 5, CourseTextTest 8, LessonTextTest 3 — 112
  clean goldens, 38 capitalize, 15 display-name, 74 int.tryParse, 16 sort
  tables). On-device +3 (`DartTextOnDeviceTest`): ART's `java.util.regex` is
  ICU-backed, so the regex classes are re-proven exhaustively on the emulator.
  e2e 4/4 locally (penny, API 36).
- Adversarial review (subagent, differential, verified by execution against
  the REAL compiled classes): 0 mismatches over 700k random/token markdown
  inputs and 300k+ int.tryParse/sort-key inputs (boundaries 2^63/2^64, 1000
  leading zeros); all 52k capitalize mismatches trace to the 749 documented
  code points (scope confirmed exact, never the reverse, no length changes);
  ≤33 stable / 34 unstable re-confirmed over 20k trials. Folded in: surrogate-
  in-parens goldens on ART (lone surrogates under ICU), and the golden helper
  now requires the Dart RangeError stand-in for "Dart threw" cases (any
  exception used to count). Noted, not changed: lessonTitle/FileName take Int
  (Dart int is 64-bit) — index is one of 8 hard-coded nodes, unreachable.
- Decisions: accepted divergences recorded above (newer-Unicode case pairs;
  >33-item tie order). Quirks preserved: `C# ` eaten by the heading regex,
  `* ` bullets leave a space, every one-line `(…)` deleted lazily, empty `_`
  part throws RangeError with Dart's text.

### 005 — Expense dashboard aggregation (+ Money.format parity fix)
- Branch `migration/005-expense-aggregation` → PR. Review: OpenCode agent
  (via Herdr) — merge authorised by maintainer once OC is green AND ci-ok green.
- Ported: `ExpenseAggregation` — `categoriesFor` (:171-179), `categoryTotals`
  (:184-189, :1137), `onExpensesFetched` / `onExpensesFetchFailed`
  (:146-169), `incomeFor` (:127-144), `barFlex` (:1074, :1084);
  `ExpenseDashboardText` — month names / initial month / year range, amount
  label, subtitle, palette (verified against Flutter 3.22 colors.dart),
  icon mapping with tags fallback.
- Characterised by EXECUTING the verbatim Dart methods on jsonDecode'd
  fixtures; Dart emitted all Kotlin golden literals (31 load fixtures incl. 15
  malformed shapes, 16 income, 24 flex, 42 toStringAsFixed). Doubles compared
  bit-for-bit.
- Found + fixed a merged-code parity bug: `Money.format` (001) — see quirks.
- Quirks preserved: subtotal-then-accumulate (0.8999999999999999 vs flat
  0.9 — my hand-derived "flat" value was wrong; the executed one is in the
  test); partial total survives a malformed payload; fetch failure keeps the
  previous total; only Dart-double income survives (int → 0.0); flex rounds
  half away from zero, clamps to int64, throws on NaN/Infinity.
- Tests: JVM 132 (116 + ExpenseAggregationTest 7 + MoneyTest +1 table);
  on-device +2 (`ExpenseAggregationOnDeviceTest`: Android BigDecimal is not
  the JDK's); e2e 6/6 locally incl. unchanged Analysis screen. Mutation run
  13/13 killed (incl. reverting Money to %.2f).
