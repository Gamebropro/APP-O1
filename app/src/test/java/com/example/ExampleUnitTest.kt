package com.example

import com.example.crypto.CryptoEngine
import com.example.crypto.MediaType
import com.example.crypto.VaultMetadata
import com.example.crypto.ViewPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleUnitTest {
    @Test
    fun testVaultPackagingAndMetadataInspection() {
        val samplePayload = "Secret cryptographic data payload for verification".toByteArray()
        val metadata = VaultMetadata(
            id = "test-vault-12345",
            title = "Top Secret Classified",
            mediaType = MediaType.IMAGE,
            mimeType = "image/png",
            createdAt = 1700000000000L,
            expiresAt = 1800000000000L,
            viewPolicy = ViewPolicy.VIEW_ONCE,
            maxViewSeconds = 30,
            watermarkText = "CONFIDENTIAL",
            originalFileName = "secret.png"
        )

        val packageBytes = CryptoEngine.packageVault(
            rawMediaBytes = samplePayload,
            metadata = metadata,
            passcode = "P@ssw0rd123!"
        )

        assertNotNull(packageBytes)
        assertTrue(packageBytes.isNotEmpty())

        val parsedMeta = CryptoEngine.inspectMetadata(packageBytes)
        assertEquals("test-vault-12345", parsedMeta.id)
        assertEquals("Top Secret Classified", parsedMeta.title)
        assertEquals(MediaType.IMAGE, parsedMeta.mediaType)
        assertEquals(ViewPolicy.VIEW_ONCE, parsedMeta.viewPolicy)
        assertEquals(30, parsedMeta.maxViewSeconds)
    }
}
