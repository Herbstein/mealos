# Creating MealOS recipes with ChatGPT

> Android is deprecated and no longer maintained as of October 6, 2026. Android-specific setup and UI instructions below are historical. Shared services and recipe formats remain in use; see the [active Apple client](../ios/README.md) for current client setup.

MealOS ships a reusable prompt at `app/src/main/assets/chatgpt_recipe_prompt.txt`.

1. In MealOS, open **Recipes** and tap **Copy AI recipe prompt**.
2. Paste it into ChatGPT and add what you want to cook.
3. Ask ChatGPT to generate the recipe.
4. Share the JSON response to MealOS, or save it with the `.mealosrecipe` extension and open it on Android.
5. MealOS opens every imported recipe as a draft. Review all four editor sections before saving.

The prompt and file format currently use `mealos.recipe` schema version `1`. Future schema versions should retain an explicit migration path rather than silently accepting changed fields.
