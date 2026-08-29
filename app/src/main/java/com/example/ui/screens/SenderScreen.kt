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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.CryptoEngine
import com.example.crypto.MediaType
import com.example.crypto.VaultMetadata
import com.example.crypto.ViewPolicy
import com.example.data.VaultFileManager
import com.example.ui.theme.PolishAmber
import com.example.ui.theme.PolishAmberContainer
import com.example.ui.theme.PolishCardBorder
import com.example.ui.theme.PolishCardBorderAccent
import com.example.ui.theme.PolishCardBorderDashed
import com.example.ui.theme.PolishCrimson
import com.example.ui.theme.PolishEmerald
import com.example.ui.theme.PolishEmeraldContainer
import com.example.ui.theme.PolishEmeraldText
import com.example.ui.theme.PolishOnPrimaryContainer
import com.example.ui.theme.PolishPrimary
import com.example.ui.theme.PolishPrimaryContainer
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
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SenderScreen(
    onPackageCreated: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedMediaBytes by remember { mutableStateOf<ByteArray?>(null) }
    var selectedMediaType by remember { mutableStateOf(MediaType.IMAGE) }
    var selectedFileName by remember { mutableStateOf("") }
    var mimeType by remember { mutableStateOf("image/jpeg") }

    var packageTitle by remember { mutableStateOf("Top Secret Media") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var selectedPolicy by remember { mutableStateOf(ViewPolicy.VIEW_ONCE) }
    var selfDestructTimerSeconds by remember { mutableIntStateOf(30) }
    var expiryHours by remember { mutableIntStateOf(24) }
    var watermarkText by remember { mutableStateOf("CONFIDENTIAL") }

    var isPackaging by remember { mutableStateOf(false) }
    var createdVaultFile by remember { mutableStateOf<File?>(null) }
    var saveResultInfo by remember { mutableStateOf<com.example.data.VaultSaveResult?>(null) }

    // Media Picker launcher
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    VaultFileManager.readBytesFromUri(context, uri)
                }
                if (bytes != null && bytes.isNotEmpty()) {
                    val name = VaultFileManager.getFileNameFromUri(context, uri)
                    val cMime = context.contentResolver.getType(uri) ?: "application/octet-stream"
                    selectedMediaBytes = bytes
                    selectedFileName = name
                    mimeType = cMime
                    selectedMediaType = VaultFileManager.detectMediaType(name, cMime)
                    if (packageTitle == "Top Secret Media") {
                        packageTitle = name.substringBeforeLast(".")
                    }
                    Toast.makeText(context, "මාධ්‍ය ගොනුව තෝරාගන්නා ලදී ($name)", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "ගොනුව කියවීමට නොහැකි විය", Toast.LENGTH_SHORT).show()
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
                text = "Secure Vault Builder",
                color = PolishTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )
            Text(
                text = "ෆොටෝ හෝ වීඩියෝ විශේෂිතව AES-256 encrypt කර, එක් වරක් පමණක් බැලිය හැකි කුඩා .svault ගොනුවක් සාදන්න.",
                color = PolishTextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }

        // 1. Choose or Generate Media Card
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
                            text = "1. මාධ්‍ය තෝරන්න (Select Photo / Video)",
                            color = PolishPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedMediaBytes != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PolishEmeraldContainer)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "LOADED (${(selectedMediaBytes!!.size / 1024)} KB)",
                                    color = PolishEmeraldText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (selectedMediaBytes == null) {
                        // Polished dashed dropzone
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(PolishSurfaceVariant)
                                .border(1.dp, PolishCardBorderDashed, RoundedCornerShape(16.dp))
                                .clickable { mediaPickerLauncher.launch("*/*") }
                                .padding(vertical = 24.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(PolishPrimaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = "Upload",
                                        tint = PolishOnPrimaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Select Photo or Video",
                                        color = PolishTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "MP4, MKV, JPG, PNG up to 25MB",
                                        color = PolishTextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { mediaPickerLauncher.launch("*/*") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PolishPrimary,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pick_media_button")
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Gallery / Files", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    val sampleBytes = VaultFileManager.generateSampleSecureImage(
                                        "TOP SECRET MISSION",
                                        "This is an ultra-secure encrypted payload message protected with AES-256-GCM and FLAG_SECURE."
                                    )
                                    selectedMediaBytes = sampleBytes
                                    selectedMediaType = MediaType.IMAGE
                                    selectedFileName = "classified_intel.png"
                                    mimeType = "image/png"
                                    packageTitle = "Classified Intel Document"
                                    Toast.makeText(context, "Secure Sample Image Generated!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PolishPrimary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PolishCardBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("generate_sample_button")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Demo Media", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    } else {
                        // Media Selected Info Pill
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(PolishSurfaceVariant)
                                .border(1.dp, PolishCardBorder, RoundedCornerShape(14.dp))
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
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(PolishPrimaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (selectedMediaType == MediaType.IMAGE) Icons.Default.AddPhotoAlternate else Icons.Default.PlayCircle,
                                            contentDescription = null,
                                            tint = PolishOnPrimaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = selectedFileName,
                                            color = PolishTextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${selectedMediaType.name} • ${(selectedMediaBytes!!.size / 1024)} KB",
                                            color = PolishTextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Text(
                                    text = "Change",
                                    color = PolishPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PolishPrimaryContainer)
                                        .clickable { mediaPickerLauncher.launch("*/*") }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Package Title & Password Settings
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
                    Text(
                        text = "2. ගුප්තකේතන මුරපදය (Master Password)",
                        color = PolishPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = packageTitle,
                        onValueChange = { packageTitle = it },
                        label = { Text("ගොනුවේ නම / Title", color = PolishTextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PolishPrimary,
                            unfocusedBorderColor = PolishCardBorder,
                            focusedTextColor = PolishTextPrimary,
                            unfocusedTextColor = PolishTextPrimary,
                            focusedContainerColor = PolishSurfaceVariant,
                            unfocusedContainerColor = PolishSurfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("package_title_input")
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("මුරපදය (Password)", color = PolishTextMuted) },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = PolishPrimary) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = PolishTextMuted
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PolishPrimary,
                            unfocusedBorderColor = PolishCardBorder,
                            focusedTextColor = PolishTextPrimary,
                            unfocusedTextColor = PolishTextPrimary,
                            focusedContainerColor = PolishSurfaceVariant,
                            unfocusedContainerColor = PolishSurfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sender_password_input")
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("මුරපදය තහවුරු කරන්න (Confirm Password)", color = PolishTextMuted) },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = confirmPassword.isNotEmpty() && confirmPassword != password,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PolishPrimary,
                            unfocusedBorderColor = PolishCardBorder,
                            focusedTextColor = PolishTextPrimary,
                            unfocusedTextColor = PolishTextPrimary,
                            focusedContainerColor = PolishSurfaceVariant,
                            unfocusedContainerColor = PolishSurfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sender_confirm_password_input")
                    )

                    if (confirmPassword.isNotEmpty() && confirmPassword != password) {
                        Text(
                            text = "මුරපද දෙක සමාන නොවේ (Passwords do not match)",
                            color = PolishCrimson,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 3. Security Policy & Self-Destruct Rules
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
                        text = "3. ආරක්ෂක නීති සහ View Limit (Security Rules)",
                        color = PolishPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // View Policy Selection
                    Text(
                        text = "නැරඹිය හැකි වාර ගණන (View Limit Policy):",
                        color = PolishTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    ViewPolicy.values().forEach { policy ->
                        val isSelected = selectedPolicy == policy
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) PolishPrimaryContainer else PolishSurfaceVariant)
                                .border(
                                    1.dp,
                                    if (isSelected) PolishPrimary else PolishCardBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedPolicy = policy }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (policy == ViewPolicy.VIEW_ONCE) Icons.Default.Security else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = if (isSelected) PolishPrimary else PolishTextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = policy.label,
                                        color = if (isSelected) PolishOnPrimaryContainer else PolishTextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = PolishPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Self Destruct Timer
                    Text(
                        text = "ස්වයංක්‍රීයව විනාශ වන කාල සීමාව (Self-Destruct Timer):",
                        color = PolishTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    val timerOptions = listOf(10, 30, 60, 120, 0)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        timerOptions.forEach { seconds ->
                            val isSelected = selfDestructTimerSeconds == seconds
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) PolishPrimaryContainer else PolishSurfaceVariant)
                                    .border(
                                        1.dp,
                                        if (isSelected) PolishPrimary else PolishCardBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selfDestructTimerSeconds = seconds }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (seconds == 0) "No limit" else "${seconds}s",
                                    color = if (isSelected) PolishOnPrimaryContainer else PolishTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Expiry Period
                    Text(
                        text = "ගොනුවේ කල් ඉකුත්වීම (Package Expiration):",
                        color = PolishTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    val expiryOptions = listOf(Pair("1 Hour", 1), Pair("24 Hours", 24), Pair("7 Days", 168), Pair("Never", 0))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        expiryOptions.forEach { (label, hrs) ->
                            val isSelected = expiryHours == hrs
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) PolishAmberContainer else PolishSurfaceVariant)
                                    .border(
                                        1.dp,
                                        if (isSelected) PolishAmber else PolishCardBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { expiryHours = hrs }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) PolishAmber else PolishTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Watermark Text
                    OutlinedTextField(
                        value = watermarkText,
                        onValueChange = { watermarkText = it },
                        label = { Text("වෝටර්මාක් සටහන (Dynamic Watermark Text)", color = PolishTextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PolishPrimary,
                            unfocusedBorderColor = PolishCardBorder,
                            focusedTextColor = PolishTextPrimary,
                            unfocusedTextColor = PolishTextPrimary,
                            focusedContainerColor = PolishSurfaceVariant,
                            unfocusedContainerColor = PolishSurfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Important Notice Card in Theme Style
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(PolishSurfaceVariantSubtle)
                    .border(1.dp, PolishCardBorderAccent, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = PolishPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Important: The exported .svault package is fully self-contained and heavily encrypted. It can only be unlocked with the master password in Secure Vault.",
                        color = PolishOnPrimaryContainer,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // 4. Build & Export Button
        item {
            val isFormValid = selectedMediaBytes != null &&
                    password.isNotBlank() &&
                    password == confirmPassword

            Button(
                onClick = {
                    if (isFormValid && !isPackaging) {
                        isPackaging = true
                        scope.launch {
                            try {
                                val expiresAt = if (expiryHours > 0) {
                                    System.currentTimeMillis() + (expiryHours * 3600 * 1000L)
                                } else 0L

                                val metadata = VaultMetadata(
                                    id = UUID.randomUUID().toString(),
                                    title = packageTitle.ifBlank { "Secure Media Vault" },
                                    mediaType = selectedMediaType,
                                    mimeType = mimeType,
                                    createdAt = System.currentTimeMillis(),
                                    expiresAt = expiresAt,
                                    viewPolicy = selectedPolicy,
                                    maxViewSeconds = selfDestructTimerSeconds,
                                    watermarkText = watermarkText,
                                    originalFileName = selectedFileName
                                )

                                val vaultBytes = withContext(Dispatchers.Default) {
                                    CryptoEngine.packageVault(
                                        rawMediaBytes = selectedMediaBytes!!,
                                        metadata = metadata,
                                        passcode = password
                                    )
                                }

                                val saveRes = withContext(Dispatchers.IO) {
                                    VaultFileManager.saveVaultPackageToDownloads(
                                        context = context,
                                        packageBytes = vaultBytes,
                                        baseName = "${metadata.title}_${metadata.id.take(6)}"
                                    )
                                }

                                createdVaultFile = saveRes.internalFile
                                saveResultInfo = saveRes
                                onPackageCreated(saveRes.internalFile)
                                Toast.makeText(
                                    context,
                                    "ගොනුව Download Folder (${saveRes.downloadFilePath ?: "Downloads/SecureVault"}) හි සාර්ථකව සුරැකිණි!",
                                    Toast.LENGTH_LONG
                                ).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "දෝෂයකි: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isPackaging = false
                            }
                        }
                    }
                },
                enabled = isFormValid && !isPackaging,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PolishPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("build_vault_button")
            ) {
                if (isPackaging) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("AES-256 Encrypting & Saving to Downloads...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Build .svault Package (Download Folder එකට Save කරන්න)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Export / Share Success Card
        if (createdVaultFile != null) {
            val res = saveResultInfo
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PolishSurface),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PolishEmerald),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(PolishEmeraldContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = PolishEmeraldText, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "පැකේජය Download Folder හි සුරැකිණි!",
                                color = PolishEmeraldText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Path location box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(PolishSurfaceVariant)
                                .border(1.dp, PolishEmerald.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "📁 Download Location:",
                                    color = PolishEmeraldText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = res?.downloadFilePath ?: "Downloads/SecureVault/${createdVaultFile!!.name}",
                                    color = PolishTextPrimary,
                                    fontSize = 12.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "ප්‍රමාණය: ${createdVaultFile!!.length() / 1024} KB • AES-256-GCM Encrypted",
                                    color = PolishTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Text(
                            text = "මෙම .svault ගොනුව ඔබගේ දුරකතනයේ Download folder එකේ ඇත. අනෙක් පුද්ගලයාට WhatsApp / Telegram / Email මගින් යවන්න. ඔහුට බැලිය හැක්කේ Receiver ඇප් එකෙන් පමණි.",
                            color = PolishTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Button(
                            onClick = {
                                VaultFileManager.shareVaultPackage(
                                    context = context,
                                    file = createdVaultFile!!,
                                    title = packageTitle
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PolishPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("share_vault_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("යවන්න (Share via WhatsApp / Telegram)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

