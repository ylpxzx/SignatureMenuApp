@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.example.signaturemenuapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.signaturemenuapp.data.Recipe
import com.example.signaturemenuapp.R
import com.example.signaturemenuapp.data.MenuDish
import com.example.signaturemenuapp.data.MenuRecord
import com.example.signaturemenuapp.data.MenuStatus
import com.example.signaturemenuapp.data.SignatureMenuData
import com.example.signaturemenuapp.data.newId
import com.example.signaturemenuapp.data.nowIso
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

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
    var showPreview by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val posterWidth = LocalConfiguration.current.screenWidthDp.dp
    val captureScope = rememberCoroutineScope()
    val menuPosterLayer = rememberGraphicsLayer()

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
    val captureTitle = title.ifBlank { "周六晚餐菜单" }
    val captureNote = note.ifBlank { "忌口一起说" }
    val posterHeight = menuPosterHeight(selectedRecipes.size)
    fun saveMenuPoster() {
        if (selectedRecipes.isEmpty()) return
        captureScope.launch {
            val success = saveImageBitmapToPictures(
                context = context,
                imageBitmap = menuPosterLayer.toImageBitmap(),
                fileName = "$captureTitle-菜单图",
            )
            showImageSaveResult(context, success)
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 11.dp),
            contentPadding = PaddingValues(
                top = 0.dp,
                bottom = contentPadding.calculateBottomPadding() + 112.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Image(
                    painter = painterResource(R.drawable.menu_page_header),
                    contentDescription = "挑几道给朋友选，不用链接，发张图就行",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                )
            }
            item {
                MenuPickerSearchField(value = keyword, onValueChange = { keyword = it })
            }
            item {
                MenuMetaPanel(
                    title = title,
                    onTitleChange = { title = it },
                    note = note,
                    onNoteChange = { note = it },
                )
            }
            if (recipes.isEmpty()) {
                item { EmptyBox("没有匹配的菜谱。") }
            } else {
                item {
                    MenuSelectedCounter(count = selectedRecipes.size)
                }
                item {
                    Column {
                        recipes.forEachIndexed { index, recipe ->
                            MenuRecipePickerCard(
                                recipe = recipe,
                                index = index,
                                selected = selectedIds.contains(recipe.id),
                                onToggle = {
                                    selectedIds = if (selectedIds.contains(recipe.id)) {
                                        selectedIds - recipe.id
                                    } else {
                                        selectedIds + recipe.id
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

        MenuActionBar(
            enabled = selectedRecipes.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = 22.dp,
                    end = 22.dp,
                    bottom = contentPadding.calculateBottomPadding() + 12.dp,
                ),
            onGenerate = ::saveMenuPoster,
            onPreview = { showPreview = true },
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .requiredWidth(posterWidth)
                .requiredHeight(posterHeight)
                .graphicsLayer(alpha = 0f)
                .drawWithContent {
                    menuPosterLayer.record {
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(menuPosterLayer)
                },
        ) {
            MenuPosterCard(
                title = captureTitle,
                note = captureNote,
                recipes = selectedRecipes,
            )
        }
    }

    if (showPreview) {
        MenuPreviewDialog(
            title = captureTitle,
            note = captureNote,
            recipes = selectedRecipes,
            onSave = ::saveMenuPoster,
            onDismiss = { showPreview = false },
        )
    }
}

@Composable
private fun MenuPickerSearchField(value: String, onValueChange: (String) -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(Paper.copy(alpha = 0.72f))
            .border(1.4.dp, Color(0xFFB9CFB9), shape)
            .padding(horizontal = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_recipe_search),
            contentDescription = null,
            tint = Color(0xFF303531),
            modifier = Modifier.size(27.dp),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = Color(0xFF111815),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
            ),
            cursorBrush = SolidColor(Forest),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = "搜菜名、口味、做法",
                            color = Color(0x7A424642),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
private fun MenuMetaPanel(
    title: String,
    onTitleChange: (String) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(Paper.copy(alpha = 0.78f))
            .border(1.3.dp, Line, shape),
    ) {
        MenuMetaRow(label = "菜单标题", value = title, onValueChange = onTitleChange)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0x42979382)),
        )
        MenuMetaRow(label = "菜单备注", value = note, onValueChange = onNoteChange)
    }
}

@Composable
private fun MenuMetaRow(label: String, value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .width(88.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = label,
                color = Forest,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(1.dp)
                    .height(24.dp)
                    .background(Color(0x61979382)),
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = Color(0xFF101411),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            cursorBrush = SolidColor(Forest),
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(R.drawable.ic_menu_edit),
            contentDescription = null,
            tint = Forest,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun MenuSelectedCounter(count: Int) {
    Row(
        modifier = Modifier
            .padding(start = 5.dp, top = 1.dp, bottom = 1.dp)
            .height(25.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text("已选", color = Forest, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(count.toString(), color = Ginger, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text("道", color = Forest, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MenuRecipePickerCard(
    recipe: Recipe,
    index: Int,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    val shape = RoundedCornerShape(9.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(79.dp)
            .clip(shape)
            .background(Paper.copy(alpha = if (selected) 0.88f else 0.78f))
            .border(1.dp, Line.copy(alpha = 0.88f), shape)
            .clickable(onClick = onToggle)
            .padding(start = 12.dp, top = 8.dp, end = 14.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MenuRecipeIndex(index = index)
        Spacer(Modifier.width(24.dp))
        MethodSticker(recipe.cookingMethod, Modifier.size(52.dp))
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = recipe.name,
                color = Color(0xFF080D0A),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MenuRecipeMetaText(if (recipe.estimatedMinutes > 0) "${recipe.estimatedMinutes}分钟" else "未估时")
                MenuMetaDotDivider()
                MenuRecipeMetaText(if (recipe.servingCount > 0) "${recipe.servingCount} 人" else "- 人")
            }
            Spacer(Modifier.height(5.dp))
            FlowRow(
                modifier = Modifier.height(18.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                menuRecipeTags(recipe).forEach { tag ->
                    MenuRecipeTag(tag)
                }
            }
        }
        Spacer(Modifier.width(14.dp))
        MenuSelectButton(selected = selected, onClick = onToggle)
    }
}

@Composable
private fun MenuRecipeIndex(index: Int) {
    val color = when {
        index % 4 == 3 -> Color(0xFF2D7FC4)
        index % 2 == 0 -> Ginger
        else -> Forest
    }
    val background = when {
        index % 4 == 3 -> Color(0xFFE5F0FB)
        index % 2 == 0 -> Color(0xFFFFF0DF)
        else -> Color(0xFFE8F4E7)
    }
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = displayMenuIndex(index),
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun MenuRecipeMetaText(text: String) {
    Text(
        text = text,
        color = Color(0xFF5F625E),
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 14.sp,
        maxLines = 1,
        softWrap = false,
    )
}

@Composable
private fun MenuMetaDotDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(10.dp)
            .background(Color(0x75979382)),
    )
}

@Composable
private fun MenuRecipeTag(text: String) {
    Box(
        modifier = Modifier
            .height(18.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFEDF6E7))
            .border(1.dp, Color(0xFFD3E1CA), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Forest,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 12.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun MenuSelectButton(selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .size(31.dp)
            .clip(shape)
            .background(
                if (selected) {
                    Brush.linearGradient(listOf(Color(0xFF15915E), Color(0xFF087044)))
                } else {
                    SolidColor(Color.Transparent)
                },
            )
            .border(if (selected) 0.dp else 1.8.dp, Color(0xFF5C605C), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_menu_check),
                contentDescription = null,
                tint = Paper,
                modifier = Modifier.size(21.dp),
            )
        }
    }
}

@Composable
private fun MenuActionBar(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onGenerate: () -> Unit,
    onPreview: () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val primaryShape = RoundedCornerShape(17.dp)
        Row(
            modifier = Modifier
                .weight(1f)
                .height(58.dp)
                .shadow(8.dp, primaryShape, clip = false)
                .clip(primaryShape)
                .background(
                    if (enabled) {
                        Brush.linearGradient(listOf(Color(0xFF198B5B), Color(0xFF087044)))
                    } else {
                        SolidColor(Color(0xFF9AB7A6))
                    },
                )
                .clickable(enabled = enabled, onClick = onGenerate),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_menu_image),
                contentDescription = null,
                tint = Paper,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(11.dp))
            Text(
                text = "生成菜单图",
                color = Paper,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
            )
        }

        val previewShape = RoundedCornerShape(17.dp)
        Box(
            modifier = Modifier
                .width(92.dp)
                .height(58.dp)
                .clip(previewShape)
                .background(Paper.copy(alpha = 0.94f))
                .border(1.5.dp, Forest, previewShape)
                .clickable(enabled = enabled, onClick = onPreview),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "预览",
                color = Forest,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun MenuPreviewDialog(
    title: String,
    note: String,
    recipes: List<Recipe>,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x8F08130E))
                .padding(18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 390.dp)
                    .fillMaxHeight(0.86f)
                    .shadow(22.dp, RoundedCornerShape(18.dp), clip = false)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Paper),
            ) {
                MenuPreviewDialogHead(onDismiss = onDismiss)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Paper)
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                ) {
                    MenuPosterCard(
                        title = title.ifBlank { "周六晚餐菜单" },
                        note = note.ifBlank { "忌口一起说" },
                        recipes = recipes,
                    )
                }
                MenuPreviewSaveButton(onClick = onSave)
            }
        }
    }
}

@Composable
private fun MenuPreviewDialogHead(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Paper)
            .drawDashedBottomLine(color = Color(0x3D819A78), dashed = false)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "菜单预览图",
            color = Color(0xFF06432D),
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFFEEF5E8).copy(alpha = 0.88f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Text("×", color = Color(0xFF06432D), fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp)
        }
    }
}

@Composable
private fun MenuPreviewSaveButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(Color(0xFF087044))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_menu_image),
            contentDescription = null,
            tint = Paper,
            modifier = Modifier.size(25.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "保存图片",
            color = Paper,
            fontSize = 21.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun menuPosterHeight(recipeCount: Int) = (560 + recipeCount * 104).coerceAtLeast(720).dp

@Composable
private fun MenuPosterCard(
    title: String,
    note: String,
    recipes: List<Recipe>,
) {
    val posterHeight = menuPosterHeight(recipes.size)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(posterHeight)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFFFFDF5), Color(0xFFFFFAF0)))),
    ) {
        MenuPosterBackground()
        MenuPosterFrame()
        MenuPosterOrangeMarks(Modifier.align(Alignment.TopEnd).padding(top = 190.dp, end = 32.dp))
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 43.dp)
                .width(136.dp)
                .height(38.dp)
                .graphicsLayer(rotationZ = -6f)
                .background(Color(0xFFB9C5AA).copy(alpha = 0.82f)),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 32.dp, top = 96.dp, end = 32.dp, bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MenuPosterBrand()
            Text(
                text = title,
                color = Color(0xFF063E29),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 40.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(top = 41.dp)
                    .fillMaxWidth(),
            )
            MenuPosterReplyText(Modifier.padding(top = 22.dp))
            MenuPosterNote(note = note, modifier = Modifier.padding(top = 28.dp))
            MenuPosterRecipeList(recipes = recipes, modifier = Modifier.padding(top = 36.dp))
            MenuPosterFoot(Modifier.padding(top = 20.dp))
        }
        MenuPosterStamp(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 36.dp, bottom = 36.dp),
        )
    }
}

@Composable
private fun MenuPosterBackground() {
    Canvas(Modifier.fillMaxSize()) {
        var x = 0f
        while (x < size.width) {
            var y = 0f
            while (y < size.height) {
                drawCircle(Color(0x0D063E29), radius = 0.85.dp.toPx(), center = Offset(x, y))
                y += 9.dp.toPx()
            }
            x += 9.dp.toPx()
        }
        x = 5.dp.toPx()
        while (x < size.width) {
            var y = 4.dp.toPx()
            while (y < size.height) {
                drawCircle(Color(0x0DF47B17), radius = 0.75.dp.toPx(), center = Offset(x, y))
                y += 11.dp.toPx()
            }
            x += 11.dp.toPx()
        }
    }
}

@Composable
private fun MenuPosterFrame() {
    Canvas(Modifier.fillMaxSize()) {
        val left = 26.dp.toPx()
        val top = 64.dp.toPx()
        val bottom = 20.dp.toPx()
        val stroke = 1.4.dp.toPx()
        drawRoundRect(
            color = Color(0x94063E29),
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(size.width - left * 2, size.height - top - bottom),
            cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
            style = Stroke(width = stroke),
        )
    }
}

@Composable
private fun MenuPosterOrangeMarks(modifier: Modifier = Modifier) {
    Canvas(modifier.size(54.dp)) {
        val orange = Color(0xFFF47B17)
        val stroke = 4.dp.toPx()
        drawLine(orange, Offset(14.dp.toPx(), 4.dp.toPx()), Offset(6.dp.toPx(), 30.dp.toPx()), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(orange, Offset(30.dp.toPx(), 14.dp.toPx()), Offset(25.dp.toPx(), 40.dp.toPx()), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(orange, Offset(45.dp.toPx(), 28.dp.toPx()), Offset(40.dp.toPx(), 50.dp.toPx()), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
private fun MenuPosterBrand() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MenuPosterShortLine()
        Icon(
            painter = painterResource(R.drawable.ic_menu_pot),
            contentDescription = null,
            tint = Color(0xFF063E29),
            modifier = Modifier.size(19.dp),
        )
        Text("拿手菜单", color = Color(0xFF063E29), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        MenuPosterShortLine()
    }
}

@Composable
private fun MenuPosterShortLine() {
    Box(
        modifier = Modifier
            .width(43.dp)
            .height(1.4.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF063E29).copy(alpha = 0.85f)),
    )
}

@Composable
private fun MenuPosterReplyText(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = "回复编号就行",
                color = Color(0xFF063E29),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.width(18.dp))
            Text(
                text = "♡",
                color = Color(0xFFF47B17),
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.graphicsLayer(rotationZ = -13f),
            )
        }
        Canvas(
            modifier = Modifier
                .padding(top = 28.dp)
                .width(148.dp)
                .height(18.dp),
        ) {
            drawLine(
                color = Color(0xB3063E29),
                start = Offset(0f, size.height * 0.68f),
                end = Offset(size.width, size.height * 0.36f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun MenuPosterNote(note: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(Color(0xFFEEF5E8).copy(alpha = 0.76f))
            .border(1.dp, Color(0x85819A78), RoundedCornerShape(13.dp))
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_nav_menu),
            contentDescription = null,
            tint = Color(0xFF063E29),
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = "备注：$note",
            color = Color(0xFF063E29),
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MenuPosterRecipeList(recipes: List<Recipe>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        recipes.forEachIndexed { index, recipe ->
            if (index > 0) {
                MenuPosterDashedDivider()
            }
            MenuPosterRecipeRow(recipe = recipe, index = index)
        }
    }
}

@Composable
private fun MenuPosterRecipeRow(recipe: Recipe, index: Int) {
    val warm = index % 2 == 0
    val accent = if (warm) Color(0xFFF47B17) else Color(0xFF063E29)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MenuPosterNumber(text = displayMenuIndex(index), color = accent)
        Spacer(Modifier.width(26.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = recipe.name,
                color = Color(0xFF082017),
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_recipe_clock),
                    contentDescription = null,
                    tint = Color(0xFF6B706B),
                    modifier = Modifier.size(19.dp),
                )
                Text(
                    text = menuPosterMinutes(recipe),
                    color = Color(0xFF626A63),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xEA819A78)),
                )
                MenuPosterTag(menuPosterTag(recipe))
            }
        }
        Spacer(Modifier.width(18.dp))
        Icon(
            painter = painterResource(menuPosterIconRes(recipe.cookingMethod)),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(40.dp),
        )
    }
}

@Composable
private fun MenuPosterNumber(text: String, color: Color) {
    Box(
        modifier = Modifier.size(50.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = color,
                radius = size.minDimension / 2f - 1.2.dp.toPx(),
                style = Stroke(
                    width = 1.4.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())),
                ),
            )
        }
        Text(
            text = text,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun MenuPosterTag(text: String) {
    Box(
        modifier = Modifier
            .height(30.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(Color(0xFFEEF5E8).copy(alpha = 0.95f))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color(0xFF063E29),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun MenuPosterDashedDivider() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp),
    ) {
        drawLine(
            color = Color(0x8F819A78),
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
        )
    }
}

@Composable
private fun MenuPosterFoot(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MenuPosterDashedDivider()
        Row(
            modifier = Modifier.padding(top = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(17.dp),
        ) {
            MenuPosterShortLine()
            Text("♡", color = Color(0xFF063E29), fontSize = 26.sp, fontWeight = FontWeight.Medium)
            MenuPosterShortLine()
        }
    }
}

@Composable
private fun MenuPosterStamp(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(94.dp)
            .graphicsLayer(rotationZ = -15f),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0x94063E29),
                radius = size.minDimension / 2f - 2.dp.toPx(),
                style = Stroke(width = 1.4.dp.toPx()),
            )
            drawCircle(
                color = Color(0x5C063E29),
                radius = size.minDimension / 2f - 12.dp.toPx(),
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
                ),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("好吃常在", color = Color(0xC7063E29), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Icon(
                painter = painterResource(R.drawable.ic_menu_check),
                contentDescription = null,
                tint = Color(0xCC063E29),
                modifier = Modifier.size(34.dp),
            )
            Text("家常味道", color = Color(0xC7063E29), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

private fun Modifier.drawDashedBottomLine(color: Color, dashed: Boolean = true): Modifier =
    this.then(
        Modifier.drawBehind {
            val pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())) else null
            drawLine(
                color = color,
                start = Offset(0f, size.height - 1.dp.toPx()),
                end = Offset(size.width, size.height - 1.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
                pathEffect = pathEffect,
            )
        },
    )

private fun menuPosterMinutes(recipe: Recipe): String =
    if (recipe.estimatedMinutes > 0) "${recipe.estimatedMinutes}分钟" else "未估时"

private fun menuPosterTag(recipe: Recipe): String =
    recipe.tasteTags.firstOrNull { it.isNotBlank() } ?: recipe.cookingMethod.ifBlank { "家常" }

private fun menuPosterIconRes(method: String): Int {
    val value = method.ifBlank { "炒" }
    return when {
        listOf("汤", "拌", "炝", "腌", "冻", "卷").any { value.contains(it) } -> R.drawable.ic_menu_bowl
        listOf("炒", "爆", "熘", "滑", "煎", "贴", "炸", "烹").any { value.contains(it) } -> R.drawable.ic_menu_wok
        else -> R.drawable.ic_menu_pot
    }
}

private fun displayMenuIndex(index: Int): String = (index + 1).toString().padStart(2, '0')

private fun menuRecipeTags(recipe: Recipe): List<String> =
    recipe.tasteTags.filter { it.isNotBlank() }.take(3).ifEmpty {
        listOf(recipe.cookingMethod.ifBlank { "家常" })
    }

@Composable
internal fun MenuEditorScreen(
    data: SignatureMenuData,
    menu: MenuRecord?,
    onBack: () -> Unit,
    onSave: (MenuRecord) -> Unit,
) {
    var title by remember(menu?.id) { mutableStateOf(menu?.title ?: "周六晚餐菜单") }
    var note by remember(menu?.id) { mutableStateOf(menu?.note ?: "忌口一起说") }
    var dateKey by remember(menu?.id) { mutableStateOf(menu?.dateKey ?: LocalDate.of(2026, 7, 6).toString()) }
    var time by remember(menu?.id) { mutableStateOf(menu?.time ?: "18:30") }
    var dinerCount by remember(menu?.id) { mutableStateOf(menu?.dinerCount ?: 4) }
    var status by remember(menu?.id) { mutableStateOf(menu?.status ?: MenuStatus.Pending) }
    var keyword by remember { mutableStateOf("") }
    var selectedIds by remember(menu?.id) { mutableStateOf(menu?.recipeIds?.toSet() ?: data.recipes.take(2).map { it.id }.toSet()) }
    var error by remember { mutableStateOf("") }
    var showDateDialog by remember { mutableStateOf(false) }

    val recipes = remember(data.recipes, keyword) {
        val query = keyword.trim()
        data.recipes.filter { recipe ->
            recipe.isAvailable && (
                query.isBlank() || listOf(recipe.name, recipe.description, recipe.cookingMethod, recipe.tasteTags.joinToString(" "))
                    .joinToString(" ")
                    .contains(query, ignoreCase = true)
                )
        }
    }
    val selectedRecipes = data.recipes.filter { selectedIds.contains(it.id) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        MenuEditTopBar(title = if (menu == null) "新增菜单" else "编辑菜单", onBack = onBack)
        Image(
            painter = painterResource(R.drawable.menu_edit_header),
            contentDescription = "记一顿菜单，谁来吃，几点吃，选哪几道",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 18.dp),
            contentScale = ContentScale.FillWidth,
        )

        MenuEditFormPanel(
            title = title,
            onTitleChange = { title = it },
            note = note,
            onNoteChange = { note = it },
            dateLabel = menuEditDateTimeLabel(dateKey, time),
            onDateClick = { showDateDialog = true },
            status = status,
            onToggleStatus = { status = if (status == MenuStatus.Pending) MenuStatus.Served else MenuStatus.Pending },
            dinerCount = dinerCount,
            onDinerChange = { dinerCount = (dinerCount + it).coerceIn(1, 99) },
        )

        MenuEditSectionHead(
            selectedCount = selectedRecipes.size,
            modifier = Modifier.padding(top = 25.dp),
        )
        MenuEditSelectedRecipes(
            recipes = selectedRecipes,
            onRemove = { recipe -> selectedIds = selectedIds - recipe.id },
        )
        MenuEditSearchField(value = keyword, onValueChange = { keyword = it })
        Spacer(Modifier.height(10.dp))
        if (recipes.isEmpty()) {
            EmptyBox("没有匹配的可用菜谱。")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recipes.forEachIndexed { index, recipe ->
                    MenuEditRecipeRow(
                        recipe = recipe,
                        index = index,
                        selected = selectedIds.contains(recipe.id),
                        onToggle = {
                            selectedIds = if (selectedIds.contains(recipe.id)) {
                                selectedIds - recipe.id
                            } else {
                                selectedIds + recipe.id
                            }
                        },
                    )
                }
            }
        }
        if (error.isNotBlank()) {
            Notice(error, tone = "error")
        }

        MenuEditSaveButton(
            onClick = {
                if (selectedIds.isEmpty()) {
                    error = "至少选择一道菜。"
                    return@MenuEditSaveButton
                }
                val normalizedDate = runCatching { LocalDate.parse(dateKey).toString() }.getOrElse {
                    error = "出餐日期格式应为 yyyy-MM-dd。"
                    return@MenuEditSaveButton
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
                        time = menuEditNormalizeTime(time) ?: "18:30",
                        status = status,
                        dinerCount = dinerCount.coerceIn(1, 99),
                        recipeIds = orderedIds,
                        dishes = dishes,
                        updatedAt = nowIso(),
                    ),
                )
            },
            modifier = Modifier.padding(top = 16.dp),
        )
        Spacer(Modifier.height(34.dp))
    }

    if (showDateDialog) {
        MenuEditDateTimeDialog(
            dateKey = dateKey,
            time = time,
            onDismiss = { showDateDialog = false },
            onConfirm = { selectedDateKey, selectedTime ->
                dateKey = selectedDateKey
                time = selectedTime
                showDateDialog = false
            },
        )
    }
}

@Composable
private fun MenuEditTopBar(title: String, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(38.dp)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_recipe_back),
                contentDescription = "返回",
                tint = Color(0xFF0B1912),
                modifier = Modifier.size(29.dp),
            )
        }
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                color = Color(0xFF101511),
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 25.sp,
            )
            Canvas(
                modifier = Modifier
                    .width(80.dp)
                    .height(8.dp),
            ) {
                drawLine(
                    color = Color(0xFF258058),
                    start = androidx.compose.ui.geometry.Offset(0f, size.height * 0.72f),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.42f),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun MenuEditFormPanel(
    title: String,
    onTitleChange: (String) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    dateLabel: String,
    onDateClick: () -> Unit,
    status: MenuStatus,
    onToggleStatus: () -> Unit,
    dinerCount: Int,
    onDinerChange: (Int) -> Unit,
) {
    val shape = RoundedCornerShape(13.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Paper.copy(alpha = 0.58f))
            .border(1.3.dp, Color(0xFFA9C4AD), shape),
    ) {
        MenuEditTextRow(label = "菜单名", value = title, onValueChange = onTitleChange)
        MenuEditDivider()
        MenuEditTextRow(label = "备注", value = note, onValueChange = onNoteChange, placeholder = "写点忌口或提醒")
        MenuEditDivider()
        MenuEditValueRow(label = "出餐日期", value = dateLabel, onClick = onDateClick)
        MenuEditDivider()
        MenuEditStatusRow(status = status, onClick = onToggleStatus)
        MenuEditDivider()
        MenuEditDinerRow(dinerCount = dinerCount, onChange = onDinerChange)
    }
}

@Composable
private fun MenuEditTextRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MenuEditRowLabel(label)
        MenuEditPlainInput(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(R.drawable.ic_recipe_edit),
            contentDescription = null,
            tint = Forest,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun MenuEditValueRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MenuEditRowLabel(label)
        Text(
            text = value,
            color = Color(0xFF111815),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 19.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        MenuEditChevron()
    }
}

@Composable
private fun MenuEditDateTimeDialog(
    dateKey: String,
    time: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    val initialMillis = remember(dateKey) { menuEditDateToUtcMillis(dateKey) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    var editingTime by remember(time) { mutableStateOf(time) }
    var timeError by remember { mutableStateOf("") }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val normalizedTime = menuEditNormalizeTime(editingTime)
                    if (normalizedTime == null) {
                        timeError = "请输入 00:00-23:59"
                    } else {
                        val selectedMillis = datePickerState.selectedDateMillis ?: initialMillis
                        onConfirm(menuEditUtcMillisToDate(selectedMillis).toString(), normalizedTime)
                    }
                },
            ) {
                Text("确定", color = Forest, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color(0xFF5F625E))
            }
        },
    ) {
        DatePicker(
            state = datePickerState,
            showModeToggle = false,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "出餐时间",
                color = Forest,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Paper.copy(alpha = 0.72f))
                    .border(1.2.dp, Color(0xFFA9C4AD), RoundedCornerShape(11.dp))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = editingTime,
                    onValueChange = {
                        editingTime = it.take(5)
                        timeError = ""
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color(0xFF111815),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    cursorBrush = SolidColor(Forest),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (editingTime.isBlank()) {
                                Text(
                                    text = "例如：18:30",
                                    color = Color(0x70484B46),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }
            if (timeError.isNotBlank()) {
                Text(
                    text = timeError,
                    color = Color(0xFFD66718),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun MenuEditStatusRow(status: MenuStatus, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MenuEditRowLabel("状态")
        Box(
            modifier = Modifier
                .weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            val served = status == MenuStatus.Served
            Text(
                text = if (served) "已出餐" else "待出餐",
                color = if (served) Forest else Color(0xFFF26F0C),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (served) Color(0xFFEFF7EA) else Paper.copy(alpha = 0.84f))
                    .border(1.2.dp, if (served) Color(0xFFBDD2B6) else Color(0xFFF47B17), RoundedCornerShape(9.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        MenuEditChevron()
    }
}

@Composable
private fun MenuEditDinerRow(dinerCount: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MenuEditRowLabel("就餐人数")
        Text(
            text = "${dinerCount}人",
            color = Color(0xFF111815),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            MenuEditStepperButton("-", onClick = { onChange(-1) })
            Text(
                text = dinerCount.toString(),
                color = Color(0xFF141914),
                fontSize = 19.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(34.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            MenuEditStepperButton("+", onClick = { onChange(1) })
        }
    }
}

@Composable
private fun MenuEditStepperButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFEEF6E8))
            .border(1.dp, Color(0xFFB8CBB3), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Forest, fontSize = 21.sp, fontWeight = FontWeight.Medium, lineHeight = 21.sp)
    }
}

@Composable
private fun MenuEditRowLabel(text: String) {
    Text(
        text = text,
        color = Color(0xFF111815),
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 19.sp,
        modifier = Modifier.width(88.dp),
    )
}

@Composable
private fun MenuEditDivider() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = 17.dp),
    ) {
        drawLine(
            color = Color(0xFFA6AB9A).copy(alpha = 0.38f),
            start = androidx.compose.ui.geometry.Offset.Zero,
            end = androidx.compose.ui.geometry.Offset(size.width, 0f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()), 0f),
        )
    }
}

@Composable
private fun MenuEditChevron() {
    Canvas(
        modifier = Modifier.size(24.dp),
    ) {
        val stroke = 2.dp.toPx()
        drawLine(
            color = Forest,
            start = androidx.compose.ui.geometry.Offset(size.width * 0.38f, size.height * 0.22f),
            end = androidx.compose.ui.geometry.Offset(size.width * 0.68f, size.height * 0.50f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Forest,
            start = androidx.compose.ui.geometry.Offset(size.width * 0.68f, size.height * 0.50f),
            end = androidx.compose.ui.geometry.Offset(size.width * 0.38f, size.height * 0.78f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun MenuEditSectionHead(selectedCount: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Top,
        ) {
            Text("加入菜谱", color = Color(0xFF111815), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp)
            Icon(
                painter = painterResource(R.drawable.menu_section_burst),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(25.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("已选", color = Forest, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(selectedCount.toString(), color = Ginger, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("道", color = Forest, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun MenuEditSelectedRecipes(recipes: List<Recipe>, onRemove: (Recipe) -> Unit) {
    if (recipes.isEmpty()) return
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        recipes.forEach { recipe ->
            MenuEditSelectedRecipeChip(recipe = recipe, onRemove = { onRemove(recipe) })
        }
    }
}

@Composable
private fun MenuEditSelectedRecipeChip(recipe: Recipe, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .width(172.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(Color(0xFFF0F7EB))
            .border(1.dp, Color(0xFFD4DFCC), RoundedCornerShape(11.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MethodSticker(recipe.cookingMethod, Modifier.size(32.dp))
        Spacer(Modifier.width(7.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = recipe.name,
                color = Color(0xFF101511),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${menuEditRecipeMinutes(recipe)} · ${menuEditRecipeServings(recipe)}",
                color = Color(0xFF697069),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Paper.copy(alpha = 0.76f))
                .border(1.dp, Forest.copy(alpha = 0.20f), RoundedCornerShape(8.dp))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_recipe_add),
                contentDescription = "移除${recipe.name}",
                tint = Forest,
                modifier = Modifier
                    .size(17.dp)
                    .graphicsLayer(rotationZ = 45f),
            )
        }
    }
}

@Composable
private fun MenuEditSearchField(value: String, onValueChange: (String) -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(shape)
            .background(Paper.copy(alpha = 0.62f))
            .border(1.3.dp, Color(0xFFA9C4AD), shape)
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_recipe_search),
            contentDescription = null,
            tint = Color(0xFF606762),
            modifier = Modifier.size(24.dp),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = Color(0xFF101511),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            ),
            cursorBrush = SolidColor(Forest),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = "搜菜名、口味、做法",
                            color = Color(0x75484B46),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
private fun MenuEditRecipeRow(
    recipe: Recipe,
    index: Int,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(shape)
            .background(Paper.copy(alpha = 0.70f))
            .border(1.dp, Line.copy(alpha = 0.88f), shape)
            .clickable(onClick = onToggle)
            .padding(start = 12.dp, top = 9.dp, end = 12.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MenuEditRecipeIndex(index)
        Spacer(Modifier.width(9.dp))
        MethodSticker(recipe.cookingMethod, Modifier.size(58.dp))
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = recipe.name,
                color = Color(0xFF101511),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 19.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.padding(top = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_recipe_clock),
                    contentDescription = null,
                    tint = Color(0xFF5F625E),
                    modifier = Modifier.size(15.dp),
                )
                MenuEditRecipeMetaText(menuEditRecipeMinutes(recipe))
                Box(Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF8AA180)))
                MenuEditRecipeMetaText(menuEditRecipeServings(recipe))
            }
            FlowRow(
                modifier = Modifier.padding(top = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                menuRecipeTags(recipe).forEach { tag ->
                    MenuRecipeTag(tag)
                }
            }
        }
        Spacer(Modifier.width(9.dp))
        MenuEditRecipeToggle(selected = selected, onClick = onToggle)
    }
}

@Composable
private fun MenuEditRecipeIndex(index: Int) {
    Box(
        modifier = Modifier
            .width(34.dp)
            .height(42.dp)
            .clip(CircleShape)
            .background(Color(0xFFEEF6E8)),
        contentAlignment = Alignment.Center,
    ) {
        Text(displayMenuIndex(index), color = Forest, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MenuEditRecipeMetaText(text: String) {
    Text(text, color = Color(0xFF5F625E), fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 14.sp)
}

@Composable
private fun MenuEditRecipeToggle(selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(7.dp)
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(shape)
            .background(if (selected) Color(0xFF087044) else Paper.copy(alpha = 0.74f))
            .border(if (selected) 0.dp else 1.7.dp, Forest, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(if (selected) R.drawable.ic_menu_check else R.drawable.ic_recipe_add),
            contentDescription = null,
            tint = if (selected) Paper else Forest,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun MenuEditSaveButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(15.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(7.dp, shape, clip = false)
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF0B7047), Color(0xFF06432B), Color(0xFF04371F))))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(29.dp)
                .clip(CircleShape)
                .border(2.dp, Paper, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_menu_check), contentDescription = null, tint = Paper, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text("保存菜单", color = Paper, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp)
    }
}

@Composable
private fun MenuEditPlainInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(
            color = Color(0xFF111815),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 19.sp,
        ),
        cursorBrush = SolidColor(Forest),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isBlank() && placeholder.isNotBlank()) {
                    Text(
                        text = placeholder,
                        color = Color(0x70484B46),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                innerTextField()
            }
        },
    )
}

private fun menuEditRecipeMinutes(recipe: Recipe): String =
    if (recipe.estimatedMinutes > 0) "${recipe.estimatedMinutes}分钟" else "未估时"

private fun menuEditRecipeServings(recipe: Recipe): String =
    if (recipe.servingCount > 0) "${recipe.servingCount}人" else "未设人数"

private fun menuEditDateTimeLabel(dateKey: String, time: String): String =
    listOf(menuArchiveDateLabel(dateKey), menuArchiveWeekday(dateKey), time).filter { it.isNotBlank() }.joinToString(" ")

private fun menuEditDateToUtcMillis(dateKey: String): Long =
    runCatching {
        LocalDate.parse(dateKey).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }.getOrElse {
        LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }

private fun menuEditUtcMillisToDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

private fun menuEditNormalizeTime(value: String): String? {
    val match = Regex("""^(\d{1,2}):(\d{1,2})$""").matchEntire(value.trim()) ?: return null
    val hour = match.groupValues[1].toIntOrNull() ?: return null
    val minute = match.groupValues[2].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
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
    var activeStatus by remember { mutableStateOf(MenuStatus.Pending) }
    var collapsedDateKeys by remember { mutableStateOf(setOf<String>()) }
    var deletingMenu by remember { mutableStateOf<MenuRecord?>(null) }
    val menuGroups = remember(data.menus, data.recipes, activeStatus, keyword) {
        val query = keyword.trim()
        val filteredMenus = data.menus
            .filter { it.status == activeStatus }
            .filter { menu ->
                query.isBlank() || menuArchiveSearchText(menu, data.recipes)
                    .contains(query, ignoreCase = true)
            }
            .sortedWith(compareByDescending<MenuRecord> { it.dateKey }.thenByDescending { it.time })

        val groups = linkedMapOf<String, MutableList<MenuRecord>>()
        filteredMenus.forEach { menu ->
            groups.getOrPut(menu.dateKey) { mutableListOf() } += menu
        }
        groups.map { (dateKey, menus) ->
            MenuArchiveDateGroup(
                dateKey = dateKey,
                dateLabel = menuArchiveDateLabel(dateKey),
                weekday = menuArchiveWeekday(dateKey),
                menus = menus,
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(
            top = 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 98.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item {
            MenuArchiveSearchBox(value = keyword, onValueChange = { keyword = it })
        }
        item {
            MenuArchiveAddButton(onClick = onCreate)
        }
        item {
            MenuArchiveStatusTabs(
                activeStatus = activeStatus,
                onStatusChange = { activeStatus = it },
            )
        }
        if (menuGroups.isEmpty()) {
            item {
                EmptyBox("暂时没有匹配的菜单。")
            }
        } else {
            menuGroups.forEach { group ->
                item(key = group.dateKey) {
                    MenuArchiveDateSection(
                        group = group,
                        recipes = data.recipes,
                        collapsed = collapsedDateKeys.contains(group.dateKey),
                        onToggleCollapsed = {
                            collapsedDateKeys = if (collapsedDateKeys.contains(group.dateKey)) {
                                collapsedDateKeys - group.dateKey
                            } else {
                                collapsedDateKeys + group.dateKey
                            }
                        },
                        onEdit = onEdit,
                        onMarkServed = { onChangeStatus(it, MenuStatus.Served) },
                        onDelete = { deletingMenu = it },
                    )
                }
            }
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

@Composable
private fun MenuArchiveSearchBox(value: String, onValueChange: (String) -> Unit) {
    val shape = RoundedCornerShape(15.dp)
    Row(
        modifier = Modifier
            .padding(start = 2.dp, end = 2.dp, bottom = 17.dp)
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .background(Paper.copy(alpha = 0.72f))
            .border(1.4.dp, Color(0xFFA8C3AD), shape)
            .padding(horizontal = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_recipe_search),
            contentDescription = null,
            tint = Forest,
            modifier = Modifier.size(27.dp),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = Color(0xFF101511),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            ),
            cursorBrush = SolidColor(Forest),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = "搜菜名、菜单名",
                            color = Color(0x7A484B46),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
private fun MenuArchiveAddButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .padding(start = 2.dp, end = 2.dp, bottom = 18.dp)
            .fillMaxWidth()
            .height(53.dp)
            .clip(shape)
            .background(Paper.copy(alpha = 0.52f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        DashedRoundBorder(color = Color(0xFF8EB99D), radius = 14f, strokeWidth = 1.4f)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(25.dp)
                    .clip(CircleShape)
                    .border(2.dp, Forest, CircleShape)
                    .padding(3.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_recipe_add),
                    contentDescription = null,
                    tint = Forest,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = "新增菜单",
                color = Forest,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun MenuArchiveStatusTabs(
    activeStatus: MenuStatus,
    onStatusChange: (MenuStatus) -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .padding(start = 2.dp, end = 2.dp, bottom = 26.dp)
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(Paper.copy(alpha = 0.64f))
            .border(1.2.dp, Line, shape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(MenuStatus.Pending to "待出餐", MenuStatus.Served to "已出餐").forEach { (status, label) ->
            val selected = activeStatus == status
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        if (selected) {
                            Brush.linearGradient(listOf(Color(0xFF198B5B), Color(0xFF087044)))
                        } else {
                            SolidColor(Color.Transparent)
                        },
                    )
                    .clickable { onStatusChange(status) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (selected) Paper else Color(0xFF1A201B),
                    fontSize = 16.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun MenuArchiveDateSection(
    group: MenuArchiveDateGroup,
    recipes: List<Recipe>,
    collapsed: Boolean,
    onToggleCollapsed: () -> Unit,
    onEdit: (String) -> Unit,
    onMarkServed: (String) -> Unit,
    onDelete: (MenuRecord) -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(start = 2.dp, end = 2.dp, bottom = 24.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MenuArchiveDateHead(
            dateLabel = group.dateLabel,
            weekday = group.weekday,
            collapsed = collapsed,
            onClick = onToggleCollapsed,
        )
        if (!collapsed) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                group.menus.forEach { menu ->
                    MenuArchiveRecordCard(
                        menu = menu,
                        recipes = recipes,
                        onEdit = { onEdit(menu.id) },
                        onMarkServed = { onMarkServed(menu.id) },
                        onDelete = { onDelete(menu) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuArchiveDateHead(
    dateLabel: String,
    weekday: String,
    collapsed: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(Color(0x242F8C56)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2F8C56)),
            )
        }
        Spacer(Modifier.width(7.dp))
        Text(
            text = dateLabel,
            color = Color(0xFF101511),
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Spacer(Modifier.width(22.dp))
        Text(
            text = weekday,
            color = Color(0xFF101511),
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Spacer(Modifier.weight(1f))
        MenuArchiveChevron(collapsed = collapsed)
    }
}

@Composable
private fun MenuArchiveChevron(collapsed: Boolean) {
    Canvas(Modifier.size(20.dp)) {
        val strokeWidth = 3.dp.toPx()
        val left = androidx.compose.ui.geometry.Offset(3.dp.toPx(), if (collapsed) 7.dp.toPx() else 13.dp.toPx())
        val center = androidx.compose.ui.geometry.Offset(10.dp.toPx(), if (collapsed) 14.dp.toPx() else 6.dp.toPx())
        val right = androidx.compose.ui.geometry.Offset(17.dp.toPx(), if (collapsed) 7.dp.toPx() else 13.dp.toPx())
        drawLine(Color(0xFF101511), left, center, strokeWidth = strokeWidth, cap = StrokeCap.Square)
        drawLine(Color(0xFF101511), center, right, strokeWidth = strokeWidth, cap = StrokeCap.Square)
    }
}

@Composable
private fun MenuArchiveRecordCard(
    menu: MenuRecord,
    recipes: List<Recipe>,
    onEdit: () -> Unit,
    onMarkServed: () -> Unit,
    onDelete: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Paper.copy(alpha = 0.78f))
            .border(1.2.dp, Line.copy(alpha = 0.92f), shape)
            .padding(start = 14.dp, top = 18.dp, end = 14.dp, bottom = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            MenuArchivePotIcon()
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Text(
                        text = menu.title,
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF0A100C),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 24.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    MenuArchiveStatusBadge(status = menu.status)
                }
                MenuArchiveMetaRow(menu = menu)
            }
        }

        val dishes = menuArchiveDisplayDishes(menu, recipes)
        if (dishes.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(top = 22.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                dishes.forEach { dish ->
                    MenuArchiveDishChip(dish)
                }
            }
        } else {
            Spacer(Modifier.height(18.dp))
        }

        DashedDivider(color = Color(0x949FB297))

        MenuArchiveActions(
            status = menu.status,
            onEdit = onEdit,
            onMarkServed = onMarkServed,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun MenuArchivePotIcon() {
    Box(
        modifier = Modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(Color(0xFFFFF0DC))
            .border(1.dp, Color(0xFFF8C98B), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_menu_pot),
            contentDescription = null,
            tint = Ginger,
            modifier = Modifier.size(34.dp),
        )
    }
}

@Composable
private fun MenuArchiveStatusBadge(status: MenuStatus) {
    val served = status == MenuStatus.Served
    val shape = RoundedCornerShape(9.dp)
    Box(
        modifier = Modifier
            .height(27.dp)
            .clip(shape)
            .background(if (served) Color(0xFFEDF6E7) else Color(0xFFFFF2DF))
            .border(1.dp, if (served) Color(0xFFD2DFC9) else Color(0xFFF4C58C), shape)
            .padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (served) "已出餐" else "待出餐",
            color = if (served) Forest else Color(0xFFDE650F),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun MenuArchiveMetaRow(menu: MenuRecord) {
    Row(
        modifier = Modifier
            .padding(top = 10.dp)
            .fillMaxWidth()
            .height(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_recipe_clock),
            contentDescription = null,
            tint = Color(0xFF313631),
            modifier = Modifier.size(18.dp),
        )
        MenuArchiveMetaText(menu.time)
        MenuArchiveMetaDivider()
        Icon(
            painter = painterResource(R.drawable.ic_menu_user),
            contentDescription = null,
            tint = Color(0xFF313631),
            modifier = Modifier.size(18.dp),
        )
        MenuArchiveMetaText("${menu.dinerCount}人")
        MenuArchiveMetaDivider()
        Icon(
            painter = painterResource(R.drawable.ic_menu_orders),
            contentDescription = null,
            tint = Color(0xFF313631),
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = menu.note.ifBlank { "没有备注" },
            modifier = Modifier.weight(1f),
            color = Color(0xFF626761),
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MenuArchiveMetaText(text: String) {
    Text(
        text = text,
        color = Color(0xFF626761),
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        maxLines = 1,
        softWrap = false,
    )
}

@Composable
private fun MenuArchiveMetaDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(16.dp)
            .background(Color(0x6B979382)),
    )
}

@Composable
private fun MenuArchiveDishChip(dish: MenuDish) {
    Box(
        modifier = Modifier
            .height(35.dp)
            .widthIn(min = 92.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFEFF4E8))
            .border(1.dp, Color(0xFFD5DDCC), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "${dish.name} ×${dish.count}",
            color = Color(0xFF151B16),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MenuArchiveActions(
    status: MenuStatus,
    onEdit: () -> Unit,
    onMarkServed: () -> Unit,
    onDelete: () -> Unit,
) {
    val actions = if (status == MenuStatus.Pending) {
        listOf(
            MenuArchiveActionSpec("编辑", R.drawable.ic_menu_edit, Forest, onEdit),
            MenuArchiveActionSpec("标记已出餐", R.drawable.ic_menu_check, Forest, onMarkServed),
            MenuArchiveActionSpec("删除", R.drawable.ic_menu_delete, Color(0xFFD66718), onDelete),
        )
    } else {
        listOf(
            MenuArchiveActionSpec("编辑", R.drawable.ic_menu_edit, Forest, onEdit),
            MenuArchiveActionSpec("删除", R.drawable.ic_menu_delete, Color(0xFFD66718), onDelete),
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        actions.forEachIndexed { index, action ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(Color(0x57979382)),
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
                    .clickable(onClick = action.onClick),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(action.iconRes),
                    contentDescription = null,
                    tint = action.color,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = action.label,
                    color = action.color,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DashedRoundBorder(color: Color, radius: Float, strokeWidth: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokePx = strokeWidth.dp.toPx()
        val halfStroke = strokePx / 2f
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(halfStroke, halfStroke),
            size = androidx.compose.ui.geometry.Size(size.width - strokePx, size.height - strokePx),
            cornerRadius = CornerRadius(radius.dp.toPx(), radius.dp.toPx()),
            style = Stroke(
                width = strokePx,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())),
            ),
        )
    }
}

@Composable
private fun DashedDivider(color: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp),
    ) {
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
            end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
            strokeWidth = 1.4.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
        )
    }
}

private data class MenuArchiveDateGroup(
    val dateKey: String,
    val dateLabel: String,
    val weekday: String,
    val menus: List<MenuRecord>,
)

private data class MenuArchiveActionSpec(
    val label: String,
    val iconRes: Int,
    val color: Color,
    val onClick: () -> Unit,
)

private fun menuArchiveSearchText(menu: MenuRecord, recipes: List<Recipe>): String =
    listOf(
        menu.title,
        menu.note,
        menuArchiveDisplayDishes(menu, recipes).joinToString(" ") { it.name },
    ).joinToString(" ")

private fun menuArchiveDisplayDishes(menu: MenuRecord, recipes: List<Recipe>): List<MenuDish> {
    val byId = recipes.associateBy { it.id }
    val dishes = menu.dishes.mapNotNull { dish ->
        val name = dish.name.ifBlank { byId[dish.recipeId]?.name.orEmpty() }
        if (name.isBlank()) null else dish.copy(name = name, count = dish.count.coerceAtLeast(1))
    }
    if (dishes.isNotEmpty()) return dishes
    return menu.recipeIds.mapNotNull { recipeId ->
        byId[recipeId]?.let { recipe ->
            MenuDish(recipeId = recipeId, name = recipe.name, count = 1)
        }
    }
}

private fun menuArchiveDateLabel(dateKey: String): String {
    val date = runCatching { LocalDate.parse(dateKey) }.getOrNull() ?: return dateKey
    return "${date.monthValue}月${date.dayOfMonth}日"
}

private fun menuArchiveWeekday(dateKey: String): String {
    val date = runCatching { LocalDate.parse(dateKey) }.getOrNull() ?: return ""
    val labels = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    return labels[date.dayOfWeek.value - 1]
}
