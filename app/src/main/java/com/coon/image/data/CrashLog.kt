package com.coon.image.data

import android.content.Context
import android.os.Build
import com.coon.image.util.Storage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 把崩溃/异常的堆栈写入 /sdcard/CoonImage/crash.log，便于排查。
 * 任何异常都不应阻断主流程，写文件失败则静默忽略。
 */
object CrashLog {
    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun write(context: Context, stage: String, t: Throwable) {
        try {
            val sb = StringBuilder()
            sb.append("时间: ${fmt.format(Date())}\n")
            sb.append("阶段: $stage\n")
            sb.append("设备: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.SDK_INT})\n")
            sb.append("异常: ${t.javaClass.name}: ${t.message}\n")
            sb.append("堆栈:\n")
            t.stackTraceToString().lineSequence().take(40).forEach { sb.append("  ").append(it).append('\n') }
            sb.append("\n----- 以上为最近一次崩溃，下面是更早的记录 -----\n\n")
            val dir = Storage.getCoonDir(context)
            val file = File(dir, "crash.log")
            val old = if (file.exists()) file.readText() else ""
            file.writeText(sb.toString() + old)
        } catch (_: Throwable) {
            // 写日志本身失败也绝不抛异常
        }
    }
}
