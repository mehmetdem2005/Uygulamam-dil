package com.mehmetdem.dil.feature.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mehmetdem.dil.core.designsystem.DilElevatedCard
import com.mehmetdem.dil.core.designsystem.DilSectionHeader

@Composable
fun HomeScreen(
    onCreateLesson: () -> Unit,
    onCreateYouTubeLesson: () -> Unit,
    onCreatePdfLesson: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            HeroCard(onCreateLesson = onCreateLesson)
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DilSectionHeader(title = "Kaynak ekle")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onCreateYouTubeLesson,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(vertical = 14.dp),
                    ) {
                        Icon(Icons.Filled.PlayCircle, contentDescription = null)
                        Text("YouTube", modifier = Modifier.padding(start = 8.dp))
                    }
                    OutlinedButton(
                        onClick = onCreatePdfLesson,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(vertical = 14.dp),
                    ) {
                        Icon(Icons.Filled.Description, contentDescription = null)
                        Text("PDF", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DilSectionHeader(title = "Derslerim")
                EmptyLessonsCard(onCreateLesson = onCreateLesson)
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun HeroCard(onCreateLesson: () -> Unit) {
    val gradient = Brush.linearGradient(
        colors = listOf(Color(0xFF5C4FD0), Color(0xFF8173EA), Color(0xFF4EBEAA)),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(gradient)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "Kaynağı seç.\nDersi sen tasarla.",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Video veya PDF'nin yalnızca istediğin bölümünü, belirlediğin formatta öğren.",
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(
            onClick = onCreateLesson,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF4A3DB2),
            ),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text("Yeni ders oluştur", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun EmptyLessonsCard(onCreateLesson: () -> Unit) {
    DilElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoStories,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Text("Henüz kaydedilmiş ders yok", style = MaterialTheme.typography.titleMedium)
        Text(
            "İlk dersini oluşturduğunda kartlar, ilerleme ve indirilen sesler burada kalıcı olarak görünecek.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onCreateLesson, shape = MaterialTheme.shapes.extraLarge) {
            Text("İlk dersi oluştur")
        }
    }
}
