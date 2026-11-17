package com.learning.mylittleschool

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object LittleGeniusColors {
    val Blue = Color(0xFF3157D5)
    val BlueDeep = Color(0xFF253EAF)
    val Purple = Color(0xFF7557D9)
    val Ink = Color(0xFF17233C)
    val Muted = Color(0xFF687386)
    val Canvas = Color(0xFFF6F8FD)
    val SoftBlue = Color(0xFFEAF1FF)
    val SoftMint = Color(0xFFE8F7EF)
    val SoftAmber = Color(0xFFFFF4DD)
}

@Composable
internal fun PremiumHeroCard(
    eyebrow: String,
    title: String,
    subtitle: String,
    decoration: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(LittleGeniusColors.BlueDeep, LittleGeniusColors.Blue, LittleGeniusColors.Purple)
                )
            )
    ) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 38.dp, y = (-46).dp)
                .size(150.dp)
                .background(Color.White.copy(alpha = 0.09f), CircleShape)
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 24.dp, y = 36.dp)
                .size(92.dp)
                .background(Color.White.copy(alpha = 0.07f), CircleShape)
        )
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                eyebrow.uppercase(),
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Text(
                "$decoration  $title",
                color = Color.White,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(subtitle, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, lineHeight = 19.sp)
            content()
        }
    }
}

@Composable
internal fun PageHeading(
    eyebrow: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            eyebrow.uppercase(),
            color = LittleGeniusColors.Blue,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.1.sp
        )
        Text(title, color = LittleGeniusColors.Ink, fontSize = 26.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = LittleGeniusColors.Muted, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
internal fun SectionHeading(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, color = LittleGeniusColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        if (subtitle != null) {
            Text(subtitle, color = LittleGeniusColors.Muted, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
internal fun ProgressTrack(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = Color.White.copy(alpha = 0.25f),
    fillColor: Color = Color.White
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(trackColor)
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(fillColor, CircleShape)
        )
    }
}

@Composable
internal fun AccentPill(text: String, color: Color = LittleGeniusColors.SoftBlue) {
    Surface(color = color, shape = CircleShape) {
        Text(
            text,
            Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            color = LittleGeniusColors.Ink,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
