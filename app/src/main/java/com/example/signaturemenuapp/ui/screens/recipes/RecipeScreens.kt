@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.signaturemenuapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.signaturemenuapp.R
import com.example.signaturemenuapp.data.Recipe
import com.example.signaturemenuapp.data.SignatureMenuData
import com.example.signaturemenuapp.data.newId
import com.example.signaturemenuapp.data.nowIso

@Composable
internal fun RecipeListScreen(
    data: SignatureMenuData,
    contentPadding: PaddingValues,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
) {
    var keyword by remember { mutableStateOf("") }
    var sortKey by remember { mutableStateOf("难度") }
    val filtered = remember(data.recipes, keyword, sortKey) {
        val query = keyword.trim()
        data.recipes
            .filter { recipe ->
                query.isBlank() || listOf(
                    recipe.name,
                    recipe.description,
                    recipe.cookingMethod,
                    recipe.tasteTags.joinToString(" "),
                ).joinToString(" ").contains(query, ignoreCase = true)
            }
            .sortedWith(
                when (sortKey) {
                    "难度" -> compareByDescending<Recipe> { it.difficulty }.thenBy { it.name }
                    "熟练度" -> compareByDescending<Recipe> { it.proficiency }.thenBy { it.name }
                    "步骤数" -> compareBy<Recipe> { it.steps.size }.thenBy { it.name }
                    "人数" -> compareBy<Recipe> { it.servingCount }.thenBy { it.name }
                    else -> compareByDescending<Recipe> { it.proficiency }.thenBy { it.name }
                },
            )
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
                RecipeListHeader()
            }
            item {
                SearchField(value = keyword, onValueChange = { keyword = it }, placeholder = "搜菜名、口味、做法")
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("难度", "熟练度", "步骤数", "人数").forEach { option ->
                        SmallChip(
                            text = option,
                            selected = sortKey == option,
                            modifier = Modifier.weight(1f),
                            onClick = { sortKey = option },
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item { EmptyBox("没有匹配的菜谱。") }
            } else {
                items(filtered, key = { it.id }) { recipe ->
                    RecipeRow(recipe = recipe, onClick = { onOpen(recipe.id) })
                }
            }
        }

        FloatingActionButton(
            onClick = onAdd,
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
}

@Composable
private fun RecipeListHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
            .padding(horizontal = 4.dp),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawLine(
                color = Ginger,
                start = Offset(size.width * 0.64f, 20.dp.toPx()),
                end = Offset(size.width * 0.61f, 36.dp.toPx()),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Ginger,
                start = Offset(size.width * 0.69f, 30.dp.toPx()),
                end = Offset(size.width * 0.75f, 24.dp.toPx()),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Ginger,
                start = Offset(size.width * 0.70f, 45.dp.toPx()),
                end = Offset(size.width * 0.76f, 53.dp.toPx()),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Forest.copy(alpha = 0.86f),
                start = Offset(size.width * 0.50f, 91.dp.toPx()),
                end = Offset(size.width * 0.68f, 86.dp.toPx()),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        Text(
            text = "我的菜",
            color = Forest,
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 8.dp),
        )
        Text(
            text = "会做什么，一眼看完",
            color = Forest.copy(alpha = 0.92f),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 2.dp, top = 70.dp),
        )
    }
}

@Composable
internal fun RecipeDetailScreen(
    recipe: Recipe,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            BackBar(title = "菜谱详情", onBack = onBack)
        }
        item {
            CardShell {
                Image(
                    painter = painterResource(R.drawable.recipe_detail_header_bg),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(118.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MethodSticker(recipe.cookingMethod, Modifier.size(76.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(recipe.name, color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text(
                            recipe.description.ifBlank { "这道菜还没有备注。" },
                            color = Ash,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    recipe.tasteTags.forEach { Tag(it) }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                StatCard("难度", "${recipe.difficulty}/5", Modifier.weight(1f))
                StatCard("熟练", "${recipe.proficiency}/5", Modifier.weight(1f))
                StatCard("做过", "${recipe.cookedCount} 次", Modifier.weight(1f))
            }
        }
        item {
            SectionTitle("食材")
            Spacer(Modifier.height(8.dp))
            if (recipe.ingredients.isEmpty()) {
                EmptyBox("还没有记录食材。")
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recipe.ingredients.forEach { ingredient ->
                        Tag(
                            listOf(ingredient.name, ingredient.amount, ingredient.unit)
                                .filter { it.isNotBlank() }
                                .joinToString(" "),
                        )
                    }
                }
            }
        }
        item {
            SectionTitle("做法")
        }
        if (recipe.steps.isEmpty()) {
            item { EmptyBox("还没有记录步骤。") }
        } else {
            items(recipe.steps, key = { it.id }) { step ->
                StepCard(step)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Tomato),
                    border = BorderStroke(1.dp, Tomato.copy(alpha = 0.42f)),
                    shape = RoundedCornerShape(13.dp),
                ) {
                    Text("删除")
                }
                Button(
                    onClick = onEdit,
                    modifier = Modifier.weight(1.35f),
                    colors = ButtonDefaults.buttonColors(containerColor = Forest, contentColor = Paper),
                    shape = RoundedCornerShape(13.dp),
                ) {
                    Text("编辑菜谱", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除菜谱") },
            text = { Text("确定删除「${recipe.name}」吗？菜单记录里也会移除这道菜。") },
            confirmButton = {
                TextButton(onClick = onDelete) { Text("删除", color = Tomato) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("取消") }
            },
        )
    }
}

@Composable
internal fun RecipeEditorScreen(
    recipe: Recipe?,
    onBack: () -> Unit,
    onSave: (Recipe) -> Unit,
) {
    var name by remember(recipe?.id) { mutableStateOf(recipe?.name.orEmpty()) }
    var description by remember(recipe?.id) { mutableStateOf(recipe?.description.orEmpty()) }
    var method by remember(recipe?.id) { mutableStateOf(recipe?.cookingMethod ?: "炒") }
    var serving by remember(recipe?.id) { mutableStateOf((recipe?.servingCount ?: 2).toString()) }
    var minutes by remember(recipe?.id) { mutableStateOf((recipe?.estimatedMinutes ?: 20).toString()) }
    var difficulty by remember(recipe?.id) { mutableStateOf((recipe?.difficulty ?: 2).toString()) }
    var proficiency by remember(recipe?.id) { mutableStateOf((recipe?.proficiency ?: 2).toString()) }
    var tags by remember(recipe?.id) { mutableStateOf(recipe?.tasteTags?.joinToString("、").orEmpty()) }
    var note by remember(recipe?.id) { mutableStateOf(recipe?.privateNote.orEmpty()) }
    var available by remember(recipe?.id) { mutableStateOf(recipe?.isAvailable ?: true) }
    var ingredientsText by remember(recipe?.id) {
        mutableStateOf(recipe?.ingredients?.joinToString("\n") { listOf(it.name, it.amount, it.unit, it.note).filter(String::isNotBlank).joinToString(" ") }.orEmpty())
    }
    var stepsText by remember(recipe?.id) {
        mutableStateOf(recipe?.steps?.joinToString("\n") { "${it.title}：${it.description}" }.orEmpty())
    }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        BackBar(title = if (recipe == null) "新增菜谱" else "编辑菜谱", onBack = onBack)
        Image(
            painter = painterResource(R.drawable.recipe_form_hero_new),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.FillWidth,
        )

        CardShell {
            FormField("菜名", name, { name = it }, "比如 番茄炒蛋")
            FormField("描述", description, { description = it }, "这道菜的特点", singleLine = false)
            Text("做法", color = Ash, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CookingMethodOptions.forEach { option ->
                    SmallChip(text = option, selected = method == option, onClick = { method = option })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                FormField("人份", serving, { serving = it.onlyDigits() }, "2", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                FormField("分钟", minutes, { minutes = it.onlyDigits() }, "20", Modifier.weight(1f), keyboardType = KeyboardType.Number)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                FormField("难度", difficulty, { difficulty = it.onlyDigits() }, "1-5", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                FormField("熟练度", proficiency, { proficiency = it.onlyDigits() }, "1-5", Modifier.weight(1f), keyboardType = KeyboardType.Number)
            }
            FormField("口味标签", tags, { tags = it }, "家常、下饭、清爽")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("可加入菜单", color = Ink, fontWeight = FontWeight.SemiBold)
                    Text("关闭后只保留记录，不参与挑菜", color = Muted, fontSize = 12.sp)
                }
                Switch(checked = available, onCheckedChange = { available = it })
            }
        }

        CardShell {
            FormField("食材", ingredientsText, { ingredientsText = it }, "每行一个：鸡蛋 3 个", singleLine = false, minLines = 4)
        }

        CardShell {
            FormField("步骤", stepsText, { stepsText = it }, "每行一步：炒蛋：热锅滑熟", singleLine = false, minLines = 5)
            FormField("私密备注", note, { note = it }, "火候、替换食材、家人口味", singleLine = false)
        }

        if (error.isNotBlank()) {
            Notice(error, tone = "error")
        }

        Button(
            onClick = {
                if (name.trim().isBlank()) {
                    error = "菜名不能为空。"
                    return@Button
                }
                val saved = (recipe ?: Recipe(id = newId(), createdAt = nowIso())).copy(
                    name = name.trim(),
                    description = description.trim(),
                    cookingMethod = method,
                    servingCount = serving.toIntOrNull()?.coerceIn(0, 99) ?: 2,
                    estimatedMinutes = minutes.toIntOrNull()?.coerceIn(0, 24 * 60) ?: 20,
                    difficulty = difficulty.toIntOrNull()?.coerceIn(1, 5) ?: 2,
                    proficiency = proficiency.toIntOrNull()?.coerceIn(1, 5) ?: 2,
                    tasteTags = parseTags(tags).ifEmpty { listOf(method) },
                    isAvailable = available,
                    privateNote = note.trim(),
                    ingredients = parseIngredients(ingredientsText),
                    steps = parseSteps(stepsText),
                    updatedAt = nowIso(),
                )
                onSave(saved)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Forest, contentColor = Paper),
            shape = RoundedCornerShape(15.dp),
        ) {
            Text("保存菜谱", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
    }
}

private val CookingMethodOptions = listOf(
    "汤",
    "炒",
    "蒸",
    "炖",
    "拌",
    "煎",
    "烤",
    "烹",
    "炸",
    "爆",
    "熘",
    "贴",
    "烧",
    "焖",
    "汆",
    "煮",
    "烩",
    "炝",
    "腌",
    "卤",
    "冻",
    "熏",
    "卷",
    "滑",
    "焗",
)
