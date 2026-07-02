package com.example.signaturemenuapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.signaturemenuapp.R
import com.example.signaturemenuapp.data.Recipe
import com.example.signaturemenuapp.data.SignatureMenuData

@Composable
internal fun HomeScreen(
    data: SignatureMenuData,
    contentPadding: PaddingValues,
    onAddRecipe: () -> Unit,
    onPlanMenu: () -> Unit,
    onOpenRecipe: (String) -> Unit,
) {
    val recent = data.recipes.sortedByDescending { it.updatedAt }.take(5)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(
            top = 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 18.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFEDF8EF)),
            ) {
                Image(
                    painter = painterResource(R.drawable.home_header_hd),
                    contentDescription = "会做的菜，随手记一下。挑几道发给朋友选。",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth,
                )
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HomePrimaryAction(onClick = onPlanMenu)
                HomeSecondaryAction(onClick = onAddRecipe)
            }
        }

        item {
            HomeSectionHead()
        }

        if (recent.isEmpty()) {
            item { EmptyBox("还没有菜谱，先记一道最常做的菜。") }
        } else {
            items(recent, key = { it.id }) { recipe ->
                HomeRecipeRow(recipe = recipe, onClick = { onOpenRecipe(recipe.id) })
            }
        }
    }
}

@Composable
private fun HomePrimaryAction(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(Color(0xFF06432B))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "挑几道给大家选",
            color = Color(0xFFFFF8E8),
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.width(10.dp))
        Image(
            painter = painterResource(R.drawable.ic_home_paper_plane_white),
            contentDescription = null,
            modifier = Modifier
                .width(54.dp)
                .height(32.dp),
        )
    }
}

@Composable
private fun HomeSecondaryAction(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(Color(0xFFFFFDF7))
            .border(2.dp, Color(0xFF7DBB9C), RoundedCornerShape(17.dp))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_home_add),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            colorFilter = ColorFilter.tint(Color(0xFF0B5A3B)),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "记一道新菜",
            color = Color(0xFF0B5A3B),
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
        )
    }
}

@Composable
private fun HomeSectionHead() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, top = 16.dp, end = 10.dp, bottom = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "最近很顺手",
                color = Ink,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.width(7.dp))
            Image(
                painter = painterResource(R.drawable.ic_home_recent_spark),
                contentDescription = null,
                modifier = Modifier
                    .width(30.dp)
                    .height(20.dp),
            )
        }
        Image(
            painter = painterResource(R.drawable.ic_home_star),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun HomeRecipeRow(recipe: Recipe, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(Paper)
            .border(BorderStroke(1.dp, Color(0xFFB0BBB7)), RoundedCornerShape(13.dp))
            .clickable(onClick = onClick)
            .padding(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MethodSticker(recipe.cookingMethod, Modifier.size(58.dp))
        Spacer(Modifier.width(11.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = recipe.name,
                color = Ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = recipeHomeMeta(recipe),
                color = Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = "做过${recipe.cookedCount}次 · 难度${difficultyStars(recipe.difficulty)}",
                color = Ash,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .height(28.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0xFFFFF3D8))
                .padding(horizontal = 13.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = proficiencyText(recipe.proficiency),
                color = Color(0xFFF47B17),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
        }
    }
}

private fun recipeHomeMeta(recipe: Recipe): String {
    val minutes = if (recipe.estimatedMinutes > 0) "${recipe.estimatedMinutes} 分钟" else "未估时"
    return "${recipe.ingredients.size} 种食材 · $minutes · ${recipe.steps.size} 步"
}

private fun difficultyStars(value: Int): String {
    val count = value.coerceIn(0, 5)
    return if (count > 0) "★".repeat(count) else "未设"
}
