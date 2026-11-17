package com.learning.mylittleschool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val RoyalBlue = LittleGeniusColors.Blue
private val Ink = LittleGeniusColors.Ink
private val SoftBlue = LittleGeniusColors.SoftBlue

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
        val learningDatabase = LearningDatabase.getInstance(applicationContext)
        val appSettingsStore = AppSettingsStore(applicationContext)
        setContent { LittleGeniusApp(learningDatabase, appSettingsStore) }
    }
}

@Composable
fun LittleGeniusApp(
    learningDatabase: LearningDatabase,
    appSettingsStore: AppSettingsStore
) {
    val context = LocalContext.current
    val storedProfileId by appSettingsStore.activeProfileId.collectAsState(initial = "")
    val activeProfileId = storedProfileId.ifBlank { DEFAULT_PROFILE_ID }
    val progressStore = remember(context, activeProfileId) {
        ProgressStore(context, activeProfileId)
    }
    var selectedClass by remember { mutableStateOf<SchoolClass?>(null) }
    var selectedTab by remember { mutableStateOf("Home") }
    var selectedSubject by remember { mutableStateOf<String?>(null) }
    var selectedLesson by remember { mutableStateOf<LearningLesson?>(null) }
    var refreshProgress by remember { mutableIntStateOf(0) }
    val profileScope = rememberCoroutineScope()
    var profileLoaded by remember(activeProfileId) { mutableStateOf(false) }
    var pendingProfileId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(activeProfileId) {
        val profile = learningDatabase.childProfileDao().getProfile(activeProfileId)
        selectedClass = profile?.let { saved -> schoolClasses.firstOrNull { it.id == saved.classId } }
        profileLoaded = true
        if (pendingProfileId == activeProfileId) pendingProfileId = null
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = RoyalBlue,
            secondary = LittleGeniusColors.Purple,
            background = LittleGeniusColors.Canvas,
            surface = Color.White
        )
    ) {
        Surface(Modifier.fillMaxSize(), color=MaterialTheme.colorScheme.background) {
            when {
                storedProfileId.isBlank() || !profileLoaded || pendingProfileId != null -> LoadingScreen()
                selectedClass == null -> ClassSelectionScreen { schoolClass ->
                    selectedClass = schoolClass
                    profileScope.launch {
                        val profiles = learningDatabase.childProfileDao()
                        val current = profiles.getProfile(activeProfileId)
                        if (current == null) {
                            profiles.insert(
                                ChildProfileEntity(
                                    profileId = activeProfileId,
                                    displayName = "Student",
                                    classId = schoolClass.id,
                                    createdAt = System.currentTimeMillis(),
                                    isDefault = activeProfileId == DEFAULT_PROFILE_ID
                                )
                            )
                        } else {
                            profiles.updateClass(activeProfileId, schoolClass.id)
                        }
                    }
                }
                selectedLesson != null -> LessonScreen(
                    lesson = selectedLesson!!,
                    onBack = { selectedLesson = null },
                    onResult = { correct -> progressStore.recordLesson(selectedLesson!!.id, correct); refreshProgress++ },
                    database = learningDatabase,
                    profileId = activeProfileId,
                    settingsStore = appSettingsStore
                )
                selectedSubject != null -> ChapterScreen(
                    selectedClass!!,
                    selectedSubject!!,
                    { selectedSubject = null },
                    { lesson ->
                        progressStore.saveLastOpenedLesson(lesson.classId, lesson.subject, lesson.id)
                        selectedLesson = lesson
                    },
                    progressStore,
                    selectedTab == "Practice"
                )
                else -> DashboardScreen(
                    selectedClass!!, selectedTab, { selectedTab = it },
                    { selectedClass = null; selectedSubject = null },
                    { selectedSubject = it }, progressStore, refreshProgress,
                    { last ->
                        val targetClass = schoolClasses.firstOrNull { it.id == last.classId }
                        val targetLesson = lessonsFor(last.classId, last.subject)
                            .firstOrNull { it.id == last.lessonId }
                        if (targetClass != null && targetLesson != null) {
                            selectedClass = targetClass
                            selectedSubject = null
                            selectedTab = "Home"
                            selectedLesson = targetLesson
                            progressStore.saveLastOpenedLesson(
                                targetLesson.classId,
                                targetLesson.subject,
                                targetLesson.id
                            )
                        }
                    },
                    learningDatabase,
                    appSettingsStore,
                    activeProfileId,
                    { selectedProfile ->
                        pendingProfileId = selectedProfile.profileId
                        selectedLesson = null
                        selectedSubject = null
                        selectedTab = "Home"
                        profileScope.launch {
                            appSettingsStore.setActiveProfileId(selectedProfile.profileId)
                        }
                    },
                    { selectedTab = "Profile" },
                    { favorite ->
                        val targetClass = schoolClasses.firstOrNull { it.id == favorite.classId }
                        val targetLesson = lessonsFor(favorite.classId, favorite.subject)
                            .firstOrNull { it.id == favorite.lessonId }
                        if (targetClass != null && targetLesson != null) {
                            selectedClass = targetClass
                            selectedSubject = null
                            selectedTab = "Home"
                            selectedLesson = targetLesson
                            progressStore.saveLastOpenedLesson(
                                targetLesson.classId,
                                targetLesson.subject,
                                targetLesson.id
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ClassSelectionScreen(onSelect: (SchoolClass) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PremiumHeroCard(
                eyebrow = "LITTLE GENIUS · PERSONALIZED LEARNING",
                title = "Your next adventure starts here",
                subtitle = "Choose a class to see learning activities made for your level.",
                decoration = "🚀"
            ) {
                AccentPill("9 CLASS LEVELS", Color.White.copy(alpha = 0.92f))
            }
        }
        item {
            PageHeading(
                eyebrow = "GET STARTED",
                title = "Choose your class",
                subtitle = "You can change this later from the Home screen."
            )
        }
        items(schoolClasses, key = { it.id }) { item ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(item) },
                shape = RoundedCornerShape(21.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(50.dp).background(
                            if (item.id.startsWith("class")) Color(0xFFEDE9FF) else SoftBlue,
                            RoundedCornerShape(16.dp)
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (item.id.startsWith("class")) "📚" else "🌱", fontSize = 23.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(item.title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(item.subtitle, color = LittleGeniusColors.Muted, fontSize = 12.sp)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = "Select ${item.title}", tint = RoyalBlue)
                }
            }
        }
    }
}

@Composable fun DashboardScreen(
    schoolClass: SchoolClass,
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onChangeClass: () -> Unit,
    onSubjectSelected: (String) -> Unit,
    store: ProgressStore,
    refresh: Int,
    onResumeLesson: (LastOpenedLesson) -> Unit,
    learningDatabase: LearningDatabase,
    appSettingsStore: AppSettingsStore,
    activeProfileId: String,
    onActivateProfile: (ChildProfileEntity) -> Unit,
    onOpenParentDashboard: () -> Unit,
    onOpenFavorite: (FavoriteLessonEntity) -> Unit
) {
    val selectedNavigationTab = if (selectedTab in listOf("Worksheet","Study","Profile","Library","Favorites","Profiles","Backup","Homework")) "More" else selectedTab

    Scaffold(bottomBar={
        NavigationBar(containerColor=Color.White, tonalElevation=8.dp) {
            listOf("Home","Learn","Practice","Progress","More").forEach { tab ->
                val icon=when(tab) {
                    "Home" -> Icons.Default.Home
                    "Learn" -> Icons.Default.MenuBook
                    "Practice" -> Icons.Default.Extension
                    "Progress" -> Icons.Default.BarChart
                    else -> Icons.Default.MoreHoriz
                }
                NavigationBarItem(
                    selected=selectedNavigationTab==tab,
                    onClick={onTabSelected(tab)},
                    icon={Icon(icon,contentDescription=tab)},
                    label={Text(tab,fontSize=10.sp)},
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = RoyalBlue,
                        selectedTextColor = Ink,
                        indicatorColor = SoftBlue,
                        unselectedIconColor = Color(0xFF8791A4),
                        unselectedTextColor = Color(0xFF8791A4)
                    )
                )
            }
        }
    }) { padding ->
        when(selectedTab) {
            "Home","Learn" -> HomeContent(
                schoolClass, padding, onChangeClass, onSubjectSelected,
                store, refresh, onResumeLesson, onTabSelected
            )
            "Practice" -> PracticeAndPlayScreen(schoolClass,padding,store,onSubjectSelected)
            "Worksheet" -> WorksheetCenterScreen(schoolClass,padding,store)
            "Study" -> DailyStudyPlannerScreen(store,padding)
            "Homework" -> HomeworkPlannerScreen(learningDatabase, activeProfileId, padding)
            "Library" -> StudyLibraryScreen(schoolClass, padding, onSubjectSelected)
            "Progress" -> { val ignored=refresh; ProgressScreen(store,padding,learningDatabase,activeProfileId,schoolClass) }
            "More" -> MoreOptionsScreen(padding,onTabSelected)
            "Profile" -> ParentDashboardScreen(store, padding) { onTabSelected("Backup") }
            "Backup" -> BackupRestoreScreen(
                database = learningDatabase,
                settingsStore = appSettingsStore,
                padding = padding,
                onBack = { onTabSelected("Profile") }
            )
            "History" -> LessonHistoryScreen(store,padding,onResumeLesson)
            "Profiles" -> ChildProfilesScreen(
                database = learningDatabase,
                activeProfileId = activeProfileId,
                parentStore = store,
                padding = padding,
                onActivateProfile = onActivateProfile,
                onOpenParentDashboard = onOpenParentDashboard
            )
            "Favorites" -> FavoriteLessonsScreen(
                database = learningDatabase,
                profileId = activeProfileId,
                padding = padding,
                onOpenLesson = onOpenFavorite
            )
        }
    }
}

@Composable
fun MoreOptionsScreen(padding: PaddingValues, onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PageHeading(
            eyebrow = "YOUR LEARNING SPACE",
            title = "More to explore",
            subtitle = "Helpful tools for practice, planning and family progress."
        )

        val options = listOf(
            Triple("Worksheet Center", "Practice subject-wise worksheets", "Worksheet"),
            Triple("Study Materials & Downloads", "Offline lessons and official learning sources", "Library"),
            Triple("Favorite Lessons", "Quickly reopen lessons you saved", "Favorites"),
            Triple("Child Profiles", "Keep each child's learning progress separate", "Profiles"),
            Triple("Homework Planner", "Track subjects, deadlines and reminders", "Homework"),
            Triple("Daily Study Planner", "View and complete your daily study tasks", "Study"),
            Triple("Parent Dashboard", "Review learning progress and reports", "Profile"),
            Triple("Learning History", "Reopen your 10 most recent lessons", "History")
        )

        options.forEach { (title, detail, destination) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clickable { onSelect(destination) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val emoji = when (destination) {
                        "Worksheet" -> "📝"
                        "Library" -> "📚"
                        "Favorites" -> "❤️"
                        "Profiles" -> "👨‍👧"
                        "Study" -> "🗓️"
                        "Homework" -> "📌"
                        "Profile" -> "👨‍👩‍👧"
                        else -> "🕘"
                    }
                    Box(
                        Modifier.size(44.dp).background(SoftBlue, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text(emoji, fontSize = 21.sp) }
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title, color = Ink, fontWeight = FontWeight.Bold)
                        Text(detail, color = LittleGeniusColors.Muted, fontSize = 12.sp)
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Open $title",
                        tint = RoyalBlue
                    )
                }
            }
        }
    }
}

@Composable
fun HomeContent(
    schoolClass: SchoolClass,
    padding: PaddingValues,
    onChangeClass: () -> Unit,
    onSubjectSelected: (String) -> Unit,
    store: ProgressStore,
    refresh: Int,
    onResumeLesson: (LastOpenedLesson) -> Unit,
    onNavigate: (String) -> Unit
) {
    val ignored = refresh
    val lastLesson = store.lastOpenedLesson()
    val resumableLesson = lastLesson?.let { last ->
        lessonsFor(last.classId, last.subject).firstOrNull { it.id == last.lessonId }
    }
    val lastLessonClass = lastLesson?.let { last ->
        schoolClasses.firstOrNull { it.id == last.classId }
    }
    val completed = store.completedCount()
    val attempts = store.attempts()
    val correct = store.correctAnswers()
    val accuracy = if (attempts > 0) (correct * 100) / attempts else 0
    val tasksDone = store.studyTasksDoneCount()
    val streak = store.studyStreakDays()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("LITTLE GENIUS", color = RoyalBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                    Text("Hello, Student! 👋", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    AccentPill("${schoolClass.title} · READY TO LEARN")
                }
                IconButton(onClick = onChangeClass) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Change class", tint = RoyalBlue)
                }
            }
        }

        item {
            PremiumHeroCard(
                eyebrow = "YOUR LEARNING JOURNEY",
                title = "Small steps, big achievements!",
                subtitle = "Explore a subject, practise and grow a little every day.",
                decoration = "🌟"
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$completed lessons", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(Modifier.weight(1f))
                    Text("🔥 $streak day streak", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                ProgressTrack(progress = tasksDone / 5f)
                Text("Today's goal · $tasksDone of 5 study tasks", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
            }
        }

        item {
            SectionHeading("Your learning summary", "A quick look at how you're doing today")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    Triple("Practice answers", attempts.toString(), SoftBlue),
                    Triple("Accuracy", "$accuracy%", Color(0xFFE7F8EC)),
                    Triple("Today's tasks", "$tasksDone/5", Color(0xFFFFF4D9))
                ).forEach { stat ->
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(17.dp),
                        colors = CardDefaults.cardColors(containerColor = stat.third)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(stat.second, color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                            Text(stat.first, color = LittleGeniusColors.Muted, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeading("Quick start", "Jump straight into your next activity")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickStartCard("Practice", "Quiz & games", "🎮", SoftBlue, Modifier.weight(1f)) {
                        onNavigate("Practice")
                    }
                    QuickStartCard("Library", "Offline books", "📚", LittleGeniusColors.SoftMint, Modifier.weight(1f)) {
                        onNavigate("Library")
                    }
                    QuickStartCard("Homework", "Due dates", "📌", LittleGeniusColors.SoftAmber, Modifier.weight(1f)) {
                        onNavigate("Homework")
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Today's learning", color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("$tasksDone of 5 study tasks completed today", color = Color.Gray, fontSize = 12.sp)
                    }
                    Text("$tasksDone/5", color = RoyalBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }

        if (lastLesson != null && resumableLesson != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftBlue)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "CONTINUE LEARNING",
                            color = RoyalBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            resumableLesson.title,
                            color = Ink,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${resumableLesson.subject} • ${lastLessonClass?.title ?: lastLesson.classId}",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Button(
                            onClick = { onResumeLesson(lastLesson) },
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                        ) {
                            Text("Continue lesson")
                        }
                    }
                }
            }
        }

        item {
            SectionHeading("Explore subjects", "Choose a subject to start learning")
        }

        items(subjects.chunked(2)) { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { subject ->
                    Card(
                        modifier = Modifier.weight(1f).height(118.dp)
                            .clickable { onSubjectSelected(subject.name) },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = subject.tint)
                    ) {
                        Column(
                            Modifier.fillMaxSize().padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(subject.emoji, fontSize = 26.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    subject.name,
                                    color = Ink,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = "Open ${subject.name}",
                                    tint = RoyalBlue
                                )
                            }
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        item {
            Text(
                "Keep learning every day. Your progress is saved on this device.",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun QuickStartCard(
    title: String,
    detail: String,
    emoji: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(112.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(11.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                Modifier.size(36.dp).background(tint, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) { Text(emoji, fontSize = 18.sp) }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                Text(detail, color = LittleGeniusColors.Muted, fontSize = 10.sp, maxLines = 1)
            }
        }
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
            PageHeading(
                eyebrow = "SUBJECT PRACTICE",
                title = "Let's practise! ⭐",
                subtitle = "${schoolClass.title} · Choose a subject to start a quiz."
            )
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
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
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

@Composable
fun LessonScreen(
    lesson: LearningLesson,
    onBack: () -> Unit,
    onResult: (Boolean) -> Unit,
    database: LearningDatabase,
    profileId: String,
    settingsStore: AppSettingsStore
) {
    val savedLanguage by settingsStore.preferredLanguage.collectAsState(initial = "English")
    val favoriteFlow = remember(database, profileId, lesson.id) {
        database.favoriteLessonDao().observeIsFavorite(profileId, lesson.id)
    }
    val isFavorite by favoriteFlow.collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    var language by remember { mutableStateOf(savedLanguage) }
    var selectedAnswer by remember(lesson.id) { mutableStateOf<Int?>(null) }
    var submitted by remember(lesson.id) { mutableStateOf(false) }

    LaunchedEffect(savedLanguage) { language = savedLanguage }

    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        TextButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = null)
            Text(" Back to lessons")
        }
        Text(lesson.subject.uppercase(), color = RoyalBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                lesson.title,
                modifier = Modifier.weight(1f),
                color = Ink,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = {
                    scope.launch {
                        if (isFavorite) {
                            database.favoriteLessonDao().remove(profileId, lesson.id)
                        } else {
                            database.favoriteLessonDao().save(
                                FavoriteLessonEntity(
                                    profileId = profileId,
                                    lessonId = lesson.id,
                                    classId = lesson.classId,
                                    subject = lesson.subject,
                                    title = lesson.title,
                                    savedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
            ) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavorite) Color(0xFFE14D68) else LittleGeniusColors.Muted
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        SingleChoiceSegmentedButtonRow {
            listOf("English", "Hindi").forEachIndexed { index, value ->
                SegmentedButton(
                    selected = language == value,
                    onClick = {
                        language = value
                        scope.launch { settingsStore.setPreferredLanguage(value) }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = 2)
                ) { Text(value) }
            }
        }
        Spacer(Modifier.height(14.dp))
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Text(
                if (language == "English") lesson.english else lesson.hindi,
                Modifier.padding(18.dp),
                color = Ink,
                fontSize = 17.sp,
                lineHeight = 25.sp
            )
        }
        Spacer(Modifier.height(12.dp))
        LessonVoiceControls(text = if (language == "English") lesson.english else lesson.hindi, language = language)
        Spacer(Modifier.height(22.dp))
        Text("Quick check", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(lesson.question, color = Ink, fontSize = 16.sp)
        Spacer(Modifier.height(10.dp))
        lesson.options.forEachIndexed { index, option ->
            OutlinedButton(
                onClick = { if (!submitted) selectedAnswer = index },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (selectedAnswer == index) SoftBlue else Color.Transparent,
                    contentColor = Ink
                )
            ) { Text(option, Modifier.fillMaxWidth()) }
        }
        Button(
            onClick = {
                val correct = selectedAnswer == lesson.correctIndex
                onResult(correct)
                submitted = true
            },
            enabled = selectedAnswer != null && !submitted,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Check answer") }
        if (submitted) {
            val correct = selectedAnswer == lesson.correctIndex
            Text(
                if (correct) "Correct! Well done. ⭐" else "Not quite. Read the explanation.",
                color = if (correct) Color(0xFF16803C) else Color(0xFFB45309),
                fontWeight = FontWeight.Bold
            )
            Text(lesson.explanation, color = Ink)
            TextButton(onClick = { selectedAnswer = null; submitted = false }) { Text("Try again") }
        }
    }
}

@Composable
fun LessonHistoryScreen(
    store: ProgressStore,
    padding: PaddingValues,
    onResumeLesson: (LastOpenedLesson) -> Unit
) {
    val history = store.lessonHistory()

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Learning History", color = Ink, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text("Your 10 most recently opened lessons", color = Color.Gray)
        }

        if (history.isEmpty()) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Text(
                        "No lesson history yet. Open a lesson to see it here.",
                        Modifier.padding(18.dp),
                        color = Color.Gray
                    )
                }
            }
        } else {
            items(history) { entry ->
                val lesson = lessonsFor(entry.classId, entry.subject)
                    .firstOrNull { it.id == entry.lessonId }
                val classTitle = schoolClasses.firstOrNull { it.id == entry.classId }?.title
                    ?: entry.classId
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            lesson?.title ?: entry.lessonId,
                            color = Ink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            "${entry.subject} • $classTitle",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Text(
                            java.text.DateFormat.getDateTimeInstance().format(
                                java.util.Date(entry.openedAt)
                            ),
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                        Text(
                            if (entry.completed) "Completed" else "In Progress",
                            color = if (entry.completed) Color(0xFF16803C) else RoyalBlue,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        if (lesson != null) {
                            OutlinedButton(
                                onClick = {
                                    onResumeLesson(
                                        LastOpenedLesson(entry.classId, entry.subject, entry.lessonId)
                                    )
                                }
                            ) {
                                Text("Open lesson")
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class ProgressReward(
    val title: String,
    val detail: String,
    val unlocked: Boolean,
    val emoji: String,
    val progress: Float,
    val progressLabel: String
)

@Composable fun ProgressScreen(
    store: ProgressStore,
    padding: PaddingValues,
    database: LearningDatabase,
    profileId: String,
    schoolClass: SchoolClass
) {
    val completed = store.completedCount()
    val attempts = store.attempts()
    val correct = store.correctAnswers()
    val accuracy = if (attempts == 0) 0 else correct * 100 / attempts
    val totalStudyDays = store.totalStudyDays()
    val longestStreak = store.longestStudyStreak()
    val practicalGuidesDone = store.practicalGuidesDoneCount()
    val profiles by remember(database) { database.childProfileDao().observeProfiles() }
        .collectAsState(initial = emptyList())
    val profileName = profiles.firstOrNull { it.profileId == profileId }?.displayName ?: "Student"

    val rewards = listOf(
        ProgressReward("First Step", "Submit your first practice answer", attempts >= 1, "🌱", attempts.coerceAtMost(1).toFloat(), "$attempts/1 answer"),
        ProgressReward("Lesson Explorer", "Complete 5 unique lessons", completed >= 5, "📚", (completed / 5f).coerceAtMost(1f), "$completed/5 lessons"),
        ProgressReward("Practice Star", "Submit 10 practice answers", attempts >= 10, "⭐", (attempts / 10f).coerceAtMost(1f), "$attempts/10 answers"),
        ProgressReward(
            "Accuracy Star",
            "At least 80% accuracy in 5 or more attempts",
            attempts >= 5 && accuracy >= 80,
            "🎯",
            minOf(attempts / 5f, accuracy / 80f).coerceIn(0f, 1f),
            "$attempts/5 attempts · $accuracy% accuracy"
        ),
        ProgressReward("Study Starter", "Study on at least 1 day", totalStudyDays >= 1, "📅", totalStudyDays.coerceAtMost(1).toFloat(), "$totalStudyDays/1 day"),
        ProgressReward("Hands-on Explorer", "Complete 3 practical guides", practicalGuidesDone >= 3, "🔬", (practicalGuidesDone / 3f).coerceAtMost(1f), "$practicalGuidesDone/3 guides"),
        ProgressReward("3-Day Champion", "Study on 3 consecutive days", longestStreak >= 3, "🏆", (longestStreak / 3f).coerceAtMost(1f), "$longestStreak/3 days"),
        ProgressReward("Weekly Learner", "Study on all 7 consecutive days", longestStreak >= 7, "🥇", (longestStreak / 7f).coerceAtMost(1f), "$longestStreak/7 days")
    )

    val unlockedCount = rewards.count { it.unlocked }

    Column(
        Modifier.fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PageHeading(
            eyebrow = "YOUR ACHIEVEMENTS",
            title = "My Progress",
            subtitle = "Your learning activity is saved on this device."
        )
        PremiumHeroCard(
            eyebrow = "KEEP GOING",
            title = if (attempts == 0) "Your journey starts with one answer" else "You're building a learning habit",
            subtitle = "Every lesson and practice answer helps you grow.",
            decoration = "🏆"
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$completed lessons", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("$accuracy% accuracy", color = Color.White, fontWeight = FontWeight.Bold)
            }
            ProgressTrack(progress = accuracy / 100f)
            Text("$attempts practice answers · $practicalGuidesDone guides completed", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
        }
        ProgressTile("Lessons completed", completed.toString(), "Unique lessons answered correctly")
        ProgressTile("Practice attempts", attempts.toString(), "Quiz, game and puzzle answers")
        ProgressTile("Answer accuracy", "$accuracy%", "$correct correct answers out of $attempts attempts")
        ProgressTile("Practical guides completed", practicalGuidesDone.toString(), "Hands-on activities marked complete")
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
                        LinearProgressIndicator(
                            progress = { reward.progress },
                            modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
                            color = if (reward.unlocked) Color(0xFF16803C) else RoyalBlue,
                            trackColor = Color(0xFFE5EAF3)
                        )
                    }
                    Text(
                        if (reward.unlocked) "Unlocked" else reward.progressLabel,
                        color = if (reward.unlocked) Color(0xFF16803C) else Color.Gray,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }
        }
        LearningCertificateCard(
            studentName = profileName,
            className = schoolClass.title,
            store = store
        )
        Text("Keep practising a little every day. ⭐", color = RoyalBlue, fontWeight = FontWeight.SemiBold)
    }
}

@Composable private fun ProgressTile(title:String,value:String,detail:String) { Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) { Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title,color=Ink,fontWeight=FontWeight.Bold); Text(detail,color=Color.Gray,fontSize=12.sp) }; Text(value,color=RoyalBlue,fontSize=25.sp,fontWeight=FontWeight.Bold) } } }

@Composable fun SimpleContent(title:String,description:String,padding:PaddingValues,onChangeClass:(()->Unit)?=null) { Column(Modifier.fillMaxSize().padding(padding).padding(24.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) { Text("🌟",fontSize=44.sp); Spacer(Modifier.height(12.dp)); Text(title,color=Ink,fontSize=24.sp,fontWeight=FontWeight.Bold); Text(description,color=Color.Gray); if(onChangeClass!=null) { Spacer(Modifier.height(16.dp)); OutlinedButton(onClick=onChangeClass) { Text("Change class") } } } }
