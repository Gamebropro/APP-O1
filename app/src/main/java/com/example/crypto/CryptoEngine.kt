package com.example.crypto

import android.content.Context
import android.graphics.BitmapFactory
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoEngine {
    private const val MAGIC_HEADER = "SVAULT1" // 7 ASCII bytes
    private const val ITERATION_COUNT = 10000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16
    private const val IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128

    private val secureRandom = SecureRandom()

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH_BITS)
        val secretKey = factory.generateSecret(spec)
        return SecretKeySpec(secretKey.encoded, "AES")
    }

    private fun compress(data: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream(data.size)
        GZIPOutputStream(bos).use { gzip ->
            gzip.write(data)
        }
        return bos.toByteArray()
    }

    private fun decompress(compressedData: ByteArray): ByteArray {
        val bis = ByteArrayInputStream(compressedData)
        val bos = ByteArrayOutputStream()
        GZIPInputStream(bis).use { gzip ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (gzip.read(buffer).also { bytesRead = it } != -1) {
                bos.write(buffer, 0, bytesRead)
            }
        }
        return bos.toByteArray()
    }

    fun packageVault(
        rawMediaBytes: ByteArray,
        metadata: VaultMetadata,
        passcode: String
    ): ByteArray {
        val salt = ByteArray(SALT_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH_BYTES).also { secureRandom.nextBytes(it) }

        val secretKey = deriveKey(passcode, salt)

        // Compress media before encryption for compactness
        val compressedMedia = compress(rawMediaBytes)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val encryptedPayload = cipher.doFinal(compressedMedia)

        val metaJson = JSONObject().apply {
            put("id", metadata.id)
            put("title", metadata.title)
            put("mediaType", metadata.mediaType.name)
            put("mimeType", metadata.mimeType)
            put("createdAt", metadata.createdAt)
            put("expiresAt", metadata.expiresAt)
            put("viewPolicy", metadata.viewPolicy.name)
            put("maxViewSeconds", metadata.maxViewSeconds)
            put("watermarkText", metadata.watermarkText)
            put("originalFileName", metadata.originalFileName)
            put("uncompressedSize", rawMediaBytes.size.toLong())
        }
        val metaBytes = metaJson.toString().toByteArray(StandardCharsets.UTF_8)

        val magicBytes = MAGIC_HEADER.toByteArray(StandardCharsets.US_ASCII)

        val totalSize = magicBytes.size +
                salt.size +
                iv.size +
                4 + metaBytes.size +
                4 + encryptedPayload.size

        val buffer = ByteBuffer.allocate(totalSize)
        buffer.put(magicBytes)
        buffer.put(salt)
        buffer.put(iv)
        buffer.putInt(metaBytes.size)
        buffer.put(metaBytes)
        buffer.putInt(encryptedPayload.size)
        buffer.put(encryptedPayload)

        return buffer.array()
    }

    fun inspectMetadata(vaultPackageBytes: ByteArray): VaultMetadata {
        val buffer = ByteBuffer.wrap(vaultPackageBytes)
        val magicBytes = ByteArray(7)
        buffer.get(magicBytes)
        val magicString = String(magicBytes, StandardCharsets.US_ASCII)
        if (magicString != MAGIC_HEADER) {
            throw IllegalArgumentException("Invalid Vault file format (Magic mismatch). Not a valid .svault container.")
        }

        val salt = ByteArray(SALT_LENGTH_BYTES)
        buffer.get(salt)

        val iv = ByteArray(IV_LENGTH_BYTES)
        buffer.get(iv)

        val metaLen = buffer.int
        if (metaLen <= 0 || metaLen > 1024 * 1024) {
            throw IllegalArgumentException("Corrupted metadata header")
        }
        val metaBytes = ByteArray(metaLen)
        buffer.get(metaBytes)

        val json = JSONObject(String(metaBytes, StandardCharsets.UTF_8))
        return VaultMetadata(
            id = json.optString("id"),
            title = json.optString("title", "Encrypted Vault Media"),
            mediaType = try {
                MediaType.valueOf(json.optString("mediaType", "IMAGE"))
            } catch (_: Exception) {
                MediaType.IMAGE
            },
            mimeType = json.optString("mimeType", "image/jpeg"),
            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
            expiresAt = json.optLong("expiresAt", 0L),
            viewPolicy = try {
                ViewPolicy.valueOf(json.optString("viewPolicy", "VIEW_ONCE"))
            } catch (_: Exception) {
                ViewPolicy.VIEW_ONCE
            },
            maxViewSeconds = json.optInt("maxViewSeconds", 30),
            watermarkText = json.optString("watermarkText", ""),
            originalFileName = json.optString("originalFileName", ""),
            uncompressedSize = json.optLong("uncompressedSize", 0L)
        )
    }

    fun decryptVault(
        context: Context,
        vaultPackageBytes: ByteArray,
        passcode: String
    ): DecryptedVaultMedia {
        val buffer = ByteBuffer.wrap(vaultPackageBytes)
        val magicBytes = ByteArray(7)
        buffer.get(magicBytes)
        val magicString = String(magicBytes, StandardCharsets.US_ASCII)
        if (magicString != MAGIC_HEADER) {
            throw IllegalArgumentException("Invalid Vault format. File signature does not match.")
        }

        val salt = ByteArray(SALT_LENGTH_BYTES)
        buffer.get(salt)

        val iv = ByteArray(IV_LENGTH_BYTES)
        buffer.get(iv)

        val metaLen = buffer.int
        val metaBytes = ByteArray(metaLen)
        buffer.get(metaBytes)

        val json = JSONObject(String(metaBytes, StandardCharsets.UTF_8))
        val metadata = VaultMetadata(
            id = json.optString("id"),
            title = json.optString("title", "Encrypted Vault Media"),
            mediaType = try {
                MediaType.valueOf(json.optString("mediaType", "IMAGE"))
            } catch (_: Exception) {
                MediaType.IMAGE
            },
            mimeType = json.optString("mimeType", "image/jpeg"),
            createdAt = json.optLong("createdAt", 0L),
            expiresAt = json.optLong("expiresAt", 0L),
            viewPolicy = try {
                ViewPolicy.valueOf(json.optString("viewPolicy", "VIEW_ONCE"))
            } catch (_: Exception) {
                ViewPolicy.VIEW_ONCE
            },
            maxViewSeconds = json.optInt("maxViewSeconds", 30),
            watermarkText = json.optString("watermarkText", ""),
            originalFileName = json.optString("originalFileName", ""),
            uncompressedSize = json.optLong("uncompressedSize", 0L)
        )

        val payloadLen = buffer.int
        val encryptedPayload = ByteArray(payloadLen)
        buffer.get(encryptedPayload)

        // Decrypt with AES-GCM
        val secretKey = deriveKey(passcode, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        val decryptedCompressed = try {
            cipher.doFinal(encryptedPayload)
        } catch (e: Exception) {
            throw IllegalArgumentException("Decryption failed! Incorrect password or corrupted vault file.")
        }

        val rawBytes = decompress(decryptedCompressed)

        var bitmap: android.graphics.Bitmap? = null
        var tempVideoFile: File? = null

        if (metadata.mediaType == MediaType.IMAGE) {
            bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
            if (bitmap == null) {
                throw IllegalStateException("Failed to decode image from decrypted stream")
            }
        } else {
            // Ephemeral private cache file for video playback
            val cacheDir = File(context.cacheDir, "ephemeral_vault").apply { mkdirs() }
            val tempFile = File(cacheDir, "temp_stream_${System.currentTimeMillis()}.mp4")
            tempFile.writeBytes(rawBytes)
            tempVideoFile = tempFile
        }

        return DecryptedVaultMedia(
            metadata = metadata,
            rawBytes = rawBytes,
            bitmap = bitmap,
            tempVideoFile = tempVideoFile
        )
    }
}
