package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.LocalSecurityFlagUpdater
import com.example.security.SecurityManager
import com.example.ui.theme.PolishAmber
import com.example.ui.theme.PolishAmberContainer
import com.example.ui.theme.PolishCardBorder
import com.example.ui.theme.PolishCrimson
import com.example.ui.theme.PolishCrimsonContainer
import com.example.ui.theme.PolishCrimsonText
import com.example.ui.theme.PolishEmerald
import com.example.ui.theme.PolishEmeraldContainer
import com.example.ui.theme.PolishEmeraldText
import com.example.ui.theme.PolishPrimary
import com.example.ui.theme.PolishPrimaryContainer
import com.example.ui.theme.PolishSurface
import com.example.ui.theme.PolishSurfaceVariant
import com.example.ui.theme.PolishSurfaceVariantSubtle
import com.example.ui.theme.PolishTextMuted
import com.example.ui.theme.PolishTextPrimary
import com.example.ui.theme.PolishTextSecondary

@Composable
fun SecurityLogsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val flagUpdater = LocalSecurityFlagUpdater.current
    var auditLogs by remember { mutableStateOf<List<String>>(emptyList()) }
    var isFlagSecureOn by remember { mutableStateOf(SecurityManager.isFlagSecureEnabled(context)) }

    fun reloadLogs() {
        auditLogs = SecurityManager.getAuditLogs(context)
    }

    LaunchedEffect(Unit) {
        reloadLogs()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Security & Diagnostics",
                color = PolishTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )
            Text(
                text = "හාර්ඩ්වෙයාර් මට්ටමින් Screen Capture අවහිර කිරීම් සහ ගුප්තකේතන ලොග් පරීක්ෂා කරන්න.",
                color = PolishTextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }

        // Hardware Diagnostics Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PolishSurface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PolishCardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Hardware Guard Indicators",
                        color = PolishPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // FLAG_SECURE Switch Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isFlagSecureOn) PolishEmeraldContainer else PolishAmberContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = if (isFlagSecureOn) PolishEmeraldText else PolishAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Window FLAG_SECURE",
                                    color = PolishTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isFlagSecureOn) "Anti-Screenshot Active" else "Preview Mode (Emulator)",
                                    color = PolishTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Switch(
                            checked = isFlagSecureOn,
                            onCheckedChange = { isChecked ->
                                isFlagSecureOn = isChecked
                                SecurityManager.setFlagSecureEnabled(context, isChecked)
                                flagUpdater(isChecked)
                                reloadLogs()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PolishEmerald,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = PolishCardBorder
                            ),
                            modifier = Modifier.testTag("flag_secure_switch")
                        )
                    }

                    if (isFlagSecureOn) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(PolishAmberContainer.copy(alpha = 0.5f))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "⚠️ අවවාදයයි: Browser Web Emulator වලදී FLAG_SECURE ක්‍රියාත්මක වූ විට තිරය සම්පූර්ණයෙන්ම කලු පැහැයෙන් දිස්විය හැක (Screen recording block වන බැවින්). Preview බැලීම සඳහා මෙය OFF කරන්න.",
                                color = PolishAmber,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PolishPrimaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = PolishPrimary, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("AES-256-GCM Cipher", color = PolishTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("PBKDF2 SHA-256 (10,000 rounds)", color = PolishTextMuted, fontSize = 11.sp)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(PolishPrimaryContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("ENFORCED", color = PolishPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PolishAmberContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = PolishAmber, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("In-Memory Ephemeral Buffer", color = PolishTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Zero storage leak / In-memory zeroing", color = PolishTextMuted, fontSize = 11.sp)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(PolishAmberContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("READY", color = PolishAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Audit Trail Header & Logs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ක්‍රිප්ටෝග්‍රැෆික් ලොග් සටහන් (Audit Trail)",
                    color = PolishTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedButton(
                    onClick = {
                        SecurityManager.clearRegistry(context)
                        reloadLogs()
                        Toast.makeText(context, "Registry cleared", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PolishCrimson),
                    modifier = Modifier.testTag("clear_logs_button")
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        items(auditLogs) { logLine ->
            val isDanger = logLine.contains("BURNED") || logLine.contains("DESTROYED")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDanger) PolishCrimsonContainer else PolishSurfaceVariant)
                    .border(
                        1.dp,
                        if (isDanger) PolishCrimson.copy(alpha = 0.3f) else PolishCardBorder,
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = logLine,
                    color = if (isDanger) PolishCrimsonText else PolishTextPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

