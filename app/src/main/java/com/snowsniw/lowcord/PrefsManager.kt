package com.snowsniw.lowcord

import android.content.Context

object PrefsManager {
    private const val PREFS_NAME = "lowcord_prefs"
    private const val KEY_PERF_MODE = "perf_mode"
    private const val KEY_AMOLED_MODE = "amoled_mode"
    private const val KEY_PROXY_ENABLED = "proxy_enabled"
    private const val KEY_PROXY_URL = "proxy_url"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isPerfMode(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PERF_MODE, true)

    fun setPerfMode(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PERF_MODE, enabled).apply()
    }

    fun isAmoledMode(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AMOLED_MODE, false)

    fun setAmoledMode(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AMOLED_MODE, enabled).apply()
    }

    fun isProxyEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PROXY_ENABLED, false)

    fun setProxyEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PROXY_ENABLED, enabled).apply()
    }

    fun getProxyUrl(context: Context): String =
        prefs(context).getString(KEY_PROXY_URL, "socks5://127.0.0.1:1080") ?: "socks5://127.0.0.1:1080"

    fun setProxyUrl(context: Context, url: String) {
        prefs(context).edit().putString(KEY_PROXY_URL, url.trim()).apply()
    }
}

