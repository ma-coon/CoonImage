package com.coon.image.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.coon.image.data.AiModel
import com.coon.image.data.ModelCatalog
import com.coon.image.util.Permissions
import com.coon.image.viewmodel.MainViewModel
import com.coon.image.viewmodel.UiState

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var cameraGranted by remember { mutableStateOf(Permissions.cameraGranted(context)) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        cameraGranted = it
    }

    CoonTheme {
        when {
            !state.hasApiKey ->
                ApiKeyScreen(onSave = viewModel::saveApiKey)

            state.capturedBitmap == null ->
                CameraCaptureScreen(
                    hasPermission = cameraGranted,
                    onNeedPermission = { cameraLauncher.launch(Manifest.permission.CAMERA) },
                    onCaptured = viewModel::onCaptured
                )

            else ->
                EditScreen(
                    state = state,
                    onKeywordChange = viewModel::setKeyword,
                    onModelChange = viewModel::setModel,
                    onProcess = { viewModel.process() },
                    onRetake = viewModel::reset
                )
        }
    }
}

@Composable
fun ApiKeyScreen(onSave: (String) -> Unit) {
    var key by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("欢迎使用 CoonImage", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "请输入阿里云百炼 / DashScope 的 API Key（仅保存在本机加密存储中，输入一次即可）。",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            label = { Text("DashScope API Key") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(enabled = key.isNotBlank(), onClick = { onSave(key) }, modifier = Modifier.fillMaxWidth()) {
            Text("保存并开始")
        }
    }
}

@Composable
fun EditScreen(
    state: UiState,
    onKeywordChange: (String) -> Unit,
    onModelChange: (AiModel) -> Unit,
    onProcess: () -> Unit,
    onRetake: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        state.capturedBitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(220.dp)
            )
        }
        OutlinedTextField(
            value = state.keyword,
            onValueChange = onKeywordChange,
            label = { Text("输入关键字，如：换天空 / 抠图 / 去除无关人员 / 换装为红裙") },
            modifier = Modifier.fillMaxWidth()
        )
        ModelDropdown(selected = state.selectedModel, onSelect = onModelChange)
        Text(
            "提示：输入「换天空 / 换背景 / 抠图」会由 AI 自动分割主体并生成新背景（更精准）；" +
                    "「换装 / 去除无关人员」等仍走通用图像编辑。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        Button(onClick = onProcess, enabled = !state.isProcessing, modifier = Modifier.fillMaxWidth()) {
            if (state.isProcessing) CircularProgressIndicator(Modifier.size(18.dp)) else Text("开始 AI 处理")
        }
        if (state.status.isNotEmpty()) Text(state.status, style = MaterialTheme.typography.bodyMedium)
        if (state.error != null) Text("错误：${state.error}", color = Color.Red)
        state.resultBitmap?.let { rb ->
            Text("处理结果：", style = MaterialTheme.typography.titleMedium)
            Image(bitmap = rb.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxWidth().height(260.dp))
            state.resultPath?.let { p -> Text("已保存到系统图库「CoonImage」相册：\n$p", style = MaterialTheme.typography.bodySmall) }
        }
        OutlinedButton(onClick = onRetake, modifier = Modifier.fillMaxWidth()) { Text("重新拍照") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelDropdown(selected: AiModel, onSelect: (AiModel) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("选择大模型") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ModelCatalog.models.forEach { m ->
                DropdownMenuItem(
                    text = { Text("${m.displayName}\n${m.description}") },
                    onClick = { onSelect(m); expanded = false }
                )
            }
        }
    }
}
