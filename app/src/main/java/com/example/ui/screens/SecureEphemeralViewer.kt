package com.example.ui.screens

import android.graphics.Bitmap
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.crypto.DecryptedVaultMedia
import com.example.crypto.MediaType
import com.example.ui.components.CountdownTimerBar
import com.example.ui.components.WatermarkOverlay
import com.example.ui.theme.PolishCrimson
import com.example.ui.theme.PolishCrimsonContainer
import com.example.ui.theme.PolishCrimsonText
import com.example.ui.theme.PolishEmerald
import com.example.ui.theme.PolishEmeraldContainer
import com.example.ui.theme.PolishEmeraldText
import com.example.ui.theme.PolishPrimary
import com.example.ui.theme.PolishPrimaryContainer
import kotlinx.coroutines.delay

@Composable
fun SecureEphemeralViewer(
    decryptedMedia: DecryptedVaultMedia,
    onCloseAndBurn: () -> Unit
) {
    val context = LocalContext.current
    val metadata = decryptedMedia.metadata

    val initialSeconds = remember {
        if (metadata.maxViewSeconds > 0) metadata.maxViewSeconds else 0
    }
    var remainingSeconds by remember { mutableIntStateOf(initialSeconds) }
    var isDestroyed by remember { mutableStateOf(false) }

    // Transform states for Image zoom & pan
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Self-destruct timer countdown
    LaunchedEffect(key1 = initialSeconds) {
        if (initialSeconds > 0) {
            while (remainingSeconds > 0 && !isDestroyed) {
                delay(1000L)
                remainingSeconds -= 1
            }
            if (remainingSeconds <= 0) {
                isDestroyed = true
                delay(400L)
                decryptedMedia.wipe()
                onCloseAndBurn()
            }
        }
    }

    // Clean up memory buffer upon exit
    DisposableEffect(Unit) {
        onDispose {
            decryptedMedia.wipe()
        }
    }

    // Intercept back button to trigger secure burn
    BackHandler {
        decryptedMedia.wipe()
        onCloseAndBurn()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Media Content Area
        if (metadata.mediaType == MediaType.IMAGE && decryptedMedia.bitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                offset += pan
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = decryptedMedia.bitmap.asImageBitmap(),
                    contentDescription = "Encrypted Image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                )
            }
        } else if (metadata.mediaType == MediaType.VIDEO && decryptedMedia.tempVideoFile != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoPath(decryptedMedia.tempVideoFile.absolutePath)
                            val mediaController = MediaController(ctx)
                            mediaController.setAnchorView(this)
                            setMediaController(mediaController)
                            setOnPreparedListener { mp ->
                                mp.isLooping = true
                                start()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "මාධ්‍ය දත්ත රහස්‍යව ප්‍රදර්ශනය කළ නොහැක",
                    color = PolishCrimson,
                    fontSize = 16.sp
                )
            }
        }

        // Anti-Camera Watermark Overlay
        WatermarkOverlay(
            customText = metadata.watermarkText,
            vaultId = metadata.id
        )

        // Top Security App Bar
        Surface(
            color = Color(0xF01E293B),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                .clip(CircleShape)
                                .background(Color(0xFF334155)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = PolishEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = metadata.title,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "🔒 FLAG_SECURE Active • In-Memory Buffer",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            decryptedMedia.wipe()
                            onCloseAndBurn()
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF334155))
                            .testTag("close_viewer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close & Burn",
                            tint = Color.White
                        )
                    }
                }

                // Countdown Timer Bar if set
                if (initialSeconds > 0) {
                    CountdownTimerBar(
                        remainingSeconds = remainingSeconds,
                        totalSeconds = initialSeconds
                    )
                }
            }
        }

        // Bottom Action Bar: Burn & Destroy Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Button(
                onClick = {
                    decryptedMedia.wipe()
                    onCloseAndBurn()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PolishCrimson,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("burn_destroy_button")
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "දැන්ම විනාශ කර වසන්න (Burn & Destroy)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

