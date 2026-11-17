package com.learning.mylittleschool

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.UUID

private val profileClassOptions = schoolClasses

@Composable
fun ChildProfilesScreen(
    database: LearningDatabase,
    activeProfileId: String,
    parentStore: ProgressStore,
    padding: PaddingValues,
    onActivateProfile: (ChildProfileEntity) -> Unit,
    onOpenParentDashboard: () -> Unit
) {
    var unlocked by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    if (!parentStore.hasParentPin()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Parent control required / अभिभावक नियंत्रण आवश्यक", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Set a parent PIN in the Parent Dashboard before managing child profiles.", color = Color.Gray)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onOpenParentDashboard) { Text("Open Parent Dashboard") }
        }
        return
    }

    if (!unlocked) {
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Child profiles / बच्चों की प्रोफ़ाइल", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Enter the parent PIN to continue / आगे बढ़ने के लिए PIN डालें", color = Color.Gray)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { value ->
                    if (value.length <= 6 && value.all(Char::isDigit)) {
                        pin = value
                        error = ""
                    }
                },
                label = { Text("Parent PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
            if (error.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(error, color = Color(0xFFB3261E))
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    unlocked = parentStore.verifyParentPin(pin)
                    error = if (unlocked) "" else "Incorrect PIN / PIN सही नहीं है"
                    pin = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Unlock / खोलें") }
        }
        return
    }

    val profilesFlow = remember(database) { database.childProfileDao().observeProfiles() }
    val profiles by profilesFlow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var childName by remember { mutableStateOf("") }
    var selectedClassId by remember { mutableStateOf(profileClassOptions.first().id) }
    var classMenuExpanded by remember { mutableStateOf(false) }
    var addError by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PageHeading(
                eyebrow = "PARENT CONTROLS · अभिभावक नियंत्रण",
                title = "Child profiles",
                subtitle = "Each child has separate favorites, lessons, practice and study progress."
            )
        }

        item { Text("Choose a child / बच्चा चुनें", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        items(profiles, key = { it.profileId }) { profile ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (profile.profileId == activeProfileId) Color(0xFFEAF8EF) else Color.White
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(profile.displayName, color = LittleGeniusColors.Ink, fontWeight = FontWeight.Bold)
                        Text(profileClassTitle(profile.classId), color = LittleGeniusColors.Muted, fontSize = 12.sp)
                        if (profile.profileId == activeProfileId) {
                            Text("Currently active / अभी उपयोग में", color = Color(0xFF16803C), fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    OutlinedButton(
                        enabled = profile.profileId != activeProfileId,
                        onClick = { onActivateProfile(profile) }
                    ) { Text(if (profile.profileId == activeProfileId) "Active" else "Switch") }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Add a child / बच्चा जोड़ें", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = childName,
                        onValueChange = { childName = it.take(40); addError = "" },
                        label = { Text("Child's name / बच्चे का नाम") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box {
                        OutlinedButton(onClick = { classMenuExpanded = true }) {
                            Text(profileClassTitle(selectedClassId))
                        }
                        DropdownMenu(
                            expanded = classMenuExpanded,
                            onDismissRequest = { classMenuExpanded = false }
                        ) {
                            profileClassOptions.forEach { schoolClass ->
                                DropdownMenuItem(
                                    text = { Text(schoolClass.title) },
                                    onClick = {
                                        selectedClassId = schoolClass.id
                                        classMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    if (addError.isNotBlank()) Text(addError, color = Color(0xFFB3261E))
                    Button(
                        onClick = {
                            val name = childName.trim()
                            if (name.isBlank()) {
                                addError = "Enter the child's name / बच्चे का नाम लिखें"
                            } else {
                                val profile = ChildProfileEntity(
                                    profileId = UUID.randomUUID().toString(),
                                    displayName = name,
                                    classId = selectedClassId,
                                    createdAt = System.currentTimeMillis(),
                                    isDefault = false
                                )
                                scope.launch {
                                    database.childProfileDao().insert(profile)
                                    childName = ""
                                    addError = ""
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Add profile / प्रोफ़ाइल जोड़ें") }
                    Text(
                        "Profiles are stored only on this device. Switching profiles keeps each child's saved learning data separate.",
                        color = LittleGeniusColors.Muted,
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            TextButton(onClick = { unlocked = false }) { Text("Lock profile controls / नियंत्रण लॉक करें") }
        }
    }
}

private fun profileClassTitle(classId: String): String =
    profileClassOptions.firstOrNull { it.id == classId }?.title
        ?: classId.removePrefix("class_").toIntOrNull()?.let { "Class $it" }
        ?: classId
