package com.mehmetdem.dil.feature.lesson

import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mehmetdem.dil.core.designsystem.DilElevatedCard
import com.mehmetdem.dil.core.designsystem.DilSectionHeader
import com.mehmetdem.dil.core.model.ContentRange
import com.mehmetdem.dil.core.model.LessonConfigValidator
import com.mehmetdem.dil.core.model.LessonFormat
import com.mehmetdem.dil.core.model.LessonSessionConfig
import com.mehmetdem.dil.core.model.PlaybackPreferences
import com.mehmetdem.dil.core.model.SourceKind
import com.mehmetdem.dil.core.model.SourceSelection
import com.mehmetdem.dil.core.model.TimecodeParser
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewLessonScreen(
    initialSourceKind: SourceKind = SourceKind.YOUTUBE,
    onBack: () -> Unit,
    onCreate: (LessonSessionConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var sourceKindName by rememberSaveable { mutableStateOf(initialSourceKind.name) }
    val sourceKind = SourceKind.valueOf(sourceKindName)
    var youtubeUrl by rememberSaveable { mutableStateOf("") }
    var pdfUri by rememberSaveable { mutableStateOf("") }
    var pdfName by rememberSaveable { mutableStateOf("") }
    var startValue by rememberSaveable { mutableStateOf(if (sourceKind == SourceKind.YOUTUBE) "00:00" else "1") }
    var endValue by rememberSaveable { mutableStateOf(if (sourceKind == SourceKind.YOUTUBE) "01:00" else "1") }
    var lessonTitle by rememberSaveable { mutableStateOf("") }
    var instruction by rememberSaveable {
        mutableStateOf("Cümleyi göster, Türkçe anlamını ver ve ardından anlaşılır biçimde açıkla.")
    }
    var totalCards by rememberSaveable { mutableIntStateOf(10) }
    var cardsPerRequest by rememberSaveable { mutableIntStateOf(1) }
    var cardWidth by rememberSaveable { mutableFloatStateOf(1f) }
    var ttsEnabled by rememberSaveable { mutableStateOf(true) }
    var voiceCommandsEnabled by rememberSaveable { mutableStateOf(false) }
    var backgroundEnabled by rememberSaveable { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            pdfUri = uri.toString()
            pdfName = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
                ?: "Seçilen PDF"
            formError = null
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Yeni ders") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                DilElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    DilSectionHeader(title = "1. Kaynak")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = sourceKind == SourceKind.YOUTUBE,
                            onClick = {
                                sourceKindName = SourceKind.YOUTUBE.name
                                startValue = "00:00"
                                endValue = "01:00"
                                formError = null
                            },
                            label = { Text("YouTube") },
                            leadingIcon = { Icon(Icons.Filled.PlayCircle, contentDescription = null) },
                        )
                        FilterChip(
                            selected = sourceKind == SourceKind.PDF,
                            onClick = {
                                sourceKindName = SourceKind.PDF.name
                                startValue = "1"
                                endValue = "1"
                                formError = null
                            },
                            label = { Text("PDF") },
                            leadingIcon = { Icon(Icons.Filled.Description, contentDescription = null) },
                        )
                    }

                    if (sourceKind == SourceKind.YOUTUBE) {
                        OutlinedTextField(
                            value = youtubeUrl,
                            onValueChange = {
                                youtubeUrl = it
                                formError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("YouTube video bağlantısı") },
                            placeholder = { Text("https://youtube.com/watch?v=...") },
                            singleLine = true,
                        )
                    } else {
                        OutlinedButton(
                            onClick = { pdfPicker.launch(arrayOf("application/pdf")) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            contentPadding = PaddingValues(vertical = 15.dp),
                        ) {
                            Icon(Icons.Filled.Description, contentDescription = null)
                            Text(
                                text = if (pdfName.isBlank()) "PDF dosyası seç" else pdfName,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }

                    RangeFields(
                        sourceKind = sourceKind,
                        startValue = startValue,
                        endValue = endValue,
                        onStartChange = { startValue = it; formError = null },
                        onEndChange = { endValue = it; formError = null },
                    )
                }
            }

            item {
                DilElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    DilSectionHeader(title = "2. Öğretim formatı")
                    OutlinedTextField(
                        value = lessonTitle,
                        onValueChange = { lessonTitle = it; formError = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Ders adı") },
                        placeholder = { Text("Örneğin: B2 İngilizce cümle çalışması") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = instruction,
                        onValueChange = { instruction = it; formError = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Bana nasıl öğretmeli?") },
                        supportingText = {
                            Text("Çeviri, IPA, gramer, örnek, soru veya başka bir ders biçimini burada belirle.")
                        },
                        minLines = 4,
                        maxLines = 8,
                    )
                }
            }

            item {
                DilElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    DilSectionHeader(title = "3. Akış")
                    SliderSetting(
                        label = "Toplam kart",
                        valueText = totalCards.toString(),
                        value = totalCards.toFloat(),
                        range = 1f..50f,
                        steps = 48,
                        onValueChange = {
                            totalCards = it.roundToInt()
                            cardsPerRequest = cardsPerRequest.coerceAtMost(totalCards)
                        },
                    )
                    SliderSetting(
                        label = "Her API isteğinde kart",
                        valueText = cardsPerRequest.toString(),
                        value = cardsPerRequest.toFloat(),
                        range = 1f..totalCards.coerceAtMost(10).toFloat(),
                        steps = (totalCards.coerceAtMost(10) - 2).coerceAtLeast(0),
                        onValueChange = { cardsPerRequest = it.roundToInt() },
                    )
                    SliderSetting(
                        label = "Kart genişliği",
                        valueText = "%${(cardWidth * 100).roundToInt()}",
                        value = cardWidth,
                        range = 0.72f..1f,
                        steps = 6,
                        onValueChange = { cardWidth = it },
                    )
                    HorizontalDivider()
                    ToggleRow(
                        title = "Sesli oku",
                        description = "Kartları Edge TTS sesiyle oynatır.",
                        checked = ttsEnabled,
                        onCheckedChange = {
                            ttsEnabled = it
                            if (!it) backgroundEnabled = false
                        },
                    )
                    ToggleRow(
                        title = "Mikrofon komutları",
                        description = "Devam, tekrar, dur ve açıkla komutlarını dinler.",
                        checked = voiceCommandsEnabled,
                        onCheckedChange = { voiceCommandsEnabled = it },
                    )
                    ToggleRow(
                        title = "Ekran kapalı devam et",
                        description = "Görünür bildirimli ders oturumu kullanır.",
                        checked = backgroundEnabled,
                        onCheckedChange = {
                            backgroundEnabled = it
                            if (it) ttsEnabled = true
                        },
                    )
                }
            }

            if (formError != null) {
                item {
                    Text(
                        text = formError.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        val result = buildConfig(
                            sourceKind = sourceKind,
                            youtubeUrl = youtubeUrl,
                            pdfUri = pdfUri,
                            pdfName = pdfName,
                            startValue = startValue,
                            endValue = endValue,
                            lessonTitle = lessonTitle,
                            instruction = instruction,
                            totalCards = totalCards,
                            cardsPerRequest = cardsPerRequest,
                            cardWidth = cardWidth,
                            ttsEnabled = ttsEnabled,
                            voiceCommandsEnabled = voiceCommandsEnabled,
                            backgroundEnabled = backgroundEnabled,
                        )
                        result.fold(
                            onSuccess = { config ->
                                val issues = LessonConfigValidator.validate(config)
                                if (issues.isEmpty()) onCreate(config) else formError = issues.first().message
                            },
                            onFailure = { formError = it.message ?: "Ders ayarları geçersiz." },
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    contentPadding = PaddingValues(vertical = 16.dp),
                ) {
                    Text("Ders çalışma alanını aç")
                }
            }
        }
    }
}

@Composable
private fun RangeFields(
    sourceKind: SourceKind,
    startValue: String,
    endValue: String,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = startValue,
            onValueChange = onStartChange,
            modifier = Modifier.weight(1f),
            label = { Text(if (sourceKind == SourceKind.YOUTUBE) "Başlangıç" else "İlk sayfa") },
            placeholder = { Text(if (sourceKind == SourceKind.YOUTUBE) "00:00" else "1") },
            keyboardOptions = KeyboardOptions(keyboardType = if (sourceKind == SourceKind.PDF) KeyboardType.Number else KeyboardType.Text),
            singleLine = true,
        )
        OutlinedTextField(
            value = endValue,
            onValueChange = onEndChange,
            modifier = Modifier.weight(1f),
            label = { Text(if (sourceKind == SourceKind.YOUTUBE) "Bitiş" else "Son sayfa") },
            placeholder = { Text(if (sourceKind == SourceKind.YOUTUBE) "01:00" else "1") },
            keyboardOptions = KeyboardOptions(keyboardType = if (sourceKind == SourceKind.PDF) KeyboardType.Number else KeyboardType.Text),
            singleLine = true,
        )
    }
}

@Composable
private fun SliderSetting(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(valueText, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun buildConfig(
    sourceKind: SourceKind,
    youtubeUrl: String,
    pdfUri: String,
    pdfName: String,
    startValue: String,
    endValue: String,
    lessonTitle: String,
    instruction: String,
    totalCards: Int,
    cardsPerRequest: Int,
    cardWidth: Float,
    ttsEnabled: Boolean,
    voiceCommandsEnabled: Boolean,
    backgroundEnabled: Boolean,
): Result<LessonSessionConfig> = runCatching {
    val range = when (sourceKind) {
        SourceKind.YOUTUBE -> {
            val start = TimecodeParser.parseMillis(startValue)
                ?: error("Başlangıç zamanı SS, DD:SS veya SS:DD:SS biçiminde olmalıdır.")
            val end = TimecodeParser.parseMillis(endValue)
                ?: error("Bitiş zamanı SS, DD:SS veya SS:DD:SS biçiminde olmalıdır.")
            ContentRange.Time(start, end)
        }

        SourceKind.PDF -> ContentRange.Pages(
            startPage = startValue.toIntOrNull() ?: error("İlk sayfa sayı olmalıdır."),
            endPageInclusive = endValue.toIntOrNull() ?: error("Son sayfa sayı olmalıdır."),
        )
    }

    LessonSessionConfig(
        source = SourceSelection(
            kind = sourceKind,
            locator = if (sourceKind == SourceKind.YOUTUBE) youtubeUrl.trim() else pdfUri,
            displayName = if (sourceKind == SourceKind.YOUTUBE) "YouTube videosu" else pdfName.ifBlank { "PDF" },
            range = range,
        ),
        format = LessonFormat(
            title = lessonTitle.trim(),
            instruction = instruction.trim(),
            totalBlockCount = totalCards,
            blocksPerRequest = cardsPerRequest,
            cardWidthFraction = cardWidth,
            playback = PlaybackPreferences(
                ttsEnabled = ttsEnabled,
                voiceCommandsEnabled = voiceCommandsEnabled,
                continueWhenScreenOff = backgroundEnabled,
            ),
        ),
    )
}
