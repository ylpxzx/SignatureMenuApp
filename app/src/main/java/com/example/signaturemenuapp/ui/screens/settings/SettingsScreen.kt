package com.example.signaturemenuapp.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.signaturemenuapp.data.AppProfile
import com.example.signaturemenuapp.data.SignatureMenuData
import com.example.signaturemenuapp.data.SignatureMenuStore
import java.time.LocalDate

@Composable
internal fun SettingsScreen(
    data: SignatureMenuData,
    store: SignatureMenuStore,
    contentPadding: PaddingValues,
    onUpdateProfile: (AppProfile) -> Unit,
    onImport: (SignatureMenuData) -> Unit,
) {
    val context = LocalContext.current
    var notice by remember { mutableStateOf("") }
    var noticeTone by remember { mutableStateOf("info") }
    var editTarget by remember { mutableStateOf<String?>(null) }
    var editDraft by remember { mutableStateOf("") }
    var pendingImport by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(store.exportJson(data).toByteArray(Charsets.UTF_8))
            } ?: error("无法写入导出文件。")
        }.onSuccess {
            notice = "已导出 ${data.recipes.size} 道菜谱、${data.menus.size} 份菜单。"
            noticeTone = "success"
        }.onFailure {
            notice = it.message ?: "导出失败。"
            noticeTone = "error"
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: error("无法读取导入文件。")
        }.onSuccess {
            pendingImport = it
        }.onFailure {
            notice = it.message ?: "导入文件读取失败。"
            noticeTone = "error"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(
            top = 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SettingsRow("昵称", data.profile.displayName, "人") {
                editTarget = "昵称"
                editDraft = data.profile.displayName
            }
            SettingsRow("用户名", data.profile.username, "证") {
                editTarget = "用户名"
                editDraft = data.profile.username
            }
        }
        item {
            SectionTitle("数据")
            Spacer(Modifier.height(8.dp))
            SettingsRow("导出数据", "导出菜谱、菜单与记录", "↑") {
                exportLauncher.launch("signature-menu-export-${LocalDate.now()}.json")
            }
            SettingsRow("导入数据", "仅选择 JSON 文件，追加恢复", "↓") {
                importLauncher.launch(arrayOf("application/json", "text/json"))
            }
        }
        if (notice.isNotBlank()) {
            item { Notice(notice, noticeTone) }
        }
    }

    editTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { editTarget = null },
            title = { Text("修改$target") },
            text = {
                OutlinedTextField(
                    value = editDraft,
                    onValueChange = { editDraft = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val value = editDraft.trim()
                        if (value.isBlank()) return@TextButton
                        if (target == "用户名" && value.length < 3) {
                            Toast.makeText(context, "用户名至少 3 个字符。", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        onUpdateProfile(
                            if (target == "昵称") data.profile.copy(displayName = value.take(24))
                            else data.profile.copy(username = value.take(32)),
                        )
                        editTarget = null
                    },
                ) { Text("保存", color = Forest, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { editTarget = null }) { Text("取消") }
            },
        )
    }

    pendingImport?.let { json ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("导入数据") },
            text = { Text("导入将追加恢复数据，系统已存在的数据不做覆盖。是否继续导入？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        runCatching {
                            store.appendImport(data, json)
                        }.onSuccess { (nextData, result) ->
                            onImport(nextData)
                            notice = "已导入 ${result.recipes} 道菜谱、${result.menus} 份菜单。"
                            noticeTone = "success"
                        }.onFailure {
                            notice = it.message ?: "导入失败。"
                            noticeTone = "error"
                        }
                        pendingImport = null
                    },
                ) { Text("继续导入", color = Forest, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) { Text("取消") }
            },
        )
    }
}
