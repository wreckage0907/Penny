# Migration ledger

Status of the Flutter → native Kotlin port. Update after every chunk.
Statuses: `not started` → `in progress` → `ported (tests green)` → `verified (e2e + visual)` → `merged`.

## Feature parity

| Feature | Flutter source (`mobile/lib/`) | Native status | Chunk / PR | Notes |
|---|---|---|---|---|
| Budget split + investment projection math | `Pages/expenseTracker/income_analysis.dart:120-155` | merged | 001 / PR #10 (c758ebf) | Quirks preserved: 7 rates / 4 used (incl. duplicate 0.10 dead tail); iterative compounding (raw-bits locked); unguarded inputs (neg/NaN/Inf/±0.0/underflow tested); 21 JVM tests + 1 e2e |
| Analysis screen (real data, charts) | `Pages/expenseTracker/income_analysis.dart` | verified on FAKES (e2e + visual) | 001 + 013 | `AnalysisLogic` + `AnalysisTab`; int-income quirk wipes the expenses here (one try) |
| Expense/income API client + DTOs | `Pages/expenseTracker/expense_service.dart` (pure Dart, 164 lines) | merged | 002 / PR #11 (3d30b01) | Backend quirks mirrored exactly; 45 new JVM tests (URL/JSON/MockWebServer contract); decisions in quirks section |
| Expense dashboard (CRUD, pie, month/year) | `Pages/expenseTracker/expense_dashboard.dart` | verified on FAKES (e2e + visual) | 005 + 009 | `ExpenseAggregation` (categories, subtotal-then-accumulate total, partial-total-on-throw, income int quirk, bar flex) + `ExpenseDashboardText`; carry-forwards in 005 log |
| Practice MCQ models + per-question reveal (NO scoring exists in Flutter) | `Pages/practice/mcqpage.dart`, `list_of_modules.dart` | verified on FAKES (e2e + visual) | 003 / PR #12 (14dff35) + 011 | "Grading" = reveal-only; score/results would be NEW behaviour (separate ledger row if ever wanted). Backend ignores `num_question` (never worked: old param was plural) |
| Course module list + sorting | `Pages/coursePage/course_page.dart` (+ learning_page, lesson_page) | verified on FAKES (e2e + visual) | 004 / PR #13 (34d8503) | `CourseText`: filter-empty + numeric-prefix sort, `capitalize`, display names, lesson title/file name; Dart string semantics in `DartText`. Screen carry-forwards in 004 log |
| Lesson markdown + TTS cleaner | `Pages/coursePage/lesson_page.dart:53-66` | merged | 004 / PR #13 (34d8503) | `LessonText.cleanMarkdownText`; 112 Dart-executed goldens; Dart `\s`/`.`/multiline-`^`/trim sets reproduced exactly |
| Auth (login/signup/onboarding) | `Services/auth.dart`, `Pages/login/*`, `Pages/home/onboarding_page.dart` | verified on FAKES (e2e + visual) | 007 | Screens + `AuthFlows` on `FakeAuthService` / `FakeUserDirectory` / `InMemoryUserPrefs`; real auth stack pending maintainer decision; image upload not mimicked |
| Home (greeting, news carousel, grid) | `Pages/home/home.dart` | verified on FAKES (e2e + visual) | 008 | `HomeLogic` + `HomeScreen`; news = bundled `finance.json` (20 articles, 20 pages); avatar via `ProfileImages` seam (fake: no pictures) |
| Chatbot (Gemini) | `Pages/chatbot/chatbot_page.dart` | not started | — | Flutter calls Gemini directly with a bundled key — security decision needed before porting |
| Stock emulator | `Pages/stocks/stock_profile.dart` | verified on FAKES (e2e + visual) | 014 | `StockLogic` + `StockScreen`; local-only trading kept; market = `StockMarket` seam with a deterministic synthetic fake (real: yfinance via /stocks/live, /historical) |
| Profile settings / edit profile | `Pages/home/profile_settings.dart`, `edit_profile.dart` | verified on FAKES (e2e + visual) | 012 | `ProfileFlows` + screens; picture via system photo picker → `ProfileImages` PUT/DELETE; Firebase error texts mimicked (unverified) |

## Service mimicry (maintainer decision 2026-11-17)

The backend/auth stack is undecided (Firebase or something else). Until it is
chosen, screens depend on **interfaces** (`PennyServices`) and the app ships
with in-memory **fakes that mimic the current services' verified semantics**
(backend quirks above, Firebase auth rules). Fakes are NEW behaviour, not
parity: no persistence across process death, no network, no latency. Each
screen slice adds its seam + fake; the real HTTP clients from 002/003 stay as
alternative implementations. Swapping stacks later = implementing the
interfaces, not touching screens. Auth flows remain ask-first for any REAL
implementation.

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
  crash — CORRECTED in 011 by execution: the EMPTY case reads "RangeError
  (index): Index out of range: no indices are valid: 0"; "index should be
  less than N: I" is the non-empty wording.
- **Dart mutable state:** `savings`/`years`/`annualReturns` are instance fields;
  a projection taken before income load is 6 × 0.0. Relevant to initial-frame UI.
- **Money formatting (CORRECTED in 005):** the 001 claim that Java `%.2f`
  "matches Dart `toStringAsFixed(2)` for all realistic amounts" was WRONG —
  `%.2f` rounds the shortest decimal repr, Dart rounds the exact binary value
  (1.005 → Dart "1.00", `%.2f` "1.01"; also 2.675, 0.015, 100.005, 999999.995,
  1e15+0.125). Never visible in 001 (its goldens are not near-ties). 005 swaps
  in exact `BigDecimal(double)` HALF_UP + Dart's "-0.00"/NaN/Infinity spelling;
  42 Dart-emitted goldens, on JVM and ART. The |x| ≥ 1e21 exponential
  spelling was closed in 006 (shortest round-trip digits, 5,965 Dart-emitted
  goldens) — `Money.format` now has NO known divergence from Dart.

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
- Tests: JVM 132 (124 + ExpenseAggregationTest 7 + MoneyTest +1 table);
  on-device +2 (`ExpenseAggregationOnDeviceTest`: Android BigDecimal is not
  the JDK's); e2e 6/6 locally incl. unchanged Analysis screen. Mutation run
  13/13 killed (incl. reverting Money to %.2f).

### 006 — App shell on fake services (+ Money ≥1e21 parity)
- Branch `migration/006-app-shell` → PR. Review: OpenCode via Herdr; merge
  when OC GREEN + ci-ok green (maintainer rule, round 5).
- Ported: route table verbatim (main.dart:44-56) as `AppRoutes`; AuthWrapper
  decision (main_functions.dart:11-23: waiting → spinner, user → Home, else
  Login, rendered INLINE like Flutter); AppColours as `PennyColors` + Material3
  `PennyTheme`; NavHost with every named route; edge-to-edge insets (API 36).
- Seam: `AuthService` (+ `AuthState`, `PennyUser`) with `FakeAuthService`
  mimicking the Firebase rules auth.dart:15-51 branches on (null on failure;
  weak-password < 6; email-already-in-use case-insensitive; invalid-email;
  sign-up signs in). Demo account `demo@penny.app` / `penny123`.
- TEMPORARY UI (replaced by 007/008): shell login ("Continue with demo
  account") and a shell home listing every route; unmigrated routes render
  `NotMigratedScreen`. The 001 Analysis demo is no longer the launch screen;
  its e2e renders the composable directly (assertions unchanged).
- Money: |x| ≥ 1e21 now returns Dart's `toStringAsExponential()` spelling
  (shortest round-trip digits; floor+ceiling candidates — half-even-only
  mutation fails 3 power-of-two goldens). Even-digit tiebreak is the
  ECMAScript rule, unreached by any golden.
- Tests: JVM 142 (+AuthServiceTest 4, AppRoutesTest 4, MoneyTest +2 incl. a
  5,965-line Dart-emitted resource); e2e 9/9 (+AppShellE2eTest 2,
  LaunchE2eTest 1). Screenshots: `migration/shots/slice006-{login,home}.png`.
- Visual divergence (deferred to screen slices): Google Fonts (dmSans,
  poppins, darkerGrotesque) not bundled yet — system font.

### 007 — Auth screens (login / sign-up / onboarding) on fakes
- Branch `migration/007-auth-screens` → PR. OC review + ci-ok → merge.
- Seams: `AuthService` grows customClaims, `signInWithProvider` (Google /
  GitHub, `SocialSignIn` = auth.dart's result map), `verifyPhoneNumber` /
  `signInWithSmsCode`; new `UserDirectory` (GET/POST /onboarding/{username},
  /set-custom-claims) and `UserPrefs` (SharedPreferences username/fullName).
  Fakes: `FakeUserDirectory` (POST overwrites, coins 0, blank or "/" username
  → FastAPI 404 {"detail":"Not Found"}), fixed fake Google/GitHub accounts
  (first sign-in isNewUser), phone code `123456` for E.164-ish numbers.
  Demo world: username `demo` / password `penny123` (claims demo / Demo User).
- Pure flows (`AuthFlows`), Flutter quirks kept: login takes a USERNAME
  (email lookup via directory), every failure silent (print only);
  fullName = Dart interpolation (`null` printed for a missing part); sign-up
  → onboarding; onboarding posts profile → claims "first last" → home, errors
  as SnackBar "An error occurred: Exception: …"; Google onboards only new
  users, GitHub on the LOGIN page also when the username claim is missing
  (signup page checks isNewUser only); phone always → onboarding
  (`isFirstTime = true` placeholder).
- Not mimicked yet: profile image upload (POST /prof) — avatar is a static
  placeholder; Google prefill is dead code in Flutter (route passes no args).
- Navigation divergence (stack shape only): Flutter pushes '/home' after
  sign-in; here "home" pops to the root, which AuthWrapper renders as Home.
- Visual: brand icons for Google/GitHub are letter badges ("G", "GH"); fonts
  are system (deferred). Screenshots: `migration/shots/slice007-{login,signup}.png`.
- Tests: JVM 152 (+AuthFlowsTest 10); e2e 14/14 (+AuthE2eTest 5: username
  login, silent wrong password, sign-up → onboarding → home → sign-out →
  log back in with the new username, Dart onboarding error text, phone flow).
  006 shell tests now drive the real login screen.

### 008 — Home on fakes
- Branch `migration/008-home` → PR. OC review + ci-ok → merge.
- Ported (`HomeLogic` + `HomeScreen`, home.dart): identity — claims win only
  when BOTH username and fullName claims exist, else both from prefs;
  greeting `'WELCOME,\n$fullName'` (Dart prints "null"); 2×2 grid with the
  HARD-CODED course progress ("74%" text, 0.7 ring) and Flutter routes
  (/coursepage, /budget, /stockprofile, /practice); Finance News = bundled
  `finance.json`, carousel ALWAYS 20 pages (page ≥ article count would be a
  Dart RangeError — unreachable, the file has exactly 20), null fields →
  "No title" / "No description" / via.placeholder.com; tap opens the article
  URL; dots indicator; bottom bar (home disabled, chat → /chatbot, person →
  /profilesettings); profile picture via new `ProfileImages` seam
  (`GET /prof/{username}` → url|null), refreshed every 50 min (signed URLs
  expire in 1 h). Coil 2.7.0 added for network images.
- TEMPORARY: the /profilesettings placeholder carries "Sign out" (Flutter's
  only sign-out lives there) via `AuthFlows.signOut` = auth sign-out +
  clearUserData (removes username/fullName prefs) — 007 review note 1.
- 007 review folded in: onboarding catches ALL exceptions like Dart
  `catch (e)` (test with an IOException-throwing directory); misnamed claims
  test renamed. Not mimicked (noted): phone auto-retrieval
  (verificationCompleted) — the fake always asks for the code.
- Divergences: the initial-frame RangeError flash (articles empty before
  readJson) cannot happen — the asset is read synchronously; Home's nested
  MaterialApp/Navigator is flattened into the single NavHost.
- Visual: asset illustrations sized in dp = PNG px (Flutter logical pixels).
  Screenshot `migration/shots/slice008-home.png`.
- Tests: JVM 160 (+HomeLogicTest 7, AuthFlowsTest +1); e2e 18/18
  (+HomeE2eTest 4; shell/auth e2e now use the real Home + profile sign-out).

### 009 — Expense tracker + dashboard screen on a fake backend
- Branch `migration/009-expense-tracker` → PR. OC review + ci-ok → merge.
- Seam: `ExpenseApi` extracted from the 002 `ExpenseService` (unchanged
  HTTP impl); `FakeExpenseBackend` mimics backend/routers/expenseTracker.py
  per user: amounts stored as floats, POST/PUT income ADD, missing filtered
  month → int 0 (int-income trigger, now exercised end-to-end), DELETE
  removes every same-name expense + cascades, UPDATE name+amount first hit
  (may move category), 404s → 400 {"detail":"404: …"} wrapped as
  "Failed to <action>: <body>", Python-truthiness filters. Demo user seeded
  for the current and previous month (`FakeExpenseRegistry.demo`).
- Screen (`ExpenseTrackerScreen`): tracker app bar + Dashboard/Analysis tabs
  (Analysis = placeholder until its slice); month/year pickers (±5 years);
  ring pie (centerSpaceRadius 70, radius 10, sectionsSpace 3, palette from
  005), Total, legend; income/expense card with flex bars (flex ≤ 0 → no bar,
  NaN/Inf → error stand-in); category cards → bottom sheet with edit/delete;
  add-expense and add-income dialogs; green/red SnackBars.
- Flows (`DashboardFlows`), Dart quirks kept: username = claim else prefs
  (no username → "Null check operator used on a null value" inside the try);
  "Please fill all fields"; `double.parse` outside the try → unparseable
  amount is SILENT (dialog stays); NaN/Infinity parse but fail jsonEncode
  ("Converting object to an encodable object failed: NaN"); doubled
  "Exception: Failed to …" SnackBars; edit keeps last valid amount, prefills
  Dart `toString` ("5.0"), int old amount → "type 'int' is not a subtype of
  type 'double'"; income tryParse → "Invalid amount entered", call NOT
  awaited, display = ENTERED amount while the backend adds; the sheet keeps
  its own list (edits mutate the tapped entry, deletes drop same name+amount).
- New Dart semantics in `DartText` (Dart-emitted goldens): `doubleTryParse`
  (46 cases) and `doubleToString` (34 cases); `Money` ≥1e21 now delegates to
  the shared shortest-digits engine (5,965 goldens still green).
- Divergences: delete-success toast rendered as a green SnackBar; Flutter's
  persistent bottom sheet → Material3 modal sheet; Flutter's NaN-income build
  error replaces the whole dashboard, here only the bar row shows the error.
- Tests: JVM 177 (+FakeExpenseBackendTest 8, DashboardFlowsTest 7,
  DartTextTest +2); e2e 24/24 (+ExpenseTrackerE2eTest 6: totals/legend/
  categories, month switch + int-0 income, add validation/silent/success,
  sheet edit + delete, income garbage + entered-not-sum, analysis tab).
  Screenshot `migration/shots/slice009-dashboard.png`.
- 008 review note folded in: unused `username` state removed from Home.

### 010 — Courses: module list → learning path → lesson + TTS, on fakes
- Branch `migration/010-courses` → PR. OC review + ci-ok → merge.
- Seam: `CourseContent` (`GET /subfolder`, `GET /get-file/{subfolder}/{file}`)
  with `FakeCourseContent`: 3 sample modules × 8 lessons whose display names
  hit exactly learning_page's three background keys; the title list is
  UNORDERED and contains "" like the backend's Python set; a missing file is
  a backend 500. `Speech` seam (flutter_tts) with `AndroidSpeech`
  (android.speech.tts) and a recording fake for tests.
- Ported: `CourseFlows` — modules = 004 filter + numeric sort, any malformed
  list → empty page; lesson content or the literal "Failed to load content";
  `LearningPath` — the CustomPainter geometry with Dart op order (circles,
  offset winding path, ≤ 40 hit test, 1900 logical px, reverse scroll so
  lesson 1 is at the bottom); `LessonSpeech` — en-US, pitch 0.8, 004 cleaner.
  Screens: course list (banner, "Modules", 5 "Module Name" skeleton tiles
  while loading, RangeError name → error tile), learning path (background by
  display name; unknown module → error stand-in like Flutter's TypeError),
  lesson (markdown via mikepenz multiplatform-markdown-renderer-m3 0.27.0 with
  Flutter's MarkdownStyleSheet sizes; speaker toggle; `late content` →
  tapping before load does nothing; the FutureBuilder-in-build quirk is kept:
  every toggle RE-FETCHES the lesson and flashes the loader).
- OC review round 1 → CHANGES REQUESTED (fixed): the TextToSpeech engine was
  never shut down (one leaked engine per lesson visit) and the dispose-time
  stop() ran on an already-cancelled scope. `Speech.release()` (stop +
  shutdown) now runs directly in onDispose; the init callback's ready/pending
  state is lock-guarded (binder thread vs main); completion handler set after
  speak like Flutter. e2e asserts release on leaving the lesson (mutation:
  removing it fails the test).
- Unverified Flutter suspicion (not reproduced): the painter fires onTap
  during paint with a retained tap location, so a repaint after returning
  from a lesson might reopen it.
- Test fix: `AuthE2eTest.wrongPasswordIsSilent` intermittently failed
  `assertIsDisplayed` while the IME animated; it now asserts the behaviour
  (no Home, still signed out, no prefs saved) — stronger, not looser.
- Tests: JVM 182 (+CourseFlowsTest 5); e2e 27/27 (+CourseE2eTest 3: sorted
  names with "" dropped; path tap → lesson → spoken cleaned text + stop;
  module without background → error). Note: touch-injection offsets are
  relative to a node's VISIBLE bounds (cost one debugging round).
  Screenshots `migration/shots/slice010-{courses,path,lesson}.png`.

### 011 — Practice list + MCQ page on a fake questions backend
- Branch `migration/011-practice` → PR. OC review + ci-ok → merge.
- Seam: `McqApi` (003 `McqService` implements it); its response half is now
  `McqService.interpret(code, body)`, shared by `FakeMcqBackend`, so the
  fake's error strings are the HTTP client's by construction (003 contract
  tests unchanged and green). The fake mimics questionGen.py: 10 questions
  regardless of `num_question`, display-name top key, "A) "-prefixed
  options, letter answers, unknown chapter → 400 {"detail":"Invalid
  chapter"}. Chapter 1 has real sample questions; the others generic.
- Screens: PracticeList — "List of Modules", one finished module (opens
  MCQPage(index 0)), two "unfinished" modules whose tap only prints (kept);
  MCQPage — FutureBuilder states (loader, "Error: Exception: Failed to load
  questions: Exception: …", data), AppBar hard-coded "Chapter 1" (parity
  default kept), "n/N" progress bar, "Question n/N", Hint → info toast
  (SnackBar here), options coloured by the 003 reveal table with ✓/✕,
  one-shot selection, "Next Question" always enabled and a no-op on the last
  question (no results screen), options never re-prefixed.
- Decisions taken as parity defaults (ledger carry-forwards from 003): empty
  chapter → the Dart RangeError text as an error screen (crash-equivalent);
  out-of-range chapter index → "type 'Null' is not a subtype of type
  'String'" error (unreachable: only index 0 is tappable).
- Tests: JVM 186 (+FakeMcqBackendTest 4); e2e 33/33 (+PracticeE2eTest 6:
  unfinished no-op + Chapter 1 opens, wrong answer reveal + lock, next to
  10/10 and no-op, hint, double-wrapped fetch error, empty-chapter
  RangeError). Screenshots `migration/shots/slice011-{practice,mcq}.png`.

### 012 — Profile Settings + Edit Profile on fakes
- Branch `migration/012-profile` → PR. OC review + ci-ok → merge.
- Seams grown: `AuthService.changePassword` (re-auth + update) and
  `deleteCurrentUser` with `AuthFailure(dartText)`; `UserDirectory`
  update/delete user + update/delete claims (fake: PUT replaces the `user`
  map and DROPS `coins`; DELETE cascades to the user's expenses doc and
  profile picture via an `onUserDeleted` hook, like delete_user);
  `ProfileImages.upload/delete` (DELETE succeeds even with no picture);
  `PennyServices.pickImage` (system PickVisualMedia by default; injectable).
- Flows (`ProfileFlows`), Flutter quirks kept: identity rules differ by page
  (Settings = Home's both-claims rule, Edit = username-claim rule); change
  password checks the mismatch first, never clears the dialog fields, shows
  `Failed to change password: <FirebaseAuthException.toString()>` — texts
  MIMICKED: "[firebase_auth/invalid-credential] The supplied auth credential is
  incorrect, malformed or has expired." / "[firebase_auth/weak-password]
  Password should be at least 6 characters" (unverifiable without a live
  project); phone users hit `user.email!` → "Null check operator used on a
  null value"; delete account = DELETE user → Storage delete of the loaded
  picture URL → DELETE claims → user.delete() → sign out + clear prefs → login
  — with a picture, the Storage delete FAILS after the server already wiped
  the data and the account survives (mimicked object-not-found, unverified);
  Edit Profile prefills `?? ''` (any non-string → all empty), shows the
  username as `username ?? ' '`, Save fires FOUR toasts (success, error, info,
  warning) all "Profile Updated Successfully", and ALWAYS saves the full name
  to prefs even if the update failed or no username was found; picture delete
  reports success even when there was nothing to delete.
- Divergences: toastification → a small `ToastHost` (opaque, tinted, 4 s);
  the delete-account success SnackBar is not shown after navigating to login
  (Flutter's root ScaffoldMessenger keeps it visible); returning from Edit
  Profile does not refresh Settings' name (same as Flutter: no initState).
- The 008 TEMPORARY sign-out placeholder is gone (real Sign Out button).
- Tests: JVM 195 (+ProfileFlowsTest 9); e2e 38/38 (+ProfileE2eTest 5:
  identity + sign out, change password mismatch→success→next login, edit
  prefill + four toasts + claims, picture upload→delete, delete account).
  Screenshots `migration/shots/slice012-{settings,edit-toasts}.png`.

### 013 — Analysis tab on the fake backend (+ 012 review follow-ups)
- Branch `migration/013-analysis` → PR. OC review + ci-ok → merge.
- Ported (`AnalysisLogic` + `AnalysisTab`, income_analysis.dart): income THEN
  expenses in ONE try — an int income (the backend's `0` for a month without
  income) is a TypeError that zeroes the income AND wipes the expenses, so a
  month with expenses but no income entry shows "No data available for $month
  $year" (e2e-asserted); lazy `.cast<Map>()` (element types unchecked at
  load); 50/30/20 split via the 001 `BudgetSplit`; investment bar chart (001
  projection, 6 year groups × 4 rods, maxY = largest value, `$value.toInt()`
  labels); per-category pie pager (amount/total*100, only when the total is
  finite and > 0; centerSpaceRadius 80) with the page's OWN palette (pink,
  deepPurple, deepOrange, blueAccent, teal, yellow), card total via
  `amount as double? ?? 0.0`, names via Dart interpolation; flex bars with the
  005 half-away rounding. Goldens emitted by executing the verbatim Dart
  (dart013/an.dart, 9 fixtures).
- 012 review notes folded in: deleteAccount/saveProfile catch every exception
  like Dart `catch (e)` (test with an IOException directory); a second
  picture tap supersedes a pending picker request instead of hanging it.
- Process note: a CI watch printed `ci-ok:` with an empty conclusion (race in
  the status read); PR #21 was confirmed completed/success after merging.
  From now on `gh pr checks` must show ci-ok pass immediately before merge.
- Tests: JVM 199 (+AnalysisLogicTest 3, ProfileFlowsTest +1); e2e 41/41
  (+AnalysisTabE2eTest 3: demo split + first category, expenses-without-
  income month → no data, previous month). Screenshots
  `migration/shots/slice013-analysis{,-pies}.png`.

### 014 — Market (stocks) page on a synthetic fake market
- Branch `migration/014-stocks` → PR. OC review + ci-ok → merge.
- Seam: `StockMarket` (`/stocks/live`, `/stocks/historical`: chronological
  `{"YYYY-MM-DD HH:MM:SS": {Open, High, Low, Close, …}}`, unknown ticker →
  `{}`); `FakeStockMarket` = seeded random walk per ticker (390 minute bars
  from 09:30 "live"; weekday daily bars, end-exclusive, for history).
  Backend `/stocks/buy|sell` stay unused (broken; Flutter trades locally).
- Ported (`StockLogic` + `StockScreen`), quirks kept and Dart-executed:
  every price is HARD-CODED ($387 / Open $133 / High $129 / Low $333;
  stockDetails' numbers unused); holdings are a local double map with NO
  validation — "Bought 0.0 / -1.0 / NaN shares", sell allowed iff qty <=
  available (negatives sell "up", NaN fails "Not enough shares to sell"),
  exactly 0 removes the key; `${holdings[s] ?? 0}` prints "0.0" (executed —
  Dart infers the literal as double); search = ticker OR name contains
  (case-insensitive; "inc" matches all six INC. names); picking a stock
  always loads LIVE data whatever range is selected; 1D refreshes every
  minute; date ranges subtract n×24 h so DST shifts the start date (executed
  under America/New_York: 2026-03-09 00:30 − 1 day → 2026-03-07); an int
  Close aborts the update and keeps the old points; chart x bounds are the
  FIRST/LAST points, y the min/max; green line, no axes.
- Tests: JVM 204 (+StockLogicTest 5); e2e 44/44 (+StockE2eTest 3: hard-coded
  card + 0.0 holdings, buy → oversell error → sell to 0.0, search → NKE +
  range switch). Screenshot `migration/shots/slice014-market.png`.
