# Migration ledger

Status of the Flutter → native Kotlin port. Update after every chunk.
Statuses: `not started` → `in progress` → `ported (tests green)` → `verified (e2e + visual)` → `merged`.

## Feature parity

| Feature | Flutter source (`mobile/lib/`) | Native status | Chunk / PR | Notes |
|---|---|---|---|---|
| Budget split + investment projection math | `Pages/expenseTracker/income_analysis.dart:120-155` | ported (tests green) | 001 | Quirks preserved: 7 rates / 4 used; iterative compounding; unguarded inputs |
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

## Chunk log

### 001 — Migration scaffolding + first parity slice (budget math)
- Branch: `migration/kotlin` → PR to `main`.
- Added: `android/` Gradle project (Compose, JDK 17, AGP 8.13.0), pure core
  `BudgetSplit` + `InvestmentProjection`, 11 JVM characterisation tests,
  1 instrumented e2e test, minimal Analysis screen, scripts (`run.sh`,
  `e2e.sh`), pre-commit hook, CI (`changes` → unit/e2e/backend → `ci-ok`),
  this playbook + ledger.
- Parity source: `income_analysis.dart:120-155`, read verbatim; goldens
  hand-derived; float op order preserved (exact-equality test).
- Open decisions: none blocking. Chatbot key handling and backend model
  migration flagged above for later slices.
