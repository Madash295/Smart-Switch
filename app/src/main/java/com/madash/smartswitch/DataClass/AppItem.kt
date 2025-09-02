package com.madash.smartswitch.DataClass

import android.graphics.drawable.Drawable

data class AppItem(
    val packageName: String,
    val name: String,
    val icon: Drawable?,
    val size: String,
    val apkPath: String,
    val isSystemApp: Boolean
)