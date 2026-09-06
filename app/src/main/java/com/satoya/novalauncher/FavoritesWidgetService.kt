package com.satoya.novalauncher

import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.graphics.drawable.toBitmap

class FavoritesWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        FavoritesFactory(applicationContext)

    private class FavoritesFactory(
        private val context: android.content.Context
    ) : RemoteViewsFactory {
        private var apps: List<AppEntry> = emptyList()

        override fun onCreate() = reload()

        override fun onDataSetChanged() = reload()

        override fun onDestroy() {
            apps = emptyList()
        }

        override fun getCount(): Int = apps.size

        override fun getViewAt(position: Int): RemoteViews? {
            val app = apps.getOrNull(position) ?: return null
            return RemoteViews(context.packageName, R.layout.widget_favorite_item).apply {
                setImageViewBitmap(R.id.widget_favorite_item_icon, app.icon.toBitmap(64, 64))
                setTextViewText(R.id.widget_favorite_item_label, app.label)
                setOnClickFillInIntent(
                    R.id.widget_favorite_item,
                    Intent().apply {
                        putExtra("favorite_package_name", app.packageName)
                        putExtra("favorite_activity_name", app.activityName)
                    }
                )
            }
        }

        override fun getLoadingView(): RemoteViews? = null

        override fun getViewTypeCount(): Int = 1

        override fun getItemId(position: Int): Long = position.toLong()

        override fun hasStableIds(): Boolean = true

        private fun reload() {
            val repository = LauncherRepository(context)
            val appsByKey = repository.loadApps().associateBy { it.key }
            val favoriteFolder = repository.folders()
                .firstOrNull { it.name == "お気に入り" }
            apps = favoriteFolder?.appKeys
                ?.mapNotNull { appsByKey[it] }
                .orEmpty()
                .distinctBy { it.key }
        }
    }
}
