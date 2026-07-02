package com.example.signaturemenuapp.data

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class AppProfile(
    val displayName: String = "我的厨房",
    val username: String = "signature_user",
)

data class Ingredient(
    val id: String = newId(),
    val name: String = "",
    val amount: String = "",
    val unit: String = "",
    val note: String = "",
)

data class RecipeStep(
    val id: String = newId(),
    val order: Int = 1,
    val title: String = "",
    val description: String = "",
    val estimatedMinutes: Int = 0,
)

data class Recipe(
    val id: String = newId(),
    val name: String = "",
    val description: String = "",
    val cookingMethod: String = "炒",
    val servingCount: Int = 2,
    val estimatedMinutes: Int = 20,
    val difficulty: Int = 2,
    val isAvailable: Boolean = true,
    val tasteTags: List<String> = emptyList(),
    val proficiency: Int = 2,
    val priceRange: String = "",
    val cookedCount: Int = 0,
    val privateNote: String = "",
    val ingredients: List<Ingredient> = emptyList(),
    val steps: List<RecipeStep> = emptyList(),
    val createdAt: String = nowIso(),
    val updatedAt: String = nowIso(),
)

data class MenuDish(
    val recipeId: String = "",
    val name: String = "",
    val count: Int = 1,
)

enum class MenuStatus {
    Pending,
    Served,
}

data class MenuRecord(
    val id: String = newId(),
    val title: String = "",
    val note: String = "",
    val dateKey: String = LocalDate.now().toString(),
    val time: String = "18:30",
    val status: MenuStatus = MenuStatus.Pending,
    val dinerCount: Int = 4,
    val recipeIds: List<String> = emptyList(),
    val dishes: List<MenuDish> = emptyList(),
    val createdAt: String = nowIso(),
    val updatedAt: String = nowIso(),
)

data class SignatureMenuData(
    val profile: AppProfile = AppProfile(),
    val recipes: List<Recipe> = emptyList(),
    val menus: List<MenuRecord> = emptyList(),
)

data class ImportResult(
    val recipes: Int,
    val menus: Int,
)

fun newId(): String = UUID.randomUUID().toString()

fun nowIso(): String = Instant.now().toString()
