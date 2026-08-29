package com.example.crypto

import android.graphics.Bitmap
import java.io.File

enum class MediaType {
    IMAGE,
    VIDEO
}

enum class ViewPolicy(val label: String, val maxViews: Int) {
    VIEW_ONCE("View Once (එක් වරක් පමණක් බැලිය හැක)", 1),
    TWO_VIEWS("2 Views (දෙවරක් බැලිය හැක)", 2),
    THREE_VIEWS("3 Views (තෙවරක් බැලිය හැක)", 3),
    UNLIMITED("Unlimited (සීමාවක් නැත)", 999999)
}

data class VaultMetadata(
    val id: String,
    val title: String,
    val mediaType: MediaType,
    val mimeType: String,
    val createdAt: Long,
    val expiresAt: Long = 0L, // 0 means no expiration
    val viewPolicy: ViewPolicy = ViewPolicy.VIEW_ONCE,
    val maxViewSeconds: Int = 30, // 0 means no timer
    val watermarkText: String = "",
    val originalFileName: String = "",
    val uncompressedSize: Long = 0L
)

data class DecryptedVaultMedia(
    val metadata: VaultMetadata,
    val rawBytes: ByteArray,
    val bitmap: Bitmap? = null,
    val tempVideoFile: File? = null
) {
    fun wipe() {
        try {
            rawBytes.fill(0)
            bitmap?.recycle()
            tempVideoFile?.let {
                if (it.exists()) {
                    it.writeBytes(ByteArray(0))
                    it.delete()
                }
            }
        } catch (_: Exception) {
        }
    }
}
