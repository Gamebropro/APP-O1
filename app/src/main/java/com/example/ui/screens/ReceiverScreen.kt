package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.CryptoEngine
import com.example.crypto.DecryptedVaultMedia
import com.example.crypto.MediaType
import com.example.crypto.VaultMetadata
import com.example.data.VaultFileInfo
import com.example.data.VaultFileManager
import com.example.security.SecurityManager
import com.example.ui.components.PasswordModal
import com.example.ui.theme.PolishAmber
import com.example.ui.theme.PolishAmberContainer
import com.example.ui.theme.PolishCardBorder
import com.example.ui.theme.PolishCardBorderAccent
import com.example.ui.theme.PolishCrimson
import com.example.ui.theme.PolishCrimsonContainer
import com.example.ui.theme.PolishCrimsonText
import com.example.ui.theme.PolishEmerald
import com.example.ui.theme.PolishEmeraldContainer
import com.example.ui.theme.PolishEmeraldText
import com.example.ui.theme.PolishOnPrimaryContainer
import com.example.ui.theme.PolishPrimary
import com.example.ui.theme.PolishPrimaryContainer
import com.example.ui.theme.PolishSecondary
import com.example.ui.theme.PolishSurface
import com.example.ui.theme.PolishSurfaceVariant
import com.example.ui.theme.PolishSurfaceVariantSubtle
import com.example.ui.theme.PolishTextMuted
import com.example.ui.theme.PolishTextPrimary
import com.example.ui.theme.PolishTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReceiverScreen(
    incomingVaultUri: Uri? = null,
    onOpenViewer: (DecryptedVaultMedia) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var loadedPackageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var loadedFileName by remember { mutableStateOf("") }
    var parsedMetadata by remember { mutableStateOf<VaultMetadata?>(null) }
    var inspectError by remember { mutableStateOf<String?>(null) }

    var showPasswordModal by remember { mutableStateOf(false) }
    var isDecrypting by remember { mutableStateOf(false) }
    var decryptErrorMessage by remember { mutableStateOf<String?>(null) }

    var discoveredVaultFiles by remember { mutableStateOf<List<VaultFileInfo>>(emptyList()) }

    fun refreshDiscoveredFiles() {
        scope.launch(Dispatchers.IO) {
            val list = VaultFileManager.listAllAvailableVaultPackages(context)
            withContext(Dispatchers.Main) {
                discoveredVaultFiles = list
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshDiscoveredFiles()
    }

    // Function to inspect a vault package
    fun processVaultBytes(bytes: ByteArray, fileName: String) {
        try {
            val meta = CryptoEngine.inspectMetadata(bytes)
            loadedPackageBytes = bytes
            loadedFileName = fileName
            parsedMetadata = meta
            inspectError = null
        } catch (e: Exception) {
            loadedPackageBytes = null
            parsedMetadata = null
            inspectError = "ගොනුව කියවීමට නොහැකි විය: ${e.message}"
        }
    }

    // Handle incoming URI from outside
    LaunchedEffect(incomingVaultUri) {
        if (incomingVaultUri != null) {
            val bytes = withContext(Dispatchers.IO) {
                VaultFileManager.readBytesFromUri(context, incomingVaultUri)
            }
            if (bytes != null) {
                val name = VaultFileManager.getFileNameFromUri(context, incomingVaultUri)
                processVaultBytes(bytes, name)
            }
        }
    }

    // Vault file picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    VaultFileManager.readBytesFromUri(context, uri)
                }
                if (bytes != null && bytes.isNotEmpty()) {
                    val name = VaultFileManager.getFileNameFromUri(context, uri)
                    processVaultBytes(bytes, name)
                } else {
                    Toast.makeText(context, "ගොනුව විවෘත කළ නොහැක", Toast.LENGTH_SHORT).show()
                }
            }
        }
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
                text = "Receiver Vault",
                color = PolishTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )
            Text(
                text = "ලැබුණු .svault ගොනුව තෝරා මුරපදය මගින් විවෘත කරන්න. Screen capture සහ Screen recording සම්පූර්ණයෙන්ම වළක්වා ඇත.",
                color = PolishTextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }

        // 1. Instructions for Receiver Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(PolishSurfaceVariantSubtle)
                    .border(1.dp, PolishCardBorderAccent, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = PolishPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ලබන්නා සඳහා උපදෙස් (Receiver Guide)",
                            color = PolishOnPrimaryContainer,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "1. WhatsApp / Telegram මගින් ලැබුණු .svault ගොනුව ඔබගේ Download Folder එකේ Save කරගන්න.\n2. 'ගොනු අතරින් තෝරන්න' ක්ලික් කර එම ගොනුව තෝරන්න (හෝ පහත Discovered ලැයිස්තුවෙන් තෝරන්න).\n3. යවන්නා ලබාදුන් Master Password එක ඇතුළත් කර ආරක්ෂිතව බලන්න.",
                        color = PolishTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // 2. Open Vault File Action Card
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. ගොනුව විවෘත කරන්න (Import .svault)",
                            color = PolishPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = { refreshDiscoveredFiles() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = PolishTextSecondary)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { filePickerLauncher.launch("*/*") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PolishPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("open_file_picker_button")
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ගොනු තෝරන්න", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { refreshDiscoveredFiles() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PolishPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PolishCardBorder),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan Downloads", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Quick Demo Creator for instant testing
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                try {
                                    val (demoBytes, _) = withContext(Dispatchers.Default) {
                                        val sampleImage = VaultFileManager.generateSampleSecureImage(
                                            "Secure Document #1049",
                                            "Top Secret Payload - Confidential View-Once Document"
                                        )
                                        val meta = VaultMetadata(
                                            id = java.util.UUID.randomUUID().toString(),
                                            title = "Demo Confidential Note",
                                            mediaType = MediaType.IMAGE,
                                            mimeType = "image/png",
                                            createdAt = System.currentTimeMillis(),
                                            expiresAt = System.currentTimeMillis() + (24 * 3600 * 1000L),
                                            viewPolicy = com.example.crypto.ViewPolicy.VIEW_ONCE,
                                            maxViewSeconds = 30,
                                            watermarkText = "DEMO-USER-PROTECTED",
                                            originalFileName = "demo_document.png"
                                        )
                                        val packaged = CryptoEngine.packageVault(
                                            rawMediaBytes = sampleImage,
                                            metadata = meta,
                                            passcode = "1234"
                                        )
                                        Pair(packaged, meta)
                                    }
                                    withContext(Dispatchers.IO) {
                                        VaultFileManager.saveVaultPackageToDownloads(context, demoBytes, "Demo_Confidential_Note")
                                    }
                                    refreshDiscoveredFiles()
                                    processVaultBytes(demoBytes, "Demo_Confidential_Note.svault")
                                    Toast.makeText(context, "සාම්පල .svault එකක් සෑදිණි! Password: 1234", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = com.example.ui.theme.PolishEmeraldText),
                        border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.PolishEmerald.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = com.example.ui.theme.PolishEmeraldText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "⚡ සාම්පල Encrypted Vault එකක් පරීක්ෂා කරන්න (Password: 1234)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.PolishEmeraldText
                        )
                    }

                    if (inspectError != null) {
                        Text(
                            text = inspectError!!,
                            color = PolishCrimson,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 2. Active Package Inspection & Verification Card
        if (parsedMetadata != null && loadedPackageBytes != null) {
            val meta = parsedMetadata!!
            val (canView, statusReason) = SecurityManager.canViewVault(context, meta)
            val isConsumed = SecurityManager.isVaultConsumed(context, meta.id)

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PolishSurface),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (!canView) PolishCrimson else PolishEmerald
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (canView) PolishEmeraldContainer else PolishCrimsonContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (meta.mediaType == MediaType.IMAGE) Icons.Default.LockOpen else Icons.Default.PlayCircle,
                                        contentDescription = null,
                                        tint = if (canView) PolishEmeraldText else PolishCrimsonText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = meta.title,
                                        color = PolishTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${meta.mediaType.name} • ${(loadedPackageBytes!!.size / 1024)} KB",
                                        color = PolishTextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Policy Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (canView) PolishEmeraldContainer else PolishCrimsonContainer)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isConsumed) "BURNED" else meta.viewPolicy.name,
                                    color = if (canView) PolishEmeraldText else PolishCrimsonText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Detailed Security Constraints Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(PolishSurfaceVariant)
                                .border(1.dp, PolishCardBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "🔒 නීති: ${meta.viewPolicy.label}",
                                    color = PolishPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (meta.maxViewSeconds > 0) {
                                    Text(
                                        text = "⏱️ ස්වයංක්‍රීයව විනාශ වන කාලය: තත්පර ${meta.maxViewSeconds}යි",
                                        color = PolishAmber,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (meta.expiresAt > 0L) {
                                    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(meta.expiresAt))
                                    Text(
                                        text = "📅 කල් ඉකුත්වන දිනය: $dateStr",
                                        color = PolishTextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = "🚫 තිර ඡායාරූප (Screenshots) සහ Screen Recordings සම්පූර්ණයෙන්ම අවහිර කර ඇත.",
                                    color = PolishTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Security Status Notice
                        if (!canView) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PolishCrimsonContainer)
                                    .border(1.dp, PolishCrimson, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = PolishCrimsonText,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "ප්‍රවේශය අවහිර කර ඇත (Access Blocked)",
                                            color = PolishCrimsonText,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = statusReason,
                                            color = PolishCrimsonText.copy(alpha = 0.85f),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    decryptErrorMessage = null
                                    showPasswordModal = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PolishPrimary,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("unlock_vault_button")
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("විවෘත කර බලන්න (Enter Password & View)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 3. Discovered Vault Packages in Downloads / Storage Section
        if (discoveredVaultFiles.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "සොයාගත් .svault ගොනු (Discovered Packages)",
                        color = PolishTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${discoveredVaultFiles.size} Found",
                        color = PolishPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            items(discoveredVaultFiles) { vaultInfo ->
                val isSelected = loadedFileName == vaultInfo.name
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) PolishPrimaryContainer else PolishSurface)
                        .border(
                            1.dp,
                            if (isSelected) PolishPrimary else PolishCardBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            scope.launch {
                                val bytes = withContext(Dispatchers.IO) {
                                    if (vaultInfo.file != null) {
                                        vaultInfo.file.readBytes()
                                    } else if (vaultInfo.uri != null) {
                                        VaultFileManager.readBytesFromUri(context, vaultInfo.uri)
                                    } else null
                                }
                                if (bytes != null) {
                                    processVaultBytes(bytes, vaultInfo.name)
                                }
                            }
                        }
                        .padding(12.dp)
                ) {
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
                                    .background(if (isSelected) PolishSurface else PolishSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isSelected) PolishPrimary else PolishTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = vaultInfo.name,
                                    color = PolishTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${vaultInfo.sizeBytes / 1024} KB • ${vaultInfo.locationLabel}",
                                    color = PolishTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Text(
                            text = if (isSelected) "Selected" else "Open",
                            color = if (isSelected) PolishOnPrimaryContainer else PolishPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Password Prompt Modal
    if (showPasswordModal && parsedMetadata != null && loadedPackageBytes != null) {
        PasswordModal(
            metadata = parsedMetadata!!,
            isLoading = isDecrypting,
            errorMessage = decryptErrorMessage,
            onDismiss = {
                if (!isDecrypting) {
                    showPasswordModal = false
                    decryptErrorMessage = null
                }
            },
            onConfirm = { password ->
                isDecrypting = true
                decryptErrorMessage = null
                scope.launch {
                    try {
                        val decrypted = withContext(Dispatchers.Default) {
                            CryptoEngine.decryptVault(
                                context = context,
                                vaultPackageBytes = loadedPackageBytes!!,
                                passcode = password
                            )
                        }

                        // Register view count and burn rule
                        SecurityManager.registerVaultView(context, parsedMetadata!!)

                        showPasswordModal = false
                        isDecrypting = false
                        onOpenViewer(decrypted)
                    } catch (e: Exception) {
                        isDecrypting = false
                        decryptErrorMessage = "මුරපදය වැරදියි හෝ ගොනුව දූෂිත වී ඇත! (Incorrect password or corrupted vault)."
                    }
                }
            }
        )
    }
}

