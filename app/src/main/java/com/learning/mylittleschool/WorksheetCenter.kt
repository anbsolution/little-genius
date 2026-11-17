package com.learning.mylittleschool

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

private val WorksheetBlue = Color(0xFF2457D6)
private val WorksheetInk = Color(0xFF17233C)

internal object WorksheetTimer {
    fun durationSeconds(questionCount: Int): Int = (questionCount.coerceAtLeast(1) * 60).coerceIn(180, 600)

    fun formatRemaining(seconds: Int): String = String.format(
        Locale.ENGLISH,
        "%02d:%02d",
        seconds.coerceAtLeast(0) / 60,
        seconds.coerceAtLeast(0) % 60
    )
}

@Composable
fun WorksheetCenterScreen(
    schoolClass: SchoolClass,
    padding: PaddingValues,
    store: ProgressStore
) {
    var selectedSubject by remember { mutableStateOf<String?>(null) }
    var questionIndex by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }
    var answerSubmitted by remember { mutableStateOf(false) }
    var lastAnswerCorrect by remember { mutableStateOf(false) }
    var timedTestMode by remember { mutableStateOf(false) }
    var timedTestRunning by remember { mutableStateOf(false) }
    var timedOut by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableIntStateOf(0) }
    val missedQuestions = remember { mutableStateListOf<LearningLesson>() }
    val answeredQuestionIds = remember { mutableStateListOf<String>() }

    val availableSubjects = remember(schoolClass.id) {
        listOf("English", "Mathematics", "Hindi", "EVS / Science", "Computer", "Social Studies")
            .filter { subject ->
                lessonsFor(schoolClass.id, subject)
                    .any { !it.title.startsWith("Getting Started:") }
            }
    }

    val questions = remember(schoolClass.id, selectedSubject) {
        selectedSubject?.let { subject ->
            lessonsFor(schoolClass.id, subject)
                .filter { !it.title.startsWith("Getting Started:") }
        } ?: emptyList()
    }

    fun startWorksheet(subject: String) {
        val questionCount = lessonsFor(schoolClass.id, subject)
            .count { !it.title.startsWith("Getting Started:") }
        selectedSubject = subject
        questionIndex = 0
        selectedAnswer = null
        score = 0
        finished = false
        answerSubmitted = false
        timedOut = false
        missedQuestions.clear()
        answeredQuestionIds.clear()
        remainingSeconds = if (timedTestMode) WorksheetTimer.durationSeconds(questionCount) else 0
        timedTestRunning = timedTestMode
    }

    LaunchedEffect(timedTestRunning) {
        if (!timedTestRunning) return@LaunchedEffect
        while (remainingSeconds > 0) {
            delay(1_000L)
            val next = (remainingSeconds - 1).coerceAtLeast(0)
            remainingSeconds = next
            if (next == 0) {
                timedOut = true
                finished = true
                answerSubmitted = false
                questions.drop(questionIndex).forEach { unanswered ->
                    if (unanswered.id !in answeredQuestionIds && missedQuestions.none { it.id == unanswered.id }) {
                        missedQuestions.add(unanswered)
                    }
                }
                timedTestRunning = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PageHeading(
            "WORKSHEET CENTER",
            "Practice and test yourself",
            "${schoolClass.title} · Subject-wise worksheets"
        )

        if (selectedSubject == null) {
            Text("Choose a subject", fontWeight = FontWeight.Bold, fontSize = 18.sp)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Timed test / समयबद्ध टेस्ट", color = WorksheetInk, fontWeight = FontWeight.Bold)
                        Text(
                            if (timedTestMode) "One minute per question · minimum 3 minutes"
                            else "Practice at your own pace / अपनी गति से अभ्यास करें",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    Switch(checked = timedTestMode, onCheckedChange = { timedTestMode = it })
                }
            }

            availableSubjects.forEach { subject ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    TextButton(
                        onClick = { startWorksheet(subject) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            subject,
                            modifier = Modifier.weight(1f),
                            color = WorksheetInk
                        )
                        Text(
                            "${lessonsFor(schoolClass.id, subject).count {
                                !it.title.startsWith("Getting Started:")
                            }} questions",
                            color = WorksheetBlue
                        )
                    }
                }
            }

            if (availableSubjects.isEmpty()) {
                Text("More worksheets are coming soon.", color = Color.Gray)
            }
        } else {
            TextButton(
                onClick = {
                    timedTestRunning = false
                    selectedSubject = null
                    questionIndex = 0
                    selectedAnswer = null
                    score = 0
                    finished = false
                    answerSubmitted = false
                    missedQuestions.clear()
                    answeredQuestionIds.clear()
                }
            ) {
                Text("← Change subject")
            }

            Text(
                selectedSubject!!,
                color = WorksheetInk,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            if (finished) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFEAF1FF)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            if (timedOut) "Time is up! / समय पूरा हुआ!" else "Worksheet completed! ⭐",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "$score / ${questions.size}",
                            color = WorksheetBlue,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Correct answers", color = WorksheetInk)
                        if (timedTestMode) {
                            Text(
                                "Timed test · ${WorksheetTimer.formatRemaining(remainingSeconds)} remaining",
                                color = Color.Gray
                            )
                        }

                        if (missedQuestions.isNotEmpty()) {
                            Text("Review answers / उत्तर समझें", color = WorksheetInk, fontWeight = FontWeight.Bold)
                            missedQuestions.forEach { missed ->
                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(missed.question, color = WorksheetInk, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Correct answer: ${missed.options.getOrNull(missed.correctIndex).orEmpty()}",
                                        color = Color(0xFF2E7D32),
                                        fontSize = 13.sp
                                    )
                                    Text(missed.explanation, color = WorksheetInk, fontSize = 13.sp)
                                }
                            }
                        }

                        Button(
                            onClick = {
                                startWorksheet(selectedSubject!!)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Try again")
                        }

                        OutlinedButton(
                            onClick = {
                                timedTestRunning = false
                                selectedSubject = null
                                questionIndex = 0
                                selectedAnswer = null
                                score = 0
                                finished = false
                                missedQuestions.clear()
                                answeredQuestionIds.clear()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Choose another subject")
                        }
                    }
                }
            } else if (questions.isNotEmpty()) {
                val question = questions[questionIndex]

                LinearProgressIndicator(
                    progress = {
                        (questionIndex + 1).toFloat() / questions.size.toFloat()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = WorksheetBlue
                )

                Text(
                    "Question ${questionIndex + 1} of ${questions.size}",
                    color = WorksheetBlue,
                    fontWeight = FontWeight.Bold
                )
                if (timedTestMode) {
                    Text(
                        "Time left · ${WorksheetTimer.formatRemaining(remainingSeconds)}",
                        color = if (remainingSeconds <= 60) Color(0xFFB3261E) else WorksheetBlue,
                        fontWeight = FontWeight.Bold
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(question.title, fontWeight = FontWeight.Bold, color = WorksheetInk)
                        Text(question.question, color = WorksheetInk, fontSize = 17.sp)

                        question.options.forEachIndexed { index, option ->
                            OutlinedButton(
                                onClick = { if (!answerSubmitted) selectedAnswer = index },
                                enabled = !answerSubmitted,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selectedAnswer == index)
                                        Color(0xFFEAF1FF) else Color.Transparent
                                )
                            ) {
                                Text(option, modifier = Modifier.fillMaxWidth(), color = WorksheetInk)
                            }
                        }

                        if (answerSubmitted) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (lastAnswerCorrect)
                                        Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        if (lastAnswerCorrect) "Correct! ✓"
                                        else "Try again next time",
                                        color = if (lastAnswerCorrect)
                                            Color(0xFF2E7D32) else Color(0xFFB45309),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Text(question.explanation, color = WorksheetInk)
                                }
                            }

                            Button(
                                onClick = {
                                    answerSubmitted = false
                                    selectedAnswer = null
                                    if (questionIndex + 1 < questions.size) {
                                        questionIndex++
                                    } else {
                                        finished = true
                                        timedTestRunning = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (questionIndex + 1 < questions.size) "Continue" else "View result")
                            }
                        } else {
                            Button(
                                onClick = {
                                    val correct = selectedAnswer == question.correctIndex
                                    lastAnswerCorrect = correct
                                    store.recordLesson(question.id, correct)
                                    if (question.id !in answeredQuestionIds) answeredQuestionIds.add(question.id)
                                    if (correct) score++
                                    else if (missedQuestions.none { it.id == question.id }) missedQuestions.add(question)
                                    answerSubmitted = true
                                },
                                enabled = selectedAnswer != null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Submit answer")
                            }
                        }
                    }
                }
            }
        }

        Text(
            "Each answer is saved on this device. The worksheet score is shown at the end; available starter lessons determine its length.",
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}
