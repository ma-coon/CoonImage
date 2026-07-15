package com.coon.image.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.OutputStream

/**
 * 把图片写入系统图库（MediaStore），无需「所有文件访问」权限，且在相册中可见、卸载后保留。
 *  Android 10+：通过 MediaStore 写入 Pictures/CoonImage，不需要任何权限。
 *  Android 9 及以下：回退到公共 Pictures/CoonImage（需 WRITE_EXTERNAL_STORAGE，已在 Manifest 声明）。
 * 返回可在界面展示的位置描述（Uri 字符串或文件路径）。失败时抛出，由调用方回退到内部存储。
 */
object MediaStoreSaver {

    private const val ALBUM = "CoonImage"

    fun saveBytes(context: Context, bytes: ByteArray, fileName: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$ALBUM")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("MediaStore 插入失败（系统拒绝）")
            resolver.openOutputStream(uri).use { out: OutputStream? ->
                out ?: throw IllegalStateException("无法打开媒体输出流")
                out.write(bytes)
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri.toString()
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                ALBUM
            )
            if (!dir.exists() && !dir.mkdirs()) {
                throw IllegalStateException("无法创建公共目录 ${dir.absolutePath}")
            }
            val file = File(dir, fileName)
            file.outputStream().use { it.write(bytes) }
            // 让图库扫描到该文件
            val intent = android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            intent.data = android.net.Uri.fromFile(file)
            context.sendBroadcast(intent)
            return file.absolutePath
        }
    }

    fun saveBitmap(context: Context, bitmap: Bitmap, fileName: String): String {
        val bytes = java.io.ByteArrayOutputStream().also {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }.toByteArray()
        return saveBytes(context, bytes, fileName)
    }
}
