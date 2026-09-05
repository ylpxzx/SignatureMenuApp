package com.example.signaturemenuapp.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.signaturemenuapp.R
import com.example.signaturemenuapp.data.SignatureMenuData
import com.example.signaturemenuapp.data.SignatureMenuStore
import java.time.LocalDate

@Composable
internal fun SettingsScreen(
    data: SignatureMenuData,
    store: SignatureMenuStore,
    contentPadding: PaddingValues,
    onImport: (SignatureMenuData) -> Unit,
) {
    val context = LocalContext.current
    var notice by remember { mutableStateOf("") }
    var noticeTone by remember { mutableStateOf("info") }
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
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(
            top = 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item {
            Text(
                text = "数据",
                color = Color(0xFF101512),
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 21.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
            )
            SettingsGroup {
                SettingsListRow(
                    title = "导出数据",
                    subtitle = "导出菜谱、菜单与记录",
                    iconRes = R.drawable.ic_settings_cloud_upload,
                    onClick = { exportLauncher.launch("signature-menu-export-${LocalDate.now()}.json") },
                )
                SettingsDivider()
                SettingsListRow(
                    title = "导入数据",
                    subtitle = "从备份文件恢复数据",
                    iconRes = R.drawable.ic_settings_download,
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/json")) },
                )
            }
        }

        if (notice.isNotBlank()) {
            item {
                SettingsNotice(notice, noticeTone)
            }
        }
    }

    pendingImport?.let { json ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("导入数据") },
            text = { Text("导入将追加恢复数据，系统已存在的数据不做覆盖。是否继续导入？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingImport = null
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
                    },
                ) { Text("继续导入", color = Forest, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Paper.copy(alpha = 0.76f))
            .border(1.2.dp, Line.copy(alpha = 0.92f), shape),
        content = content,
    )
}

@Composable
private fun SettingsListRow(
    title: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    value: String? = null,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (subtitle == null) 64.dp else 72.dp)
            .clickable(onClick = onClick)
            .padding(start = 11.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconTile(iconRes = iconRes, tint = Forest)
        Spacer(Modifier.width(10.dp))
        if (subtitle == null) {
            Text(
                text = title,
                color = Color(0xFF121713),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 21.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = value.orEmpty(),
                color = Color(0xFF565953),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        } else {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color(0xFF121713),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 21.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    color = Color(0xFF676A65),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        SettingsChevron(color = Forest)
    }
}

@Composable
private fun SettingsIconTile(@DrawableRes iconRes: Int, tint: Color) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (tint == Forest) Color(0xFFEEF7E8) else Color(0xFFFFF1E2))
            .border(
                1.dp,
                if (tint == Forest) Color(0xFFD2DFCA) else Color(0xFFF47B17).copy(alpha = 0.24f),
                RoundedCornerShape(10.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(23.dp),
        )
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Line.copy(alpha = 0.78f)),
    )
}

@Composable
private fun SettingsChevron(color: Color) {
    Text(
        text = "›",
        color = color,
        fontSize = 34.sp,
        fontWeight = FontWeight.Light,
        lineHeight = 34.sp,
        modifier = Modifier.padding(start = 8.dp),
    )
}

@Composable
private fun SettingsNotice(text: String, tone: String) {
    val error = tone == "error"
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 17.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (error) Color(0xFFFFF0EF) else Mint.copy(alpha = 0.70f))
            .border(1.2.dp, if (error) Tomato.copy(alpha = 0.34f) else Leaf.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Text(
            text = text,
            color = if (error) Tomato else Forest,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 18.sp,
        )
    }
}
