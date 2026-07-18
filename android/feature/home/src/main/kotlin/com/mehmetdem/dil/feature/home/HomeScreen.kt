package com.mehmetdem.dil.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mehmetdem.dil.core.designsystem.DilBlueSoft
import com.mehmetdem.dil.core.designsystem.DilBorder
import com.mehmetdem.dil.core.designsystem.DilGreenSoft
import com.mehmetdem.dil.core.designsystem.DilMaxContentWidth
import com.mehmetdem.dil.core.designsystem.DilMuted
import com.mehmetdem.dil.core.designsystem.DilOuterPadding
import com.mehmetdem.dil.core.designsystem.DilRedSoft
import com.mehmetdem.dil.core.designsystem.DilTeal
import com.mehmetdem.dil.core.model.ContentRange
import com.mehmetdem.dil.core.model.LessonJobState
import com.mehmetdem.dil.core.model.SourceKind
import com.mehmetdem.dil.core.model.StoredLesson
import com.mehmetdem.dil.core.model.TimecodeParser

@Composable
fun HomeScreen(
    lessons: List<StoredLesson>,
    onCreateYouTubeLesson: () -> Unit,
    onCreatePdfLesson: () -> Unit,
    onOpenLesson: (String) -> Unit,
    onDeleteLesson: (String) -> Unit,
    onOpenLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var search by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<StoredLesson?>(null) }
    val filtered = remember(lessons, search) {
        lessons.filter { search.isBlank() || it.config.format.title.contains(search, ignoreCase = true) || it.config.source.displayName.contains(search, ignoreCase = true) }
    }
    pendingDelete?.let { lesson ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Dersi sil?") },
            text = { Text("“${lesson.config.format.title}” cihazdan kaldırılacak.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteLesson(lesson.id)
                    pendingDelete = null
                }) { Text("Sil", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Vazgeç") } },
        )
    }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().widthIn(max = DilMaxContentWidth),
            contentPadding = PaddingValues(horizontal = DilOuterPadding, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Merhaba", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Kendi kaynağından yeni bir ders oluştur.", color = DilMuted)
                }
            }
            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Kaydedilmiş derslerde ara") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = DilBorder),
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CreateSourceCard("YouTube'dan\nDers Oluştur", "Seçtiğin zaman aralığını işle", Icons.Filled.PlayArrow, Color(0xFFE90020), DilRedSoft, onCreateYouTubeLesson, Modifier.weight(1f))
                    CreateSourceCard("PDF'den\nDers Oluştur", "Seçtiğin sayfaları işle", Icons.Filled.Description, Color(0xFF1478F2), DilBlueSoft, onCreatePdfLesson, Modifier.weight(1f))
                }
            }
            item { SectionTitle(if (search.isBlank()) "Derslerim" else "Arama Sonuçları", "Kütüphaneyi Aç", onOpenLibrary) }
            if (filtered.isEmpty()) {
                item { EmptyLessonState(search.isNotBlank()) }
            } else {
                items(filtered.take(5).size) { index ->
                    val lesson = filtered[index]
                    RealLessonCard(lesson, { onOpenLesson(lesson.id) }, { pendingDelete = lesson })
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun CreateSourceCard(title: String, subtitle: String, icon: ImageVector, tint: Color, background: Color, onClick: () -> Unit, modifier: Modifier) {
    Card(modifier = modifier.height(126.dp).clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = background.copy(alpha = .58f)), border = BorderStroke(1.dp, background), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).background(background, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint) }
                Icon(Icons.Filled.ArrowForwardIos, null, modifier = Modifier.size(16.dp))
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = DilMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SectionTitle(title: String, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(action, color = DilTeal, style = MaterialTheme.typography.labelLarge, modifier = Modifier.clickable(onClick = onAction).padding(6.dp))
    }
}

@Composable
private fun RealLessonCard(lesson: StoredLesson, onOpen: () -> Unit, onDelete: () -> Unit) {
    val progress = lesson.completedBlockCount.toFloat() / lesson.config.format.totalBlockCount
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, DilBorder), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(width = 70.dp, height = 58.dp).background(if (lesson.config.source.kind == SourceKind.PDF) DilBlueSoft else DilRedSoft, RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                Icon(if (lesson.config.source.kind == SourceKind.PDF) Icons.Filled.Description else Icons.Filled.PlayArrow, null, tint = if (lesson.config.source.kind == SourceKind.PDF) Color(0xFF1478F2) else Color(0xFFE90020))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(lesson.config.format.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(sourceMeta(lesson), color = DilMuted, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.weight(1f), color = DilTeal, trackColor = DilBorder)
                    Text("${lesson.completedBlockCount}/${lesson.config.format.totalBlockCount}", color = DilTeal, style = MaterialTheme.typography.labelLarge)
                }
                Text(stateLabel(lesson.state), color = stateColor(lesson.state), style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.DeleteOutline, "Dersi sil", tint = DilMuted) }
        }
    }
}

@Composable
private fun EmptyLessonState(searching: Boolean) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, DilBorder), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(48.dp).background(DilGreenSoft, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.AutoStories, null, tint = DilTeal) }
            Text(if (searching) "Eşleşen ders bulunamadı" else "Henüz kaydedilmiş ders yok", fontWeight = FontWeight.Bold)
            Text(if (searching) "Farklı bir arama yapabilirsin." else "Oluşturduğun dersler gerçek cihaz deposunda burada saklanır.", color = DilMuted)
        }
    }
}

private fun sourceMeta(lesson: StoredLesson): String = when (val range = lesson.config.source.range) {
    is ContentRange.Time -> "YouTube · ${TimecodeParser.formatMillis(range.startMillis)}–${TimecodeParser.formatMillis(range.endMillisExclusive)}"
    is ContentRange.Pages -> "PDF · ${range.startPage}–${range.endPageInclusive}. sayfalar"
}

private fun stateLabel(state: LessonJobState): String = when (state) {
    LessonJobState.CREATED -> "Kaydedildi"
    LessonJobState.INGESTING -> "Kaynak işleniyor"
    LessonJobState.GENERATING -> "Kartlar üretiliyor"
    LessonJobState.PAUSED -> "Duraklatıldı"
    LessonJobState.READY -> "Hazır"
    LessonJobState.FAILED -> "İşlem başarısız"
    LessonJobState.CANCELLED -> "Durduruldu"
    LessonJobState.DRAFT -> "Taslak"
}

private fun stateColor(state: LessonJobState): Color = when (state) {
    LessonJobState.READY -> Color(0xFF159E68)
    LessonJobState.FAILED, LessonJobState.CANCELLED -> Color(0xFFD43C50)
    else -> DilMuted
}
