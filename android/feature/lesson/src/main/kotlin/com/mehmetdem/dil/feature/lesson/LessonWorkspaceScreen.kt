package com.mehmetdem.dil.feature.lesson

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.ui.viewinterop.AndroidView
import com.mehmetdem.dil.core.designsystem.DilBlueSoft
import com.mehmetdem.dil.core.designsystem.DilBorder
import com.mehmetdem.dil.core.designsystem.DilGreenSoft
import com.mehmetdem.dil.core.designsystem.DilMaxContentWidth
import com.mehmetdem.dil.core.designsystem.DilMuted
import com.mehmetdem.dil.core.designsystem.DilOuterPadding
import com.mehmetdem.dil.core.designsystem.DilTeal
import com.mehmetdem.dil.core.model.ContentRange
import com.mehmetdem.dil.core.model.LessonBlock
import com.mehmetdem.dil.core.model.LessonJobState
import com.mehmetdem.dil.core.model.SourceKind
import com.mehmetdem.dil.core.model.StoredLesson
import com.mehmetdem.dil.core.model.YouTubeVideoIdParser
import kotlin.math.ceil

@Composable
fun LessonWorkspaceScreen(
    lesson: StoredLesson,
    blocks: List<LessonBlock>,
    onBack: () -> Unit,
    onOpenDetails: () -> Unit,
    onDelete: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val requestTotal = ceil(lesson.config.format.totalBlockCount.toDouble() / lesson.config.format.blocksPerRequest).toInt()

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Dersi sil?") },
            text = { Text("Cihazdaki ders kaydı ve ilerleme bilgisi kaldırılacak.") },
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
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") }
                    Text("▣", color = DilTeal, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "  Öğrenme Oturumu",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.DeleteOutline, "Dersi sil", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            item { RequestStatusCard(lesson, blocks.size, requestTotal) }
            item { GenerationControlCard(lesson, onPause, onResume, onRetry) }
            item { SourceSummary(lesson) }
            if (blocks.isEmpty()) {
                item { EmptyResultCard(lesson.state) }
            } else {
                items(blocks, key = LessonBlock::index) { block ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                        GeneratedCard(
                            lesson,
                            block,
                            Modifier.fillMaxWidth(lesson.config.format.cardWidthFraction),
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                        shape = RoundedCornerShape(13.dp),
                    ) {
                        Icon(Icons.Filled.DeleteOutline, null)
                        Text("Sil", modifier = Modifier.padding(start = 6.dp))
                    }
                    Button(
                        onClick = onOpenDetails,
                        modifier = Modifier.weight(1.8f).heightIn(min = 52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DilTeal),
                        shape = RoundedCornerShape(13.dp),
                    ) {
                        Text("Ders Ayrıntıları")
                        Icon(Icons.Filled.OpenInNew, null, modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun RequestStatusCard(lesson: StoredLesson, completedCards: Int, requestTotal: Int) {
    val running = lesson.state == LessonJobState.INGESTING || lesson.state == LessonJobState.GENERATING
    val completedRequests = ceil(completedCards.toDouble() / lesson.config.format.blocksPerRequest).toInt()
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, DilBorder),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.size(40.dp).background(DilBlueSoft, RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                Icon(if (running) Icons.Filled.Sync else Icons.Filled.CheckCircle, null, tint = DilTeal)
            }
            Column(Modifier.weight(1f)) {
                Text("${completedRequests.coerceAtMost(requestTotal)} / $requestTotal istek sonucu", fontWeight = FontWeight.Bold)
                Text(workspaceStateText(lesson.state), color = DilMuted)
            }
            Text(
                if (running) "● İşleniyor" else "● Kaydedildi",
                color = if (running) Color(0xFF168CC0) else Color(0xFF16A56D),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun GenerationControlCard(
    lesson: StoredLesson,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
) {
    val metrics = lesson.generationMetrics
    val hasControl = lesson.state in setOf(LessonJobState.GENERATING, LessonJobState.PAUSED, LessonJobState.FAILED)
    if (!hasControl && lesson.lastSyncError.isNullOrBlank() && metrics.providerRequestCount == 0) return
    Card(
        colors = CardDefaults.cardColors(containerColor = if (lesson.lastSyncError.isNullOrBlank()) Color.White else Color(0xFFFFF7F7)),
        border = BorderStroke(1.dp, if (lesson.lastSyncError.isNullOrBlank()) DilBorder else Color(0xFFF3C9CE)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            if (!lesson.lastSyncError.isNullOrBlank()) {
                Text("Bağlantı / üretim bilgisi", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Text(lesson.lastSyncError.orEmpty(), color = DilMuted, style = MaterialTheme.typography.bodyMedium)
            }
            if (metrics.providerRequestCount > 0) {
                Text(
                    "${metrics.providerRequestCount} API isteği · ${metrics.totalTokens} token · ${metrics.providerLatencyMillis / 1_000.0} sn sağlayıcı süresi",
                    color = DilMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            when (lesson.state) {
                LessonJobState.GENERATING -> OutlinedButton(onClick = onPause, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Icon(Icons.Filled.PauseCircle, null)
                    Text("Üretimi Duraklat", modifier = Modifier.padding(start = 7.dp))
                }
                LessonJobState.PAUSED -> Button(
                    onClick = onResume,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DilTeal),
                ) {
                    Icon(Icons.Filled.PlayArrow, null)
                    Text("Kaldığı Yerden Devam Et", modifier = Modifier.padding(start = 7.dp))
                }
                LessonJobState.FAILED -> Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DilTeal),
                ) {
                    Icon(Icons.Filled.Refresh, null)
                    Text("Yeniden Dene", modifier = Modifier.padding(start = 7.dp))
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun SourceSummary(lesson: StoredLesson) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, DilBorder),
        shape = RoundedCornerShape(16.dp),
    ) {
        if (lesson.config.source.kind == SourceKind.YOUTUBE) YouTubeEmbed(lesson) else {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 88.dp).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(52.dp).background(DilBlueSoft, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Description, null, tint = Color(0xFF1478F2))
                }
                Column {
                    Text(lesson.config.source.displayName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(rangeText(lesson.config.source.range), color = DilMuted)
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YouTubeEmbed(lesson: StoredLesson) {
    val videoId = YouTubeVideoIdParser.parse(lesson.config.source.locator)
    val range = lesson.config.source.range as? ContentRange.Time
    if (videoId == null || range == null) return
    val start = range.startMillis / 1_000
    val end = range.endMillisExclusive / 1_000
    AndroidView(
        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.mediaPlaybackRequiresUserGesture = true
                webViewClient = WebViewClient()
                loadDataWithBaseURL(
                    "https://www.youtube.com",
                    """<html><body style='margin:0;background:#111827'><iframe width='100%' height='100%' src='https://www.youtube.com/embed/$videoId?start=$start&end=$end&playsinline=1' frameborder='0' allow='accelerometer; encrypted-media' allowfullscreen></iframe></body></html>""",
                    "text/html",
                    "UTF-8",
                    null,
                )
            }
        },
    )
}

@Composable
private fun EmptyResultCard(state: LessonJobState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DilBlueSoft.copy(alpha = .28f)),
        border = BorderStroke(1.dp, Color(0xFFCBE3F0)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.Info, null, tint = DilTeal, modifier = Modifier.size(38.dp))
            Text("Henüz oluşturulmuş kart yok", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(
                if (state == LessonJobState.CREATED) {
                    "Ders ayarları kaydedildi; güvenli sunucu oturumu hazırlanıyor."
                } else {
                    workspaceStateText(state)
                },
                color = DilMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun GeneratedCard(lesson: StoredLesson, block: LessonBlock, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, DilBorder),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Kart ${block.index + 1}",
                    color = DilTeal,
                    modifier = Modifier.background(DilGreenSoft, RoundedCornerShape(7.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF15A368), modifier = Modifier.size(18.dp))
                    Text("Kaydedildi", color = Color(0xFF15A368))
                }
            }
            lesson.config.format.orderedFields().forEach { field ->
                val value = block.fieldValues[field.key] ?: when (field.key) {
                    "source_text" -> block.sourceText
                    "translation" -> block.translation
                    "explanation" -> block.explanation
                    "pronunciation" -> block.pronunciation
                    else -> null
                }
                if (!value.isNullOrBlank()) {
                    Text(field.label, color = DilMuted, style = MaterialTheme.typography.labelLarge)
                    Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            if (block.fieldValues.isEmpty() && block.targetText.isNotBlank()) {
                Text(block.targetText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            LinearProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = DilTeal,
                trackColor = DilBorder,
            )
        }
    }
}

private fun workspaceStateText(state: LessonJobState): String = when (state) {
    LessonJobState.DRAFT -> "Taslak"
    LessonJobState.CREATED -> "Ders ayarları cihazda saklandı"
    LessonJobState.INGESTING -> "Kaynak sunucuda işleniyor"
    LessonJobState.GENERATING -> "Ders kartları hazırlanıyor"
    LessonJobState.PAUSED -> "İşlem duraklatıldı"
    LessonJobState.READY -> "Ders hazır"
    LessonJobState.FAILED -> "İşlem başarısız"
    LessonJobState.CANCELLED -> "İşlem durduruldu"
}

private fun rangeText(range: ContentRange): String = when (range) {
    is ContentRange.Time -> "${timeText(range.startMillis)}–${timeText(range.endMillisExclusive)}"
    is ContentRange.Pages -> "${range.startPage}–${range.endPageInclusive}. sayfalar"
}

private fun timeText(millis: Long): String {
    val seconds = millis / 1_000
    return "%02d:%02d".format(seconds / 60, seconds % 60)
}
