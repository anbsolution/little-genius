package com.learning.mylittleschool

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private data class StudyTask(
    val id: String,
    val time: String,
    val title: String,
    val detail: String
)

private val dailyStudyTasks = listOf(
    StudyTask("reading", "4:00 PM", "English reading", "Read a short story or lesson"),
    StudyTask("math", "4:20 PM", "Mathematics", "Practise numbers and problems"),
    StudyTask("hindi", "4:40 PM", "Hindi learning", "Read words and sentences"),
    StudyTask("science", "5:00 PM", "EVS / Science", "Explore nature and science"),
    StudyTask("revision", "5:20 PM", "Daily revision", "Review what you learned today")
)

@Composable
fun DailyStudyPlannerScreen(store: ProgressStore, padding: PaddingValues) {
    var refresh by remember { mutableIntStateOf(0) }
    val doneCount = dailyStudyTasks.count { store.isStudyTaskDone(it.id) }
    val dateText = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy"))
    val progress = doneCount.toFloat() / dailyStudyTasks.size.toFloat()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("DAILY STUDY PLAN", color = Color(0xFF2457D6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Today's timetable", color = Color(0xFF17233C), fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text(dateText, color = Color.Gray)
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF1FF))
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Daily progress", color = Color(0xFF17233C), fontWeight = FontWeight.Bold)
                    Text("$doneCount of ${dailyStudyTasks.size} tasks completed", color = Color(0xFF2457D6), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF2457D6),
                        trackColor = Color.White
                    )
                }
            }
        }
        items(dailyStudyTasks.size) { index ->
            val task = dailyStudyTasks[index]
            val checked = remember(refresh, task.id) { store.isStudyTaskDone(task.id) }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = {
                            store.setStudyTaskDone(task.id, it)
                            refresh++
                        }
                    )
                    Column(Modifier.weight(1f)) {
                        Text(task.time, color = Color(0xFF2457D6), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(task.title, color = Color(0xFF17233C), fontWeight = FontWeight.SemiBold)
                        Text(task.detail, color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }
        item {
            Text(
                "This is a suggested timetable. Parents can adjust the times to suit the child's daily routine.",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}
