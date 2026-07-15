package com.coon.image.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * 应用仅需相机权限；图片保存在应用内部存储（getExternalFilesDir），无需任何存储权限。
 */
object Permissions {
    fun cameraGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
}
