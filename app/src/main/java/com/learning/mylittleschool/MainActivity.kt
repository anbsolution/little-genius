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
data class Subject(val name: String, val emoji: String, val tint: Color)

private val schoolClasses = listOf(
    SchoolClass("pre_nursery","Pre-Nursery","First little steps"), SchoolClass("nursery","Nursery","Explore and discover"),
    SchoolClass("lkg","LKG","Learn the basics"), SchoolClass("ukg","UKG","Get school ready"),
    SchoolClass("class_1","Class 1","Build strong foundations"), SchoolClass("class_2","Class 2","Learn something new"),
    SchoolClass("class_3","Class 3","Grow your knowledge"), SchoolClass("class_4","Class 4","Explore more subjects"),
    SchoolClass("class_5","Class 5","Become a smart learner")
)
private val subjects = listOf(
    Subject("English","📘",Color(0xFFEAF1FF)), Subject("Mathematics","🔢",Color(0xFFFFEAF2)),
    Subject("Hindi","अ",Color(0xFFFFF4D9)), Subject("EVS / Science","🌱",Color(0xFFE7F8EC)),
    Subject("Computer","💻",Color(0xFFF0EAFE)), Subject("Social Studies","🌍",Color(0xFFE5F7FA))
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val progressStore = ProgressStore(applicationContext)
        setContent { LittleGeniusApp(progressStore) }
    }
}

@Composable
fun LittleGeniusApp(progressStore: ProgressStore) {
    var selectedClass by remember { mutableStateOf<SchoolClass?>(null) }
    var selectedTab by remember { mutableStateOf("Home") }
    var selectedSubject by remember { mutableStateOf<String?>(null) }
    var selectedLesson by remember { mutableStateOf<LearningLesson?>(null) }
    var refreshProgress by remember { mutableIntStateOf(0) }
    MaterialTheme(colorScheme = lightColorScheme(primary=RoyalBlue,background=Color(0xFFF7F9FE),surface=Color.White)) {
        Surface(Modifier.fillMaxSize(), color=MaterialTheme.colorScheme.background) {
            when {
                selectedClass == null -> ClassSelectionScreen { selectedClass = it }
                selectedLesson != null -> LessonScreen(selectedLesson!!, onBack={selectedLesson=null}, onResult={correct -> progressStore.recordLesson(selectedLesson!!.id,correct); refreshProgress++})
                selectedSubject != null -> ChapterScreen(selectedClass!!,selectedSubject!!,{selectedSubject=null},{selectedLesson=it},progressStore,selectedTab=="Practice")
                else -> DashboardScreen(selectedClass!!,selectedTab,{selectedTab=it},{selectedClass=null;selectedSubject=null},{selectedSubject=it},progressStore,refreshProgress)
            }
        }
    }
}

@Composable fun ClassSelectionScreen(onSelect:(SchoolClass)->Unit) {
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { Text("LITTLE GENIUS",color=RoyalBlue,fontSize=12.sp,fontWeight=FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text("Choose your class",color=Ink,fontSize=28.sp,fontWeight=FontWeight.Bold); Text("A happy place to learn, practise and grow.",color=Color.Gray); Spacer(Modifier.height(12.dp)) }
        items(schoolClasses) { item -> Card(Modifier.fillMaxWidth().clickable { onSelect(item) },shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) { Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically) { Box(Modifier.size(48.dp).background(SoftBlue,RoundedCornerShape(15.dp)),contentAlignment=Alignment.Center) { Text("★",color=RoyalBlue,fontSize=22.sp) }; Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(item.title,color=Ink,fontWeight=FontWeight.Bold,fontSize=17.sp); Text(item.subtitle,color=Color.Gray,fontSize=12.sp) }; Icon(Icons.Default.ChevronRight,contentDescription="Select class",tint=RoyalBlue) } } }
    }
}

@Composable fun DashboardScreen(schoolClass:SchoolClass,selectedTab:String,onTabSelected:(String)->Unit,onChangeClass:()->Unit,onSubjectSelected:(String)->Unit,store:ProgressStore,refresh:Int) {
    Scaffold(bottomBar={ NavigationBar(containerColor=Color.White) { listOf("Home","Learn","Practice","Worksheet","Study","Progress","Profile").forEach { tab -> val icon=when(tab){"Home"->Icons.Default.Home;"Learn"->Icons.Default.MenuBook;"Practice"->Icons.Default.Extension;"Worksheet"->Icons.Default.Assignment;"Study"->Icons.Default.CalendarToday;"Progress"->Icons.Default.BarChart;else->Icons.Default.Person}; NavigationBarItem(selected=selectedTab==tab,onClick={onTabSelected(tab)},icon={Icon(icon,contentDescription=tab)},label={Text(tab,fontSize=10.sp)}) } } }) { padding ->
        when(selectedTab) {
            "Home","Learn" -> HomeContent(schoolClass,padding,onChangeClass,onSubjectSelected)
            "Practice" -> PracticeScreen(schoolClass,padding,onSubjectSelected)
            "Worksheet" -> WorksheetCenterScreen(schoolClass,padding,store)
            "Study" -> DailyStudyPlannerScreen(store,padding)
            "Progress" -> { val ignored=refresh; ProgressScreen(store,padding) }
            else -> ParentDashboardScreen(store,padding)
        }
    }
}

@Composable fun HomeContent(schoolClass:SchoolClass,padding:PaddingValues,onChangeClass:()->Unit,onSubjectSelected:(String)->Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item { Row(verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("LITTLE GENIUS",color=RoyalBlue,fontSize=11.sp,fontWeight=FontWeight.Bold); Text("Hello, Student! 👋",color=Ink,fontSize=25.sp,fontWeight=FontWeight.Bold); Text(schoolClass.title,color=Color.Gray) }; IconButton(onClick=onChangeClass) { Icon(Icons.Default.SwapHoriz,contentDescription="Change class",tint=RoyalBlue) } } }
        item { Card(shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=RoyalBlue)) { Column(Modifier.fillMaxWidth().padding(20.dp)) { Text("YOUR LEARNING JOURNEY",color=Color(0xFFDCE7FF),fontSize=11.sp); Spacer(Modifier.height(8.dp)); Text("Ready to learn?",color=Color.White,fontSize=25.sp,fontWeight=FontWeight.Bold); Text("Choose a subject and start exploring.",color=Color.White) } } }
        item { Text("Explore subjects",color=Ink,fontSize=20.sp,fontWeight=FontWeight.Bold) }
        items(subjects.chunked(2)) { pair -> Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) { pair.forEach { subject -> Card(Modifier.weight(1f).height(112.dp).clickable { onSubjectSelected(subject.name) },shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=subject.tint)) { Column(Modifier.fillMaxSize().padding(14.dp),verticalArrangement=Arrangement.SpaceBetween) { Text(subject.emoji,fontSize=25.sp); Text(subject.name,color=Ink,fontWeight=FontWeight.Bold,fontSize=13.sp) } } }; if(pair.size==1) Spacer(Modifier.weight(1f)) } }
    }
}

@Composable fun PracticeScreen(schoolClass:SchoolClass,padding:PaddingValues,onSubjectSelected:(String)->Unit) {
    val availableSubjects = remember(schoolClass.id) {
        subjects.filter { subject ->
            lessonsFor(schoolClass.id,subject.name).any { !it.title.startsWith("Getting Started:") }
        }
    }
    LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Text("PRACTICE ZONE",color=RoyalBlue,fontSize=12.sp,fontWeight=FontWeight.Bold)
            Text("Let's practise! ⭐",color=Ink,fontSize=27.sp,fontWeight=FontWeight.Bold)
            Text("${schoolClass.title} • Choose a subject to start a quiz.",color=Color.Gray)
        }
        if (availableSubjects.isEmpty()) {
            item { Text("More practice sets are coming soon.",color=Color.Gray) }
        } else {
            items(availableSubjects) { subject ->
                val count = lessonsFor(schoolClass.id,subject.name).count { !it.title.startsWith("Getting Started:") }
                Card(Modifier.fillMaxWidth().clickable { onSubjectSelected(subject.name) },shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) {
                    Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                        Text(subject.emoji,fontSize=28.sp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(subject.name,color=Ink,fontWeight=FontWeight.Bold)
                            Text("$count practice activities",color=Color.Gray,fontSize=12.sp)
                        }
                        Icon(Icons.Default.ChevronRight,contentDescription="Open practice",tint=RoyalBlue)
                    }
                }
            }
        }
        item { Text("Your quiz results are saved on this device and appear in Progress.",color=Color.Gray,fontSize=12.sp) }
    }
}

@Composable fun ChapterScreen(schoolClass:SchoolClass,subject:String,onBack:()->Unit,onLessonSelected:(LearningLesson)->Unit,store:ProgressStore,practiceMode:Boolean=false) {
    val lessons=remember(schoolClass.id,subject) { lessonsFor(schoolClass.id,subject) }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        TextButton(onClick=onBack) { Icon(Icons.Default.ArrowBack,contentDescription=null); Text(" Back") }
        Text(subject,color=Ink,fontSize=27.sp,fontWeight=FontWeight.Bold)
        Text(if(practiceMode) "${schoolClass.title} • Practice quiz" else "${schoolClass.title} • Learning activities",color=Color.Gray)
        Spacer(Modifier.height(16.dp))
        lessons.filter { !it.title.startsWith("Getting Started:") }.forEach { lesson ->
            Card(Modifier.fillMaxWidth().clickable { onLessonSelected(lesson) },shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) {
                Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text(if(practiceMode) "✏️" else "📖",fontSize=28.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(lesson.title,color=Ink,fontWeight=FontWeight.Bold)
                        Text(if(practiceMode) "Start practice quiz" else "Lesson and quick quiz",color=Color.Gray,fontSize=12.sp)
                    }
                    Icon(Icons.Default.ChevronRight,contentDescription="Open lesson",tint=RoyalBlue)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Text("Starter examples only. Full class-wise syllabus content requires review.",color=Color.Gray,fontSize=12.sp)
    }
}

@Composable fun LessonScreen(lesson:LearningLesson,onBack:()->Unit,onResult:(Boolean)->Unit) {
    var language by remember { mutableStateOf("English") }; var selectedAnswer by remember(lesson.id) { mutableStateOf<Int?>(null) }; var submitted by remember(lesson.id) { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(20.dp)) { TextButton(onClick=onBack) { Icon(Icons.Default.ArrowBack,contentDescription=null); Text(" Back to lessons") }; Text(lesson.subject.uppercase(),color=RoyalBlue,fontSize=11.sp,fontWeight=FontWeight.Bold); Text(lesson.title,color=Ink,fontSize=26.sp,fontWeight=FontWeight.Bold); Spacer(Modifier.height(14.dp)); SingleChoiceSegmentedButtonRow { listOf("English","Hindi").forEachIndexed { index,value -> SegmentedButton(selected=language==value,onClick={language=value},shape=SegmentedButtonDefaults.itemShape(index=index,count=2)) { Text(value) } } }; Spacer(Modifier.height(14.dp)); Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) { Text(if(language=="English") lesson.english else lesson.hindi,Modifier.padding(18.dp),color=Ink,fontSize=17.sp,lineHeight=25.sp) }; Spacer(Modifier.height(12.dp)); LessonVoiceControls(text=if(language=="English") lesson.english else lesson.hindi,language=language); Spacer(Modifier.height(22.dp)); Text("Quick check",color=Ink,fontSize=20.sp,fontWeight=FontWeight.Bold); Text(lesson.question,color=Ink,fontSize=16.sp); Spacer(Modifier.height(10.dp)); lesson.options.forEachIndexed { index,option -> OutlinedButton(onClick={if(!submitted) selectedAnswer=index},modifier=Modifier.fillMaxWidth().padding(vertical=4.dp),colors=ButtonDefaults.outlinedButtonColors(containerColor=if(selectedAnswer==index) SoftBlue else Color.Transparent,contentColor=Ink)) { Text(option,Modifier.fillMaxWidth()) } }; Button(onClick={val correct=selectedAnswer==lesson.correctIndex; onResult(correct); submitted=true},enabled=selectedAnswer!=null&&!submitted,modifier=Modifier.fillMaxWidth()) { Text("Check answer") }; if(submitted) { val correct=selectedAnswer==lesson.correctIndex; Text(if(correct) "Correct! Well done. ⭐" else "Not quite. Read the explanation.",color=if(correct) Color(0xFF16803C) else Color(0xFFB45309),fontWeight=FontWeight.Bold); Text(lesson.explanation,color=Ink); TextButton(onClick={selectedAnswer=null;submitted=false}) { Text("Try again") } } }
}

@Composable fun ProgressScreen(store:ProgressStore,padding:PaddingValues) {
    val completed = store.completedCount()
    val attempts = store.attempts()
    val correct = store.correctAnswers()
    val accuracy = if (attempts == 0) 0 else correct * 100 / attempts
    val totalStudyDays = store.totalStudyDays()
    val longestStreak = store.longestStudyStreak()

    data class Reward(val title: String, val detail: String, val unlocked: Boolean, val emoji: String)

    val rewards = listOf(
        Reward("First Step", "Complete your first quiz attempt", attempts >= 1, "🌱"),
        Reward("Lesson Explorer", "Complete 5 unique lessons", completed >= 5, "📚"),
        Reward("Practice Star", "Reach 10 quiz attempts", attempts >= 10, "⭐"),
        Reward("Accuracy Star", "At least 80% accuracy in 5 or more attempts", attempts >= 5 && accuracy >= 80, "🎯"),
        Reward("Study Starter", "Study on at least 1 day", totalStudyDays >= 1, "📅"),
        Reward("3-Day Champion", "Study on 3 consecutive days", longestStreak >= 3, "🏆"),
        Reward("Weekly Learner", "Study on all 7 consecutive days", longestStreak >= 7, "🥇")
    )

    val unlockedCount = rewards.count { it.unlocked }

    Column(
        Modifier.fillMaxSize().padding(padding).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("My Progress", color = Ink, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("Your learning activity is saved on this device.", color = Color.Gray)
        ProgressTile("Lessons completed", completed.toString(), "Unique lessons answered correctly")
        ProgressTile("Quiz attempts", attempts.toString(), "Submitted answers")
        ProgressTile("Answer accuracy", "$accuracy%", "$correct correct answers out of $attempts attempts")
        ProgressTile("Study days", totalStudyDays.toString(), "Days with completed study tasks")
        ProgressTile("Longest study streak", "$longestStreak days", "Best consecutive study run")

        Text("Learning Rewards", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("$unlockedCount of ${rewards.size} badges unlocked", color = RoyalBlue, fontWeight = FontWeight.SemiBold)

        rewards.forEach { reward ->
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (reward.unlocked) Color(0xFFEAF8EF) else Color.White
                )
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(reward.emoji, fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(reward.title, color = Ink, fontWeight = FontWeight.Bold)
                        Text(reward.detail, color = Color.Gray, fontSize = 12.sp)
                    }
                    Text(
                        if (reward.unlocked) "Unlocked" else "Locked",
                        color = if (reward.unlocked) Color(0xFF16803C) else Color.Gray,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }
        }
        Text("Keep practising a little every day. ⭐", color = RoyalBlue, fontWeight = FontWeight.SemiBold)
    }
}

@Composable private fun ProgressTile(title:String,value:String,detail:String) { Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) { Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title,color=Ink,fontWeight=FontWeight.Bold); Text(detail,color=Color.Gray,fontSize=12.sp) }; Text(value,color=RoyalBlue,fontSize=25.sp,fontWeight=FontWeight.Bold) } } }

@Composable fun SimpleContent(title:String,description:String,padding:PaddingValues,onChangeClass:(()->Unit)?=null) { Column(Modifier.fillMaxSize().padding(padding).padding(24.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) { Text("🌟",fontSize=44.sp); Spacer(Modifier.height(12.dp)); Text(title,color=Ink,fontSize=24.sp,fontWeight=FontWeight.Bold); Text(description,color=Color.Gray); if(onChangeClass!=null) { Spacer(Modifier.height(16.dp)); OutlinedButton(onClick=onChangeClass) { Text("Change class") } } } }
