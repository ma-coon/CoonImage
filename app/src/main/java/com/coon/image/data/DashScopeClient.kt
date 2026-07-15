package com.coon.image.data

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 封装对阿里云百炼 / DashScope 的调用：
 *  - 通义万相·图像编辑 (wanx-x-painting)：图+指令 -> 异步任务
 *  - 通义万相·文生图 (wanx2.1-t2i-plus)：文字 -> 异步任务
 *  - 通义千问 (qwen-plus)：把中文关键字理解成编辑指令（智能路由）
 * 图像类接口均为异步任务，提交后轮询 task 状态直到成功。
 */
class DashScopeClient(private val apiKey: String) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    private val imageEditUrl = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/image-synthesis"
    private val text2ImageUrl = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text2image/image-synthesis"
    private val taskUrl = "https://dashscope.aliyuncs.com/api/v1/tasks/"
    private val qwenUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"

    private val jsonMedia = "application/json".toMediaType()

    suspend fun process(
        model: AiModel,
        keyword: String,
        baseImageB64: String?,
        onLog: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        when (model.endpointKind) {
            EndpointKind.TEXT_TO_IMAGE -> {
                onLog("调用文生图模型 ${model.id}")
                submitAndWait(text2ImageUrl, model.id, buildPrompt(keyword), null, onLog)
            }
            EndpointKind.QWEN_ROUTE -> {
                onLog("通义千问理解关键字中...")
                val refined = refineWithQwen(keyword, onLog)
                onLog("通义千问生成指令：$refined")
                onLog("调用图像编辑模型...")
                submitAndWait(imageEditUrl, "wanx-x-painting", refined, baseImageB64, onLog)
            }
            EndpointKind.IMAGE_EDIT -> {
                onLog("调用图像编辑模型 ${model.id}")
                submitAndWait(imageEditUrl, model.id, buildPrompt(keyword), baseImageB64, onLog)
            }
        }
    }

    suspend fun download(url: String): ByteArray = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url).build()
        val resp = http.newCall(req).execute()
        if (!resp.isSuccessful) throw IOException("下载结果失败: HTTP ${resp.code}")
        resp.body?.bytes() ?: throw IOException("下载结果为空")
    }

    private suspend fun refineWithQwen(keyword: String, onLog: (String) -> Unit): String {
        val body = JSONObject().apply {
            put("model", "qwen-plus")
            put("temperature", 0.3)
            put("messages", JSONArray().apply {
                put(
                    JSONObject().put("role", "system").put(
                        "content",
                        "你是图像编辑助手。用户会给出中文的修图意图，请把它改写成一段简洁、准确、" +
                                "可直接用于 AI 图像编辑的中文指令（只输出指令本身，不要解释，不要结尾标点）。"
                    )
                )
                put(JSONObject().put("role", "user").put("content", keyword))
            })
        }
        val req = Request.Builder().url(qwenUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body.toString().toRequestBody(jsonMedia))
            .build()
        val resp = http.newCall(req).execute()
        val text = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) throw IOException("通义千问调用失败: $text")
        val json = JSONObject(text)
        return json.getJSONArray("choices").getJSONObject(0)
            .getJSONObject("message").getString("content").trim()
    }

    private suspend fun submitAndWait(
        url: String,
        model: String,
        prompt: String,
        baseImageB64: String?,
        onLog: (String) -> Unit
    ): String {
        val input = JSONObject().apply {
            put("prompt", prompt)
            if (!baseImageB64.isNullOrBlank()) {
                put("base_image_url", "data:image/jpeg;base64,$baseImageB64")
            }
        }
        val body = JSONObject().apply {
            put("model", model)
            put("input", input)
            put(
                "parameters",
                JSONObject().apply {
                    put("n", 1)
                    put("size", "1024*1024")
                    put("prompt_extend", true)
                }
            )
        }
        val req = Request.Builder().url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("X-DashScope-Async", "enable")
            .post(body.toString().toRequestBody(jsonMedia))
            .build()
        val resp = http.newCall(req).execute()
        val text = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) throw IOException("提交任务失败($model): $text")
        val taskId = JSONObject(text).getJSONObject("output").getString("task_id")
        onLog("任务已提交，task_id=$taskId，等待处理...")
        return pollTask(taskId, onLog)
    }

    private suspend fun pollTask(taskId: String, onLog: (String) -> Unit): String {
        var attempts = 0
        while (attempts < 60) {
            attempts++
            val req = Request.Builder().url(taskUrl + taskId)
                .addHeader("Authorization", "Bearer $apiKey")
                .build()
            val resp = http.newCall(req).execute()
            val json = JSONObject(resp.body?.string().orEmpty())
            val status = json.optJSONObject("output")?.optString("task_status", "PENDING") ?: "PENDING"
            when (status) {
                "SUCCEEDED" -> {
                    val results = json.getJSONObject("output").optJSONArray("results")
                    if (results != null && results.length() > 0) {
                        return results.getJSONObject(0).getString("url")
                    }
                    throw IOException("任务成功但未返回图像")
                }
                "FAILED", "UNKNOWN" -> {
                    val msg = json.getJSONObject("output").optString("message", "未知错误")
                    throw IOException("图像处理失败: $msg")
                }
                else -> {
                    onLog("处理中... ($attempts)")
                    delay(3000)
                }
            }
        }
        throw IOException("等待超时，请稍后在 DashScope 控制台查看任务 $taskId")
    }

    companion object {
        /** 把图片等比缩放到最长边不超过 maxEdge，降低 base64 体积与内存压力。 */
        fun scaleToMax(bitmap: Bitmap, maxEdge: Int): Bitmap {
            val w = bitmap.width
            val h = bitmap.height
            val longest = maxOf(w, h)
            if (longest <= maxEdge) return bitmap
            val ratio = maxEdge.toFloat() / longest
            val tw = (w * ratio).toInt().coerceAtLeast(1)
            val th = (h * ratio).toInt().coerceAtLeast(1)
            return try {
                Bitmap.createScaledBitmap(bitmap, tw, th, true)
            } catch (_: Throwable) {
                bitmap
            }
        }

        fun bitmapToBase64(bitmap: Bitmap): String {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        }
    }
}

/**
 * 把用户的关键字映射成 DashScope 能理解的编辑指令。
 * 命中已知意图时使用精心撰写的指令，否则原样透传作为指令。
 */
fun buildPrompt(keyword: String): String {
    val k = keyword.trim()
    return when {
        k.contains("天空") ->
            "将照片中的天空替换为晴朗明媚的蓝天和蓬松白云，保持地面、建筑与人物不变，光影自然协调。"
        k.contains("换装") || k.contains("衣服") || k.contains("服装") ->
            "将人物的服装替换为：$k，保持人物面部特征和姿态不变，材质与光影真实自然。"
        k.contains("抠图") || k.contains("去背景") || k.contains("扣图") ->
            "将照片主体从背景中精确抠出，背景替换为纯白色，边缘平滑自然。"
        k.contains("去人") || k.contains("去除") || k.contains("路人") || k.contains("无关人员") ->
            "移除照片中除主要人物以外的路人和其他无关人物，并自然、无缝地补全背景。"
        else -> k
    }
}
