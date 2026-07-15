package com.coon.image.util

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 把处理好的图片保存到应用内部存储目录下的 CoonImage 文件夹：
 *   …/Android/data/com.coon.image/files/CoonImage/
 * 该目录属于本应用私有空间，从 Android 4.4 起**无需任何存储权限**即可读写，
 * 也就不会再弹出「所有文件访问」授权页，也不会因权限问题崩溃。
 * 用户可在手机「文件管理」中进入 Android/data/com.coon.image/files/CoonImage 找到文件。
 */
object Storage {
    private const val DIR_NAME = "CoonImage"

    fun getCoonDir(context: Context): File {
        val root = context.getExternalFilesDir(null)
            ?: context.filesDir
        val dir = File(root, DIR_NAME)
        if (!dir.exists() && !dir.mkdirs()) {
            throw IOException("无法创建目录 ${dir.absolutePath}")
        }
        return dir
    }

    fun saveBytes(context: Context, bytes: ByteArray, fileName: String): File {
        val dir = getCoonDir(context)
        val file = File(dir, fileName)
        try {
            FileOutputStream(file).use { out -> out.write(bytes) }
        } catch (e: Exception) {
            throw IOException("保存失败: ${e.message}", e)
        }
        return file
    }

    fun saveBitmap(context: Context, bitmap: Bitmap, fileName: String): File {
        val dir = getCoonDir(context)
        val file = File(dir, fileName)
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: Exception) {
            throw IOException("保存失败: ${e.message}", e)
        }
        return file
    }
}
