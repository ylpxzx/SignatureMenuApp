@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.signaturemenuapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.signaturemenuapp.R
import com.example.signaturemenuapp.data.MenuDish
import com.example.signaturemenuapp.data.MenuRecord
import com.example.signaturemenuapp.data.MenuStatus
import com.example.signaturemenuapp.data.SignatureMenuData
import com.example.signaturemenuapp.data.newId
import com.example.signaturemenuapp.data.nowIso
import java.time.LocalDate

@Composable
internal fun MenuBuilderEntryScreen(
    data: SignatureMenuData,
    contentPadding: PaddingValues,
    onSaveMenu: (MenuRecord) -> Unit,
) {
    var keyword by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("周六晚餐菜单") }
    var note by remember { mutableStateOf("忌口一起说") }
    var selectedIds by remember(data.recipes) { mutableStateOf(data.recipes.take(2).map { it.id }.toSet()) }

    val recipes = remember(data.recipes, keyword) {
        val query = keyword.trim()
        data.recipes.filter { recipe ->
            recipe.isAvailable && (
                query.isBlank() || listOf(recipe.name, recipe.cookingMethod, recipe.tasteTags.joinToString(" "))
                    .joinToString(" ")
                    .contains(query, ignoreCase = true)
                )
        }
    }
    val selectedRecipes = data.recipes.filter { selectedIds.contains(it.id) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(
            top = 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 20.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Image(
                painter = painterResource(R.drawable.menu_page_header),
                contentDescription = "挑几道给朋友选",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
            )
        }
        item {
            SearchField(value = keyword, onValueChange = { keyword = it }, placeholder = "搜菜名、口味、做法")
        }
        item {
            CardShell {
                FormField("菜单标题", title, { title = it }, "周六晚餐菜单")
                FormField("菜单备注", note, { note = it }, "忌口一起说")
            }
        }
        item {
            SectionTitle("可选菜谱", caption = "已选 ${selectedRecipes.size} 道")
        }
        if (recipes.isEmpty()) {
            item { EmptyBox("没有匹配的菜谱。") }
        } else {
            items(recipes, key = { it.id }) { recipe ->
                SelectRecipeRow(
                    recipe = recipe,
                    selected = selectedIds.contains(recipe.id),
                    onToggle = {
                        selectedIds = if (selectedIds.contains(recipe.id)) selectedIds - recipe.id else selectedIds + recipe.id
                    },
                )
            }
        }
        item {
            Button(
                onClick = {
                    val orderedIds = data.recipes.map { it.id }.filter { selectedIds.contains(it) }
                    val dishes = data.recipes
                        .filter { orderedIds.contains(it.id) }
                        .map { MenuDish(recipeId = it.id, name = it.name, count = 1) }
                    onSaveMenu(
                        MenuRecord(
                            id = newId(),
                            title = title.trim().ifBlank { "周六晚餐菜单" },
                            note = note.trim().ifBlank { "忌口一起说" },
                            dateKey = LocalDate.now().toString(),
                            time = "18:30",
                            status = MenuStatus.Pending,
                            dinerCount = 4,
                            recipeIds = orderedIds,
                            dishes = dishes,
                            createdAt = nowIso(),
                            updatedAt = nowIso(),
                        ),
                    )
                },
                enabled = selectedRecipes.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Forest, contentColor = Paper),
                shape = RoundedCornerShape(15.dp),
            ) {
                Text("保存菜单", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun MenuEditorScreen(
    data: SignatureMenuData,
    menu: MenuRecord?,
    onBack: () -> Unit,
    onSave: (MenuRecord) -> Unit,
) {
    var title by remember(menu?.id) { mutableStateOf(menu?.title ?: "今晚家常菜单") }
    var note by remember(menu?.id) { mutableStateOf(menu?.note ?: "忌口一起说") }
    var dateKey by remember(menu?.id) { mutableStateOf(menu?.dateKey ?: LocalDate.now().toString()) }
    var time by remember(menu?.id) { mutableStateOf(menu?.time ?: "18:30") }
    var dinerCount by remember(menu?.id) { mutableStateOf((menu?.dinerCount ?: 4).toString()) }
    var status by remember(menu?.id) { mutableStateOf(menu?.status ?: MenuStatus.Pending) }
    var keyword by remember { mutableStateOf("") }
    var selectedIds by remember(menu?.id) { mutableStateOf(menu?.recipeIds?.toSet() ?: data.recipes.take(2).map { it.id }.toSet()) }
    var error by remember { mutableStateOf("") }

    val recipes = remember(data.recipes, keyword) {
        data.recipes.filter { recipe ->
            recipe.isAvailable && (
                keyword.isBlank() || listOf(recipe.name, recipe.cookingMethod, recipe.tasteTags.joinToString(" "))
                    .joinToString(" ")
                    .contains(keyword, ignoreCase = true)
                )
        }
    }
    val selectedRecipes = data.recipes.filter { selectedIds.contains(it.id) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        BackBar(title = if (menu == null) "新增菜单" else "编辑菜单", onBack = onBack)
        Image(
            painter = painterResource(R.drawable.menu_edit_header),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.FillWidth,
        )

        CardShell {
            FormField("菜单名", title, { title = it }, "今晚家常菜单")
            FormField("备注", note, { note = it }, "忌口、人数、想吃什么")
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                FormField("出餐日期", dateKey, { dateKey = it }, "2026-07-02", Modifier.weight(1f))
                FormField("时间", time, { time = it }, "18:30", Modifier.weight(0.72f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                FormField("人数", dinerCount, { dinerCount = it.onlyDigits() }, "4", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                SmallChip(
                    text = if (status == MenuStatus.Pending) "待出餐" else "已出餐",
                    selected = status == MenuStatus.Served,
                    modifier = Modifier.weight(1f),
                    onClick = { status = if (status == MenuStatus.Pending) MenuStatus.Served else MenuStatus.Pending },
                )
            }
        }

        SectionTitle("加入菜谱", caption = "已选 ${selectedRecipes.size} 道")
        if (selectedRecipes.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                selectedRecipes.forEach { recipe ->
                    SmallChip(
                        text = recipe.name,
                        selected = true,
                        onClick = { selectedIds = selectedIds - recipe.id },
                    )
                }
            }
        }
        SearchField(value = keyword, onValueChange = { keyword = it }, placeholder = "搜菜名、做法、口味")
        recipes.forEach { recipe ->
            SelectRecipeRow(
                recipe = recipe,
                selected = selectedIds.contains(recipe.id),
                onToggle = {
                    selectedIds = if (selectedIds.contains(recipe.id)) selectedIds - recipe.id else selectedIds + recipe.id
                },
            )
        }
        if (recipes.isEmpty()) {
            EmptyBox("没有匹配的可用菜谱。")
        }
        if (error.isNotBlank()) {
            Notice(error, tone = "error")
        }
        Button(
            onClick = {
                if (selectedIds.isEmpty()) {
                    error = "至少选择一道菜。"
                    return@Button
                }
                val normalizedDate = runCatching { LocalDate.parse(dateKey).toString() }.getOrElse {
                    error = "出餐日期格式应为 yyyy-MM-dd。"
                    return@Button
                }
                val orderedIds = data.recipes.map { it.id }.filter { selectedIds.contains(it) }
                val dishes = data.recipes
                    .filter { orderedIds.contains(it.id) }
                    .map { MenuDish(recipeId = it.id, name = it.name, count = 1) }
                onSave(
                    (menu ?: MenuRecord(id = newId(), createdAt = nowIso())).copy(
                        title = title.trim().ifBlank { "家常菜单" },
                        note = note.trim(),
                        dateKey = normalizedDate,
                        time = if (Regex("""^\d{2}:\d{2}$""").matches(time.trim())) time.trim() else "18:30",
                        status = status,
                        dinerCount = dinerCount.toIntOrNull()?.coerceIn(1, 99) ?: 4,
                        recipeIds = orderedIds,
                        dishes = dishes,
                        updatedAt = nowIso(),
                    ),
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Forest, contentColor = Paper),
            shape = RoundedCornerShape(15.dp),
        ) {
            Text("保存菜单", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
internal fun MenuArchiveScreen(
    data: SignatureMenuData,
    contentPadding: PaddingValues,
    onEdit: (String) -> Unit,
    onCreate: () -> Unit,
    onChangeStatus: (String, MenuStatus) -> Unit,
    onDelete: (String) -> Unit,
) {
    var keyword by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("待出餐") }
    var deletingMenu by remember { mutableStateOf<MenuRecord?>(null) }
    val menus = remember(data.menus, filter, keyword) {
        val query = keyword.trim()
        data.menus
            .filter {
                (filter == "待出餐" && it.status == MenuStatus.Pending) || (filter == "已出餐" && it.status == MenuStatus.Served)
            }
            .filter { menu ->
                query.isBlank() || listOf(
                    menu.title,
                    menu.note,
                    menu.dishes.joinToString(" ") { it.name },
                ).joinToString(" ").contains(query, ignoreCase = true)
            }
            .sortedWith(compareByDescending<MenuRecord> { it.dateKey }.thenByDescending { it.time })
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(
                top = 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 90.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SearchField(value = keyword, onValueChange = { keyword = it }, placeholder = "搜菜名、菜单名")
            }
            item {
                SecondaryAction("新增菜单", "记一顿要出的菜", onCreate)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("待出餐", "已出餐").forEach { option ->
                        SmallChip(
                            text = option,
                            selected = filter == option,
                            modifier = Modifier.weight(1f),
                            onClick = { filter = option },
                        )
                    }
                }
            }
            if (menus.isEmpty()) {
                item { EmptyBox("还没有菜单记录。") }
            } else {
                items(menus, key = { it.id }) { menu ->
                    MenuCard(
                        menu = menu,
                        recipes = data.recipes,
                        actions = {
                            TextButton(onClick = { onEdit(menu.id) }) {
                                Text("编辑", color = Forest, fontWeight = FontWeight.Bold)
                            }
                            TextButton(
                                onClick = {
                                    onChangeStatus(
                                        menu.id,
                                        if (menu.status == MenuStatus.Pending) MenuStatus.Served else MenuStatus.Pending,
                                    )
                                },
                            ) {
                                Text(
                                    if (menu.status == MenuStatus.Pending) "已出餐" else "待出餐",
                                    color = Forest,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            TextButton(onClick = { deletingMenu = menu }) {
                                Text("删除", color = Tomato, fontWeight = FontWeight.Bold)
                            }
                        },
                    )
                }
            }
        }
        FloatingActionButton(
            onClick = onCreate,
            containerColor = Forest,
            contentColor = Paper,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 22.dp, bottom = contentPadding.calculateBottomPadding() + 20.dp),
        ) {
            Text("+", fontSize = 32.sp, fontWeight = FontWeight.Medium)
        }
    }

    deletingMenu?.let { menu ->
        AlertDialog(
            onDismissRequest = { deletingMenu = null },
            title = { Text("删除菜单") },
            text = { Text("确定删除「${menu.title}」吗？如果它已出餐，相关菜谱做过次数会同步减回去。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deletingMenu = null
                        onDelete(menu.id)
                    },
                ) { Text("删除", color = Tomato) }
            },
            dismissButton = {
                TextButton(onClick = { deletingMenu = null }) { Text("取消") }
            },
        )
    }
}
