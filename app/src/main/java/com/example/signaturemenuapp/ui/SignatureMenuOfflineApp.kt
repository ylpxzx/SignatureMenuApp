package com.example.signaturemenuapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.signaturemenuapp.data.SignatureMenuData
import com.example.signaturemenuapp.data.SignatureMenuStore
import com.example.signaturemenuapp.data.deleteMenu
import com.example.signaturemenuapp.data.deleteRecipe
import com.example.signaturemenuapp.data.updateMenuStatus
import com.example.signaturemenuapp.data.upsertMenu
import com.example.signaturemenuapp.data.upsertRecipe

@Composable
fun SignatureMenuOfflineApp() {
    val context = LocalContext.current
    val store = remember { SignatureMenuStore(context.applicationContext) }
    var data by remember { mutableStateOf(store.load()) }
    var screen by remember { mutableStateOf<Screen>(Screen.Main(MainTab.Home)) }

    fun persist(next: SignatureMenuData) {
        data = next
        store.save(next)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Paper, Rice, Color(0xFFEFF8F2)),
                ),
            ),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                val current = screen as? Screen.Main
                if (current != null) {
                    BottomTabs(
                        current = current.tab,
                        onSelect = { tab -> screen = Screen.Main(tab) },
                    )
                }
            },
        ) { innerPadding ->
            when (val currentScreen = screen) {
                is Screen.Main -> {
                    when (currentScreen.tab) {
                        MainTab.Home -> HomeScreen(
                            data = data,
                            contentPadding = innerPadding,
                            onAddRecipe = { screen = Screen.RecipeEditor(null) },
                            onPlanMenu = { screen = Screen.MenuEditor(null) },
                            onOpenRecipe = { screen = Screen.RecipeDetail(it) },
                        )

                        MainTab.Recipes -> RecipeListScreen(
                            data = data,
                            contentPadding = innerPadding,
                            onAdd = { screen = Screen.RecipeEditor(null) },
                            onOpen = { screen = Screen.RecipeDetail(it) },
                        )

                        MainTab.Menu -> MenuBuilderEntryScreen(
                            data = data,
                            contentPadding = innerPadding,
                            onSaveMenu = { menu ->
                                persist(upsertMenu(data, menu))
                                screen = Screen.Main(MainTab.Archive)
                            },
                        )

                        MainTab.Archive -> MenuArchiveScreen(
                            data = data,
                            contentPadding = innerPadding,
                            onEdit = { screen = Screen.MenuEditor(it) },
                            onCreate = { screen = Screen.MenuEditor(null) },
                            onChangeStatus = { menuId, status -> persist(updateMenuStatus(data, menuId, status)) },
                            onDelete = { persist(deleteMenu(data, it)) },
                        )

                        MainTab.Settings -> SettingsScreen(
                            data = data,
                            store = store,
                            contentPadding = innerPadding,
                            onUpdateProfile = { profile -> persist(data.copy(profile = profile)) },
                            onImport = { imported -> persist(imported) },
                        )
                    }
                }

                is Screen.RecipeDetail -> {
                    val recipe = data.recipes.firstOrNull { it.id == currentScreen.recipeId }
                    if (recipe == null) {
                        LaunchedEffect(currentScreen.recipeId) { screen = Screen.Main(MainTab.Recipes) }
                    } else {
                        RecipeDetailScreen(
                            recipe = recipe,
                            onBack = { screen = Screen.Main(MainTab.Recipes) },
                            onEdit = { screen = Screen.RecipeEditor(recipe.id) },
                            onDelete = {
                                persist(deleteRecipe(data, recipe.id))
                                screen = Screen.Main(MainTab.Recipes)
                            },
                        )
                    }
                }

                is Screen.RecipeEditor -> {
                    RecipeEditorScreen(
                        recipe = currentScreen.recipeId?.let { id -> data.recipes.firstOrNull { it.id == id } },
                        onBack = { screen = Screen.Main(MainTab.Recipes) },
                        onSave = { recipe ->
                            persist(upsertRecipe(data, recipe))
                            screen = Screen.Main(MainTab.Recipes)
                        },
                    )
                }

                is Screen.MenuEditor -> {
                    MenuEditorScreen(
                        data = data,
                        menu = currentScreen.menuId?.let { id -> data.menus.firstOrNull { it.id == id } },
                        onBack = { screen = Screen.Main(MainTab.Archive) },
                        onSave = { menu ->
                            persist(upsertMenu(data, menu))
                            screen = Screen.Main(MainTab.Archive)
                        },
                    )
                }
            }
        }
    }
}
