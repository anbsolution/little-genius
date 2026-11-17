package com.learning.mylittleschool

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FavoriteLessonsScreen(
    database: LearningDatabase,
    profileId: String,
    padding: PaddingValues,
    onOpenLesson: (FavoriteLessonEntity) -> Unit
) {
    val favoritesFlow = remember(database, profileId) {
        database.favoriteLessonDao().observeFavorites(profileId)
    }
    val favorites by favoritesFlow.collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PageHeading(
                eyebrow = "SAVED FOR LATER",
                title = "Favorite lessons",
                subtitle = "Your saved lessons stay on this device and are available offline."
            )
        }

        if (favorites.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("♡", color = LittleGeniusColors.Blue, fontSize = 34.sp)
                        Text("No favorites yet", color = LittleGeniusColors.Ink, fontWeight = FontWeight.Bold)
                        Text(
                            "Open a lesson and tap the heart to save it here.",
                            color = LittleGeniusColors.Muted
                        )
                    }
                }
            }
        } else {
            items(favorites, key = { "${it.profileId}:${it.lessonId}" }) { favorite ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenLesson(favorite) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "Favorite lesson",
                            tint = Color(0xFFE14D68)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(favorite.title, color = LittleGeniusColors.Ink, fontWeight = FontWeight.Bold)
                            Text(
                                "${schoolClassTitle(favorite.classId)} · ${favorite.subject}",
                                color = LittleGeniusColors.Muted,
                                fontSize = 12.sp
                            )
                        }
                        Text("Open", color = LittleGeniusColors.Blue, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private fun schoolClassTitle(classId: String): String = when (classId) {
    "pre_nursery" -> "Pre-Nursery"
    "nursery" -> "Nursery"
    "lkg" -> "LKG"
    "ukg" -> "UKG"
    else -> classId.removePrefix("class_").toIntOrNull()?.let { "Class $it" } ?: classId
}
