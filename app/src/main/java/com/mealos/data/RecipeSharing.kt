package com.mealos.data

import android.content.Context
import android.content.ClipData
import android.content.Intent
import androidx.core.content.FileProvider
import com.mealos.model.Recipe
import java.io.File

fun shareRecipe(context: Context, recipe: Recipe) {
    val directory = File(context.cacheDir, "recipe_exports").apply { mkdirs() }
    val safeName = RecipeDocument.ingredientId(recipe.shortName.ifBlank { recipe.name }).ifBlank { "mealos-recipe" }
    val file = File(directory, "$safeName.$RECIPE_DOCUMENT_EXTENSION")
    file.writeText(RecipeDocument.encode(recipe))
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = RECIPE_DOCUMENT_MIME
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newUri(context.contentResolver, recipe.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }, "Share MealOS recipe"))
}

fun loadChatGptRecipePrompt(context: Context): String =
    context.assets.open("chatgpt_recipe_prompt.txt").bufferedReader().use { it.readText() }
