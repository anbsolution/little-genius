package com.learning.mylittleschool

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ParentBlue = Color(0xFF2457D6)
private val ParentInk = Color(0xFF17233C)

@Composable
fun ParentDashboardScreen(
    store: ProgressStore,
    padding: PaddingValues
) {
    var unlocked by remember { mutableStateOf(false) }
    var setupMode by remember { mutableStateOf(!store.hasParentPin()) }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    if (!unlocked) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Parent Dashboard",
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                color = ParentInk
            )
            Spacer(Modifier.height(8.dp))

            Text(
                if (setupMode) "Create your parent PIN"
                else "Enter your parent PIN",
                color = Color.Gray
            )

            Spacer(Modifier.height(18.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = {
                    if (it.length <= 6 && it.all(Char::isDigit)) {
                        pin = it
                        error = ""
                    }
                },
                label = { Text("4–6 digit PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (setupMode) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = {
                        if (it.length <= 6 && it.all(Char::isDigit)) {
                            confirmPin = it
                            error = ""
                        }
                    },
                    label = { Text("Confirm PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (error.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(error, color = Color(0xFFB3261E))
            }

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = {
                    if (setupMode) {
                        if (pin.length !in 4..6) {
                            error = "PIN must contain 4 to 6 digits"
                        } else if (pin != confirmPin) {
                            error = "PINs do not match"
                        } else {
                            store.setParentPin(pin)
                            unlocked = true
                            setupMode = false
                            pin = ""
                            confirmPin = ""
                        }
                    } else {
                        if (store.verifyParentPin(pin)) {
                            unlocked = true
                            pin = ""
                            error = ""
                        } else {
                            error = "Incorrect PIN"
                            pin = ""
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (setupMode) "Set Parent PIN" else "Unlock Dashboard")
            }
        }
        return
    }

    val weekly = store.weeklyProgress()
    val totalAttempts = weekly.sumOf { it.attempts }
    val totalCorrect = weekly.sumOf { it.correct }
    val accuracy = if (totalAttempts == 0) 0
        else totalCorrect * 100 / totalAttempts
    val studyWeekly = store.weeklyStudyProgress()
    val todayStudyDone = store.studyTasksDoneCount()
    val weeklyStudyDone = studyWeekly.sumOf { it.completed }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Parent Dashboard",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    color = ParentInk
                )
                Text("Offline learning overview", color = Color.Gray)
            }

            TextButton(onClick = { unlocked = false }) {
                Text("Lock")
            }
        }

        ParentStat("Lessons completed", store.completedCount().toString())
        ParentStat("Total quiz attempts", store.attempts().toString())
        ParentStat("Correct answers", store.correctAnswers().toString())
        ParentStat("Last 7 days accuracy", "$accuracy%")

        Text(
            "Weekly Learning Report",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ParentInk
        )

        weekly.forEach { day ->
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(day.day, fontWeight = FontWeight.SemiBold)
                    Text("${day.correct}/${day.attempts} correct")
                }

                val fraction = if (day.attempts == 0) 0f
                    else day.correct.toFloat() / day.attempts.toFloat()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .background(
                            Color(0xFFE5EAF3),
                            RoundedCornerShape(10.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .background(
                                ParentBlue,
                                RoundedCornerShape(10.dp)
                            )
                    )
                }
            }
        }

        Text(
            "Weekly Study Planner Report",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ParentInk
        )

        ParentStat("Today's tasks completed", "$todayStudyDone / 5")
        ParentStat("Tasks completed in 7 days", weeklyStudyDone.toString())

        studyWeekly.forEach { day ->
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(day.day, fontWeight = FontWeight.SemiBold)
                    Text("${day.completed}/5 completed")
                }

                val fraction = day.completed.coerceIn(0, 5).toFloat() / 5f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .background(Color(0xFFE5EAF3), RoundedCornerShape(10.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .background(ParentBlue, RoundedCornerShape(10.dp))
                    )
                }
            }
        }

        Text(
            "Suggested Daily Study Timetable",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ParentInk
        )

        val schedule = listOf(
            "4:00 PM" to "English reading",
            "4:20 PM" to "Mathematics practice",
            "4:40 PM" to "Hindi learning",
            "5:00 PM" to "EVS / Science",
            "5:20 PM" to "Revision and worksheet"
        )

        schedule.forEach { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        item.first,
                        color = ParentBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(item.second, color = ParentInk)
                }
            }
        }

        Text(
            "Timetable is a suggested template. Adjust study times to your family's routine.",
            color = Color.Gray,
            fontSize = 12.sp
        )

        Text(
            "Reports include attempts recorded after this update. Older attempts are not assigned historical dates.",
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ParentStat(title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                modifier = Modifier.weight(1f),
                color = ParentInk,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                value,
                color = ParentBlue,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
