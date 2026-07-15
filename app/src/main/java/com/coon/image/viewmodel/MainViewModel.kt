package com.coon.image.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coon.image.data.ApiKeyStore
import com.coon.image.data.AiModel
import com.coon.image.data.CrashLog
import com.coon.image.data.DashScopeClient
import com.coon.image.data.EndpointKind
import com.coon.image.data.ModelCatalog
import com.coon.image.util.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val hasApiKey: Boolean = false,
    val capturedBitmap: Bitmap? = null,
    val capturedB64: String? = null,
    val selectedModel: AiModel = ModelCatalog.default,
    val keyword: String = "",
    val status: String = "",
    val isProcessing: Boolean = false,
    val resultBitmap: Bitmap? = null,
    val resultPath: String? = null,
    val error: String? = null
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init {
        _state.update { it.copy(hasApiKey = ApiKeyStore.getApiKey(app) != null) }
    }

    fun saveApiKey(key: String) {
        ApiKeyStore.saveApiKey(getApplication(), key)
        _state.update { it.copy(hasApiKey = true, error = null) }
    }

    fun onCaptured(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            val scaled = DashScopeClient.scaleToMax(bitmap, 1024)
            val b64 = DashScopeClient.bitmapToBase64(scaled)
            if (scaled !== bitmap) bitmap.recycle()
            _state.update {
                it.copy(
                    capturedBitmap = scaled,
                    capturedB64 = b64,
                    resultBitmap = null,
                    resultPath = null,
                    error = null
                )
            }
        }
    }

    fun setModel(m: AiModel) = _state.update { it.copy(selectedModel = m) }
    fun setKeyword(s: String) = _state.update { it.copy(keyword = s) }

    fun process() {
        val cur = _state.value
        val needsImage = cur.selectedModel.endpointKind != EndpointKind.TEXT_TO_IMAGE
        if (needsImage && cur.capturedB64 == null) {
            _state.update { it.copy(error = "请先拍照") }
            return
        }
        val apiKey = ApiKeyStore.getApiKey(getApplication())
        if (apiKey == null) {
            _state.update { it.copy(error = "缺少 API Key，请先在设置中输入") }
            return
        }
        _state.update {
            it.copy(isProcessing = true, error = null, status = "开始处理...", resultBitmap = null, resultPath = null)
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = DashScopeClient(apiKey)
                val url = client.process(cur.selectedModel, cur.keyword, cur.capturedB64) { log ->
                    _state.update { s -> s.copy(status = log) }
                }
                val bytes = client.download(url)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                val file = Storage.saveBytes(getApplication(), bytes, "coon_${System.currentTimeMillis()}.png")
                _state.update {
                    it.copy(
                        isProcessing = false,
                        resultBitmap = bmp,
                        resultPath = file.absolutePath,
                        status = "处理完成，已保存到 CoonImage"
                    )
                }
            } catch (e: Throwable) {
                Log.e("CoonImage", "process failed", e)
                CrashLog.write(getApplication(), "process", e)
                val msg = (e.message ?: e.javaClass.simpleName) + if (e is OutOfMemoryError) "（内存不足，请尝试更小的图片或关闭其他应用）" else ""
                _state.update { it.copy(isProcessing = false, error = msg) }
            }
        }
    }

    fun reset() {
        _state.update {
            it.copy(
                capturedBitmap = null,
                capturedB64 = null,
                resultBitmap = null,
                resultPath = null,
                keyword = "",
                status = "",
                error = null
            )
        }
    }
}
