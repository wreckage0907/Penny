package com.penny.app.core

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** The practice-question seam: [McqService] over HTTP (003), [FakeMcqBackend] in memory. */
interface McqApi {
    /** mcqpage.dart:78-97; failures throw [McqApiException] with Dart's message. */
    suspend fun fetchQuestions(chapter: String, numQuestions: Int = 5): McqChapter
}

/**
 * Mimics `GET /generate_questions/{chapter}` (backend/routers/questionGen.py,
 * verified in 003): unknown chapter → 400 {"detail":"Invalid chapter"};
 * otherwise 200 with `{<display name>: {question_1..question_10: {...}}}` —
 * always 10 (`num_question` ignored), options pre-prefixed "A) ", `ans` a
 * letter. Responses go through [McqService.interpret], so messages are
 * byte-identical to the HTTP client's. Questions are fixed sample content
 * (the real backend generates them with Gemini).
 */
class FakeMcqBackend(
    private val chapters: Map<String, Pair<String, List<SampleQuestion>>> = SAMPLE,
) : McqApi {

    data class SampleQuestion(val question: String, val options: List<String>, val ans: String, val hint: String)

    override suspend fun fetchQuestions(chapter: String, numQuestions: Int): McqChapter {
        val (display, questions) = chapters[chapter]
            ?: return McqService.interpret(400, """{"detail":"Invalid chapter"}""")
        val payload = JsonObject(
            mapOf(
                display to JsonObject(
                    questions.mapIndexed { i, q ->
                        "question_${i + 1}" to JsonObject(
                            mapOf(
                                "question" to JsonPrimitive(q.question),
                                "options" to JsonArray(q.options.mapIndexed { j, o -> JsonPrimitive("${'A' + j}) $o") }),
                                "ans" to JsonPrimitive(q.ans),
                                "hint" to JsonPrimitive(q.hint),
                            ),
                        )
                    }.toMap(),
                ),
            ),
        )
        return McqService.interpret(200, payload.toString())
    }

    companion object {
        private fun q(question: String, a: String, b: String, c: String, d: String, ans: String, hint: String) =
            SampleQuestion(question, listOf(a, b, c, d), ans, hint)

        private val INTRO = listOf(
            q("What is the main goal of a budget?", "To spend more each month", "To plan how money is earned and spent",
                "To avoid paying taxes", "To borrow money easily", "B", "Think about planning ahead."),
            q("Which of these is a need?", "Concert tickets", "A new phone case", "Rent", "Takeaway coffee",
                "C", "Needs keep you housed, fed and safe."),
            q("In the 50/30/20 rule, what is the 20% for?", "Wants", "Needs", "Taxes", "Savings and debt repayment",
                "D", "It builds your future."),
            q("What is an emergency fund?", "Money set aside for unexpected costs", "A credit card limit",
                "A government benefit", "A loan from family", "A", "It is there for surprises."),
            q("Compound interest means…", "Interest only on the original amount", "Interest earned on interest too",
                "A fixed fee", "Interest that decreases over time", "B", "It snowballs."),
            q("Which usually has the highest risk?", "Savings account", "Fixed deposit", "Stocks", "Cash at home",
                "C", "Higher potential return, higher risk."),
            q("Net worth is…", "Monthly income", "Assets minus liabilities", "Total savings", "Credit score",
                "B", "What you own minus what you owe."),
            q("Tracking expenses helps you…", "Spend without limits", "Find where money goes", "Raise your salary",
                "Avoid taxes", "B", "You can't manage what you don't measure."),
            q("A good first financial goal is…", "Buying a luxury car", "Building an emergency fund",
                "Taking a big loan", "Day trading", "B", "Safety first."),
            q("Inflation reduces…", "Prices", "The purchasing power of money", "Interest rates", "Taxes",
                "B", "The same money buys less over time."),
        )

        private fun generic(topic: String) = List(10) { i ->
            q("$topic: sample question ${i + 1}?", "First choice", "Second choice", "Third choice", "Fourth choice",
                "${'A' + i % 4}", "Sample hint ${i + 1}.")
        }

        /** Slug → (display name, questions); slugs mirror McqChapters / questionGen.py:26-31. */
        val SAMPLE: Map<String, Pair<String, List<SampleQuestion>>> = linkedMapOf(
            "introduction_to_personal_finance" to ("Introduction to Personal Finance" to INTRO),
            "setting_financial_goals" to ("Setting Financial Goals" to generic("Goals")),
            "budgeting_and_expense_tracking" to ("Budgeting and Expense Tracking" to generic("Budgeting")),
            "online_and_mobile_banking" to ("Online and Mobile Banking" to generic("Banking")),
        )
    }
}

/** The practice list (list_of_modules.dart) and MCQ page constants (mcqpage.dart). */
object PracticeText {
    /** Tappable: opens MCQPage(index: position). */
    val FINISHED = listOf("Introduction to Personal Finance")

    /** Not tappable in practice: `onPressed: () => print("Print")`. */
    val UNFINISHED = listOf("Insurance and Risk Management", "Real Estate and Home Management")

    /** The app bar title is hard-coded for every chapter (mcqpage.dart:155) — kept. */
    const val APP_BAR_TITLE = "Chapter 1"

    /** FutureBuilder error text: "Error: ${snapshot.error}" (Dart Exception prefix). */
    fun errorText(e: McqApiException): String = "Error: Exception: ${e.message}"

    /**
     * `chapter.questions.values.elementAt(i)` on an empty chapter (executed
     * Dart 3.14): the empty case has its own wording.
     */
    fun elementAtError(index: Int, length: Int): String =
        if (length == 0) "RangeError (index): Index out of range: no indices are valid: $index"
        else "RangeError (index): Index out of range: index should be less than $length: $index"
}
