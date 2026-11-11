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

private val WorksheetBlue = Color(0xFF2457D6)
private val WorksheetInk = Color(0xFF17233C)

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "WORKSHEET CENTER",
            color = WorksheetBlue,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Practice and test yourself",
            color = WorksheetInk,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )

        Text("${schoolClass.title} • Subject-wise worksheets", color = Color.Gray)

        if (selectedSubject == null) {
            Text("Choose a subject", fontWeight = FontWeight.Bold, fontSize = 18.sp)

            availableSubjects.forEach { subject ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    TextButton(
                        onClick = {
                            selectedSubject = subject
                            questionIndex = 0
                            selectedAnswer = null
                            score = 0
                            finished = false
                        },
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
                    selectedSubject = null
                    questionIndex = 0
                    selectedAnswer = null
                    score = 0
                    finished = false
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
                        Text("Worksheet completed! ⭐", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "$score / ${questions.size}",
                            color = WorksheetBlue,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Correct answers", color = WorksheetInk)

                        Button(
                            onClick = {
                                questionIndex = 0
                                selectedAnswer = null
                                score = 0
                                finished = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Try again")
                        }

                        OutlinedButton(
                            onClick = {
                                selectedSubject = null
                                questionIndex = 0
                                selectedAnswer = null
                                score = 0
                                finished = false
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
                                    if (correct) score++
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
            "Results are saved on this device. Available starter lessons determine worksheet length.",
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}
