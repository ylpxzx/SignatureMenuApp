@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.signaturemenuapp.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.signaturemenuapp.R
import com.example.signaturemenuapp.data.Ingredient
import com.example.signaturemenuapp.data.MenuRecord
import com.example.signaturemenuapp.data.MenuStatus
import com.example.signaturemenuapp.data.Recipe
import com.example.signaturemenuapp.data.RecipeStep
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
internal fun PageHeader(title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Muted, fontSize = 13.sp)
        }
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Mint),
            contentAlignment = Alignment.Center,
        ) {
            Text("S.", color = Forest, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun BackBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
            Text("‹ 返回", color = Forest, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            title,
            modifier = Modifier.weight(1f),
            color = Ink,
            textAlign = TextAlign.Center,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(58.dp))
    }
}

@Composable
internal fun SectionTitle(
    title: String,
    caption: String? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        caption?.let {
            Spacer(Modifier.width(8.dp))
            Text(it, color = Ginger, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.weight(1f))
        if (action != null && onAction != null) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 2.dp)) {
                Text(action, color = Forest, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
internal fun CardShell(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Paper.copy(alpha = 0.82f))
            .border(1.dp, Line.copy(alpha = 0.9f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
internal fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(76.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Paper)
            .border(1.dp, Forest.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(value, color = Forest, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun PrimaryAction(title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Forest, contentColor = Paper),
        shape = RoundedCornerShape(17.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (subtitle.isNotBlank()) {
                Text(subtitle, fontSize = 11.sp, color = Paper.copy(alpha = 0.78f))
            }
        }
    }
}

@Composable
internal fun SecondaryAction(title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        border = BorderStroke(1.4.dp, Leaf.copy(alpha = 0.55f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Forest),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.width(8.dp))
            Text(subtitle, fontSize = 11.sp, color = Ash)
        }
    }
}

@Composable
internal fun RecipeRow(recipe: Recipe, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(Paper.copy(alpha = 0.82f))
            .border(1.dp, Line.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MethodSticker(recipe.cookingMethod, Modifier.size(66.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    recipe.name,
                    modifier = Modifier.weight(1f),
                    color = Ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    proficiencyText(recipe.proficiency),
                    color = Forest,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(Mint)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
            Text(
                "${recipe.steps.size} 步 · ${recipe.estimatedMinutes.takeIf { it > 0 } ?: "-"} 分钟 · ${recipe.servingCount.takeIf { it > 0 } ?: "-"} 人 · 做过 ${recipe.cookedCount} 次",
                color = Ash,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                recipe.tasteTags.take(3).forEach { Tag(it) }
            }
        }
    }
}

@Composable
internal fun SelectRecipeRow(recipe: Recipe, selected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle)
            .background(if (selected) Color(0xFFFFFAF0) else Paper.copy(alpha = 0.74f))
            .border(1.dp, if (selected) Ginger.copy(alpha = 0.55f) else Line.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
            .padding(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MethodSticker(recipe.cookingMethod, Modifier.size(52.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(recipe.name, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${recipe.estimatedMinutes} 分钟 · ${recipe.servingCount} 人", color = Ash, fontSize = 12.sp)
        }
        Checkbox(checked = selected, onCheckedChange = { onToggle() })
    }
}

@Composable
internal fun MenuCard(
    menu: MenuRecord,
    recipes: List<Recipe>,
    actions: @Composable RowScope.() -> Unit,
) {
    val names = menu.recipeIds.mapNotNull { recipeId ->
        recipes.firstOrNull { it.id == recipeId }?.name
            ?: menu.dishes.firstOrNull { it.recipeId == recipeId }?.name
    }.filter { it.isNotBlank() }

    Card(
        colors = CardDefaults.cardColors(containerColor = Paper.copy(alpha = 0.82f)),
        border = BorderStroke(1.dp, Line.copy(alpha = 0.88f)),
        shape = RoundedCornerShape(13.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(menu.title, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("${formatDate(menu.dateKey)} ${menu.time} · ${menu.dinerCount} 人", color = Ash, fontSize = 12.sp)
                }
                StatusPill(menu.status)
            }
            Text(menu.note.ifBlank { "没有备注" }, color = Muted, fontSize = 12.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                names.take(5).forEach { Tag(it) }
                if (names.size > 5) Tag("+${names.size - 5}")
            }
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                actions()
            }
        }
    }
}

@Composable
internal fun MethodSticker(method: String, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(methodStickerRes(method)),
        contentDescription = "$method 做法",
        modifier = modifier.aspectRatio(1f),
        contentScale = ContentScale.Fit,
    )
}

@Composable
internal fun StatusPill(status: MenuStatus) {
    val served = status == MenuStatus.Served
    Text(
        text = if (served) "已出餐" else "待出餐",
        color = if (served) Forest else Ginger,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (served) Mint else Color(0xFFFFF0D8))
            .padding(horizontal = 9.dp, vertical = 6.dp),
    )
}

@Composable
internal fun Tag(text: String) {
    Text(
        text = text,
        color = Forest,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(Mint)
            .padding(horizontal = 8.dp, vertical = 5.dp),
    )
}

@Composable
internal fun SmallChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .background(if (selected) Paper else Color(0xFFEFF5EF))
            .border(1.dp, if (selected) Leaf.copy(alpha = 0.45f) else Forest.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (selected) Forest else Ash,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(placeholder, color = Muted) },
        leadingIcon = { Text("⌕", color = Ash, fontSize = 22.sp) },
        shape = RoundedCornerShape(13.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Paper.copy(alpha = 0.78f),
            unfocusedContainerColor = Paper.copy(alpha = 0.72f),
            focusedBorderColor = Leaf.copy(alpha = 0.55f),
            unfocusedBorderColor = Line,
        ),
    )
}

@Composable
internal fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = Ash, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            minLines = minLines,
            placeholder = { Text(placeholder, color = Muted) },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(11.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFFBFDF7),
                unfocusedContainerColor = Color(0xFFFBFDF7),
                focusedBorderColor = Leaf.copy(alpha = 0.55f),
                unfocusedBorderColor = Forest.copy(alpha = 0.10f),
            ),
        )
    }
}

@Composable
internal fun SettingsRow(title: String, value: String, icon: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(Paper.copy(alpha = 0.78f))
            .border(1.dp, Line.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Mint),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, color = Forest, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(value, color = Ash, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("›", color = Forest, fontSize = 30.sp, fontWeight = FontWeight.Light)
    }
}

@Composable
internal fun StepCard(step: RecipeStep) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Paper)
            .border(1.dp, Forest.copy(alpha = 0.10f), RoundedCornerShape(13.dp))
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFFFB85C)),
            contentAlignment = Alignment.Center,
        ) {
            Text(step.order.toString().padStart(2, '0'), color = Forest, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(step.title.ifBlank { "步骤 ${step.order}" }, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(step.description, color = Ash, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
internal fun EmptyBox(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Paper.copy(alpha = 0.64f))
            .border(1.dp, Forest.copy(alpha = 0.14f), RoundedCornerShape(13.dp))
            .padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun Notice(text: String, tone: String) {
    val error = tone == "error"
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (error) Color(0xFFFFF0EF) else Mint.copy(alpha = 0.7f))
            .border(1.dp, if (error) Tomato.copy(alpha = 0.34f) else Leaf.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Text(text, color = if (error) Tomato else Forest, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@DrawableRes
private fun methodStickerRes(method: String): Int {
    val value = method.trim()
    METHOD_STICKER_RES[value]?.let { return it }
    val inferredMethod = METHOD_KEYWORDS.firstOrNull { (_, keywords) ->
        keywords.any { keyword -> value.contains(keyword, ignoreCase = true) }
    }?.first ?: "炒"
    return METHOD_STICKER_RES[inferredMethod] ?: R.drawable.method_chao
}

private val METHOD_STICKER_RES = mapOf(
    "煎" to R.drawable.method_jian,
    "炒" to R.drawable.method_chao,
    "烹" to R.drawable.method_peng,
    "炸" to R.drawable.method_zha,
    "爆" to R.drawable.method_bao,
    "熘" to R.drawable.method_liu,
    "贴" to R.drawable.method_tie,
    "烧" to R.drawable.method_shao,
    "焖" to R.drawable.method_men,
    "炖" to R.drawable.method_dun,
    "蒸" to R.drawable.method_zheng,
    "汆" to R.drawable.method_cuan,
    "煮" to R.drawable.method_zhu,
    "烩" to R.drawable.method_hui,
    "炝" to R.drawable.method_qiang,
    "拌" to R.drawable.method_ban,
    "腌" to R.drawable.method_yan,
    "烤" to R.drawable.method_kao,
    "卤" to R.drawable.method_lu,
    "冻" to R.drawable.method_dong,
    "熏" to R.drawable.method_xun,
    "卷" to R.drawable.method_juan,
    "滑" to R.drawable.method_hua,
    "焗" to R.drawable.method_ju,
    "汤" to R.drawable.method_tang,
)

private val METHOD_KEYWORDS = listOf(
    "汤" to listOf("汤", "羹", "煲汤", "丸子汤"),
    "蒸" to listOf("蒸", "清蒸", "上锅", "蒸锅"),
    "炒" to listOf("炒", "爆炒", "小炒", "快炒", "炒蛋"),
    "烹" to listOf("烹", "烹汁"),
    "炸" to listOf("炸", "油炸", "干炸"),
    "爆" to listOf("爆", "爆炒", "葱爆"),
    "熘" to listOf("熘", "溜", "醋溜"),
    "贴" to listOf("贴", "锅贴"),
    "烧" to listOf("烧", "红烧", "照烧"),
    "焖" to listOf("焖", "油焖", "焖烧"),
    "拌" to listOf("拌", "凉拌", "葱油", "沙拉"),
    "煎" to listOf("煎", "锅贴", "煎蛋", "烙"),
    "汆" to listOf("汆", "氽"),
    "煮" to listOf("煮", "水煮", "白煮"),
    "烩" to listOf("烩"),
    "炝" to listOf("炝"),
    "腌" to listOf("腌", "腌制"),
    "烤" to listOf("烤", "空气炸锅"),
    "卤" to listOf("卤", "卤味"),
    "冻" to listOf("冻", "冷冻", "冻糕"),
    "熏" to listOf("熏", "烟熏"),
    "卷" to listOf("卷", "菜卷"),
    "滑" to listOf("滑", "滑蛋", "滑炒"),
    "焗" to listOf("焗", "芝士焗"),
    "炖" to listOf("炖", "煲", "牛腩"),
)

internal fun proficiencyText(value: Int): String = when (value.coerceIn(1, 5)) {
    1 -> "刚会"
    2 -> "能做"
    3 -> "顺手"
    4 -> "拿手"
    else -> "招牌"
}

private fun formatDate(dateKey: String): String = runCatching {
    LocalDate.parse(dateKey).format(DateTimeFormatter.ofPattern("M月d日"))
}.getOrElse { dateKey }

internal fun String.onlyDigits(): String = filter { it.isDigit() }.take(4)

internal fun parseTags(value: String): List<String> = value
    .split("、", ",", "，", " ")
    .map { it.trim() }
    .filter { it.isNotBlank() }
    .distinct()
    .take(8)

internal fun parseIngredients(value: String): List<Ingredient> = value
    .lineSequence()
    .map { it.trim() }
    .filter { it.isNotBlank() }
    .map { line ->
        val parts = line.split(Regex("\\s+")).filter { it.isNotBlank() }
        Ingredient(
            name = parts.getOrNull(0).orEmpty(),
            amount = parts.getOrNull(1).orEmpty(),
            unit = parts.getOrNull(2).orEmpty(),
            note = parts.drop(3).joinToString(" "),
        )
    }
    .filter { it.name.isNotBlank() }
    .toList()

internal fun parseSteps(value: String): List<RecipeStep> {
    val rows = value.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
    return rows.mapIndexed { index, line ->
        val pieces = line.split("：", ":", limit = 2)
        RecipeStep(
            order = index + 1,
            title = pieces.getOrNull(0)?.trim().orEmpty().ifBlank { "步骤 ${index + 1}" },
            description = pieces.getOrNull(1)?.trim() ?: line,
            estimatedMinutes = 0,
        )
    }
}
