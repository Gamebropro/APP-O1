package com.example.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.crypto.MediaType
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class VaultSaveResult(
    val internalFile: File,
    val downloadUri: Uri?,
    val downloadFilePath: String?,
    val fileName: String,
    val isSavedToDownloads: Boolean
)

data class VaultFileInfo(
    val file: File?,
    val uri: Uri?,
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val locationLabel: String
)

object VaultFileManager {

    fun readBytesFromUri(context: Context, uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }

    fun getFileNameFromUri(context: Context, uri: Uri): String {
        var name = "media_${System.currentTimeMillis()}"
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        name = it.getString(nameIndex) ?: name
                    }
                }
            }
        } catch (_: Exception) {
        }
        return name
    }

    fun detectMediaType(fileName: String, mimeType: String?): MediaType {
        val lowerName = fileName.lowercase()
        val lowerMime = (mimeType ?: "").lowercase()
        return if (lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") || lowerName.endsWith(".3gp") || lowerMime.startsWith("video/")) {
            MediaType.VIDEO
        } else {
            MediaType.IMAGE
        }
    }

    fun saveVaultPackage(context: Context, packageBytes: ByteArray, baseName: String): File {
        val outboxDir = File(context.filesDir, "outbox_vaults").apply { mkdirs() }
        val cleanName = baseName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val fileName = if (cleanName.endsWith(".svault")) cleanName else "${cleanName}.svault"
        val file = File(outboxDir, fileName)
        file.writeBytes(packageBytes)
        return file
    }

    /**
     * Saves the encrypted .svault package directly to the device's public Download folder
     * (Download/SecureVault/) and keeps an internal cached copy for fast sharing.
     */
    fun saveVaultPackageToDownloads(context: Context, packageBytes: ByteArray, baseName: String): VaultSaveResult {
        val cleanName = baseName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val fileName = if (cleanName.endsWith(".svault")) cleanName else "${cleanName}.svault"

        // 1. Always write to app internal outbox
        val internalFile = saveVaultPackage(context, packageBytes, fileName)

        var downloadUri: Uri? = null
        var downloadFilePath: String? = null
        var isSaved = false

        // 2. Save to Public Downloads folder
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/SecureVault")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(packageBytes)
                        os.flush()
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    context.contentResolver.update(uri, contentValues, null, null)

                    downloadUri = uri
                    downloadFilePath = "Downloads/SecureVault/$fileName"
                    isSaved = true
                }
            } else {
                // Fallback for older Android versions
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val vaultDir = File(downloadsDir, "SecureVault").apply { mkdirs() }
                val targetFile = File(vaultDir, fileName)
                targetFile.writeBytes(packageBytes)

                downloadFilePath = targetFile.absolutePath
                downloadUri = Uri.fromFile(targetFile)
                isSaved = true
            }
        } catch (_: Exception) {
            // MediaStore permission or fallback write error, internalFile remains valid
        }

        return VaultSaveResult(
            internalFile = internalFile,
            downloadUri = downloadUri,
            downloadFilePath = downloadFilePath ?: "Downloads/SecureVault/$fileName",
            fileName = fileName,
            isSavedToDownloads = isSaved
        )
    }

    fun shareVaultPackage(context: Context, file: File, title: String) {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Secure Vault Package: $title")
            putExtra(Intent.EXTRA_TEXT, "🔒 Here is a secure encrypted .svault package ($title). Open in Secure Vault App with the agreed password.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share Encrypted Vault Package")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun generateSampleSecureImage(title: String, secretMessage: String): ByteArray {
        val width = 1080
        val height = 1440
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Dark Luxury Cyber Background
        val bgPaint = Paint().apply {
            shader = RadialGradient(
                width / 2f, height / 2f, width.toFloat(),
                intArrayOf(Color.parseColor("#1B2838"), Color.parseColor("#0A0E17")),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Cyber Grid Lines
        val gridPaint = Paint().apply {
            color = Color.parseColor("#1A00F59B")
            strokeWidth = 2f
        }
        for (x in 0..width step 80) {
            canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), gridPaint)
        }
        for (y in 0..height step 80) {
            canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), gridPaint)
        }

        // Central Shield Card
        val cardPaint = Paint().apply {
            color = Color.parseColor("#DD0F1923")
            style = Paint.Style.FILL
        }
        val strokePaint = Paint().apply {
            color = Color.parseColor("#00F59B")
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        val cardRect = RectF(100f, 250f, width - 100f, height - 250f)
        canvas.drawRoundRect(cardRect, 40f, 40f, cardPaint)
        canvas.drawRoundRect(cardRect, 40f, 40f, strokePaint)

        // Header Security Badge
        val badgePaint = Paint().apply {
            color = Color.parseColor("#00F59B")
            textSize = 38f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("🛡️ MILITARY-GRADE AES-256 VAULT", width / 2f, 350f, badgePaint)

        // Title
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 52f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(title, width / 2f, 460f, titlePaint)

        // Secret message area
        val msgBgPaint = Paint().apply {
            color = Color.parseColor("#2200D2FF")
        }
        val msgRect = RectF(160f, 540f, width - 160f, 920f)
        canvas.drawRoundRect(msgRect, 24f, 24f, msgBgPaint)

        val msgPaint = Paint().apply {
            color = Color.parseColor("#E0E6ED")
            textSize = 42f
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
        }
        val lines = secretMessage.chunked(32)
        var textY = 640f
        for (line in lines) {
            canvas.drawText(line, width / 2f, textY, msgPaint)
            textY += 60f
        }

        // Dynamic Watermark / Timestamp
        val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val metaPaint = Paint().apply {
            color = Color.parseColor("#8899A6")
            textSize = 28f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Generated at: $timeStr", width / 2f, 1020f, metaPaint)
        canvas.drawText("🔒 Anti-Screenshot & Screen-Record Enforced", width / 2f, 1080f, metaPaint)
        canvas.drawText("ID: ${java.util.UUID.randomUUID()}", width / 2f, 1140f, metaPaint)

        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos)
        bitmap.recycle()
        return bos.toByteArray()
    }

    fun listOutboxPackages(context: Context): List<File> {
        val outboxDir = File(context.filesDir, "outbox_vaults")
        return outboxDir.listFiles { f -> f.name.endsWith(".svault") || f.name.endsWith(".sec") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    /**
     * Lists all vault packages found in internal outbox, cache, and public Downloads/SecureVault
     */
    fun listAllAvailableVaultPackages(context: Context): List<VaultFileInfo> {
        val results = mutableListOf<VaultFileInfo>()
        val seenNames = mutableSetOf<String>()

        // 1. Internal Outbox
        val outboxDir = File(context.filesDir, "outbox_vaults")
        outboxDir.listFiles { f -> f.name.endsWith(".svault") || f.name.endsWith(".sec") }?.forEach { f ->
            if (seenNames.add(f.name)) {
                results.add(
                    VaultFileInfo(
                        file = f,
                        uri = null,
                        name = f.name,
                        sizeBytes = f.length(),
                        lastModified = f.lastModified(),
                        locationLabel = "Created Locally (Outbox)"
                    )
                )
            }
        }

        // 2. Public Downloads / SecureVault
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val vaultDir = File(downloadsDir, "SecureVault")
            if (vaultDir.exists() && vaultDir.isDirectory) {
                vaultDir.listFiles { f -> f.name.endsWith(".svault") || f.name.endsWith(".sec") }?.forEach { f ->
                    if (seenNames.add(f.name)) {
                        results.add(
                            VaultFileInfo(
                                file = f,
                                uri = null,
                                name = f.name,
                                sizeBytes = f.length(),
                                lastModified = f.lastModified(),
                                locationLabel = "Downloads/SecureVault"
                            )
                        )
                    }
                }
            }

            if (downloadsDir.exists() && downloadsDir.isDirectory) {
                downloadsDir.listFiles { f -> f.name.endsWith(".svault") || f.name.endsWith(".sec") }?.forEach { f ->
                    if (seenNames.add(f.name)) {
                        results.add(
                            VaultFileInfo(
                                file = f,
                                uri = null,
                                name = f.name,
                                sizeBytes = f.length(),
                                lastModified = f.lastModified(),
                                locationLabel = "Downloads"
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }

        return results.sortedByDescending { it.lastModified }
    }
}

