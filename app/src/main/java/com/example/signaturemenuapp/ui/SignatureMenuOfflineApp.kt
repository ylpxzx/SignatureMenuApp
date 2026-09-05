package com.example.signaturemenuapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
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
    var backStack by remember { mutableStateOf<List<Screen>>(emptyList()) }

    fun persist(next: SignatureMenuData) {
        data = next
        store.save(next)
    }

    fun navigate(next: Screen) {
        if (next == screen) return
        backStack = backStack + screen
        screen = next
    }

    fun replace(next: Screen) {
        backStack = emptyList()
        screen = next
    }

    fun goBack(fallback: Screen) {
        val previous = backStack.lastOrNull()
        if (previous == null) {
            screen = fallback
        } else {
            backStack = backStack.dropLast(1)
            screen = previous
        }
    }

    val systemBackFallback = when (screen) {
        is Screen.RecipeDetail -> Screen.Main(MainTab.Recipes)
        is Screen.RecipeEditor -> Screen.Main(MainTab.Recipes)
        is Screen.MenuEditor -> Screen.Main(MainTab.Archive)
        is Screen.Main -> null
    }
    BackHandler(enabled = systemBackFallback != null) {
        systemBackFallback?.let(::goBack)
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
            modifier = Modifier.imePadding(),
            containerColor = Color.Transparent,
            bottomBar = {
                val current = screen as? Screen.Main
                if (current != null) {
                    BottomTabs(
                        current = current.tab,
                        onSelect = { tab -> replace(Screen.Main(tab)) },
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
                            onAddRecipe = { navigate(Screen.RecipeEditor(null)) },
                            onPlanMenu = { navigate(Screen.MenuEditor(null)) },
                            onOpenRecipe = { navigate(Screen.RecipeDetail(it)) },
                        )

                        MainTab.Recipes -> RecipeListScreen(
                            data = data,
                            contentPadding = innerPadding,
                            onAdd = { navigate(Screen.RecipeEditor(null)) },
                            onOpen = { navigate(Screen.RecipeDetail(it)) },
                            onDelete = { persist(deleteRecipe(data, it)) },
                        )

                        MainTab.Menu -> MenuBuilderEntryScreen(
                            data = data,
                            contentPadding = innerPadding,
                            onSaveMenu = { menu ->
                                persist(upsertMenu(data, menu))
                                replace(Screen.Main(MainTab.Archive))
                            },
                        )

                        MainTab.Archive -> MenuArchiveScreen(
                            data = data,
                            contentPadding = innerPadding,
                            onEdit = { navigate(Screen.MenuEditor(it)) },
                            onCreate = { navigate(Screen.MenuEditor(null)) },
                            onChangeStatus = { menuId, status -> persist(updateMenuStatus(data, menuId, status)) },
                            onDelete = { persist(deleteMenu(data, it)) },
                        )

                        MainTab.Settings -> SettingsScreen(
                            data = data,
                            store = store,
                            contentPadding = innerPadding,
                            onImport = { imported -> persist(imported) },
                        )
                    }
                }

                is Screen.RecipeDetail -> {
                    val recipe = data.recipes.firstOrNull { it.id == currentScreen.recipeId }
                    if (recipe == null) {
                        LaunchedEffect(currentScreen.recipeId) { replace(Screen.Main(MainTab.Recipes)) }
                    } else {
                        SwipeBackPage(onBack = { goBack(Screen.Main(MainTab.Recipes)) }) {
                            RecipeDetailScreen(
                                recipe = recipe,
                                onBack = { goBack(Screen.Main(MainTab.Recipes)) },
                                onEdit = { navigate(Screen.RecipeEditor(recipe.id)) },
                                onDelete = {
                                    persist(deleteRecipe(data, recipe.id))
                                    replace(Screen.Main(MainTab.Recipes))
                                },
                            )
                        }
                    }
                }

                is Screen.RecipeEditor -> {
                    SwipeBackPage(onBack = { goBack(Screen.Main(MainTab.Recipes)) }) {
                        RecipeEditorScreen(
                            recipe = currentScreen.recipeId?.let { id -> data.recipes.firstOrNull { it.id == id } },
                            onBack = { goBack(Screen.Main(MainTab.Recipes)) },
                            onSave = { recipe ->
                                persist(upsertRecipe(data, recipe))
                                replace(Screen.Main(MainTab.Recipes))
                            },
                        )
                    }
                }

                is Screen.MenuEditor -> {
                    SwipeBackPage(onBack = { goBack(Screen.Main(MainTab.Archive)) }) {
                        MenuEditorScreen(
                            data = data,
                            menu = currentScreen.menuId?.let { id -> data.menus.firstOrNull { it.id == id } },
                            onBack = { goBack(Screen.Main(MainTab.Archive)) },
                            onSave = { menu ->
                                persist(upsertMenu(data, menu))
                                replace(Screen.Main(MainTab.Archive))
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeBackPage(
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val edgeWidthPx = with(density) { 28.dp.toPx() }
    val triggerPx = with(density) { 72.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(onBack, edgeWidthPx, triggerPx) {
                var fromLeftEdge = false
                var fromRightEdge = false
                var totalDrag = 0f

                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        fromLeftEdge = offset.x <= edgeWidthPx
                        fromRightEdge = offset.x >= size.width - edgeWidthPx
                        totalDrag = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        if (fromLeftEdge || fromRightEdge) {
                            totalDrag += dragAmount
                            val isBackDirection = (fromLeftEdge && totalDrag > 0f) ||
                                (fromRightEdge && totalDrag < 0f)
                            if (isBackDirection) {
                                change.consume()
                            }
                        }
                    },
                    onDragEnd = {
                        val shouldBack = (fromLeftEdge && totalDrag >= triggerPx) ||
                            (fromRightEdge && totalDrag <= -triggerPx)
                        if (shouldBack) {
                            onBack()
                        }
                    },
                    onDragCancel = {
                        totalDrag = 0f
                    },
                )
            },
    ) {
        content()
    }
}
