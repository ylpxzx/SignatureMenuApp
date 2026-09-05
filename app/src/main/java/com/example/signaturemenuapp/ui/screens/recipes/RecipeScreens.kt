@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.signaturemenuapp.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.signaturemenuapp.R
import com.example.signaturemenuapp.data.Recipe
import com.example.signaturemenuapp.data.SignatureMenuData
import com.example.signaturemenuapp.data.newId
import com.example.signaturemenuapp.data.nowIso
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun RecipeListScreen(
    data: SignatureMenuData,
    contentPadding: PaddingValues,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var keyword by remember { mutableStateOf("") }
    var sortKey by remember { mutableStateOf("difficulty") }
    var sortAscending by remember { mutableStateOf(false) }
    var openRecipeId by remember(data.recipes) { mutableStateOf<String?>(null) }
    val filtered = remember(data.recipes, keyword, sortKey, sortAscending) {
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
            .sortedWith { left, right ->
                val leftValue = recipeSortValue(left, sortKey)
                val rightValue = recipeSortValue(right, sortKey)
                val primary = when {
                    leftValue == rightValue -> 0
                    sortAscending -> leftValue.compareTo(rightValue)
                    else -> rightValue.compareTo(leftValue)
                }
                if (primary != 0) primary else left.name.compareTo(right.name)
            }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(
                top = 10.dp,
                bottom = contentPadding.calculateBottomPadding() + 90.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            item {
                RecipeListHeader()
            }
            item {
                RecipeListSearchField(value = keyword, onValueChange = { keyword = it })
            }
            item {
                RecipeSortBar(
                    sortKey = sortKey,
                    sortAscending = sortAscending,
                    onSort = { nextKey ->
                        if (sortKey == nextKey) {
                            sortAscending = !sortAscending
                        } else {
                            sortKey = nextKey
                            sortAscending = false
                        }
                    },
                )
            }

            if (filtered.isEmpty()) {
                item { EmptyBox("没有匹配的菜谱。") }
            } else {
                items(filtered, key = { it.id }) { recipe ->
                    RecipeListSwipeCard(
                        recipe = recipe,
                        isOpen = openRecipeId == recipe.id,
                        onOpenSwipe = { openRecipeId = recipe.id },
                        onCloseSwipe = {
                            if (openRecipeId == recipe.id) {
                                openRecipeId = null
                            }
                        },
                        onClick = { onOpen(recipe.id) },
                        onDelete = {
                            openRecipeId = null
                            onDelete(recipe.id)
                        },
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 22.dp, bottom = contentPadding.calculateBottomPadding() + 20.dp)
                .size(66.dp)
                .shadow(9.dp, CircleShape, clip = false)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF198B5B), Color(0xFF087044)),
                    ),
                )
                .clickable(onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_recipe_add),
                contentDescription = "新增菜谱",
                tint = Paper,
                modifier = Modifier.size(38.dp),
            )
        }
    }
}

@Composable
private fun RecipeListHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(98.dp)
            .padding(start = 4.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.recipe_list_header),
            contentDescription = "我的菜，会做什么，一眼看完",
            modifier = Modifier
                .width(280.dp)
                .height(98.dp),
            contentScale = ContentScale.FillBounds,
        )
    }
}

@Composable
private fun RecipeListSearchField(value: String, onValueChange: (String) -> Unit) {
    val shape = RoundedCornerShape(13.dp)
    Row(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(Paper.copy(alpha = 0.72f))
            .border(1.5.dp, Color(0xFFB6C7AE), shape)
            .padding(horizontal = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_recipe_search),
            contentDescription = null,
            tint = Color(0xFF2E342F),
            modifier = Modifier.size(27.dp),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = Ink,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp,
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
                            letterSpacing = 0.5.sp,
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
private fun RecipeSortBar(
    sortKey: String,
    sortAscending: Boolean,
    onSort: (String) -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .fillMaxWidth()
            .height(43.dp)
            .clip(shape)
            .background(Paper.copy(alpha = 0.78f))
            .border(1.5.dp, Line, shape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecipeSortOptions.forEachIndexed { index, option ->
            val selected = sortKey == option.key
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (selected) Mint.copy(alpha = 0.58f) else Color.Transparent)
                    .clickable { onSort(option.key) },
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = option.label,
                        color = if (selected) Forest else Color(0xFF101512),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    Text(
                        text = when {
                            !selected -> "↕"
                            sortAscending -> "↑"
                            else -> "↓"
                        },
                        color = if (selected) Forest else Color(0xFF101512),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }
            if (index < RecipeSortOptions.lastIndex) {
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .fillMaxHeight()
                        .background(Line),
                )
            }
        }
    }
}

@Composable
private fun RecipeListSwipeCard(
    recipe: Recipe,
    isOpen: Boolean,
    onOpenSwipe: () -> Unit,
    onCloseSwipe: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val density = LocalDensity.current
    val revealPx = with(density) { RecipeListDeleteRevealWidth.toPx() }
    var offsetPx by remember(recipe.id) { mutableStateOf(0f) }
    val showDeleteAction = isOpen || offsetPx < -1f

    LaunchedEffect(isOpen, revealPx) {
        offsetPx = if (isOpen) -revealPx else 0f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(12.dp)),
    ) {
        if (showDeleteAction) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(RecipeListDeleteActionWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                    .background(Color(0xFFDF533F))
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_menu_delete),
                    contentDescription = "删除${recipe.name}",
                    tint = Paper,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
        RecipeListCard(
            recipe = recipe,
            onClick = onClick,
            modifier = Modifier
                .offset { IntOffset(offsetPx.roundToInt(), 0) }
                .pointerInput(recipe.id, isOpen) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetPx = (offsetPx + dragAmount).coerceIn(-revealPx, 0f)
                        },
                        onDragEnd = {
                            if (offsetPx <= -revealPx * 0.58f) {
                                offsetPx = -revealPx
                                onOpenSwipe()
                            } else {
                                offsetPx = 0f
                                onCloseSwipe()
                            }
                        },
                        onDragCancel = {
                            offsetPx = if (isOpen) -revealPx else 0f
                        },
                    )
                },
        )
    }
}

@Composable
private fun RecipeListCard(
    recipe: Recipe,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .background(Paper)
            .border(1.2.dp, Line, shape)
            .padding(start = 10.dp, top = 10.dp, end = 12.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(72.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            MethodSticker(recipe.cookingMethod, Modifier.size(66.dp))
        }
        Spacer(Modifier.width(13.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .clipToBounds(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = recipe.name,
                    modifier = Modifier.weight(1f, fill = false),
                    color = Color(0xFF050807),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp,
                    lineHeight = 21.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                RecipeProficiencyPill(proficiencyText(recipe.proficiency))
            }
            RecipeInfoRow(recipe = recipe)
            FlowRow(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                recipeListTags(recipe).forEach { tag ->
                    RecipeListTag(tag)
                }
            }
        }
    }
}

private val RecipeListDeleteRevealWidth = 70.dp
private val RecipeListDeleteActionWidth = 80.dp

@Composable
private fun RecipeProficiencyPill(text: String) {
    Box(
        modifier = Modifier
            .height(19.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFFEDF7E9))
            .border(1.dp, Color(0xFFD1DFC9), RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Forest,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun RecipeInfoRow(recipe: Recipe) {
    Row(
        modifier = Modifier
            .padding(top = 4.dp)
            .fillMaxWidth()
            .height(18.dp)
            .clipToBounds(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_recipe_clock),
                contentDescription = null,
                tint = Color(0xFF6D716B),
                modifier = Modifier.size(13.dp),
            )
            RecipeMetaText("${recipe.steps.size}步")
        }
        RecipeMetaDivider()
        RecipeMetaText(if (recipe.estimatedMinutes > 0) "${recipe.estimatedMinutes}分钟" else "未估时")
        RecipeMetaDivider()
        RecipeMetaText(if (recipe.servingCount > 0) "${recipe.servingCount}人" else "-人")
        RecipeMetaDivider()
        Text(
            text = difficultyStars(recipe),
            color = Ginger,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            maxLines = 1,
            softWrap = false,
        )
        Text(
            text = difficultyText(recipe),
            color = Color(0xFF686B66),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun RecipeMetaText(text: String) {
    Text(
        text = text,
        color = Color(0xFF5F625E),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        softWrap = false,
    )
}

@Composable
private fun RecipeMetaDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(11.dp)
            .background(Color(0xFFD4CEBF)),
    )
}

@Composable
private fun RecipeListTag(text: String) {
    Box(
        modifier = Modifier
            .height(20.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Mint)
            .border(1.dp, Color(0xFFCFDDC5), RoundedCornerShape(5.dp))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color(0xFF123E2C),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.2.sp,
            maxLines = 1,
        )
    }
}

private data class RecipeSortOption(val key: String, val label: String)

private val RecipeSortOptions = listOf(
    RecipeSortOption("difficulty", "难度"),
    RecipeSortOption("proficiency", "熟练度"),
    RecipeSortOption("steps", "步骤数"),
    RecipeSortOption("servings", "人数"),
)

private fun recipeSortValue(recipe: Recipe, key: String): Int = when (key) {
    "difficulty" -> recipe.difficulty
    "proficiency" -> recipe.proficiency
    "steps" -> recipe.steps.size
    "servings" -> recipe.servingCount
    else -> recipe.difficulty
}

private fun difficultyStars(recipe: Recipe): String {
    val count = recipe.difficulty.coerceIn(0, 5)
    return if (count > 0) "★".repeat(count) else "未设"
}

private fun difficultyText(recipe: Recipe): String {
    val labels = listOf("未设", "简单", "中等", "偏难", "困难", "地狱")
    return labels[recipe.difficulty.coerceIn(0, 5)]
}

private fun recipeListTags(recipe: Recipe): List<String> {
    val method = recipe.cookingMethod.ifBlank { "家常" }
    val tags = recipe.tasteTags.filter { it.isNotBlank() }.take(2).toMutableList()
    if (method !in tags) {
        tags += method
    }
    return tags.take(3)
}

@Composable
internal fun RecipeDetailScreen(
    recipe: Recipe,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val captureScope = rememberCoroutineScope()
    val recipeDetailLayer = rememberGraphicsLayer()
    val captureHeight = recipeDetailCaptureHeight(recipe)
    fun saveRecipePoster() {
        captureScope.launch {
            val success = saveImageBitmapToPictures(
                context = context,
                imageBitmap = recipeDetailLayer.toImageBitmap(),
                fileName = "${recipe.name.ifBlank { "菜谱详情" }}-菜谱图",
            )
            showImageSaveResult(context, success)
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item {
                RecipeDetailTopBar(onBack = onBack, onEdit = onEdit)
            }
            item {
                RecipeDetailHero(recipe = recipe, modifier = Modifier.padding(top = 12.dp))
            }
            item {
                RecipeDetailStatGrid(recipe = recipe, modifier = Modifier.padding(top = 18.dp))
            }
            item {
                RecipeDetailIngredients(recipe = recipe, modifier = Modifier.padding(top = 29.dp))
            }
            item {
                RecipeDetailSteps(recipe = recipe, modifier = Modifier.padding(top = 24.dp))
            }
            item {
                RecipeDetailPrivateNote(
                    recipe.privateNote.ifBlank { "模拟数据，可直接编辑成自己的做法。" },
                    modifier = Modifier.padding(top = 28.dp),
                )
            }
        }

        RecipeDetailGenerateButton(
            onClick = ::saveRecipePoster,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .requiredWidth(393.dp)
                .requiredHeight(captureHeight)
                .graphicsLayer(alpha = 0f)
                .drawWithContent {
                    recipeDetailLayer.record {
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(recipeDetailLayer)
                },
        ) {
            RecipeDetailCaptureContent(recipe = recipe)
        }
    }
}

@Composable
private fun RecipeDetailCaptureContent(recipe: Recipe) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFFFFDF3), Color(0xFFF9FCF5))))
            .padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 34.dp),
    ) {
        RecipeDetailHero(recipe = recipe)
        RecipeDetailStatGrid(recipe = recipe, modifier = Modifier.padding(top = 18.dp))
        RecipeDetailIngredients(recipe = recipe, modifier = Modifier.padding(top = 29.dp))
        RecipeDetailSteps(recipe = recipe, modifier = Modifier.padding(top = 24.dp))
        RecipeDetailPrivateNote(
            recipe.privateNote.ifBlank { "模拟数据，可直接编辑成自己的做法。" },
            modifier = Modifier.padding(top = 28.dp),
        )
    }
}

private fun recipeDetailCaptureHeight(recipe: Recipe): Dp {
    val ingredientRows = maxOf(1, (recipe.ingredients.size + 1) / 2)
    val stepRows = maxOf(1, recipe.steps.size)
    val note = recipe.privateNote.ifBlank { "模拟数据，可直接编辑成自己的做法。" }
    val noteRows = maxOf(1, (note.length + 17) / 18)
    val height = 20 +
        158 +
        18 + 200 +
        29 + 35 + ingredientRows * 34 +
        24 + 35 + stepRows * 41 +
        28 + 35 + 28 + noteRows * 24 +
        34
    return height.coerceAtLeast(880).dp
}

@Composable
private fun RecipeDetailTopBar(onBack: () -> Unit, onEdit: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(42.dp)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_recipe_back),
                contentDescription = "返回",
                tint = Forest,
                modifier = Modifier.size(30.dp),
            )
        }
        Text(
            text = "详情",
            color = Forest,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 25.sp,
            modifier = Modifier.align(Alignment.Center),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(42.dp)
                .clickable(onClick = onEdit),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_recipe_edit),
                contentDescription = "编辑菜谱",
                tint = Forest,
                modifier = Modifier.size(29.dp),
            )
        }
    }
}

@Composable
private fun RecipeDetailHero(recipe: Recipe, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(158.dp)
            .clip(shape)
            .background(Paper.copy(alpha = 0.98f))
            .border(1.5.dp, Color(0x1F163F2E), shape),
    ) {
        Image(
            painter = painterResource(R.drawable.recipe_detail_header_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFFFFFDF4).copy(alpha = 0.98f),
                            Color(0xFFFFFDF4).copy(alpha = 0.86f),
                            Color(0xFFFFFDF4).copy(alpha = 0.50f),
                        ),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 18.dp, top = 18.dp, end = 14.dp, bottom = 17.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = recipe.name.ifBlank { "未命名菜谱" },
                        color = Forest,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 29.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Image(
                        painter = painterResource(R.drawable.ic_home_recent_spark),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 3.dp, top = 1.dp)
                            .size(22.dp),
                    )
                }
                Text(
                    text = "做一道好菜，奖励好心情",
                    color = Ash,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 5.dp),
                )
                Row(
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .height(31.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFF3C7B48))
                        .border(1.dp, Color(0xFF3C7B48), RoundedCornerShape(999.dp))
                        .padding(horizontal = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_recipe_cook),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = recipeDetailProficiencyText(recipe.proficiency),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
            RecipeDetailAnimatedMethodSticker(recipe.cookingMethod, Modifier.size(132.dp))
        }
    }
}

@Composable
private fun RecipeDetailAnimatedMethodSticker(method: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "recipe-detail-method-sticker")
    val yDp by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4800
                0f at 0
                -7f at 1632
                -3f at 3168
                0f at 4800
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "recipe-detail-method-sticker-y",
    )
    val xDp by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4800
                0f at 0
                0f at 1632
                4f at 3168
                0f at 4800
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "recipe-detail-method-sticker-x",
    )
    val rotation by transition.animateFloat(
        initialValue = -0.8f,
        targetValue = -0.8f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4800
                -0.8f at 0
                1.2f at 1632
                -0.4f at 3168
                -0.8f at 4800
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "recipe-detail-method-sticker-rotation",
    )
    val density = LocalDensity.current
    MethodSticker(
        method = method,
        modifier = modifier.graphicsLayer {
            translationX = with(density) { xDp.dp.toPx() }
            translationY = with(density) { yDp.dp.toPx() }
            rotationZ = rotation
        },
    )
}

@Composable
private fun RecipeDetailStatGrid(recipe: Recipe, modifier: Modifier = Modifier) {
    val stats = listOf(
        RecipeDetailStatSpec(R.drawable.ic_recipe_dish, recipe.ingredients.size.toString(), "种", "食材数"),
        RecipeDetailStatSpec(R.drawable.ic_recipe_clock, (recipe.estimatedMinutes.takeIf { it > 0 } ?: 0).let { if (it > 0) it.toString() else "-" }, "分钟", "预计时间"),
        RecipeDetailStatSpec(R.drawable.ic_nav_menu, recipe.steps.size.toString(), "步", "步骤数"),
        RecipeDetailStatSpec(R.drawable.ic_recipe_star, recipeDetailDifficultyStars(recipe.difficulty), "", recipeDetailDifficultyText(recipe.difficulty), stars = true),
        RecipeDetailStatSpec(R.drawable.ic_recipe_label, recipeDetailTasteText(recipe), "", "口味标签", compact = true),
        RecipeDetailStatSpec(R.drawable.ic_menu_user, (recipe.servingCount.takeIf { it > 0 } ?: 0).let { if (it > 0) it.toString() else "-" }, "人", "适用人数"),
        RecipeDetailStatSpec(R.drawable.ic_recipe_loop, recipe.cookedCount.coerceAtLeast(0).toString(), "次", "做过次数"),
        RecipeDetailStatSpec(R.drawable.ic_recipe_money, recipe.priceRange.ifBlank { "-" }, "", "价格范围", compact = true),
    )
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Paper.copy(alpha = 0.54f))
            .border(1.5.dp, Color(0x26163F2E), shape)
            .padding(vertical = 14.dp),
    ) {
        repeat(2) { row ->
            Row(Modifier.fillMaxWidth()) {
                repeat(4) { column ->
                    val index = row * 4 + column
                    RecipeDetailStatCell(
                        stat = stats[index],
                        showRightLine = column < 3,
                        showBottomLine = row == 0,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipeDetailStatCell(
    stat: RecipeDetailStatSpec,
    showRightLine: Boolean,
    showBottomLine: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(86.dp)
            .drawBehind {
                val stroke = 1.dp.toPx()
                val pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
                if (showRightLine) {
                    drawLine(
                        color = Color(0x33163F2E),
                        start = Offset(size.width - stroke / 2f, 0f),
                        end = Offset(size.width - stroke / 2f, size.height),
                        strokeWidth = stroke,
                        pathEffect = pathEffect,
                    )
                }
                if (showBottomLine) {
                    drawLine(
                        color = Color(0x2E163F2E),
                        start = Offset(0f, size.height - stroke / 2f),
                        end = Offset(size.width, size.height - stroke / 2f),
                        strokeWidth = stroke,
                        pathEffect = pathEffect,
                    )
                }
            }
            .padding(horizontal = 5.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(stat.iconRes),
            contentDescription = null,
            tint = Forest,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(5.dp))
        if (stat.stars) {
            Text(
                text = stat.value,
                color = Color(0xFFFF8A00),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 19.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else if (stat.unit.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stat.value,
                    color = Forest,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 23.sp,
                    maxLines = 1,
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = stat.unit,
                    color = Color(0xFF111815),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 20.sp,
                    maxLines = 1,
                )
            }
        } else {
            Text(
                text = stat.value,
                color = Color(0xFF111815),
                fontSize = if (stat.compact) 15.sp else 16.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 19.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            text = stat.label,
            color = Ash,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RecipeDetailIngredients(recipe: Recipe, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
    ) {
        RecipeDetailSectionTitle("食材清单")
        Spacer(Modifier.height(9.dp))
        if (recipe.ingredients.isEmpty()) {
            RecipeDetailEmptyText("还没有添加食材。")
        } else {
            val splitIndex = (recipe.ingredients.size + 1) / 2
            val columns = listOf(recipe.ingredients.take(splitIndex), recipe.ingredients.drop(splitIndex))
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                columns.forEach { column ->
                    Column(modifier = Modifier.weight(1f)) {
                        column.forEach { ingredient ->
                            RecipeDetailIngredientRow(recipeDetailIngredientText(ingredient))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeDetailIngredientRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .drawBehind {
                drawLine(
                    color = Color(0x2E163F2E),
                    start = Offset(0f, size.height - 1.dp.toPx()),
                    end = Offset(size.width, size.height - 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
                )
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color(0xFF14713E)),
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text = text,
            color = Color(0xFF111815),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 19.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RecipeDetailSteps(recipe: Recipe, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
    ) {
        RecipeDetailSectionTitle("步骤预览")
        Spacer(Modifier.height(9.dp))
        if (recipe.steps.isEmpty()) {
            RecipeDetailEmptyText("还没有添加步骤。")
        } else {
            Column {
                recipe.steps.forEachIndexed { index, step ->
                    RecipeDetailStepRow(
                        order = step.order.takeIf { it > 0 } ?: (index + 1),
                        text = recipeDetailStepText(step),
                        minutes = step.estimatedMinutes,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipeDetailStepRow(order: Int, text: String, minutes: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(41.dp)
            .drawBehind {
                drawLine(
                    color = Color(0x2E163F2E),
                    start = Offset(0f, size.height - 1.dp.toPx()),
                    end = Offset(size.width, size.height - 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
                )
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(27.dp)
                .clip(CircleShape)
                .background(Color(0xFF08703C)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = order.toString(),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            color = Color(0xFF111815),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 19.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (minutes > 0) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = "${minutes}分钟",
                color = Color(0xFF4F7D4D),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun RecipeDetailPrivateNote(note: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
    ) {
        RecipeDetailSectionTitle("私密备注")
        Spacer(Modifier.height(9.dp))
        Text(
            text = note,
            color = Color(0xFF111815),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 24.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFEAF7EF).copy(alpha = 0.65f))
                .padding(14.dp),
        )
    }
}

@Composable
private fun RecipeDetailSectionTitle(title: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            text = title,
            color = Color(0xFF0D1F18),
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 26.sp,
        )
        Image(
            painter = painterResource(R.drawable.ic_home_recent_spark),
            contentDescription = null,
            modifier = Modifier.size(21.dp),
        )
    }
}

@Composable
private fun RecipeDetailEmptyText(text: String) {
    Text(
        text = text,
        color = Ash,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(vertical = 10.dp),
    )
}

@Composable
private fun RecipeDetailGenerateButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(8.dp, RoundedCornerShape(999.dp), clip = false)
            .clip(RoundedCornerShape(999.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF1E8749), Color(0xFF0A6B37))))
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_menu_image),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(20.dp))
        Text(
            text = "生成菜谱图",
            color = Color.White,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 25.sp,
        )
        Spacer(Modifier.weight(1f))
        Image(
            painter = painterResource(R.drawable.ic_home_recent_spark),
            contentDescription = null,
            modifier = Modifier.size(25.dp),
        )
    }
}

private data class RecipeDetailStatSpec(
    val iconRes: Int,
    val value: String,
    val unit: String,
    val label: String,
    val stars: Boolean = false,
    val compact: Boolean = false,
)

private fun recipeDetailProficiencyText(value: Int): String {
    val labels = listOf("未设置", "刚会", "能做", "顺手", "拿手", "招牌")
    return labels.getOrElse(value.coerceIn(0, 5)) { "未设置" }
}

private fun recipeDetailDifficultyText(value: Int): String {
    val labels = listOf("未设置", "简单", "中等", "偏难", "困难", "地狱")
    return labels.getOrElse(value.coerceIn(0, 5)) { "未设置" }
}

private fun recipeDetailDifficultyStars(value: Int): String {
    val count = value.coerceIn(0, 5)
    return if (count > 0) "★".repeat(count) else "未设置"
}

private fun recipeDetailTasteText(recipe: Recipe): String =
    recipe.tasteTags.filter { it.isNotBlank() }.joinToString("、").ifBlank { "-" }

private fun recipeDetailIngredientText(ingredient: com.example.signaturemenuapp.data.Ingredient): String {
    val amount = "${ingredient.amount}${ingredient.unit}".trim()
    return if (amount.isBlank()) ingredient.name else "${ingredient.name} $amount"
}

private fun recipeDetailStepText(step: com.example.signaturemenuapp.data.RecipeStep): String =
    step.title.ifBlank { step.description.ifBlank { "未命名步骤" } }

@Composable
internal fun RecipeEditorScreen(
    recipe: Recipe?,
    onBack: () -> Unit,
    onSave: (Recipe) -> Unit,
) {
    var name by remember(recipe?.id) { mutableStateOf(recipe?.name.orEmpty()) }
    var method by remember(recipe?.id) { mutableStateOf(recipe?.cookingMethod.orEmpty()) }
    var serving by remember(recipe?.id) { mutableStateOf((recipe?.servingCount ?: 2).toString()) }
    var minutes by remember(recipe?.id) { mutableStateOf((recipe?.estimatedMinutes ?: 30).toString()) }
    var cookedCount by remember(recipe?.id) { mutableStateOf((recipe?.cookedCount ?: 0).toString()) }
    var difficulty by remember(recipe?.id) { mutableStateOf(recipe?.difficulty ?: 0) }
    var proficiency by remember(recipe?.id) { mutableStateOf(recipe?.proficiency ?: 0) }
    var tags by remember(recipe?.id) { mutableStateOf(recipe?.tasteTags?.joinToString("、").orEmpty()) }
    var priceRange by remember(recipe?.id) { mutableStateOf(recipe?.priceRange.orEmpty()) }
    var note by remember(recipe?.id) { mutableStateOf(recipe?.privateNote.orEmpty()) }
    val available = recipe?.isAvailable ?: true
    var ingredients by remember(recipe?.id) {
        mutableStateOf(
            recipe?.ingredients
                ?.map { listOf(it.name, it.amount, it.unit, it.note).filter(String::isNotBlank).joinToString(" ") }
                ?.ifEmpty { listOf("") }
                ?: listOf(""),
        )
    }
    var steps by remember(recipe?.id) {
        mutableStateOf(
            recipe?.steps
                ?.map { step -> listOf(step.title, step.description).filter(String::isNotBlank).joinToString("：") }
                ?.ifEmpty { listOf("", "") }
                ?: listOf("", ""),
        )
    }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        RecipeFormTopBar(title = if (recipe == null) "新增菜谱" else "编辑菜谱", onBack = onBack)
        Image(
            painter = painterResource(R.drawable.recipe_form_hero_new),
            contentDescription = if (recipe == null) "记一道新菜，会做什么，慢慢补全" else "编辑菜谱",
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .align(Alignment.CenterHorizontally)
                .padding(top = 2.dp),
            contentScale = ContentScale.FillWidth,
        )

        RecipeNameInput(value = name, onValueChange = { name = it })

        RecipeFormSectionTitle("做法", modifier = Modifier.padding(top = 16.dp))
        RecipeMethodPicker(selected = method, onSelect = { method = it })

        RecipeFormSectionTitle("食材清单", modifier = Modifier.padding(top = 15.dp))
        IngredientBoard(
            ingredients = ingredients,
            onChange = { index, value ->
                ingredients = ingredients.toMutableList().also { it[index] = value }
            },
            onAdd = { ingredients = ingredients + "" },
        )

        RecipeFormSectionTitle("步骤", modifier = Modifier.padding(top = 16.dp))
        Text(
            text = "左滑某一步，露出删除按钮。",
            color = Color(0xFF163F2E).copy(alpha = 0.58f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 16.sp,
            modifier = Modifier.padding(top = 5.dp),
        )
        StepEditorList(
            steps = steps,
            onChange = { index, value -> steps = steps.toMutableList().also { it[index] = value } },
            onDelete = { index ->
                steps = if (steps.size <= 1) {
                    listOf("")
                } else {
                    steps.toMutableList().also { it.removeAt(index) }
                }
            },
            onAdd = { steps = steps + "" },
        )

        RecipeSummaryStrip(
            minutes = minutes,
            serving = serving,
            cookedCount = cookedCount,
            onMinutesChange = { minutes = it.onlyDigits() },
            onServingChange = { serving = it.onlyDigits() },
            onCookedCountChange = { cookedCount = it.onlyDigits() },
        )

        RecipeExtraMeta(
            difficulty = difficulty,
            proficiency = proficiency,
            priceRange = priceRange,
            tags = tags,
            note = note,
            onDifficultyChange = { difficulty = if (difficulty == it) 0 else it },
            onProficiencyChange = { proficiency = if (proficiency == it) 0 else it },
            onPriceRangeChange = { priceRange = it },
            onTagsChange = { tags = it },
            onNoteChange = { note = it },
        )

        if (error.isNotBlank()) Notice(error, tone = "error")

        RecipeSaveButton(
            onClick = {
                if (name.trim().isBlank()) {
                    error = "菜名不能为空。"
                    return@RecipeSaveButton
                }
                if (method.isBlank()) {
                    error = "请选择做法。"
                    return@RecipeSaveButton
                }
                val ingredientText = ingredients.joinToString("\n") { it.trim() }
                val stepText = steps.joinToString("\n") { it.trim() }
                val saved = (recipe ?: Recipe(id = newId(), createdAt = nowIso())).copy(
                    name = name.trim(),
                    description = recipe?.description.orEmpty(),
                    cookingMethod = method,
                    servingCount = serving.toIntOrNull()?.coerceIn(0, 99) ?: 2,
                    estimatedMinutes = minutes.toIntOrNull()?.coerceIn(0, 24 * 60) ?: 20,
                    difficulty = difficulty.coerceIn(0, 5),
                    proficiency = proficiency.coerceIn(0, 5),
                    tasteTags = parseTags(tags).ifEmpty { listOf(method) },
                    isAvailable = available,
                    priceRange = priceRange.trim(),
                    cookedCount = cookedCount.toIntOrNull()?.coerceIn(0, 9999) ?: 0,
                    privateNote = note.trim(),
                    ingredients = parseIngredients(ingredientText),
                    steps = parseSteps(stepText),
                    updatedAt = nowIso(),
                )
                onSave(saved)
            },
            modifier = Modifier.padding(top = 16.dp),
        )
        Spacer(Modifier.height(34.dp))
    }
}

@Composable
private fun RecipeFormTopBar(title: String, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(40.dp)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_recipe_back),
                contentDescription = "返回",
                tint = Color(0xFF051512),
                modifier = Modifier.size(28.dp),
            )
        }
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                color = Color(0xFF101613),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 22.sp,
            )
            Canvas(
                modifier = Modifier
                    .width(79.dp)
                    .height(8.dp),
            ) {
                drawLine(
                    color = Color(0xFF4CA36F),
                    start = Offset(0f, size.height * 0.70f),
                    end = Offset(size.width, size.height * 0.34f),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun RecipeNameInput(value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Paper.copy(alpha = 0.76f))
            .border(1.6.dp, Color(0xFF06432B), RoundedCornerShape(12.dp))
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "菜名",
            color = Color(0xFF151817),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 20.sp,
            modifier = Modifier.width(48.dp),
        )
        RecipePlainInput(
            value = value,
            onValueChange = onValueChange,
            placeholder = "例如：红烧排骨",
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(R.drawable.ic_recipe_edit),
            contentDescription = null,
            tint = Forest,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun RecipeFormSectionTitle(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = Color(0xFF151817),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 22.sp,
        )
        TitleBurst(Modifier.padding(start = 2.dp))
    }
}

@Composable
private fun TitleBurst(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .width(22.dp)
            .height(19.dp),
    ) {
        val orange = Color(0xFFF47B17)
        drawLine(orange, Offset(size.width * 0.30f, size.height * 0.18f), Offset(size.width * 0.14f, size.height * 0.66f), 2.2.dp.toPx(), StrokeCap.Round)
        drawLine(orange, Offset(size.width * 0.62f, size.height * 0.28f), Offset(size.width * 0.84f, size.height * 0.12f), 2.dp.toPx(), StrokeCap.Round)
        drawLine(orange, Offset(size.width * 0.60f, size.height * 0.66f), Offset(size.width * 0.86f, size.height * 0.80f), 2.dp.toPx(), StrokeCap.Round)
    }
}

@Composable
private fun RecipeMethodPicker(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = 6.dp, bottom = 8.dp, start = 2.dp, end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        CookingMethodOptions.forEach { option ->
            Box(
                modifier = Modifier
                    .size(width = 58.dp, height = 66.dp)
                    .clickable { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                MethodSticker(
                    method = option,
                    modifier = Modifier.size(62.dp),
                )
                if (selected == option) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(17.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0A613F))
                            .border(2.dp, Paper, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_menu_check),
                            contentDescription = null,
                            tint = Paper,
                            modifier = Modifier.size(11.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IngredientBoard(
    ingredients: List<String>,
    onChange: (Int, String) -> Unit,
    onAdd: () -> Unit,
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFEFF7EB).copy(alpha = 0.56f))
            .border(1.5.dp, Color(0xFF06432B), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ingredients.forEachIndexed { index, ingredient ->
            IngredientChip(
                value = ingredient,
                placeholder = if (index == 0) "例如：排骨" else "食材",
                onValueChange = { onChange(index, it) },
            )
        }
        DashedIconButton(
            width = 92.dp,
            height = 40.dp,
            radius = 12.dp,
            color = Color(0xFFB6CDB3),
            onClick = onAdd,
        )
    }
}

@Composable
private fun IngredientChip(value: String, placeholder: String, onValueChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .width(if (value.length > 5) 140.dp else 118.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFEFF7EB))
            .border(1.5.dp, Color(0xFF5A6E5A), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        RecipePlainInput(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            textAlignCenter = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DashedIconButton(
    width: Dp,
    height: Dp,
    radius: Dp,
    color: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(radius))
            .dashedBorder(1.5.dp, color, radius)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_recipe_add),
            contentDescription = null,
            tint = Color(0xFF07110E),
            modifier = Modifier.size(25.dp),
        )
    }
}

@Composable
private fun StepEditorList(
    steps: List<String>,
    onChange: (Int, String) -> Unit,
    onDelete: (Int) -> Unit,
    onAdd: () -> Unit,
) {
    var openStepIndex by remember(steps.size) { mutableStateOf<Int?>(null) }
    Column(modifier = Modifier.padding(top = 10.dp)) {
        Box {
            Canvas(Modifier.matchParentSize()) {
                if (steps.size > 1) {
                    drawLine(
                        color = Color(0xFF0A613F),
                        start = Offset(27.dp.toPx(), 39.dp.toPx()),
                        end = Offset(27.dp.toPx(), size.height - 19.dp.toPx()),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 8f), 0f),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                steps.forEachIndexed { index, step ->
                    key(steps.size, index) {
                        StepSwipeRow(
                            number = index + 1,
                            value = step,
                            placeholder = when (index) {
                                0 -> "例如：焯水去浮沫"
                                1 -> "例如：炒糖色"
                                else -> "步骤 ${index + 1}"
                            },
                            isOpen = openStepIndex == index,
                            onOpen = { openStepIndex = index },
                            onClose = {
                                if (openStepIndex == index) {
                                    openStepIndex = null
                                }
                            },
                            onValueChange = { onChange(index, it) },
                            onDelete = {
                                openStepIndex = null
                                onDelete(index)
                            },
                        )
                    }
                }
            }
        }
        AddStepRow(onClick = onAdd)
    }
}

@Composable
private fun StepSwipeRow(
    number: Int,
    value: String,
    placeholder: String,
    isOpen: Boolean,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    onValueChange: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val density = LocalDensity.current
    val revealPx = with(density) { StepDeleteRevealWidth.toPx() }
    var offsetPx by remember(number) { mutableStateOf(0f) }

    LaunchedEffect(isOpen, revealPx) {
        offsetPx = if (isOpen) -revealPx else 0f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(12.dp)),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(StepDeleteActionWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                .background(Color(0xFFDF533F))
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_menu_delete),
                contentDescription = "删除步骤 $number",
                tint = Paper,
                modifier = Modifier.size(26.dp),
            )
        }
        StepInputRow(
            number = number,
            value = value,
            placeholder = placeholder,
            onValueChange = onValueChange,
            modifier = Modifier
                .offset { IntOffset(offsetPx.roundToInt(), 0) }
                .pointerInput(number, isOpen) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetPx = (offsetPx + dragAmount).coerceIn(-revealPx, 0f)
                        },
                        onDragEnd = {
                            if (offsetPx <= -revealPx * 0.58f) {
                                offsetPx = -revealPx
                                onOpen()
                            } else {
                                offsetPx = 0f
                                onClose()
                            }
                        },
                        onDragCancel = {
                            if (isOpen) {
                                offsetPx = -revealPx
                            } else {
                                offsetPx = 0f
                            }
                        },
                    )
                },
        )
    }
}

@Composable
private fun StepInputRow(
    number: Int,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.5.dp, Color(0xFF06432B), RoundedCornerShape(12.dp))
            .padding(start = 5.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 42.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Color(0xFFEEF8ED))
                .border(1.5.dp, Color(0xFFB8D0B8), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString().padStart(2, '0'),
                color = Color(0xFF0A613F),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp,
            )
        }
        RecipePlainInput(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        )
    }
}

private val StepDeleteRevealWidth = 78.dp
private val StepDeleteActionWidth = 88.dp

@Composable
private fun AddStepRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 11.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .dashedBorder(1.5.dp, Color(0xFF0A613F), 14.dp)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .border(2.dp, Color(0xFF0A613F), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_recipe_add),
                contentDescription = null,
                tint = Color(0xFF0A613F),
                modifier = Modifier.size(21.dp),
            )
        }
        Spacer(Modifier.width(9.dp))
        Text("添加一步", color = Color(0xFF0A613F), fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RecipeSummaryStrip(
    minutes: String,
    serving: String,
    cookedCount: String,
    onMinutesChange: (String) -> Unit,
    onServingChange: (String) -> Unit,
    onCookedCountChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .height(76.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Paper.copy(alpha = 0.75f))
            .border(1.5.dp, Color(0xFF06432B), RoundedCornerShape(14.dp)),
    ) {
        SummaryBox(
            iconRes = R.drawable.ic_recipe_clock,
            value = minutes,
            unit = "分钟",
            label = "预计时间",
            onValueChange = onMinutesChange,
            modifier = Modifier.weight(1f),
        )
        SummaryDivider()
        SummaryBox(
            iconRes = R.drawable.ic_menu_user,
            value = serving,
            unit = "人",
            label = "适用人数",
            onValueChange = onServingChange,
            modifier = Modifier.weight(1f),
        )
        SummaryDivider()
        SummaryBox(
            iconRes = R.drawable.ic_recipe_loop,
            value = cookedCount,
            unit = "次",
            label = "做过次数",
            onValueChange = onCookedCountChange,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryBox(
    @androidx.annotation.DrawableRes iconRes: Int,
    value: String,
    unit: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(painterResource(iconRes), contentDescription = null, tint = Forest, modifier = Modifier.size(24.dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 3.dp),
        ) {
            RecipePlainInput(
                value = value,
                onValueChange = onValueChange,
                placeholder = "0",
                keyboardType = KeyboardType.Number,
                textAlignCenter = true,
                modifier = Modifier.width(((value.length.coerceAtLeast(2) * 11) + 10).dp),
            )
            Text(unit, color = Color(0xFF0C1110), fontSize = 17.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
        }
        Text(label, color = Color(0xFF686866), fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 15.sp)
    }
}

@Composable
private fun SummaryDivider() {
    Box(
        modifier = Modifier
            .width(1.5.dp)
            .fillMaxHeight()
            .background(Color(0xFFD5D0C0)),
    )
}

@Composable
private fun RecipeExtraMeta(
    difficulty: Int,
    proficiency: Int,
    priceRange: String,
    tags: String,
    note: String,
    onDifficultyChange: (Int) -> Unit,
    onProficiencyChange: (Int) -> Unit,
    onPriceRangeChange: (String) -> Unit,
    onTagsChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier.padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RatingField(
                title = "难度",
                value = difficulty,
                display = difficultyLevelText(difficulty),
                onChange = onDifficultyChange,
                modifier = Modifier.weight(1f),
            )
            RatingField(
                title = "熟练度",
                value = proficiency,
                display = proficiencyLevelText(proficiency),
                onChange = onProficiencyChange,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ExtraInputField(
                title = "价格范围",
                iconRes = R.drawable.ic_recipe_money,
                value = priceRange,
                placeholder = "例如：20-30 元",
                onValueChange = onPriceRangeChange,
                modifier = Modifier.weight(1f),
            )
            ExtraInputField(
                title = "口味标签",
                iconRes = R.drawable.ic_recipe_label,
                value = tags,
                placeholder = "例如：清淡、麻辣",
                onValueChange = onTagsChange,
                modifier = Modifier.weight(1f),
            )
        }
        NoteField(value = note, onValueChange = onNoteChange)
    }
}

@Composable
private fun RatingField(
    title: String,
    value: Int,
    display: String,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(104.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Paper.copy(alpha = 0.70f))
            .border(1.5.dp, Color(0xFF06432B).copy(alpha = 0.82f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ExtraFieldLabel(title = title, iconRes = R.drawable.ic_recipe_star)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (1..5).forEach { level ->
                val active = level <= value
                Box(
                    modifier = Modifier
                        .size(27.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) Color(0xFFFFF1D6).copy(alpha = 0.82f) else Color(0xFFEFF7EB).copy(alpha = 0.72f))
                        .clickable { onChange(level) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_recipe_star),
                        contentDescription = null,
                        tint = if (active) Color(0xFFF47B17) else Color(0xFFB8B1A3),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        Text(display, color = Color(0xFF686866), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 15.sp)
    }
}

@Composable
private fun ExtraInputField(
    title: String,
    @androidx.annotation.DrawableRes iconRes: Int,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(84.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Paper.copy(alpha = 0.70f))
            .border(1.5.dp, Color(0xFF06432B).copy(alpha = 0.82f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ExtraFieldLabel(title = title, iconRes = iconRes)
        RecipePlainInput(value = value, onValueChange = onValueChange, placeholder = placeholder, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun NoteField(value: String, onValueChange: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(108.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Paper.copy(alpha = 0.70f))
            .border(1.5.dp, Color(0xFF06432B).copy(alpha = 0.82f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ExtraFieldLabel(title = "用户私密备注", iconRes = R.drawable.ic_recipe_info)
        RecipePlainInput(
            value = value,
            onValueChange = onValueChange,
            placeholder = "例如：下次少放盐，妈妈更喜欢软一点",
            singleLine = false,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ExtraFieldLabel(title: String, @androidx.annotation.DrawableRes iconRes: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(iconRes), contentDescription = null, tint = Forest, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(title, color = Forest, fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp)
    }
}

@Composable
private fun RecipeSaveButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(15.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(7.dp, shape, clip = false)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0B7047), Color(0xFF06432B), Color(0xFF04371F)),
                ),
            )
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
            Icon(painterResource(R.drawable.ic_recipe_check), contentDescription = null, tint = Paper, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text("保存菜谱", color = Paper, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp)
    }
}

@Composable
private fun RecipePlainInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    textAlignCenter: Boolean = false,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        textStyle = TextStyle(
            color = Color(0xFF151817),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 21.sp,
            textAlign = if (textAlignCenter) androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Start,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        cursorBrush = SolidColor(Forest),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Box(contentAlignment = if (textAlignCenter) Alignment.Center else Alignment.CenterStart) {
                if (value.isBlank()) {
                    Text(
                        text = placeholder,
                        color = Color(0xFF163F2E).copy(alpha = 0.36f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 21.sp,
                        maxLines = if (singleLine) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                innerTextField()
            }
        },
    )
}

private fun Modifier.dashedBorder(width: Dp, color: Color, radius: Dp): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(radius.toPx(), radius.toPx()),
        style = Stroke(
            width = width.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx()), 0f),
        ),
    )
}

private fun difficultyLevelText(value: Int): String {
    val labels = listOf("未设置", "简单", "中等", "偏难", "困难", "地狱")
    return labels[value.coerceIn(0, 5)]
}

private fun proficiencyLevelText(value: Int): String {
    val labels = listOf("未设置", "刚会", "能做", "顺手", "拿手", "招牌")
    return labels[value.coerceIn(0, 5)]
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
