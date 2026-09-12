package com.satoya.novalauncher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import org.json.JSONArray

class LauncherRepository(private val context: Context) {
    private val pm = context.packageManager
    private val prefs = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)

    fun loadApps(): List<AppEntry> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL).mapNotNull { info ->
            val ai = info.activityInfo ?: return@mapNotNull null
            // Use the launch activity's icon, not an intent resolver's icon override.
            AppEntry(info.loadLabel(pm).toString(), ai.packageName, ai.name, ai.loadIcon(pm))
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
        prefs.edit().putStringSet("favorites", set).commit()
        refresh()
    }

    fun folders(): List<AppFolder> = runCatching {
        val folders = JSONArray(prefs.getString("folders", "[]"))
        buildList {
            for (index in 0 until folders.length()) {
                val folder = folders.optJSONObject(index) ?: continue
                val id = folder.optString("id")
                val name = folder.optString("name")
                if (id.isBlank() || name.isBlank()) continue
                val appKeys = folder.optJSONArray("appKeys")?.let { keys ->
                    buildSet {
                        for (keyIndex in 0 until keys.length()) {
                            keys.optString(keyIndex).takeIf { it.isNotBlank() }?.let(::add)
                        }
                    }
                } ?: emptySet()
                add(AppFolder(id, name, appKeys))
            }
        }
    }.getOrDefault(emptyList())

    fun saveFolder(folder: AppFolder) {
        val updated = folders().filterNot { it.id == folder.id } + folder
        val json = JSONArray().apply {
            updated.forEach { item ->
                put(org.json.JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("appKeys", JSONArray(item.appKeys.toList()))
                })
            }
        }
        prefs.edit().putString("folders", json.toString()).commit()
        refresh()
    }

    fun deleteFolder(folderId: String) {
        val json = JSONArray().apply {
            folders().filterNot { it.id == folderId }.forEach { item ->
                put(org.json.JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("appKeys", JSONArray(item.appKeys.toList()))
                })
            }
        }
        prefs.edit().putString("folders", json.toString()).commit()
        refresh()
    }

    fun reorderFolders(orderedIds: List<String>) {
        val current = folders()
        val byId = current.associateBy { it.id }
        val reordered = buildList {
            orderedIds.forEach { id -> byId[id]?.let(::add) }
            current.forEach { folder ->
                if (folder.id !in orderedIds) add(folder)
            }
        }
        val json = JSONArray().apply {
            reordered.forEach { item ->
                put(org.json.JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("appKeys", JSONArray(item.appKeys.toList()))
                })
            }
        }
        prefs.edit().putString("folders", json.toString()).commit()
        refresh()
    }

    /**
     * Refreshes launcher surfaces after persisted settings have been changed.
     * The caller should persist first so widgets never observe stale settings.
     */
    fun refresh() {
        ClockWidgetProvider.refresh(context)
    }
}
