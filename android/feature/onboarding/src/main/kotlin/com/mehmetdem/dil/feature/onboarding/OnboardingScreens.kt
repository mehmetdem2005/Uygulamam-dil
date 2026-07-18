package com.mehmetdem.dil.feature.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mehmetdem.dil.core.designsystem.DilBlueSoft
import com.mehmetdem.dil.core.designsystem.DilBorder
import com.mehmetdem.dil.core.designsystem.DilGreenSoft
import com.mehmetdem.dil.core.designsystem.DilMaxContentWidth
import com.mehmetdem.dil.core.designsystem.DilMuted
import com.mehmetdem.dil.core.designsystem.DilPurpleSoft
import com.mehmetdem.dil.core.designsystem.DilProgressDots
import com.mehmetdem.dil.core.designsystem.DilTeal
import com.mehmetdem.dil.core.designsystem.DilVoiceBars

@Composable
fun WelcomeOnboardingScreen(onContinue: () -> Unit, onSkip: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.fillMaxWidth().widthIn(max = DilMaxContentWidth).verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("▣  Uygulamam Dil", color = DilTeal, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Videoları ve PDF'leri\nDerse Dönüştür", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("YouTube videolarından ve yüklediğin PDF'lerden, seçtiğin formatta kişiselleştirilmiş dersler oluştur.", color = DilMuted, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
            HeroIllustration()
            FeatureRow(Icons.Filled.School, "Kişiselleştirilmiş Dersler", "İhtiyacına göre seviyeye ve konuya özel içerikler.", DilBlueSoft)
            FeatureRow(Icons.Filled.AutoAwesome, "Yapay Zekâ Destekli", "Akıllı özetler, çeviri ve pratiklerle daha hızlı öğren.", DilGreenSoft)
            FeatureRow(Icons.Filled.TrendingUp, "İlerlemeni Takip Et", "Derslerini kaydet, ilerlemeni gör ve motive kal.", DilPurpleSoft)
            Spacer(Modifier.height(8.dp))
            DilProgressDots(currentStep = 1, totalSteps = 9)
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DilTeal),
                shape = RoundedCornerShape(14.dp),
            ) { Text("Devam Et  ›", style = MaterialTheme.typography.titleMedium) }
            OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth(), border = null) { Text("Geç", color = DilTeal) }
        }
    }
}

@Composable
private fun HeroIllustration() {
    Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(width = 210.dp, height = 190.dp).background(DilBlueSoft, RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Kaynağından kişisel ders", fontWeight = FontWeight.Bold)
                Box(Modifier.size(width = 170.dp, height = 68.dp).background(DilPurpleSoft, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Filled.PlayCircle, null, tint = DilTeal, modifier = Modifier.size(42.dp)) }
                Text("Seçtiğin kart biçimi", fontWeight = FontWeight.SemiBold)
                Text("Metin  ·  Açıklama  ·  Çeviri", color = DilMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, subtitle: String, background: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(48.dp).background(background, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = DilTeal) }
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, color = DilMuted)
        }
    }
}

@Composable
fun PermissionOnboardingScreen(onRequestPermissions: () -> Unit, onLater: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.fillMaxWidth().widthIn(max = DilMaxContentWidth).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("2 / 9", color = DilTeal, fontWeight = FontWeight.Bold)
            Box(Modifier.size(160.dp).background(DilBlueSoft, CircleShape), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.AutoStories, null, tint = DilTeal, modifier = Modifier.size(44.dp))
                    DilVoiceBars()
                }
            }
            Text("Mikrofonla Komut Ver", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("Sesli komut testini kullanmak istersen mikrofon izni verebilirsin. İzni daha sonra da açabilirsin.", color = DilMuted, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
            PermissionRow(Icons.Filled.Mic, "Mikrofon", "Konuşmanı Android konuşma tanıma hizmetine iletmek için gerekir.", "İsteğe bağlı", DilGreenSoft)
            PermissionRow(Icons.Filled.Lock, "Sen başlatınca dinler", "Mikrofon yalnız dinleme düğmesine bastığında açılır.", "Ekran açıkken", DilBlueSoft)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Lock, null, tint = DilMuted, modifier = Modifier.size(16.dp))
                Text("İzinler istediğiniz zaman Ayarlar ekranından değiştirilebilir.", color = DilMuted, style = MaterialTheme.typography.bodyMedium)
            }
            Card(colors = CardDefaults.cardColors(containerColor = DilBlueSoft.copy(alpha = .36f)), border = BorderStroke(1.dp, Color(0xFFCBE3F0)), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Sesli Komut Örnekleri", fontWeight = FontWeight.Bold)
                    Text("“Sonraki karta geç”     🔊")
                    Text("“Tekrar et”                  🔊")
                }
            }
            Spacer(Modifier.height(8.dp))
            DilProgressDots(currentStep = 2, totalSteps = 9)
            Button(onClick = onRequestPermissions, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), colors = ButtonDefaults.buttonColors(containerColor = DilTeal), shape = RoundedCornerShape(14.dp)) { Text("İzinleri Aç") }
            OutlinedButton(onClick = onLater, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, DilBorder)) { Text("Daha Sonra", color = DilTeal) }
        }
    }
}

@Composable
private fun PermissionRow(icon: ImageVector, title: String, subtitle: String, status: String, background: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, DilBorder), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(48.dp).background(background, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = DilTeal) }
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = DilMuted)
            }
            Text(
                status,
                modifier = Modifier.widthIn(max = 82.dp),
                color = DilTeal,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.End,
                maxLines = 2,
            )
        }
    }
}
