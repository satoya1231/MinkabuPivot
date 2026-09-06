package com.satoya.novalauncher

import android.graphics.drawable.Drawable

data class AppEntry(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: Drawable
) {
    val key: String get() = "$packageName/$activityName"
}
