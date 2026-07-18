package com.mehmetdem.dil.feature.profile

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mehmetdem.dil.core.designsystem.DilBlueSoft
import com.mehmetdem.dil.core.designsystem.DilBorder
import com.mehmetdem.dil.core.designsystem.DilGreenSoft
import com.mehmetdem.dil.core.designsystem.DilMaxContentWidth
import com.mehmetdem.dil.core.designsystem.DilMuted
import com.mehmetdem.dil.core.designsystem.DilOuterPadding
import com.mehmetdem.dil.core.designsystem.DilPurpleSoft
import com.mehmetdem.dil.core.designsystem.DilTeal

@Composable
fun ProfileScreen(
    lessonCount: Int,
    serverVerified: Boolean,
    providerRequestCount: Int,
    onVoiceSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().widthIn(max = DilMaxContentWidth),
            contentPadding = PaddingValues(horizontal = DilOuterPadding, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    "▣  Uygulamam Dil",
                    color = DilTeal,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, DilBorder),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(Modifier.size(64.dp).background(DilBlueSoft, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.AccountCircle, null, tint = DilTeal, modifier = Modifier.size(46.dp))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Yerel kullanım", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Supabase hesabı henüz bağlanmadı", color = DilMuted)
                        }
                    }
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DilGreenSoft.copy(alpha = .52f)),
                    border = BorderStroke(1.dp, Color(0xFFCDEEDC)),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.size(48.dp).background(Color.White, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.FolderOpen, null, tint = DilTeal)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Cihazdaki dersler", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("$lessonCount ders kalıcı olarak kaydedildi", color = DilMuted)
                        }
                    }
                }
            }
            item {
                Text(
                    "Ayarlar ve durum",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            item {
                SettingsCard {
                    SettingRow(
                        icon = Icons.Filled.VolumeUp,
                        title = "Sesli kullanım",
                        detail = "Mikrofonu test et",
                        background = DilBlueSoft,
                        onClick = onVoiceSettings,
                    )
                    SettingRow(Icons.Filled.Language, "Uygulama dili", "Türkçe", DilBlueSoft)
                    SettingRow(Icons.Filled.FolderOpen, "Yerel kayıt", "$lessonCount ders", DilGreenSoft)
                    SettingRow(Icons.Filled.CloudOff, "Bulut senkronizasyonu", "Bağlı değil", Color(0xFFFFF1E5))
                    SettingRow(
                        Icons.Filled.AutoAwesome,
                        "Yapay zekâ ders üretimi",
                        if (serverVerified) "Sunucu doğrulandı · $providerRequestCount istek kaydedildi" else "İlk ders oluşturulurken doğrulanacak",
                        DilPurpleSoft,
                    )
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DilBlueSoft.copy(alpha = .32f)),
                    border = BorderStroke(1.dp, Color(0xFFCBE3F0)),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Gerçek bağlantı durumu", fontWeight = FontWeight.Bold)
                        Text(
                            "Hesap, abonelik ve bulut eşitleme bağlanmadan kullanıcı adı, kota veya premium bilgisi gösterilmez.",
                            color = DilMuted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Uygulamam Dil v0.4.0", color = DilMuted, style = MaterialTheme.typography.bodyMedium)
                    Text("Geliştirme sürümü", color = DilMuted, style = MaterialTheme.typography.bodyMedium)
                }
            }
            item { Spacer(Modifier.height(6.dp)) }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, DilBorder),
        shape = RoundedCornerShape(16.dp),
    ) { Column { content() } }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    detail: String,
    background: Color,
    onClick: (() -> Unit)? = null,
) {
    val rowModifier = if (onClick == null) {
        Modifier.fillMaxWidth().heightIn(min = 68.dp)
    } else {
        Modifier.fillMaxWidth().heightIn(min = 68.dp).clickable(onClick = onClick)
    }
    Row(
        rowModifier.padding(horizontal = 11.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(36.dp).background(background, RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = DilTeal)
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(detail, color = DilMuted, style = MaterialTheme.typography.bodySmall, maxLines = 2)
        }
        if (onClick != null) Icon(Icons.Filled.ChevronRight, "Aç", tint = DilMuted)
    }
}
