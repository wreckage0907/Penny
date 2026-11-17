package com.penny.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.penny.app.PennyServices
import com.penny.app.core.McqApiException
import com.penny.app.core.McqChapter
import com.penny.app.core.McqChapters
import com.penny.app.core.McqOptionState
import com.penny.app.core.McqState
import com.penny.app.core.PracticeText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PracticeBar(title: String, tag: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = PennyPalette.text,
            modifier = Modifier.testTag(tag)) },
        navigationIcon = {
            IconButton(onClick = onBack, modifier = Modifier.testTag("back")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = PennyPalette.text)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = PennyPalette.background),
    )
}

/** PracticeList (list_of_modules.dart). */
@Composable
fun PracticeListScreen(onBack: () -> Unit, openChapter: (Int) -> Unit) {
    Scaffold(containerColor = PennyPalette.background, topBar = { PracticeBar("Practice", "practice-title", onBack) }) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(15.dp)
            .testTag("practice-page")) {
            Text("List of Modules", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = PennyPalette.text)
            PracticeText.FINISHED.forEachIndexed { i, title ->
                ModuleRow(title, PennyPalette.button, PennyPalette.background, "practice-finished-$i", chevron = true) {
                    openChapter(i)
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Color.Black.copy(alpha = 0.12f), thickness = 2.dp)
            PracticeText.UNFINISHED.forEachIndexed { i, title ->
                // onPressed: () => print("Print") — tappable but does nothing.
                ModuleRow(title, PennyPalette.card, PennyPalette.text, "practice-unfinished-$i", chevron = false) {}
            }
        }
    }
}

@Composable
private fun ModuleRow(title: String, background: Color, content: Color, tag: String, chevron: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.padding(vertical = 8.dp, horizontal = 4.dp).fillMaxWidth().height(55.dp)
            .shadow(3.dp, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)).background(background)
            .clickable(onClick = onClick).padding(12.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Medium, color = content,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (chevron) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(18.dp), tint = content)
    }
}

/** MCQPage (mcqpage.dart): FutureBuilder over the fetch, then the question page. */
@Composable
fun McqScreen(services: PennyServices, index: Int, onBack: () -> Unit) {
    var chapter by remember { mutableStateOf<McqChapter?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(index) {
        val slug = McqChapters.slug(index)
        if (slug == null) {
            // `late String chapterName = things[index]` → TypeError in initState.
            error = "type 'Null' is not a subtype of type 'String'"
            return@LaunchedEffect
        }
        try {
            chapter = services.mcq.fetchQuestions(slug)
        } catch (e: McqApiException) {
            error = PracticeText.errorText(e)
        }
    }
    val loaded = chapter
    when {
        error != null -> Box(Modifier.fillMaxSize().background(PennyPalette.background).testTag("mcq-error"), Alignment.Center) {
            Text(error!!, Modifier.padding(16.dp).testTag("mcq-error-text"))
        }
        loaded == null -> Box(Modifier.fillMaxSize().background(PennyPalette.background).testTag("mcq-loading"), Alignment.Center) {
            CircularProgressIndicator(color = PennyPalette.text)
        }
        loaded.questions.isEmpty() -> Box(Modifier.fillMaxSize().testTag("mcq-error"), Alignment.Center) {
            // values.elementAt(0) on an empty chapter: RangeError during build.
            Text(PracticeText.elementAtError(0, 0), Modifier.padding(16.dp).testTag("mcq-error-text"))
        }
        else -> QuestionPage(loaded, onBack)
    }
}

@Composable
private fun QuestionPage(chapter: McqChapter, onBack: () -> Unit) {
    var state by remember { mutableStateOf(McqState()) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val questions = chapter.questions.values.toList()
    val length = questions.size
    val current = questions[state.index]
    Scaffold(
        containerColor = PennyPalette.background,
        topBar = { PracticeBar(PracticeText.APP_BAR_TITLE, "mcq-title", onBack) },
        snackbarHost = { SnackbarHost(snackbar, Modifier.testTag("mcq-hint-toast")) },
        bottomBar = {
            Button(
                onClick = { state = state.next(length) },
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp).testTag("mcq-next"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isChecked) PennyPalette.button else PennyPalette.background,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Next Question", fontSize = 18.sp,
                    color = if (state.isChecked) PennyPalette.background else PennyPalette.text)
            }
        },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).testTag("mcq-page")) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(McqState.progressLabel(state.index, length), color = Color.Black, fontSize = 16.sp,
                    modifier = Modifier.testTag("mcq-progress-label"))
                Spacer(Modifier.width(8.dp))
                LinearProgressIndicator(
                    progress = { McqState.progress(state.index, length).toFloat() },
                    modifier = Modifier.weight(1f).height(15.dp).clip(RoundedCornerShape(20.dp)).testTag("mcq-progress"),
                    color = PennyPalette.button.copy(alpha = 0.7f),
                    trackColor = PennyPalette.card.copy(alpha = 0.6f),
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
                Card(colors = CardDefaults.cardColors(containerColor = PennyPalette.background),
                    elevation = CardDefaults.cardElevation(1.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(McqState.questionLabel(state.index, length), Modifier.weight(1f), fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold, color = PennyPalette.text,
                                )
                            Row(
                                Modifier.clickable { scope.launch { snackbar.showSnackbar(current.hint) } }.testTag("mcq-hint"),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.AutoMirrored.Filled.HelpOutline, null, Modifier.size(16.dp), tint = PennyPalette.text)
                                Spacer(Modifier.width(5.dp))
                                Text("Hint", fontSize = 16.sp, color = PennyPalette.text)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(current.question, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = PennyPalette.text,
                            modifier = Modifier.testTag("mcq-question"))
                        Spacer(Modifier.height(16.dp))
                        current.options.forEachIndexed { i, option ->
                            val letter = McqState.optionLetter(i).toString()
                            OptionButton(i, option, McqState.optionState(state.selectedAnswer, letter, current.answer, state.isChecked),
                                enabled = !state.isChecked) { state = state.select(letter) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionButton(i: Int, option: String, look: McqOptionState, enabled: Boolean, onTap: () -> Unit) {
    val (bg, border) = when (look) {
        McqOptionState.CORRECT -> Color(0xFFC8E6C9) to Color(0xFF4CAF50) // green.shade100 / green
        McqOptionState.WRONG_SELECTED -> Color(0xFFFFCDD2) to Color(0xFFF44336) // red.shade100 / red
        McqOptionState.NEUTRAL -> PennyPalette.background to Color(0xFFE0E0E0) // grey.shade300
    }
    Row(
        Modifier.padding(vertical = 4.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(bg)
            .border(1.dp, border, RoundedCornerShape(8.dp)).clickable(enabled = enabled, onClick = onTap)
            .padding(12.dp).testTag("mcq-option-$i"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Options already carry "A) " from the backend — never prefix again.
        Text(option, Modifier.weight(1f), fontSize = 16.sp, color = PennyPalette.text)
        when (look) {
            McqOptionState.CORRECT -> Icon(Icons.Filled.CheckCircle, "Correct", tint = Color(0xFF4CAF50),
                modifier = Modifier.testTag("mcq-correct-$i"))
            McqOptionState.WRONG_SELECTED -> Icon(Icons.Filled.Cancel, "Wrong", tint = Color(0xFFF44336),
                modifier = Modifier.testTag("mcq-wrong-$i"))
            McqOptionState.NEUTRAL -> Unit
        }
    }
}
