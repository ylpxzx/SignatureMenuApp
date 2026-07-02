package com.example.signaturemenuapp.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.signaturemenuapp.R

internal enum class MainTab(val label: String, @DrawableRes val iconRes: Int) {
    Home("首页", R.drawable.ic_nav_home),
    Recipes("菜谱", R.drawable.ic_nav_recipes),
    Menu("挑菜", R.drawable.ic_nav_menu),
    Archive("菜单", R.drawable.ic_nav_orders),
    Settings("我的", R.drawable.ic_nav_settings),
}

internal sealed class Screen {
    data class Main(val tab: MainTab) : Screen()
    data class RecipeDetail(val recipeId: String) : Screen()
    data class RecipeEditor(val recipeId: String?) : Screen()
    data class MenuEditor(val menuId: String?) : Screen()
}

@Composable
internal fun BottomTabs(current: MainTab, onSelect: (MainTab) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .background(Color(0xF5FFFEF9)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Ink.copy(alpha = 0.12f)),
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 23.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            MainTab.values().forEach { tab ->
                val selected = current == tab
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelect(tab) }
                        .background(if (selected) Mint else Color.Transparent)
                        .padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        painter = painterResource(tab.iconRes),
                        contentDescription = null,
                        tint = if (selected) Forest else Ink,
                        modifier = Modifier.size(25.dp),
                    )
                    Text(
                        text = tab.label,
                        color = if (selected) Forest else Ink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}
