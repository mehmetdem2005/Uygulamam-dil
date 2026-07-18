package com.mehmetdem.dil.feature.library

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mehmetdem.dil.core.designsystem.DilBlueSoft
import com.mehmetdem.dil.core.designsystem.DilBorder
import com.mehmetdem.dil.core.designsystem.DilGreenSoft
import com.mehmetdem.dil.core.designsystem.DilMaxContentWidth
import com.mehmetdem.dil.core.designsystem.DilMuted
import com.mehmetdem.dil.core.designsystem.DilOuterPadding
import com.mehmetdem.dil.core.designsystem.DilTeal
import com.mehmetdem.dil.core.model.ContentRange
import com.mehmetdem.dil.core.model.LessonJobState
import com.mehmetdem.dil.core.model.SourceKind
import com.mehmetdem.dil.core.model.StoredLesson
import java.text.DateFormat
import java.util.Date

private enum class LibraryFilter(val label: String) {
    ALL("Tümü"),
    YOUTUBE("YouTube"),
    PDF("PDF"),
}

@Composable
fun LibraryScreen(
    lessons: List<StoredLesson>,
    onOpenLesson: (String) -> Unit,
    onDeleteLesson: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(LibraryFilter.ALL) }
    var pendingDelete by remember { mutableStateOf<StoredLesson?>(null) }
    val visibleLessons = remember(lessons, query, filter) {
        lessons.filter { lesson ->
            val matchesKind = when (filter) {
                LibraryFilter.ALL -> true
                LibraryFilter.YOUTUBE -> lesson.config.source.kind == SourceKind.YOUTUBE
                LibraryFilter.PDF -> lesson.config.source.kind == SourceKind.PDF
            }
            val matchesQuery = query.isBlank() ||
                lesson.config.format.title.contains(query, ignoreCase = true) ||
                lesson.config.source.displayName.contains(query, ignoreCase = true)
            matchesKind && matchesQuery
        }
    }

    pendingDelete?.let { lesson ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Dersi sil?") },
            text = { Text("“${lesson.config.format.title}” cihazdaki kayıtlı derslerden kaldırılacak.") },
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.AutoStories, null, tint = DilTeal, modifier = Modifier.size(30.dp))
                    Text(
                        "  Kütüphane",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ders veya kaynak ara") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(15.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = DilBorder),
                )
            }
            item {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFF5F6F9)),
                ) {
                    LibraryFilter.entries.forEach { choice ->
                        val selected = choice == filter
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clickable { filter = choice }
                                .background(if (selected) DilBlueSoft else Color.Transparent),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                choice.label,
                                color = if (selected) DilTeal else DilMuted,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Kaydedilen Dersler", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "  ${visibleLessons.size} içerik",
                        color = DilMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (visibleLessons.isEmpty()) {
                item { EmptyLibrary(hasAnyLesson = lessons.isNotEmpty(), hasSearch = query.isNotBlank() || filter != LibraryFilter.ALL) }
            } else {
                items(visibleLessons, key = StoredLesson::id) { lesson ->
                    LessonRow(
                        lesson = lesson,
                        onOpen = { onOpenLesson(lesson.id) },
                        onDelete = { pendingDelete = lesson },
                    )
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun EmptyLibrary(hasAnyLesson: Boolean, hasSearch: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, DilBorder),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Outlined.AutoStories, null, tint = DilTeal, modifier = Modifier.size(42.dp))
            Text(
                if (hasAnyLesson && hasSearch) "Bu filtreye uygun ders yok" else "Henüz kayıtlı ders yok",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                if (hasAnyLesson && hasSearch) "Arama metnini veya kaynak filtresini değiştir."
                else "Oluşturduğun dersler uygulamayı kapatsan da burada saklanır.",
                color = DilMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun LessonRow(
    lesson: StoredLesson,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val isPdf = lesson.config.source.kind == SourceKind.PDF
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, DilBorder),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.size(width = 82.dp, height = 58.dp).background(if (isPdf) DilBlueSoft else DilGreenSoft, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (isPdf) Icons.Filled.Description else Icons.Filled.PlayArrow,
                    null,
                    tint = if (isPdf) Color(0xFF1478F2) else DilTeal,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(lesson.config.format.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${if (isPdf) "PDF" else "YouTube"} · ${rangeLabel(lesson.config.source.range)} · ${lesson.config.format.totalBlockCount} kart",
                    color = DilMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${stateLabel(lesson.state)} · ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(lesson.updatedAtEpochMillis))}",
                    color = DilMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.DeleteOutline, "Dersi sil", tint = DilMuted) }
        }
    }
}

private fun rangeLabel(range: ContentRange): String = when (range) {
    is ContentRange.Pages -> "${range.startPage}-${range.endPageInclusive}. sayfa"
    is ContentRange.Time -> "${timeLabel(range.startMillis)}-${timeLabel(range.endMillisExclusive)}"
}

private fun timeLabel(millis: Long): String {
    val totalSeconds = millis / 1_000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private fun stateLabel(state: LessonJobState): String = when (state) {
    LessonJobState.DRAFT -> "Taslak"
    LessonJobState.CREATED -> "Sunucuya bağlanıyor"
    LessonJobState.INGESTING -> "Kaynak işleniyor"
    LessonJobState.GENERATING -> "Kartlar hazırlanıyor"
    LessonJobState.PAUSED -> "Duraklatıldı"
    LessonJobState.READY -> "Hazır"
    LessonJobState.FAILED -> "Hata oluştu"
    LessonJobState.CANCELLED -> "Durduruldu"
}
