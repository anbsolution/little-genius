package com.learning.mylittleschool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val RoyalBlue = Color(0xFF2457D6)
private val Ink = Color(0xFF17233C)
private val SoftBlue = Color(0xFFEAF1FF)

data class SchoolClass(val id: String, val title: String, val subtitle: String)

private val schoolClasses = listOf(
    SchoolClass("pre_nursery", "Pre-Nursery", "First little steps"),
    SchoolClass("nursery", "Nursery", "Explore and discover"),
    SchoolClass("lkg", "LKG", "Learn the basics"),
    SchoolClass("ukg", "UKG", "Get school ready"),
    SchoolClass("class_1", "Class 1", "Build strong foundations"),
    SchoolClass("class_2", "Class 2", "Learn something new"),
    SchoolClass("class_3", "Class 3", "Grow your knowledge"),
    SchoolClass("class_4", "Class 4", "Explore more subjects"),
    SchoolClass("class_5", "Class 5", "Become a smart learner")
)

data class Subject(val name: String, val emoji: String, val tint: Color)

private val subjects = listOf(
    Subject("English", "📘", Color(0xFFEAF1FF)),
    Subject("Mathematics", "🔢", Color(0xFFFFEAF2)),
    Subject("Hindi", "अ", Color(0xFFFFF4D9)),
    Subject("EVS / Science", "🌱", Color(0xFFE7F8EC)),
    Subject("Computer", "💻", Color(0xFFF0EAFE)),
    Subject("Social Studies", "🌍", Color(0xFFE5F7FA))
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LittleGeniusApp() }
    }
}

@Composable
fun LittleGeniusApp() {
    var selectedClass by remember { mutableStateOf<SchoolClass?>(null) }
    var selectedTab by remember { mutableStateOf("Home") }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = RoyalBlue,
            background = Color(0xFFF7F9FE),
            surface = Color.White
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (selectedClass == null) {
                ClassSelectionScreen(onSelect = { selectedClass = it })
            } else {
                DashboardScreen(
                    schoolClass = selectedClass!!,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    onChangeClass = { selectedClass = null }
                )
            }
        }
    }
}

@Composable
fun ClassSelectionScreen(onSelect: (SchoolClass) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("LITTLE GENIUS", color = RoyalBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Choose your class", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("A happy place to learn, practise and grow.", color = Color.Gray)
            Spacer(Modifier.height(12.dp))
        }
        items(schoolClasses) { schoolClass ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(schoolClass) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(48.dp).background(SoftBlue, RoundedCornerShape(15.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("★", color = RoyalBlue, fontSize = 22.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(schoolClass.title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(schoolClass.subtitle, color = Color.Gray, fontSize = 12.sp)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = "Select class", tint = RoyalBlue)
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    schoolClass: SchoolClass,
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onChangeClass: () -> Unit
) {
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                listOf("Home", "Learn", "Practice", "Progress", "Profile").forEach { tab ->
                    val icon = when (tab) {
                        "Home" -> Icons.Default.Home
                        "Learn" -> Icons.Default.MenuBook
                        "Practice" -> Icons.Default.Extension
                        "Progress" -> Icons.Default.BarChart
                        else -> Icons.Default.Person
                    }
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        icon = { Icon(icon, contentDescription = tab) },
                        label = { Text(tab, fontSize = 10.sp) }
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            "Home", "Learn" -> HomeContent(schoolClass, padding, onChangeClass)
            "Practice" -> SimpleContent("Practice Zone", "Quizzes, puzzles and worksheets will appear here.", padding)
            "Progress" -> SimpleContent("My Progress", "Your completed lessons and learning reports will appear here.", padding)
            else -> SimpleContent("Student Profile", "Parent controls and profile settings.", padding, onChangeClass)
        }
    }
}

@Composable
fun HomeContent(schoolClass: SchoolClass, padding: PaddingValues, onChangeClass: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("LITTLE GENIUS", color = RoyalBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Hello, Student! 👋", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text(schoolClass.title, color = Color.Gray)
                }
                IconButton(onClick = onChangeClass) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Change class", tint = RoyalBlue)
                }
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = RoyalBlue)
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("YOUR LEARNING JOURNEY", color = Color(0xFFDCE7FF), fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Ready to learn?", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text("Choose a subject and start exploring.", color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    Text("✨ Learn something wonderful today!", color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }
        item {
            Text("Explore subjects", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        items(subjects.chunked(2)) { rowSubjects ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowSubjects.forEach { subject ->
                    Card(
                        modifier = Modifier.weight(1f).height(112.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = subject.tint)
                    ) {
                        Column(
                            Modifier.fillMaxSize().padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(subject.emoji, fontSize = 25.sp)
                            Text(subject.name, color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
                if (rowSubjects.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        item {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🎧", fontSize = 25.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Voice learning", color = Ink, fontWeight = FontWeight.Bold)
                        Text("Listen, speak and practise.", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleContent(title: String, description: String, padding: PaddingValues, onChangeClass: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxSize().padding(padding).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🌟", fontSize = 44.sp)
        Spacer(Modifier.height(12.dp))
        Text(title, color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(description, color = Color.Gray)
        if (onChangeClass != null) {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onChangeClass) { Text("Change class") }
        }
    }
}
