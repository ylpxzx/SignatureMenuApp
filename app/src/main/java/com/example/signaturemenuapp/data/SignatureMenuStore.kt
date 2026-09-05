package com.example.signaturemenuapp.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

class SignatureMenuStore(context: Context) {
    private val dataFile = File(context.filesDir, "signature_menu_data.json")

    fun load(): SignatureMenuData {
        if (!dataFile.exists()) {
            return seedData().also(::save)
        }

        return runCatching {
            val parsed = parseData(JSONObject(dataFile.readText()))
            val cleaned = removeLegacySeedData(deduplicateImportedData(parsed))
            if (cleaned != parsed) {
                save(cleaned)
            }
            cleaned
        }.getOrElse {
            seedData().also(::save)
        }
    }

    fun save(data: SignatureMenuData) {
        dataFile.writeText(data.toJson().toString(2))
    }

    fun exportJson(data: SignatureMenuData): String = JSONObject()
        .put("app", "SignatureMenu")
        .put("schema_version", 1)
        .put("export_type", "full")
        .put("exported_at", nowIso())
        .put(
            "user",
            JSONObject()
                .put("id", "local")
                .put("username", data.profile.username)
                .put("display_name", data.profile.displayName),
        )
        .put("recipes", JSONArray(data.recipes.map { it.toJson() }))
        .put("menus", JSONArray(data.menus.map { it.toJson() }))
        .toString(2)

    fun appendImport(current: SignatureMenuData, rawJson: String): Pair<SignatureMenuData, ImportResult> {
        val root = JSONObject(rawJson)
        val recipeArray = root.optJSONArray("recipes") ?: JSONArray()
        val menuArray = root.optJSONArray("menus") ?: JSONArray()

        if (recipeArray.length() + menuArray.length() == 0) {
            throw IllegalArgumentException("没有识别到可导入的 SignatureMenu 数据。")
        }

        var next = removeLegacySeedData(deduplicateImportedData(current))
        val idMap = mutableMapOf<String, String>()
        val servedCounts = countServedRecipes(menuArray)
        var importedRecipeCount = 0

        for (index in 0 until recipeArray.length()) {
            val source = recipeArray.optJSONObject(index) ?: continue
            val oldId = source.stringValue("id")
            val recipe = parseRecipe(source)
            if (isLegacySeedRecipe(recipe)) continue
            val existingRecipe = next.recipes.firstOrNull { oldId.isNotBlank() && it.id == oldId }
                ?: next.recipes.firstOrNull { recipeImportKey(it) == recipeImportKey(recipe) }
            if (existingRecipe != null) {
                if (oldId.isNotBlank()) {
                    idMap[oldId] = existingRecipe.id
                }
                continue
            }

            val importedRecipe = recipe.copy(
                id = oldId.ifBlank { recipe.id },
                cookedCount = (recipe.cookedCount - (servedCounts[oldId] ?: 0)).coerceAtLeast(0),
                updatedAt = nowIso(),
            )
            if (oldId.isNotBlank()) {
                idMap[oldId] = importedRecipe.id
            }
            next = next.copy(recipes = next.recipes + importedRecipe)
            importedRecipeCount += 1
        }

        var importedMenuCount = 0
        for (index in 0 until menuArray.length()) {
            val source = menuArray.optJSONObject(index) ?: continue
            val oldId = source.stringValue("id")
            val menu = parseMenu(source, idMap)
            if (isLegacySeedMenu(menu)) continue
            if (menu.dateKey.isBlank()) continue

            val existingMenuById = next.menus.firstOrNull { oldId.isNotBlank() && it.id == oldId }
            val menuToImport: MenuRecord? = if (existingMenuById != null) {
                menu.copy(
                    id = existingMenuById.id,
                    createdAt = existingMenuById.createdAt,
                    updatedAt = nowIso(),
                )
            } else {
                val duplicateMenu = next.menus.firstOrNull { menuImportKey(it) == menuImportKey(menu) }
                if (duplicateMenu == null) {
                    menu.copy(id = oldId.ifBlank { menu.id }, updatedAt = nowIso())
                } else {
                    null
                }
            }
            if (menuToImport == null) continue

            next = upsertMenu(next, menuToImport)
            importedMenuCount += 1
        }

        return removeLegacySeedData(deduplicateImportedData(next)) to ImportResult(importedRecipeCount, importedMenuCount)
    }
}

fun upsertRecipe(data: SignatureMenuData, recipe: Recipe): SignatureMenuData {
    val normalized = recipe.normalized().copy(updatedAt = nowIso())
    val exists = data.recipes.any { it.id == normalized.id }
    val recipes = if (exists) {
        data.recipes.map { if (it.id == normalized.id) normalized else it }
    } else {
        data.recipes + normalized.copy(createdAt = nowIso())
    }
    return data.copy(recipes = recipes)
}

fun deleteRecipe(data: SignatureMenuData, recipeId: String): SignatureMenuData {
    val menus = data.menus.map { menu ->
        val recipeIds = menu.recipeIds.filterNot { it == recipeId }
        val dishes = menu.dishes.filterNot { it.recipeId == recipeId }
        menu.copy(recipeIds = recipeIds, dishes = dishes, updatedAt = nowIso())
    }
    return data.copy(
        recipes = data.recipes.filterNot { it.id == recipeId },
        menus = menus,
    )
}

fun upsertMenu(data: SignatureMenuData, menu: MenuRecord): SignatureMenuData {
    val before = data.menus.firstOrNull { it.id == menu.id }
    val normalized = menu.normalized().copy(updatedAt = nowIso())
    val menus = if (before == null) {
        data.menus + normalized.copy(createdAt = nowIso())
    } else {
        data.menus.map { if (it.id == normalized.id) normalized else it }
    }
    return applyCookedTransition(data.copy(menus = menus), before, normalized)
}

fun deleteMenu(data: SignatureMenuData, menuId: String): SignatureMenuData {
    val before = data.menus.firstOrNull { it.id == menuId }
    val withoutMenu = data.copy(menus = data.menus.filterNot { it.id == menuId })
    return applyCookedTransition(withoutMenu, before, null)
}

fun updateMenuStatus(data: SignatureMenuData, menuId: String, status: MenuStatus): SignatureMenuData {
    val menu = data.menus.firstOrNull { it.id == menuId } ?: return data
    return upsertMenu(data, menu.copy(status = status))
}

private fun deduplicateImportedData(data: SignatureMenuData): SignatureMenuData {
    val duplicateRecipeIds = mutableMapOf<String, String>()
    val recipeKeys = mutableMapOf<String, Recipe>()
    val recipes = mutableListOf<Recipe>()
    data.recipes.forEach { recipe ->
        val key = recipeImportKey(recipe)
        val existing = recipeKeys[key]
        if (existing == null) {
            recipeKeys[key] = recipe
            recipes += recipe
        } else {
            duplicateRecipeIds[recipe.id] = existing.id
        }
    }

    val menusWithRecipeIds = data.menus.map { menu ->
        val recipeIds = menu.recipeIds
            .map { duplicateRecipeIds[it] ?: it }
            .filter { it.isNotBlank() }
            .distinct()
        val dishes = menu.dishes
            .map { dish -> dish.copy(recipeId = duplicateRecipeIds[dish.recipeId] ?: dish.recipeId) }
            .filter { it.recipeId.isNotBlank() || it.name.isNotBlank() }
            .distinctBy { "${it.recipeId}|${it.name.importKeyPart()}|${it.count}" }
        menu.copy(recipeIds = recipeIds, dishes = dishes)
    }

    val menuKeys = mutableSetOf<String>()
    val menus = mutableListOf<MenuRecord>()
    menusWithRecipeIds.forEach { menu ->
        if (menuKeys.add(menuImportKey(menu))) {
            menus += menu
        }
    }

    return data.copy(recipes = recipes, menus = menus)
}

private fun removeLegacySeedData(data: SignatureMenuData): SignatureMenuData {
    val legacyRecipeIds = data.recipes
        .filter(::isLegacySeedRecipe)
        .map { it.id }
        .toSet()
    if (legacyRecipeIds.isEmpty() && data.menus.none(::isLegacySeedMenu)) return data

    val recipes = data.recipes.filterNot { it.id in legacyRecipeIds }
    val menus = data.menus
        .filterNot(::isLegacySeedMenu)
        .map { menu ->
            val recipeIds = menu.recipeIds.filterNot { it in legacyRecipeIds }
            val dishes = menu.dishes.filterNot { it.recipeId in legacyRecipeIds }
            menu.copy(recipeIds = recipeIds, dishes = dishes)
        }
    return data.copy(recipes = recipes, menus = menus)
}

private fun isLegacySeedRecipe(recipe: Recipe): Boolean = when (recipe.name.trim()) {
    "番茄炒蛋" -> recipe.description == "酸甜下饭，十几分钟就能端上桌。" && recipe.cookingMethod == "炒"
    "香煎鸡腿排" -> recipe.description == "外皮脆一点，里面保持多汁。" && recipe.cookingMethod == "煎"
    "玉米排骨汤" -> recipe.description == "清甜耐喝，适合周末慢慢炖。" && recipe.cookingMethod == "炖"
    else -> false
}

private fun isLegacySeedMenu(menu: MenuRecord): Boolean =
    menu.title == "今晚家常菜单" && menu.note == "清爽一点，留一碗汤。"

private fun recipeImportKey(recipe: Recipe): String = listOf(
    recipe.name.importKeyPart(),
    recipe.description.importKeyPart(),
    recipe.cookingMethod.importKeyPart(),
    recipe.servingCount.toString(),
    recipe.estimatedMinutes.toString(),
    recipe.difficulty.toString(),
    recipe.isAvailable.toString(),
    recipe.tasteTags.joinToString(",") { it.importKeyPart() },
    recipe.proficiency.toString(),
    recipe.priceRange.importKeyPart(),
    recipe.privateNote.importKeyPart(),
    recipe.ingredients.joinToString("||") { ingredient ->
        listOf(
            ingredient.name.importKeyPart(),
            ingredient.amount.importKeyPart(),
            ingredient.unit.importKeyPart(),
            ingredient.note.importKeyPart(),
        ).joinToString("|")
    },
    recipe.steps.joinToString("||") { step ->
        listOf(
            step.order.toString(),
            step.title.importKeyPart(),
            step.description.importKeyPart(),
            step.estimatedMinutes.toString(),
        ).joinToString("|")
    },
).joinToString("\u001F")

private fun menuImportKey(menu: MenuRecord): String = listOf(
    menu.title.importKeyPart(),
    menu.note.importKeyPart(),
    menu.dateKey.importKeyPart(),
    menu.time.importKeyPart(),
    menu.status.name,
    menu.dinerCount.toString(),
    menu.recipeIds.joinToString(","),
    menu.dishes.joinToString("||") { dish ->
        listOf(dish.recipeId, dish.name.importKeyPart(), dish.count.toString()).joinToString("|")
    },
).joinToString("\u001F")

private fun String.importKeyPart(): String = trim().replace(Regex("\\s+"), " ").lowercase()

private fun applyCookedTransition(
    data: SignatureMenuData,
    before: MenuRecord?,
    after: MenuRecord?,
): SignatureMenuData {
    val delta = mutableMapOf<String, Int>()
    if (before?.status == MenuStatus.Served) {
        before.recipeIds.toSet().forEach { recipeId ->
            delta[recipeId] = (delta[recipeId] ?: 0) - 1
        }
    }
    if (after?.status == MenuStatus.Served) {
        after.recipeIds.toSet().forEach { recipeId ->
            delta[recipeId] = (delta[recipeId] ?: 0) + 1
        }
    }
    if (delta.isEmpty()) return data

    val recipes = data.recipes.map { recipe ->
        val change = delta[recipe.id] ?: return@map recipe
        recipe.copy(
            cookedCount = (recipe.cookedCount + change).coerceAtLeast(0),
            updatedAt = nowIso(),
        )
    }
    return data.copy(recipes = recipes)
}

private fun seedData(): SignatureMenuData = SignatureMenuData()

private fun SignatureMenuData.toJson(): JSONObject = JSONObject()
    .put("profile", JSONObject().put("display_name", profile.displayName).put("username", profile.username))
    .put("recipes", JSONArray(recipes.map { it.toJson() }))
    .put("menus", JSONArray(menus.map { it.toJson() }))

private fun Recipe.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("name", name)
    .put("description", description)
    .put("cooking_method", cookingMethod)
    .put("serving_count", servingCount)
    .put("estimated_minutes", estimatedMinutes)
    .put("difficulty", difficulty)
    .put("is_available", isAvailable)
    .put("taste_tags", JSONArray(tasteTags))
    .put("proficiency", proficiency)
    .put("price_range", priceRange)
    .put("cooked_count", cookedCount)
    .put("private_note", privateNote)
    .put("ingredients", JSONArray(ingredients.map { it.toJson() }))
    .put("steps", JSONArray(steps.map { it.toJson() }))
    .put("created_at", createdAt)
    .put("updated_at", updatedAt)

private fun Ingredient.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("name", name)
    .put("amount", amount)
    .put("unit", unit)
    .put("note", note)

private fun RecipeStep.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("step_order", order)
    .put("title", title)
    .put("description", description)
    .put("estimated_minutes", estimatedMinutes)

private fun MenuRecord.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("title", title)
    .put("note", note)
    .put("dateKey", dateKey)
    .put("time", time)
    .put("status", if (status == MenuStatus.Served) "served" else "pending")
    .put("dinerCount", dinerCount)
    .put("recipeIds", JSONArray(recipeIds))
    .put("dishes", JSONArray(dishes.map { it.toJson() }))
    .put("createdAt", createdAt)
    .put("updatedAt", updatedAt)

private fun MenuDish.toJson(): JSONObject = JSONObject()
    .put("recipeId", recipeId)
    .put("name", name)
    .put("count", count)

private fun parseData(root: JSONObject): SignatureMenuData {
    val profileObject = root.optJSONObject("profile") ?: root.optJSONObject("user")
    val profile = AppProfile(
        displayName = profileObject?.stringValue("display_name", "displayName") ?: "我的厨房",
        username = profileObject?.stringValue("username") ?: "signature_user",
    )
    return SignatureMenuData(
        profile = profile,
        recipes = root.optJSONArray("recipes").toObjects(::parseRecipe),
        menus = root.optJSONArray("menus").toObjects { parseMenu(it, emptyMap()) },
    )
}

private fun parseRecipe(source: JSONObject): Recipe = Recipe(
    id = source.stringValue("id").ifBlank { newId() },
    name = source.stringValue("name").ifBlank { "未命名菜谱" },
    description = source.stringValue("description"),
    cookingMethod = source.stringValue("cooking_method", "cookingMethod").ifBlank { "炒" },
    servingCount = source.intValue("serving_count", "servingCount", min = 0, max = 99, fallback = 2),
    estimatedMinutes = source.intValue("estimated_minutes", "estimatedMinutes", min = 0, max = 24 * 60, fallback = 20),
    difficulty = source.intValue("difficulty", min = 1, max = 5, fallback = 2),
    isAvailable = source.optBoolean("is_available", source.optBoolean("isAvailable", true)),
    tasteTags = source.stringList("taste_tags", "tasteTags"),
    proficiency = source.intValue("proficiency", min = 1, max = 5, fallback = 2),
    priceRange = source.stringValue("price_range", "priceRange"),
    cookedCount = source.intValue("cooked_count", "cookedCount", min = 0, max = 9999, fallback = 0),
    privateNote = source.stringValue("private_note", "privateNote"),
    ingredients = source.optJSONArray("ingredients").toObjects(::parseIngredient),
    steps = source.optJSONArray("steps").toObjects(::parseRecipeStep).ifEmpty {
        listOf(RecipeStep(title = "待补充步骤", description = "可以在编辑页补齐做法。"))
    },
    createdAt = source.stringValue("created_at", "createdAt").ifBlank { nowIso() },
    updatedAt = source.stringValue("updated_at", "updatedAt").ifBlank { nowIso() },
).normalized()

private fun parseIngredient(source: JSONObject): Ingredient = Ingredient(
    id = source.stringValue("id").ifBlank { newId() },
    name = source.stringValue("name"),
    amount = source.stringValue("amount"),
    unit = source.stringValue("unit"),
    note = source.stringValue("note"),
)

private fun parseRecipeStep(source: JSONObject): RecipeStep = RecipeStep(
    id = source.stringValue("id").ifBlank { newId() },
    order = source.intValue("step_order", "order", min = 1, max = 999, fallback = 1),
    title = source.stringValue("title"),
    description = source.stringValue("description"),
    estimatedMinutes = source.intValue("estimated_minutes", "estimatedMinutes", min = 0, max = 24 * 60, fallback = 0),
)

private fun parseMenu(source: JSONObject, idMap: Map<String, String>): MenuRecord {
    val recipeIds = menuRecipeIds(source)
        .map { idMap[it] ?: it }
        .filter { it.isNotBlank() }
        .distinct()
    val dishes = source.optJSONArray("dishes").toObjects { dish ->
        val oldRecipeId = dish.stringValue("recipeId", "recipe_id")
        MenuDish(
            recipeId = idMap[oldRecipeId] ?: oldRecipeId,
            name = dish.stringValue("name"),
            count = dish.intValue("count", min = 1, max = 99, fallback = 1),
        )
    }
    return MenuRecord(
        id = source.stringValue("id").ifBlank { newId() },
        title = source.stringValue("title").ifBlank { "导入菜单" },
        note = source.stringValue("note"),
        dateKey = source.stringValue("dateKey", "date_key").ifBlank { LocalDate.now().toString() },
        time = normalizeTime(source.stringValue("time")),
        status = if (source.stringValue("status") == "served") MenuStatus.Served else MenuStatus.Pending,
        dinerCount = source.intValue("dinerCount", "diner_count", min = 1, max = 99, fallback = 4),
        recipeIds = recipeIds,
        dishes = dishes.ifEmpty { recipeIds.map { MenuDish(recipeId = it, name = "") } },
        createdAt = source.stringValue("createdAt", "created_at").ifBlank { nowIso() },
        updatedAt = source.stringValue("updatedAt", "updated_at").ifBlank { nowIso() },
    ).normalized()
}

private fun countServedRecipes(menuArray: JSONArray): Map<String, Int> {
    val counts = mutableMapOf<String, Int>()
    for (index in 0 until menuArray.length()) {
        val menu = menuArray.optJSONObject(index) ?: continue
        if (menu.stringValue("status") != "served") continue
        menuRecipeIds(menu).forEach { recipeId ->
            counts[recipeId] = (counts[recipeId] ?: 0) + 1
        }
    }
    return counts
}

private fun menuRecipeIds(menu: JSONObject): List<String> {
    val seen = linkedSetOf<String>()
    menu.optJSONArray("dishes").forEachObject { dish ->
        seen += dish.stringValue("recipeId", "recipe_id")
    }
    menu.optJSONArray("recipeIds").forEachString { seen += it }
    menu.optJSONArray("recipe_ids").forEachString { seen += it }
    return seen.filter { it.isNotBlank() }
}

private fun Recipe.normalized(): Recipe = copy(
    name = name.ifBlank { "未命名菜谱" },
    cookingMethod = cookingMethod.ifBlank { "炒" },
    servingCount = servingCount.coerceIn(0, 99),
    estimatedMinutes = estimatedMinutes.coerceIn(0, 24 * 60),
    difficulty = difficulty.coerceIn(1, 5),
    tasteTags = tasteTags.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(8).ifEmpty { listOf(cookingMethod.ifBlank { "家常" }) },
    proficiency = proficiency.coerceIn(1, 5),
    cookedCount = cookedCount.coerceIn(0, 9999),
    ingredients = ingredients.filter { it.name.isNotBlank() },
    steps = steps.sortedBy { it.order }.mapIndexed { index, step -> step.copy(order = index + 1) },
)

private fun MenuRecord.normalized(): MenuRecord {
    val ids = recipeIds.filter { it.isNotBlank() }.distinct()
    return copy(
        title = title.ifBlank { "家常菜单" },
        dateKey = dateKey.ifBlank { LocalDate.now().toString() },
        time = normalizeTime(time),
        dinerCount = dinerCount.coerceIn(1, 99),
        recipeIds = ids,
        dishes = dishes.filter { it.recipeId.isNotBlank() || it.name.isNotBlank() }.ifEmpty {
            ids.map { MenuDish(recipeId = it, name = "") }
        },
    )
}

private fun normalizeTime(value: String): String = if (Regex("""^\d{2}:\d{2}$""").matches(value)) value else "18:30"

private fun JSONObject.stringValue(vararg names: String): String {
    for (name in names) {
        if (!has(name) || isNull(name)) continue
        val value = opt(name)
        if (value is String) return value.trim()
        if (value != null) return value.toString().trim()
    }
    return ""
}

private fun JSONObject.intValue(
    vararg names: String,
    min: Int,
    max: Int,
    fallback: Int,
): Int {
    for (name in names) {
        if (!has(name) || isNull(name)) continue
        val raw = opt(name)
        val value = when (raw) {
            is Number -> raw.toInt()
            is String -> raw.toIntOrNull()
            else -> null
        }
        if (value != null) return value.coerceIn(min, max)
    }
    return fallback.coerceIn(min, max)
}

private fun JSONObject.stringList(vararg names: String): List<String> {
    for (name in names) {
        val array = optJSONArray(name) ?: continue
        val values = mutableListOf<String>()
        array.forEachString { values += it }
        return values.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    }
    return emptyList()
}

private inline fun JSONArray?.forEachObject(block: (JSONObject) -> Unit) {
    val array = this ?: return
    for (index in 0 until array.length()) {
        array.optJSONObject(index)?.let(block)
    }
}

private inline fun JSONArray?.forEachString(block: (String) -> Unit) {
    val array = this ?: return
    for (index in 0 until array.length()) {
        val value = array.opt(index)
        val text = if (value is String) value.trim() else value?.toString()?.trim().orEmpty()
        if (text.isNotBlank()) block(text)
    }
}

private inline fun <T> JSONArray?.toObjects(parser: (JSONObject) -> T): List<T> {
    val result = mutableListOf<T>()
    this.forEachObject { result += parser(it) }
    return result
}
