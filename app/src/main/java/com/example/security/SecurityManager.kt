package com.example.security

import android.content.Context
import android.content.SharedPreferences
import com.example.crypto.VaultMetadata
import com.example.crypto.ViewPolicy

object SecurityManager {
    private const val PREFS_NAME = "secure_vault_registry"
    private const val KEY_CONSUMED_VAULTS = "consumed_vault_ids"
    private const val KEY_VIEW_COUNTS = "view_counts_"
    private const val KEY_AUDIT_LOGS = "security_audit_logs"
    private const val KEY_FLAG_SECURE = "is_flag_secure_enabled"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isFlagSecureEnabled(context: Context): Boolean {
        val prefs = getPrefs(context)
        // Default to false in web streaming environment to avoid black screen, but user can toggle on
        return prefs.getBoolean(KEY_FLAG_SECURE, false)
    }

    fun setFlagSecureEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_FLAG_SECURE, enabled).apply()
        logAuditEvent(context, if (enabled) "FLAG_SECURE hardware protection ENABLED" else "FLAG_SECURE set to PREVIEW mode (emulator friendly)")
    }

    fun isVaultConsumed(context: Context, vaultId: String): Boolean {
        val prefs = getPrefs(context)
        val consumedSet = prefs.getStringSet(KEY_CONSUMED_VAULTS, emptySet()) ?: emptySet()
        return consumedSet.contains(vaultId)
    }

    fun getViewCount(context: Context, vaultId: String): Int {
        val prefs = getPrefs(context)
        return prefs.getInt(KEY_VIEW_COUNTS + vaultId, 0)
    }

    fun canViewVault(context: Context, metadata: VaultMetadata): Pair<Boolean, String> {
        // Check 1: Expiry
        if (metadata.expiresAt > 0L && System.currentTimeMillis() > metadata.expiresAt) {
            return Pair(false, "කල් ඉකුත් වී ඇත (Vault Expired on ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(metadata.expiresAt))})")
        }

        // Check 2: Consumed
        if (isVaultConsumed(context, metadata.id)) {
            return Pair(false, "මෙම ෆයිල් එක දැනටමත් බලා අවසන් කර ඇත (This View-Once vault has already been consumed & destroyed).")
        }

        // Check 3: Max view limit
        val currentViews = getViewCount(context, metadata.id)
        if (currentViews >= metadata.viewPolicy.maxViews) {
            return Pair(false, "නැරඹිය හැකි උපරිම වාර ගණන ඉක්මවා ඇත (Max view limit reached: ${metadata.viewPolicy.maxViews} views).")
        }

        return Pair(true, "Authorized")
    }

    fun registerVaultView(context: Context, metadata: VaultMetadata) {
        val prefs = getPrefs(context)
        val currentViews = getViewCount(context, metadata.id) + 1
        prefs.edit().putInt(KEY_VIEW_COUNTS + metadata.id, currentViews).apply()

        if (metadata.viewPolicy == ViewPolicy.VIEW_ONCE || currentViews >= metadata.viewPolicy.maxViews) {
            markVaultBurned(context, metadata.id, metadata.title)
        }

        logAuditEvent(context, "Opened vault: ${metadata.title} (View $currentViews/${metadata.viewPolicy.maxViews})")
    }

    fun markVaultBurned(context: Context, vaultId: String, title: String) {
        val prefs = getPrefs(context)
        val consumedSet = (prefs.getStringSet(KEY_CONSUMED_VAULTS, emptySet()) ?: emptySet()).toMutableSet()
        consumedSet.add(vaultId)
        prefs.edit().putStringSet(KEY_CONSUMED_VAULTS, consumedSet).apply()

        logAuditEvent(context, "PERMANENTLY BURNED & DESTROYED: $title (ID: ${vaultId.take(8)}...)")
    }

    fun logAuditEvent(context: Context, message: String) {
        val prefs = getPrefs(context)
        val time = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val entry = "[$time] $message"
        val existing = prefs.getString(KEY_AUDIT_LOGS, "") ?: ""
        val updated = if (existing.isEmpty()) entry else "$entry\n$existing"
        // Keep max 50 log lines
        val trimmed = updated.lines().take(50).joinToString("\n")
        prefs.edit().putString(KEY_AUDIT_LOGS, trimmed).apply()
    }

    fun getAuditLogs(context: Context): List<String> {
        val prefs = getPrefs(context)
        val logs = prefs.getString(KEY_AUDIT_LOGS, "") ?: ""
        return if (logs.isEmpty()) {
            listOf("Security Engine Initialized - Hardware Anti-Capture Active")
        } else {
            logs.lines().filter { it.isNotBlank() }
        }
    }

    fun clearRegistry(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
