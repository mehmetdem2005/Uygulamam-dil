package com.mehmetdem.dil.feature.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mehmetdem.dil.core.designsystem.DilElevatedCard
import com.mehmetdem.dil.core.designsystem.DilSectionHeader
import com.mehmetdem.dil.core.designsystem.DilStatusPill
import com.mehmetdem.dil.core.model.ContentRange
import com.mehmetdem.dil.core.model.LessonBlock
import com.mehmetdem.dil.core.model.LessonSessionConfig
import com.mehmetdem.dil.core.model.SourceKind
import com.mehmetdem.dil.core.model.TimecodeParser
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonWorkspaceScreen(
    config: LessonSessionConfig,
    blocks: List<LessonBlock>,
    isRunning: Boolean,
    onBack: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var cardWidth by remember(config.format.cardWidthFraction) {
        mutableFloatStateOf(config.format.cardWidthFraction)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(config.format.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = onMore) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Ders menüsü")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { SourceStage(config = config) }

            item {
                SessionControlBar(
                    isRunning = isRunning,
                    completed = blocks.size,
                    total = config.format.totalBlockCount,
                    onPause = onPause,
                    onResume = onResume,
                    onStop = onStop,
                )
            }

            item {
                DilElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Kart genişliği", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "%${(cardWidth * 100).roundToInt()}",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    Slider(
                        value = cardWidth,
                        onValueChange = { cardWidth = it },
                        valueRange = 0.72f..1f,
                        steps = 6,
                    )
                }
            }

            item {
                DilSectionHeader(
                    title = "Öğretim kartları",
                    action = {
                        DilStatusPill(label = "${blocks.size}/${config.format.totalBlockCount}")
                    },
                )
            }

            if (blocks.isEmpty()) {
                item {
                    PendingLessonCard(isRunning = isRunning, widthFraction = cardWidth)
                }
            } else {
                items(blocks, key = { it.index }) { block ->
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        LessonBlockCard(
                            block = block,
                            modifier = Modifier.fillMaxWidth(cardWidth),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceStage(config: LessonSessionConfig) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171820)),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    imageVector = if (config.source.kind == SourceKind.YOUTUBE) Icons.Filled.PlayCircle else Icons.Filled.Description,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(62.dp),
                )
                Text(
                    text = config.source.displayName,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = rangeLabel(config.source.range),
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun SessionControlBar(
    isRunning: Boolean,
    completed: Int,
    total: Int,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
) {
    DilElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isRunning) "Ders hazırlanıyor" else "Ders duraklatıldı",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "$completed / $total kart işlendi",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            IconButton(onClick = if (isRunning) onPause else onResume) {
                Icon(
                    imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isRunning) "Duraklat" else "Devam et",
                )
            }
            IconButton(onClick = onStop) {
                Icon(Icons.Filled.Stop, contentDescription = "Durdur")
            }
        }
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else completed.toFloat() / total },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PendingLessonCard(isRunning: Boolean, widthFraction: Float) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        DilElevatedCard(modifier = Modifier.fillMaxWidth(widthFraction)) {
            Text(
                if (isRunning) "İlk kart bekleniyor" else "Üretim henüz başlatılmadı",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                if (isRunning) {
                    "DeepSeek'ten gelen ilk içerik parçası burada akış halinde görünecek ve tamamlanınca kaydedilecek."
                } else {
                    "Dersi devam ettirdiğinde sıradaki eksik karttan başlanır."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isRunning) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun LessonBlockCard(
    block: LessonBlock,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    block.title ?: "Kart ${block.index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Kartı sesli oku")
            }
            HorizontalDivider()
            block.sourceText?.takeIf(String::isNotBlank)?.let {
                LessonTableRow(label = "Kaynak", value = it)
                HorizontalDivider()
            }
            LessonTableRow(label = "Öğren", value = block.targetText, emphasized = true)
            block.translation?.takeIf(String::isNotBlank)?.let {
                HorizontalDivider()
                LessonTableRow(label = "Anlam", value = it)
            }
            block.pronunciation?.takeIf(String::isNotBlank)?.let {
                HorizontalDivider()
                LessonTableRow(label = "IPA", value = it)
            }
            block.explanation?.takeIf(String::isNotBlank)?.let {
                HorizontalDivider()
                LessonTableRow(label = "Açıklama", value = it)
            }
        }
    }
}

@Composable
private fun LessonTableRow(label: String, value: String, emphasized: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = if (emphasized) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

private fun rangeLabel(range: ContentRange): String = when (range) {
    is ContentRange.Time -> "${TimecodeParser.formatMillis(range.startMillis)} – ${TimecodeParser.formatMillis(range.endMillisExclusive)}"
    is ContentRange.Pages -> "${range.startPage}. – ${range.endPageInclusive}. sayfa"
}
