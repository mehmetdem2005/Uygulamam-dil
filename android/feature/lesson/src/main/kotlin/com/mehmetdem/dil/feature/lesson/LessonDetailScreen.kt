package com.mehmetdem.dil.feature.lesson

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

@Composable
fun LessonDetailScreen(
    lesson: StoredLesson,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Dersi sil?") },
            text = { Text("Bu ders ve yerel ilerleme kaydı cihazdan kaldırılacak.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Sil", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Vazgeç") } },
        )
    }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().widthIn(max = DilMaxContentWidth),
            contentPadding = PaddingValues(horizontal = DilOuterPadding, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") }
                    Column(Modifier.weight(1f)) {
                        Text(
                            lesson.config.format.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(lesson.config.source.displayName, color = DilMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.DeleteOutline, "Dersi sil", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            item { SourceSummaryCard(lesson) }
            item { ProgressCard(lesson) }
            item {
                Text("Ders formatı", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            items(lesson.config.format.orderedFields(), key = { it.id }) { field ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, DilBorder),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                    ) {
                        Box(Modifier.size(38.dp).background(DilBlueSoft, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.EditNote, null, tint = DilTeal)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(field.label, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${fieldTypeLabel(field.type.name)}${if (field.speakable) " · seslendirilebilir" else ""}",
                                color = DilMuted,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DilBlueSoft.copy(alpha = .35f)),
                    border = BorderStroke(1.dp, Color(0xFFCBE3F0)),
                    shape = RoundedCornerShape(15.dp),
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("İşleme bilgisi", fontWeight = FontWeight.Bold)
                        Text(
                            "${lesson.config.format.totalBlockCount} kart · İstek başına ${lesson.config.format.blocksPerRequest} kart · ${lesson.config.format.requestIntervalSeconds} sn aralık",
                            color = DilMuted,
                        )
                        Text(
                            if (lesson.generationMetrics.providerRequestCount > 0) {
                                "${lesson.generationMetrics.providerRequestCount} gerçek API isteği · ${lesson.generationMetrics.totalTokens} token"
                            } else {
                                "Kart içeriği sunucudan dönmeden örnek metin gösterilmez."
                            },
                            color = DilMuted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.weight(1f).heightIn(min = 54.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Filled.DeleteOutline, null)
                        Text("Sil", modifier = Modifier.padding(start = 7.dp))
                    }
                    Button(
                        onClick = onContinue,
                        modifier = Modifier.weight(1.8f).heightIn(min = 54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DilTeal),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(if (lesson.state == LessonJobState.READY) "Dersi Aç" else "Derse Devam Et")
                        Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.padding(start = 7.dp))
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun SourceSummaryCard(lesson: StoredLesson) {
    val isPdf = lesson.config.source.kind == SourceKind.PDF
    Card(
        colors = CardDefaults.cardColors(containerColor = if (isPdf) DilBlueSoft.copy(alpha = .38f) else DilGreenSoft.copy(alpha = .45f)),
        border = BorderStroke(1.dp, if (isPdf) Color(0xFFCBE3F0) else Color(0xFFCDEEDC)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(54.dp).background(Color.White, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                Icon(if (isPdf) Icons.Filled.Description else Icons.Filled.PlayArrow, null, tint = if (isPdf) Color(0xFF1478F2) else DilTeal)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(if (isPdf) "PDF kaynağı" else "YouTube kaynağı", fontWeight = FontWeight.Bold)
                Text(lesson.config.source.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(sourceRangeLabel(lesson.config.source.range), color = DilMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ProgressCard(lesson: StoredLesson) {
    val progress = if (lesson.config.format.totalBlockCount == 0) 0f
    else lesson.completedBlockCount.toFloat() / lesson.config.format.totalBlockCount
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, DilBorder),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(jobStateLabel(lesson.state), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Son kayıt: ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(lesson.updatedAtEpochMillis))}",
                        color = DilMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Icon(Icons.Filled.Schedule, null, tint = DilTeal)
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(7.dp),
                color = DilTeal,
                trackColor = DilBorder,
            )
            Text("${lesson.completedBlockCount} / ${lesson.config.format.totalBlockCount} kart tamamlandı", color = DilMuted)
        }
    }
}

private fun sourceRangeLabel(range: ContentRange): String = when (range) {
    is ContentRange.Pages -> "${range.startPage}. sayfa - ${range.endPageInclusive}. sayfa"
    is ContentRange.Time -> "${timeLabel(range.startMillis)} - ${timeLabel(range.endMillisExclusive)}"
}

private fun timeLabel(millis: Long): String {
    val seconds = millis / 1_000
    return "%02d:%02d".format(seconds / 60, seconds % 60)
}

private fun jobStateLabel(state: LessonJobState): String = when (state) {
    LessonJobState.DRAFT -> "Taslak"
    LessonJobState.CREATED -> "Sunucu bağlantısı hazırlanıyor"
    LessonJobState.INGESTING -> "Kaynak işleniyor"
    LessonJobState.GENERATING -> "Kartlar hazırlanıyor"
    LessonJobState.PAUSED -> "İşlem duraklatıldı"
    LessonJobState.READY -> "Ders hazır"
    LessonJobState.FAILED -> "İşlem başarısız"
    LessonJobState.CANCELLED -> "İşlem durduruldu"
}

private fun fieldTypeLabel(type: String): String = when (type) {
    "SHORT_TEXT" -> "Kısa metin"
    "LONG_TEXT" -> "Uzun metin"
    "PRONUNCIATION" -> "Telaffuz"
    "EXAMPLES" -> "Örnekler"
    "QUIZ" -> "Alıştırma"
    else -> type
}
