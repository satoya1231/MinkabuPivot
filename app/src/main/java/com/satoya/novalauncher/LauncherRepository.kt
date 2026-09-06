package com.satoya.novalauncher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

class LauncherRepository(private val context: Context) {
    private val pm = context.packageManager
    private val prefs = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)

    fun loadApps(): List<AppEntry> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL).mapNotNull { info ->
            val ai = info.activityInfo ?: return@mapNotNull null
            AppEntry(info.loadLabel(pm).toString(), ai.packageName, ai.name, info.loadIcon(pm))
        }.distinctBy { it.key }.sortedBy { it.label.lowercase() }
    }

    fun launch(app: AppEntry) = runCatching {
        context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(app.packageName, app.activityName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess

    fun favorites(): Set<String> = prefs.getStringSet("favorites", emptySet())?.toSet() ?: emptySet()

    fun setFavorite(app: AppEntry, value: Boolean) {
        val set = favorites().toMutableSet()
        if (value) set += app.key else set -= app.key
        prefs.edit().putStringSet("favorites", set).apply()
    }
}
