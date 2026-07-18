package com.mehmetdem.dil.feature.profile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mehmetdem.dil.core.designsystem.DilBlueSoft
import com.mehmetdem.dil.core.designsystem.DilBorder
import com.mehmetdem.dil.core.designsystem.DilMaxContentWidth
import com.mehmetdem.dil.core.designsystem.DilMuted
import com.mehmetdem.dil.core.designsystem.DilOuterPadding
import com.mehmetdem.dil.core.designsystem.DilTeal
import com.mehmetdem.dil.core.designsystem.DilVoiceBars
import java.util.Locale

@Composable
fun VoiceControlScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    var isListening by remember { mutableStateOf(false) }
    var transcript by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Mikrofon testi hazır") }
    var detectedCommand by remember { mutableStateOf<String?>(null) }
    val recognizerAvailable = remember { SpeechRecognizer.isRecognitionAvailable(context) }
    val recognizer = remember(recognizerAvailable) {
        if (recognizerAvailable) SpeechRecognizer.createSpeechRecognizer(context) else null
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        status = if (granted) "Mikrofon izni verildi" else "Mikrofon izni verilmedi"
    }

    DisposableEffect(recognizer) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                status = "Dinleniyor"
            }

            override fun onBeginningOfSpeech() {
                status = "Konuşma algılandı"
            }

            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() {
                isListening = false
                status = "Sonuç hazırlanıyor"
            }

            override fun onError(error: Int) {
                isListening = false
                status = speechErrorText(error)
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val text = results.bestRecognition().orEmpty()
                transcript = text
                detectedCommand = detectCommand(text)
                status = if (text.isBlank()) "Konuşma anlaşılamadı" else "Konuşma algılandı"
            }

            override fun onPartialResults(partialResults: Bundle?) {
                partialResults.bestRecognition()?.takeIf { it.isNotBlank() }?.let { transcript = it }
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        }
        recognizer?.setRecognitionListener(listener)
        onDispose { recognizer?.destroy() }
    }

    fun startListening() {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        if (recognizer == null) {
            status = "Bu cihazda konuşma tanıma hizmeti bulunamadı"
            return
        }
        transcript = ""
        detectedCommand = null
        status = "Mikrofon açılıyor"
        recognizer.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("tr", "TR").toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            },
        )
    }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().widthIn(max = DilMaxContentWidth),
            contentPadding = PaddingValues(horizontal = DilOuterPadding, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") }
                    Column(Modifier.weight(1f)) {
                        Text("Sesli Kullanım", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Mikrofonu ve Türkçe komut algılamayı test et.", color = DilMuted, maxLines = 2)
                    }
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DilBlueSoft.copy(alpha = .42f)),
                    border = BorderStroke(1.dp, Color(0xFFD5EAF4)),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(Modifier.size(96.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(
                                if (hasPermission) Icons.Filled.Mic else Icons.Filled.MicOff,
                                null,
                                tint = DilTeal,
                                modifier = Modifier.size(46.dp),
                            )
                        }
                        Text(status, color = DilTeal, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (isListening) DilVoiceBars()
                        Text(
                            transcript.ifBlank { "Bir komut söyle: “Sonraki karta geç”" },
                            color = if (transcript.isBlank()) DilMuted else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        detectedCommand?.let {
                            Text("Algılanan komut: $it", color = Color(0xFF159E68), fontWeight = FontWeight.SemiBold)
                        }
                        if (!hasPermission) {
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                colors = ButtonDefaults.buttonColors(containerColor = DilTeal),
                            ) { Icon(Icons.Filled.Mic, null); Text("Mikrofon İzni Ver", modifier = Modifier.padding(start = 7.dp)) }
                        } else if (isListening) {
                            OutlinedButton(onClick = { recognizer?.stopListening(); isListening = false; status = "Dinleme durduruldu" }) {
                                Icon(Icons.Filled.Stop, null)
                                Text("Durdur", modifier = Modifier.padding(start = 7.dp))
                            }
                        } else {
                            Button(onClick = { startListening() }, colors = ButtonDefaults.buttonColors(containerColor = DilTeal)) {
                                Icon(Icons.Filled.Mic, null)
                                Text("Dinlemeyi Başlat", modifier = Modifier.padding(start = 7.dp))
                            }
                        }
                    }
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, DilBorder),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.HeadsetMic, null, tint = DilTeal)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Desteklenen komut testi", fontWeight = FontWeight.SemiBold)
                            Text("Sonraki karta geç · Tekrar et · Durdur · Devam et", color = DilMuted)
                        }
                    }
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, DilBorder),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.Info, null, tint = DilTeal)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Çalışma sınırı", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Bu ekrandaki konuşma tanıma gerçektir ve yalnız ekran açıkken çalışır. Arka plan ve ekran kapalı komut servisi henüz etkin değildir.",
                                color = DilMuted,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

private fun Bundle?.bestRecognition(): String? = this
    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
    ?.firstOrNull()

private fun detectCommand(text: String): String? {
    val normalized = text.lowercase(Locale("tr", "TR"))
    return when {
        "sonraki" in normalized || "ileri" in normalized -> "Sonraki karta geç"
        "tekrar" in normalized -> "Tekrar et"
        "durdur" in normalized || "duraklat" in normalized -> "Durdur"
        "devam" in normalized -> "Devam et"
        else -> null
    }
}

private fun speechErrorText(error: Int): String = when (error) {
    SpeechRecognizer.ERROR_AUDIO -> "Mikrofon sesi alınamadı"
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mikrofon izni gerekli"
    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Konuşma hizmetine ulaşılamadı"
    SpeechRecognizer.ERROR_NO_MATCH -> "Konuşma anlaşılamadı"
    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Konuşma hizmeti meşgul"
    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Konuşma duyulmadı"
    else -> "Konuşma tanıma durdu (kod $error)"
}
