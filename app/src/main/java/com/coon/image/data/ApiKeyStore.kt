package com.coon.image.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * 把 DashScope API Key 保存在本机加密存储中，输入一次后自动保存、下次启动自动读取。
 */
object ApiKeyStore {
    private const val PREF_NAME = "coon_secure_prefs"
    private const val KEY_API = "dashscope_api_key"

    private fun prefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREF_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getApiKey(context: Context): String? {
        return prefs(context).getString(KEY_API, null)?.takeIf { it.isNotBlank() }
    }

    fun saveApiKey(context: Context, key: String) {
        prefs(context).edit().putString(KEY_API, key.trim()).apply()
    }

    fun hasApiKey(context: Context): Boolean = getApiKey(context) != null
}
