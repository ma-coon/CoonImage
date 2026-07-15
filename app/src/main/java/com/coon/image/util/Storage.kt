package com.coon.image.util

import android.graphics.Bitmap
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 把处理好的图片保存到手机存储根目录下的 CoonImage 文件夹（/sdcard/CoonImage）。
 * 在 Android 11+ 上需要「所有文件访问」权限（MANAGE_EXTERNAL_STORAGE）才能直接写根目录。
 */
object Storage {
    private const val DIR_NAME = "CoonImage"

    fun getCoonDir(): File {
        val root = Environment.getExternalStorageDirectory()
        val dir = File(root, DIR_NAME)
        if (!dir.exists() && !dir.mkdirs()) {
            throw IOException("无法创建目录 ${dir.absolutePath}（请确认已授予「所有文件访问」权限）")
        }
        return dir
    }

    fun saveBytes(bytes: ByteArray, fileName: String): File {
        val dir = getCoonDir()
        val file = File(dir, fileName)
        try {
            FileOutputStream(file).use { out -> out.write(bytes) }
        } catch (e: Exception) {
            throw IOException("保存失败: ${e.message}", e)
        }
        return file
    }

    fun saveBitmap(bitmap: Bitmap, fileName: String): File {
        val dir = getCoonDir()
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
