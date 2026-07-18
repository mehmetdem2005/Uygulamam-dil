package com.mehmetdem.dil.feature.lesson

import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Summarize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import com.mehmetdem.dil.core.model.FormatFieldType
import com.mehmetdem.dil.core.model.LessonConfigValidator
import com.mehmetdem.dil.core.model.LessonFormat
import com.mehmetdem.dil.core.model.LessonFormatField
import com.mehmetdem.dil.core.model.LessonSessionConfig
import com.mehmetdem.dil.core.model.PlaybackPreferences
import com.mehmetdem.dil.core.model.SourceKind
import com.mehmetdem.dil.core.model.SourceSelection
import com.mehmetdem.dil.core.model.TeachingMode
import com.mehmetdem.dil.core.model.TimecodeParser
import com.mehmetdem.dil.core.model.YouTubeVideoIdParser

@Composable
fun NewLessonScreen(
    initialSourceKind: SourceKind = SourceKind.YOUTUBE,
    onBack: () -> Unit,
    onCreate: (LessonSessionConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedSource by remember { mutableStateOf<SelectedSource?>(null) }
    if (selectedSource == null) {
        SourceSelectionScreen(
            initialSourceKind = initialSourceKind,
            onBack = onBack,
            onContinue = { source, teachingLanguage, targetLanguage ->
                selectedSource = SelectedSource(source, teachingLanguage, targetLanguage)
            },
            modifier = modifier,
        )
    } else {
        val selection = requireNotNull(selectedSource)
        FormatDesignerScreen(
            source = selection.source,
            initialTeachingLanguage = selection.teachingLanguage,
            initialTargetLanguage = selection.targetLanguage,
            onBack = { selectedSource = null },
            onCreate = onCreate,
            modifier = modifier,
        )
    }
}

private data class SelectedSource(
    val source: SourceSelection,
    val teachingLanguage: String,
    val targetLanguage: String,
)

@Composable
private fun SourceSelectionScreen(
    initialSourceKind: SourceKind,
    onBack: () -> Unit,
    onContinue: (SourceSelection, String, String) -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    var kindName by rememberSaveable { mutableStateOf(initialSourceKind.name) }
    val kind = SourceKind.valueOf(kindName)
    var youtubeUrl by rememberSaveable { mutableStateOf("") }
    var pdfUri by rememberSaveable { mutableStateOf("") }
    var pdfName by rememberSaveable { mutableStateOf("") }
    var start by rememberSaveable { mutableStateOf(if (kind == SourceKind.YOUTUBE) "01:20" else "1") }
    var end by rememberSaveable { mutableStateOf(if (kind == SourceKind.YOUTUBE) "08:40" else "5") }
    var lessonLanguage by rememberSaveable { mutableStateOf("Türkçe") }
    var outputLanguage by rememberSaveable { mutableStateOf("İngilizce") }
    var error by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            pdfUri = uri.toString()
            pdfName = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: "Seçilen PDF"
            error = null
        }
    }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().widthIn(max = DilMaxContentWidth),
            contentPadding = PaddingValues(horizontal = DilOuterPadding, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") }
                        Text("Ders Oluştur", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("3 / 9", color = DilTeal, fontWeight = FontWeight.Bold)
                    }
                    Text("●━━━━●━━━━●━━━━○━━━━○━━━━○", color = DilTeal, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Text("Kaynağı Seç", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Text("Dersinin kaynağını seç ve detayları gir.", color = DilMuted)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SourceChoiceCard(
                        title = "YouTube",
                        subtitle = "Video bağlantısından içerik seç ve dönüştür",
                        icon = Icons.Filled.PlayArrow,
                        selected = kind == SourceKind.YOUTUBE,
                        background = DilRedSoft,
                        onClick = { kindName = SourceKind.YOUTUBE.name; start = "01:20"; end = "08:40" },
                        modifier = Modifier.weight(1f),
                    )
                    SourceChoiceCard(
                        title = "PDF",
                        subtitle = "PDF dosyasından ders oluştur",
                        icon = Icons.Filled.Description,
                        selected = kind == SourceKind.PDF,
                        background = DilBlueSoft,
                        onClick = { kindName = SourceKind.PDF.name; start = "1"; end = "5" },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (kind == SourceKind.YOUTUBE) {
                item {
                    LabeledField("YouTube Video URL") {
                        OutlinedTextField(
                            value = youtubeUrl,
                            onValueChange = { youtubeUrl = it; error = null },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Filled.Link, null) },
                            trailingIcon = { if (youtubeUrl.isNotEmpty()) IconButton(onClick = { youtubeUrl = "" }) { Icon(Icons.Filled.Close, "Temizle") } },
                            placeholder = { Text("https://www.youtube.com/watch?v=...") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = DilBorder),
                        )
                        Text("YouTube bağlantısını yapıştırın.", color = DilMuted, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                item {
                    LabeledField("PDF Dosyası") {
                        OutlinedButton(onClick = { picker.launch(arrayOf("application/pdf")) }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Filled.Description, null)
                            Text(if (pdfName.isBlank()) "PDF dosyası seç" else pdfName, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
            item {
                LabeledField(if (kind == SourceKind.YOUTUBE) "Zaman Aralığı (opsiyonel)" else "Sayfa Aralığı") {
                    Text(if (kind == SourceKind.YOUTUBE) "Dersini yalnızca seçtiğin bölümden oluşturacağız." else "En fazla 50 sayfalık bir aralık seçebilirsin.", color = DilMuted, style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        RangeInput(start, { start = it; error = null }, if (kind == SourceKind.YOUTUBE) "Başlangıç" else "İlk sayfa", kind, Modifier.weight(1f))
                        Text("—", color = DilMuted)
                        RangeInput(end, { end = it; error = null }, if (kind == SourceKind.YOUTUBE) "Bitiş" else "Son sayfa", kind, Modifier.weight(1f))
                    }
                    Text("Belirtilen aralık dışındaki içerikler işlenmeyecektir.", color = DilMuted, style = MaterialTheme.typography.bodyMedium)
                }
            }
            item { LanguageSelector("Ders Dili", lessonLanguage) { lessonLanguage = if (lessonLanguage == "Türkçe") "İngilizce" else "Türkçe" } }
            item { LanguageSelector("Çıktı Dili", outputLanguage) { outputLanguage = if (outputLanguage == "Türkçe") "İngilizce" else "Türkçe" } }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = DilBlueSoft.copy(alpha = .38f)), border = BorderStroke(1.dp, Color(0xFFCEE7F1)), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.Info, null, tint = DilTeal)
                        Text("Yalnızca seçtiğin aralıktaki içerik işlenecek ve ders materyalleri bu bölümden oluşturulacaktır.")
                    }
                }
            }
            if (error != null) item { Text(error.orEmpty(), color = MaterialTheme.colorScheme.error) }
            item {
                Button(
                    onClick = {
                        runCatching {
                            if (kind == SourceKind.YOUTUBE) {
                                require(YouTubeVideoIdParser.parse(youtubeUrl) != null) { "Geçerli bir YouTube bağlantısı girin." }
                                val startMs = requireNotNull(TimecodeParser.parseMillis(start)) { "Başlangıç zamanı geçersiz." }
                                val endMs = requireNotNull(TimecodeParser.parseMillis(end)) { "Bitiş zamanı geçersiz." }
                                SourceSelection(kind, youtubeUrl, "YouTube", ContentRange.Time(startMs, endMs))
                            } else {
                                require(pdfUri.isNotBlank()) { "Bir PDF dosyası seçin." }
                                SourceSelection(kind, pdfUri, pdfName, ContentRange.Pages(start.toInt(), end.toInt()))
                            }
                        }.fold(
                            onSuccess = { onContinue(it, lessonLanguage, outputLanguage) },
                            onFailure = { error = it.message ?: "Kaynak aralığı geçersiz." },
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DilTeal),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Formatı Belirle", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Filled.ArrowForward, null)
                }
            }
            item { Spacer(Modifier.height(4.dp)) }
        }
    }
}

@Composable
private fun SourceChoiceCard(title: String, subtitle: String, icon: ImageVector, selected: Boolean, background: Color, onClick: () -> Unit, modifier: Modifier) {
    Card(
        modifier = modifier.height(118.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (selected) background.copy(alpha = .48f) else Color.White),
        border = BorderStroke(1.dp, if (selected) background else DilBorder),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Icon(icon, null, tint = if (title == "YouTube") Color(0xFFE60020) else Color(0xFF1478F2))
                Icon(if (selected) Icons.Filled.CheckCircle else Icons.Filled.Remove, null, tint = if (selected) DilTeal else DilMuted)
            }
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = DilMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun LabeledField(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun RangeInput(value: String, onChange: (String) -> Unit, label: String, kind: SourceKind, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = if (kind == SourceKind.PDF) KeyboardType.Number else KeyboardType.Text),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = DilBorder),
    )
}

@Composable
private fun LanguageSelector(title: String, value: String, onClick: () -> Unit) {
    LabeledField(title) {
        Card(modifier = Modifier.fillMaxWidth().height(52.dp).clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, DilBorder), shape = RoundedCornerShape(14.dp)) {
            Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (value == "Türkçe") "🇹🇷" else "🇬🇧")
                Text(value, modifier = Modifier.padding(start = 10.dp).weight(1f))
                Text("⌄", color = DilMuted)
            }
        }
        Text(if (title == "Ders Dili") "Ders içeriği bu dilde oluşturulacaktır." else "Ders materyalleri bu dilde hazırlanacaktır.", color = DilMuted, style = MaterialTheme.typography.bodyMedium)
    }
}

private data class EditableField(val id: Int, val key: String, val label: String, val type: FormatFieldType, val speakable: Boolean)

@Composable
private fun FormatDesignerScreen(
    source: SourceSelection,
    initialTeachingLanguage: String,
    initialTargetLanguage: String,
    onBack: () -> Unit,
    onCreate: (LessonSessionConfig) -> Unit,
    modifier: Modifier,
) {
    var modeName by rememberSaveable { mutableStateOf(TeachingMode.LANGUAGE.name) }
    val mode = TeachingMode.valueOf(modeName)
    var title by rememberSaveable { mutableStateOf("$initialTargetLanguage — $initialTeachingLanguage Örnek Cümleler") }
    var instruction by rememberSaveable { mutableStateOf("Kaynak cümleyi göster, Türkçe karşılığını ver ve kısa bir açıklama ekle.") }
    var level by rememberSaveable { mutableStateOf("A2") }
    var teachingLanguage by rememberSaveable { mutableStateOf(initialTeachingLanguage) }
    var targetLanguage by rememberSaveable { mutableStateOf(initialTargetLanguage) }
    var totalCards by rememberSaveable { mutableIntStateOf(10) }
    var cardsPerRequest by rememberSaveable { mutableIntStateOf(1) }
    var requestInterval by rememberSaveable { mutableIntStateOf(15) }
    var widthPercent by rememberSaveable { mutableIntStateOf(100) }
    var continuous by rememberSaveable { mutableStateOf(true) }
    var screenOff by rememberSaveable { mutableStateOf(true) }
    var microphone by rememberSaveable { mutableStateOf(true) }
    var nextFieldId by remember { mutableIntStateOf(2) }
    var copyGeneration by rememberSaveable { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    val fields = remember(initialTeachingLanguage, initialTargetLanguage) {
        mutableStateListOf(
            EditableField(0, "source_text", initialTargetLanguage, FormatFieldType.SHORT_TEXT, true),
            EditableField(1, "translation", initialTeachingLanguage, FormatFieldType.SHORT_TEXT, true),
        )
    }

    fun move(from: Int, to: Int) {
        if (from !in fields.indices || to !in fields.indices) return
        val item = fields.removeAt(from)
        fields.add(to, item)
    }

    fun updateLanguageLabels() {
        fields.indexOfFirst { it.key == "source_text" }.takeIf { it >= 0 }?.let { index ->
            fields[index] = fields[index].copy(label = targetLanguage)
        }
        fields.indexOfFirst { it.key == "translation" }.takeIf { it >= 0 }?.let { index ->
            fields[index] = fields[index].copy(label = teachingLanguage)
        }
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
                    Text("Format Belirleme", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = { copyGeneration += 1; if (!title.endsWith("— Kopya")) title = "$title — Kopya" }) { Text("Kopyala") }
                    Text("Sürüm 1", color = DilTeal, style = MaterialTheme.typography.labelLarge)
                }
            }
            item { ModeSelector(mode) { modeName = it.name; applyModePreset(it, fields, onTitle = { title = it }, onInstruction = { instruction = it }) } }
            item {
                DesignerCard("Kart Başlığı") {
                    OutlinedTextField(value = title, onValueChange = { title = it; error = null }, modifier = Modifier.fillMaxWidth(), supportingText = { Text("${title.length}/60") }, singleLine = true, shape = RoundedCornerShape(14.dp))
                }
            }
            item {
                DesignerCard("Kart Alanları") {
                    fields.forEachIndexed { index, field ->
                        FieldEditor(
                            index = index,
                            total = fields.size,
                            field = field,
                            onLabelChange = { fields[index] = field.copy(label = it); error = null },
                            onUp = { move(index, index - 1) },
                            onDown = { move(index, index + 1) },
                            onDelete = { if (fields.size > 1) fields.removeAt(index) },
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            if (fields.size < 8) {
                                val id = nextFieldId++
                                fields += EditableField(id, "field_$id", "Yeni Alan", FormatFieldType.LONG_TEXT, false)
                            }
                        },
                        enabled = fields.size < 8,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) { Icon(Icons.Filled.Add, null); Text("Alan Ekle", modifier = Modifier.padding(start = 6.dp)) }
                }
            }
            item {
                DesignerCard("Öğretim Ayarları") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SmallSelector("Seviye", level, { level = when (level) { "A1" -> "A2"; "A2" -> "B1"; "B1" -> "B2"; else -> "A1" } }, Modifier.weight(1f))
                        SmallSelector("Ders dili", teachingLanguage, {
                            teachingLanguage = teachingLanguage.nextLanguage()
                            updateLanguageLabels()
                        }, Modifier.weight(1f))
                    }
                    SmallSelector("Çıktı dili", targetLanguage, {
                        targetLanguage = targetLanguage.nextLanguage()
                        updateLanguageLabels()
                    }, Modifier.fillMaxWidth())
                    OutlinedTextField(value = instruction, onValueChange = { instruction = it; error = null }, modifier = Modifier.fillMaxWidth(), label = { Text("Özel talimat") }, minLines = 3, maxLines = 6, shape = RoundedCornerShape(14.dp))
                }
            }
            item { StepperCard("Kart Sayısı", totalCards, onMinus = { totalCards = (totalCards - 1).coerceAtLeast(1); cardsPerRequest = cardsPerRequest.coerceAtMost(totalCards) }, onPlus = { totalCards = (totalCards + 1).coerceAtMost(100) }) }
            item { StepperCard("İstek Başına Kart", cardsPerRequest, onMinus = { cardsPerRequest = (cardsPerRequest - 1).coerceAtLeast(1) }, onPlus = { cardsPerRequest = (cardsPerRequest + 1).coerceAtMost(minOf(10, totalCards)) }) }
            item {
                DesignerCard("Kart Genişliği") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { widthPercent = (widthPercent - 4).coerceAtLeast(72) }) { Text("−") }
                        Text(if (widthPercent == 100) "Ekrana Sığdır" else "%$widthPercent", modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        TextButton(onClick = { widthPercent = (widthPercent + 4).coerceAtMost(100) }) { Text("+") }
                    }
                }
            }
            item { StepperCard("İstek Aralığı", requestInterval, suffix = "saniye", onMinus = { requestInterval = (requestInterval - 1).coerceAtLeast(2) }, onPlus = { requestInterval = (requestInterval + 1).coerceAtMost(120) }) }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, DilBorder), shape = RoundedCornerShape(16.dp)) {
                    Column {
                        FormatToggle("Sürekli API İsteği", continuous) { continuous = it }
                        FormatToggle("Telefon kapalıyken çalışsın", screenOff) { screenOff = it }
                        FormatToggle("Mikrofon komutu dinle", microphone) { microphone = it }
                    }
                }
            }
            item { LivePreview(fields) }
            if (error != null) item { Text(error.orEmpty(), color = MaterialTheme.colorScheme.error) }
            item {
                Button(
                    onClick = {
                        runCatching {
                            require(title.trim().length in 1..60) { "Kart başlığı 1 ile 60 karakter arasında olmalıdır." }
                            require(instruction.trim().length >= 10) { "Özel talimat en az 10 karakter olmalıdır." }
                            require(fields.all { it.label.trim().isNotEmpty() }) { "Kart alan adları boş bırakılamaz." }
                            val format = LessonFormat(
                                formatId = "local-${source.locator.hashCode().toUInt()}-$copyGeneration",
                                revision = 1,
                                title = title.trim(),
                                instruction = instruction.trim(),
                                teachingMode = mode,
                                teachingLanguage = teachingLanguage,
                                targetLanguage = targetLanguage,
                                learnerLevel = level,
                                fields = fields.mapIndexed { index, field ->
                                    LessonFormatField(field.id.toString(), field.key, field.label.trim(), field.type, required = true, visible = true, speakable = field.speakable, position = index)
                                },
                                totalBlockCount = totalCards,
                                blocksPerRequest = cardsPerRequest,
                                requestIntervalSeconds = requestInterval,
                                continuousRequests = continuous,
                                cardWidthFraction = widthPercent / 100f,
                                playback = PlaybackPreferences(ttsEnabled = true, voiceCommandsEnabled = microphone, continueWhenScreenOff = screenOff, autoAdvance = true),
                            )
                            LessonSessionConfig(source, format).also { config ->
                                val issues = LessonConfigValidator.validate(config)
                                require(issues.isEmpty()) { issues.first().message }
                            }
                        }.fold(onSuccess = onCreate, onFailure = { error = it.message ?: "Format ayarları geçersiz." })
                    },
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DilTeal),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Dersi Kaydet", style = MaterialTheme.typography.titleMedium)
                    Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.padding(start = 8.dp))
                }
            }
            item { Spacer(Modifier.height(6.dp)) }
        }
    }
}

@Composable
private fun ModeSelector(selected: TeachingMode, onSelect: (TeachingMode) -> Unit) {
    val modes = listOf(
        Triple(TeachingMode.LANGUAGE, "Dil Öğret", Icons.Filled.School),
        Triple(TeachingMode.EXPLAIN, "Ders Anlat", Icons.Outlined.Article),
        Triple(TeachingMode.SUMMARY, "Özet", Icons.Outlined.Summarize),
        Triple(TeachingMode.TRANSLATE, "Çeviri", Icons.Filled.Translate),
    )
    Row(Modifier.fillMaxWidth().background(Color(0xFFF6F7F9), RoundedCornerShape(14.dp)), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        modes.forEach { (mode, label, icon) ->
            Column(
                Modifier.weight(1f).clickable { onSelect(mode) }.background(if (selected == mode) DilBlueSoft else Color.Transparent, RoundedCornerShape(13.dp)).padding(vertical = 11.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(icon, null, tint = if (selected == mode) DilTeal else DilMuted, modifier = Modifier.size(20.dp))
                Text(label, color = if (selected == mode) DilTeal else DilMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun applyModePreset(mode: TeachingMode, fields: MutableList<EditableField>, onTitle: (String) -> Unit, onInstruction: (String) -> Unit) {
    fields.clear()
    when (mode) {
        TeachingMode.LANGUAGE -> {
            onTitle("İngilizce — Türkçe Örnek Cümleler")
            onInstruction("Kaynak cümleyi göster, Türkçe karşılığını ver ve kısa bir açıklama ekle.")
            fields += EditableField(0, "source_text", "İngilizce", FormatFieldType.SHORT_TEXT, true)
            fields += EditableField(1, "translation", "Türkçe", FormatFieldType.SHORT_TEXT, true)
        }
        TeachingMode.EXPLAIN -> {
            onTitle("Konu Anlatımı")
            onInstruction("Konuyu sade adımlarla anlat, önemli noktaları ve anlaşılır bir örnek ekle.")
            fields += EditableField(0, "topic", "Konu", FormatFieldType.SHORT_TEXT, false)
            fields += EditableField(1, "explanation", "Açıklama", FormatFieldType.LONG_TEXT, true)
        }
        TeachingMode.SUMMARY -> {
            onTitle("Akıllı Özet")
            onInstruction("Kaynak bölümün ana fikrini ve temel maddelerini kısa, doğru biçimde özetle.")
            fields += EditableField(0, "summary", "Özet", FormatFieldType.LONG_TEXT, true)
            fields += EditableField(1, "key_points", "Önemli Noktalar", FormatFieldType.EXAMPLES, false)
        }
        TeachingMode.TRANSLATE -> {
            onTitle("Çeviri Kartları")
            onInstruction("Kaynak metni anlamı koruyarak Türkçeye çevir ve gerekli olduğunda kısa not ekle.")
            fields += EditableField(0, "source_text", "Kaynak", FormatFieldType.SHORT_TEXT, true)
            fields += EditableField(1, "translation", "Çeviri", FormatFieldType.SHORT_TEXT, true)
        }
        TeachingMode.CUSTOM -> Unit
    }
}

@Composable
private fun DesignerCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, DilBorder), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun FieldEditor(index: Int, total: Int, field: EditableField, onLabelChange: (String) -> Unit, onUp: () -> Unit, onDown: () -> Unit, onDelete: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        OutlinedTextField(value = field.label, onValueChange = onLabelChange, modifier = Modifier.weight(1f), label = { Text("${index + 1}. Alan") }, singleLine = true, shape = RoundedCornerShape(12.dp))
        IconButton(onClick = onUp, enabled = index > 0, modifier = Modifier.size(34.dp)) { Icon(Icons.Filled.ArrowUpward, "Yukarı", modifier = Modifier.size(18.dp)) }
        IconButton(onClick = onDown, enabled = index < total - 1, modifier = Modifier.size(34.dp)) { Icon(Icons.Filled.ArrowDownward, "Aşağı", modifier = Modifier.size(18.dp)) }
        IconButton(onClick = onDelete, enabled = total > 1, modifier = Modifier.size(34.dp)) { Icon(Icons.Outlined.Delete, "Sil", tint = Color(0xFFE14A5B), modifier = Modifier.size(18.dp)) }
    }
}

@Composable
private fun SmallSelector(label: String, value: String, onClick: () -> Unit, modifier: Modifier) {
    Card(modifier = modifier.height(54.dp).clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, DilBorder), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text(label, color = DilMuted, style = MaterialTheme.typography.bodyMedium)
            Text("$value  ⌄", fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun String.nextLanguage(): String = if (this == "Türkçe") "İngilizce" else "Türkçe"

@Composable
private fun StepperCard(title: String, value: Int, suffix: String? = null, onMinus: () -> Unit, onPlus: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, DilBorder), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = onMinus) { Icon(Icons.Filled.Remove, "Azalt") }
            Text(value.toString(), modifier = Modifier.padding(horizontal = 22.dp), fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onPlus) { Icon(Icons.Filled.Add, "Artır") }
            if (suffix != null) Text(suffix, color = DilMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun FormatToggle(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun LivePreview(fields: List<EditableField>) {
    DesignerCard("Önizleme") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text("● Canlı Önizleme", color = Color(0xFF159E68), modifier = Modifier.background(DilGreenSoft, RoundedCornerShape(10.dp)).padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.bodyMedium)
        }
        Row(Modifier.fillMaxWidth().background(DilBlueSoft.copy(alpha = .7f), RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))) {
            fields.take(3).forEach { Text(it.label.ifBlank { "Alan" }, modifier = Modifier.weight(1f).padding(10.dp), fontWeight = FontWeight.SemiBold) }
        }
        Row(Modifier.fillMaxWidth()) {
            fields.take(3).forEachIndexed { index, _ -> Text(if (index == 0) "he said that" else if (index == 1) "o dedi ki" else "kısa açıklama", modifier = Modifier.weight(1f).padding(10.dp)) }
        }
    }
}
