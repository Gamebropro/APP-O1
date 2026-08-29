package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.DecryptedVaultMedia
import com.example.ui.components.AdminAuthModal
import com.example.ui.components.SecurityHeader
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishCardBorder
import com.example.ui.theme.PolishOnPrimaryContainer
import com.example.ui.theme.PolishPrimary
import com.example.ui.theme.PolishPrimaryContainer
import com.example.ui.theme.PolishSurface
import com.example.ui.theme.PolishTextMuted
import com.example.ui.theme.PolishTextPrimary
import com.example.ui.theme.PolishTextSecondary

private const val ADMIN_ACCESS_KEY = "0774286700"

enum class AppRole(val title: String, val tabIndex: Int) {
    RECEIVER("Receiver Vault (ලබන්නාගේ ඇප් එක)", 0),
    SENDER("Sender Studio (යවන්නාගේ ඇප් එක)", 1),
    SECURITY("Security Logs (ආරක්ෂක ලොග්)", 2)
}

@Composable
fun MainVaultScreen(
    incomingVaultUri: Uri? = null,
    modifier: Modifier = Modifier
) {
    // Receiver is default and open to everyone without password
    var selectedRole by remember { mutableStateOf(AppRole.RECEIVER) }
    var isSenderUnlocked by remember { mutableStateOf(false) }
    var isSecurityUnlocked by remember { mutableStateOf(false) }

    // Modal state for password prompt
    var pendingRoleToUnlock by remember { mutableStateOf<AppRole?>(null) }
    var adminPasswordError by remember { mutableStateOf<String?>(null) }

    var activeDecryptedMedia by remember { mutableStateOf<DecryptedVaultMedia?>(null) }

    // Handler when user taps a navigation tab
    fun onTabSelected(role: AppRole) {
        when (role) {
            AppRole.RECEIVER -> {
                // Receiver APK has NO password requirement
                selectedRole = AppRole.RECEIVER
            }
            AppRole.SENDER -> {
                if (isSenderUnlocked) {
                    selectedRole = AppRole.SENDER
                } else {
                    adminPasswordError = null
                    pendingRoleToUnlock = AppRole.SENDER
                }
            }
            AppRole.SECURITY -> {
                if (isSecurityUnlocked) {
                    selectedRole = AppRole.SECURITY
                } else {
                    adminPasswordError = null
                    pendingRoleToUnlock = AppRole.SECURITY
                }
            }
        }
    }

    // Modal Dialog for Admin PIN Unlock (Sender / Security)
    if (pendingRoleToUnlock != null) {
        AdminAuthModal(
            sectionName = if (pendingRoleToUnlock == AppRole.SENDER) "Sender APK" else "Security Logs",
            errorMessage = adminPasswordError,
            onDismiss = {
                pendingRoleToUnlock = null
                adminPasswordError = null
            },
            onUnlock = { enteredPassword ->
                if (enteredPassword.trim() == ADMIN_ACCESS_KEY) {
                    if (pendingRoleToUnlock == AppRole.SENDER) {
                        isSenderUnlocked = true
                        selectedRole = AppRole.SENDER
                    } else if (pendingRoleToUnlock == AppRole.SECURITY) {
                        isSecurityUnlocked = true
                        selectedRole = AppRole.SECURITY
                    }
                    pendingRoleToUnlock = null
                    adminPasswordError = null
                } else {
                    adminPasswordError = "මුරපදය වැරදියි (Invalid Password)! නැවත උත්සාහ කරන්න."
                }
            }
        )
    }

    // If an ephemeral media viewer is active, show the fullscreen protected viewer
    if (activeDecryptedMedia != null) {
        SecureEphemeralViewer(
            decryptedMedia = activeDecryptedMedia!!,
            onCloseAndBurn = {
                activeDecryptedMedia = null
                selectedRole = AppRole.RECEIVER
            }
        )
    } else {
        Scaffold(
            containerColor = PolishBackground,
            topBar = {
                SecurityHeader(
                    activeModeTitle = selectedRole.title,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            },
            bottomBar = {
                Surface(
                    color = PolishSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, PolishCardBorder),
                    shadowElevation = 8.dp
                ) {
                    NavigationBar(
                        containerColor = PolishSurface,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .testTag("main_navigation_bar")
                    ) {
                        // 1. Receiver Tab (100% Free / No Password)
                        NavigationBarItem(
                            selected = selectedRole == AppRole.RECEIVER,
                            onClick = { onTabSelected(AppRole.RECEIVER) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Receiver",
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    "Receiver APK",
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedRole == AppRole.RECEIVER) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PolishOnPrimaryContainer,
                                selectedTextColor = PolishPrimary,
                                unselectedIconColor = PolishTextSecondary,
                                unselectedTextColor = PolishTextSecondary,
                                indicatorColor = PolishPrimaryContainer
                            ),
                            modifier = Modifier.testTag("nav_receiver_tab")
                        )

                        // 2. Sender Tab (Protected by Admin Password)
                        NavigationBarItem(
                            selected = selectedRole == AppRole.SENDER,
                            onClick = { onTabSelected(AppRole.SENDER) },
                            icon = {
                                Box {
                                    Icon(
                                        imageVector = Icons.Default.FileUpload,
                                        contentDescription = "Sender",
                                        modifier = Modifier.size(24.dp)
                                    )
                                    if (!isSenderUnlocked) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            tint = com.example.ui.theme.PolishAmber,
                                            modifier = Modifier
                                                .size(12.dp)
                                                .align(androidx.compose.ui.Alignment.BottomEnd)
                                        )
                                    }
                                }
                            },
                            label = {
                                Text(
                                    if (isSenderUnlocked) "Sender APK" else "Sender 🔒",
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedRole == AppRole.SENDER) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PolishOnPrimaryContainer,
                                selectedTextColor = PolishPrimary,
                                unselectedIconColor = PolishTextSecondary,
                                unselectedTextColor = PolishTextSecondary,
                                indicatorColor = PolishPrimaryContainer
                            ),
                            modifier = Modifier.testTag("nav_sender_tab")
                        )

                        // 3. Security Logs Tab (Protected by Admin Password)
                        NavigationBarItem(
                            selected = selectedRole == AppRole.SECURITY,
                            onClick = { onTabSelected(AppRole.SECURITY) },
                            icon = {
                                Box {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = "Security",
                                        modifier = Modifier.size(24.dp)
                                    )
                                    if (!isSecurityUnlocked) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            tint = com.example.ui.theme.PolishAmber,
                                            modifier = Modifier
                                                .size(12.dp)
                                                .align(androidx.compose.ui.Alignment.BottomEnd)
                                        )
                                    }
                                }
                            },
                            label = {
                                Text(
                                    if (isSecurityUnlocked) "Security Logs" else "Logs 🔒",
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedRole == AppRole.SECURITY) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PolishOnPrimaryContainer,
                                selectedTextColor = PolishPrimary,
                                unselectedIconColor = PolishTextSecondary,
                                unselectedTextColor = PolishTextSecondary,
                                indicatorColor = PolishPrimaryContainer
                            ),
                            modifier = Modifier.testTag("nav_security_tab")
                        )
                    }
                }
            },
            modifier = modifier.fillMaxSize()
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AnimatedContent(
                    targetState = selectedRole,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "RoleTransition"
                ) { role ->
                    when (role) {
                        AppRole.RECEIVER -> ReceiverScreen(
                            incomingVaultUri = incomingVaultUri,
                            onOpenViewer = { media ->
                                activeDecryptedMedia = media
                            }
                        )

                        AppRole.SENDER -> SenderScreen(
                            onPackageCreated = { file ->
                                // Package ready
                            }
                        )

                        AppRole.SECURITY -> SecurityLogsScreen()
                    }
                }
            }
        }
    }
}


