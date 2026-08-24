package com.mealos.data

import com.mealos.model.Ingredient
import com.mealos.model.IngredientCategory.BAKERY
import com.mealos.model.IngredientCategory.DAIRY
import com.mealos.model.IngredientCategory.FROZEN
import com.mealos.model.IngredientCategory.MEAT
import com.mealos.model.IngredientCategory.PANTRY
import com.mealos.model.IngredientCategory.PRODUCE
import com.mealos.model.Recipe
import com.mealos.model.RecipeStep

object RecipeCatalog {
    val recipes: List<Recipe> = listOf(
        recipe(
            id = "garlic-herb-chicken",
            name = "Garlic-herb chicken + baby potatoes + broccoli",
            shortName = "Garlic-herb chicken",
            effort = 5,
            total = 28,
            equipment = listOf("CRISPi DualZone"),
            tags = listOf("Core", "Air fryer", "Great leftovers"),
            leftovers = "Pack half with potatoes and broccoli before serving dinner.",
            ingredients = listOf(
                ing("chicken-breast", "Chicken breasts", 1.0, "lb", MEAT),
                ing("baby-potatoes", "Baby potatoes", 1.0, "lb", PRODUCE),
                ing("broccoli", "Broccoli florets", 12.0, "oz", PRODUCE),
                ing("olive-oil", "Olive oil", 1.0, "tbsp", PANTRY, true),
                ing("garlic-powder", "Garlic powder", 1.0, "tsp", PANTRY, true),
                ing("italian-seasoning", "Garlic-herb seasoning", 2.0, "tsp", PANTRY, true),
                ing("salt", "Salt", 0.5, "tsp", PANTRY, true),
                ing("black-pepper", "Black pepper", 0.25, "tsp", PANTRY, true)
            ),
            steps = listOf(
                step("Prep the potatoes", "Halve the potatoes. Toss with olive oil, garlic-herb seasoning, salt, and pepper.", "CRISPi basket 1"),
                step("Start the potatoes", "Air fry at 400°F. Shake the basket halfway through.", "CRISPi basket 1", 20 * 60),
                step("Season the chicken", "Pat dry, lightly oil, and season both sides generously.", "Prep board"),
                step("Add the chicken", "Place chicken in basket 2 at 400°F. Cook until the thickest piece reaches 165°F.", "CRISPi basket 2", 14 * 60, "Start checking at 12 minutes."),
                step("Finish with broccoli", "Add broccoli for the final 7 minutes. Rest chicken 3 minutes before packing lunch.", "CRISPi DualZone", 7 * 60)
            )
        ),
        recipe(
            "bbq-chicken", "BBQ chicken + potatoes + corn", "BBQ chicken", 5, 28,
            listOf("CRISPi DualZone"), listOf("Air fryer", "Great leftovers"),
            "Slice leftover chicken only after it cools so it stays juicy.",
            listOf(
                ing("chicken-breast", "Chicken breasts", 1.0, "lb", MEAT),
                ing("baby-potatoes", "Baby potatoes", 1.0, "lb", PRODUCE),
                ing("corn", "Corn kernels", 12.0, "oz", FROZEN),
                ing("bbq-sauce", "BBQ sauce", 0.5, "cup", PANTRY),
                ing("olive-oil", "Olive oil", 1.0, "tbsp", PANTRY, true),
                ing("salt", "Salt", 0.5, "tsp", PANTRY, true)
            ),
            listOf(
                step("Season the potatoes", "Halve, oil, and salt the potatoes.", "Prep board"),
                step("Start the potatoes", "Air fry at 400°F, shaking once.", "CRISPi basket 1", 20 * 60),
                step("Cook the chicken", "Season chicken and air fry in basket 2 at 400°F.", "CRISPi basket 2", 12 * 60),
                step("Glaze", "Brush with BBQ sauce and cook until 165°F.", "CRISPi basket 2", 3 * 60),
                step("Warm the corn", "Microwave corn, season, and pack one leftover portion.", "Microwave", 4 * 60)
            )
        ),
        recipe(
            "chicken-parmesan", "Chicken Parmesan + pasta", "Chicken Parmesan", 10, 32,
            listOf("CRISPi", "Pot"), listOf("Pasta", "Familiar"),
            "Store pasta and chicken separately so the coating stays crisp.",
            listOf(
                ing("breaded-chicken", "Breaded chicken cutlets", 4.0, "pieces", FROZEN),
                ing("pasta", "Pasta", 8.0, "oz", PANTRY),
                ing("marinara", "Marinara sauce", 2.0, "cups", PANTRY),
                ing("mozzarella", "Shredded mozzarella", 1.0, "cup", DAIRY),
                ing("parmesan", "Parmesan", 0.25, "cup", DAIRY)
            ),
            listOf(
                step("Boil the pasta", "Salt the water and cook pasta according to the package.", "Pot", 10 * 60),
                step("Crisp the chicken", "Air fry cutlets at 400°F until crisp and hot.", "CRISPi", 12 * 60),
                step("Add sauce", "Spoon marinara over each cutlet, keeping the edges exposed.", "CRISPi"),
                step("Melt the cheese", "Top with mozzarella and Parmesan; air fry until melted.", "CRISPi", 3 * 60),
                step("Serve", "Drain pasta, add marinara, and keep lunch components separate.", "Pot")
            )
        ),
        recipe(
            "buffalo-wraps", "Buffalo chicken wraps", "Buffalo chicken wraps", 7, 20,
            listOf("CRISPi"), listOf("Wrap", "Great leftovers"),
            "Keep the tortilla and greens separate until lunch.",
            listOf(
                ing("chicken-tenders", "Chicken tenders", 1.0, "lb", MEAT),
                ing("tortillas", "Large flour tortillas", 4.0, "count", BAKERY),
                ing("buffalo-sauce", "Buffalo sauce", 0.5, "cup", PANTRY),
                ing("lettuce", "Shredded lettuce", 4.0, "cups", PRODUCE),
                ing("cheddar", "Shredded cheddar", 1.0, "cup", DAIRY),
                ing("ranch", "Ranch dressing", 0.5, "cup", DAIRY)
            ),
            listOf(
                step("Season", "Lightly oil and season the chicken tenders.", "Prep board"),
                step("Air fry", "Cook at 400°F, turning once, until 165°F.", "CRISPi", 12 * 60),
                step("Sauce", "Slice the chicken and toss with buffalo sauce.", "Mixing bowl"),
                step("Build", "Layer lettuce, chicken, cheddar, and ranch in each tortilla.", "Counter"),
                step("Wrap", "Roll tightly. Pack lunch components separately.", "Counter")
            )
        ),
        recipe(
            "caesar-wraps", "Chicken Caesar wraps", "Chicken Caesar wraps", 5, 18,
            listOf("CRISPi"), listOf("Wrap", "Great leftovers"),
            "Dress only the dinner portion; pack lunch dressing separately.",
            listOf(
                ing("chicken-tenders", "Chicken tenders", 1.0, "lb", MEAT),
                ing("tortillas", "Large flour tortillas", 4.0, "count", BAKERY),
                ing("romaine", "Romaine hearts", 2.0, "count", PRODUCE),
                ing("caesar", "Caesar dressing", 0.5, "cup", DAIRY),
                ing("parmesan", "Parmesan", 0.5, "cup", DAIRY),
                ing("croutons", "Croutons", 1.0, "cup", PANTRY)
            ),
            listOf(
                step("Cook the chicken", "Season and air fry chicken at 400°F until 165°F.", "CRISPi", 12 * 60),
                step("Chop", "Chop romaine and lightly crush the croutons.", "Prep board"),
                step("Dress dinner", "Toss half the romaine with dressing, Parmesan, and croutons.", "Mixing bowl"),
                step("Build wraps", "Add sliced chicken and dressed romaine to tortillas.", "Counter"),
                step("Pack lunch", "Pack remaining components separately and refrigerate.", "Storage container")
            )
        ),
        recipe(
            "cheeseburger-fries", "Cheeseburger + air-fryer fries", "Cheeseburger + fries", 5, 24,
            listOf("CRISPi", "Pan"), listOf("Comfort food"),
            "Burgers are best fresh; save extra uncooked patty mixture instead.",
            listOf(
                ing("ground-beef", "Ground beef", 1.0, "lb", MEAT),
                ing("burger-buns", "Burger buns", 4.0, "count", BAKERY),
                ing("cheddar-slices", "Cheddar slices", 4.0, "count", DAIRY),
                ing("frozen-fries", "Frozen fries", 1.0, "lb", FROZEN),
                ing("lettuce", "Lettuce", 4.0, "leaves", PRODUCE),
                ing("tomato", "Tomato", 1.0, "count", PRODUCE)
            ),
            listOf(
                step("Start the fries", "Air fry at 400°F and shake twice.", "CRISPi", 18 * 60),
                step("Shape patties", "Form four loose patties and season both sides.", "Prep board"),
                step("Cook burgers", "Sear in a hot pan to your preferred doneness.", "Pan", 8 * 60),
                step("Melt cheese", "Add cheese during the final minute and cover briefly.", "Pan", 60),
                step("Build", "Toast buns if desired and add lettuce and tomato.", "Toaster")
            )
        ),
        recipe(
            "teriyaki-bowl", "Teriyaki chicken rice bowl + broccoli", "Teriyaki chicken bowl", 5, 25,
            listOf("CRISPi", "Rice cooker"), listOf("Bowl", "Great leftovers"),
            "Build lunch in a microwave-safe container with sauce on top.",
            listOf(
                ing("chicken-thighs", "Boneless chicken thighs", 1.0, "lb", MEAT),
                ing("rice", "White rice", 1.5, "cups", PANTRY),
                ing("broccoli", "Broccoli florets", 12.0, "oz", PRODUCE),
                ing("teriyaki", "Teriyaki sauce", 0.75, "cup", PANTRY),
                ing("sesame", "Sesame seeds", 1.0, "tbsp", PANTRY)
            ),
            listOf(
                step("Start rice", "Rinse rice and start the rice cooker.", "Rice cooker", 22 * 60),
                step("Cook chicken", "Air fry seasoned thighs at 400°F until 165°F.", "CRISPi", 14 * 60),
                step("Add broccoli", "Air fry broccoli alongside the chicken for the final minutes.", "CRISPi", 7 * 60),
                step("Sauce", "Slice chicken and toss with teriyaki sauce.", "Mixing bowl"),
                step("Build bowls", "Divide rice, broccoli, and chicken. Finish with sesame.", "Bowls")
            )
        ),
        recipe(
            "beef-burrito-bowls", "Beef burrito bowls", "Beef burrito bowls", 10, 25,
            listOf("Pan", "Rice cooker"), listOf("Bowl", "Great leftovers"),
            "Pack toppings separately so lunch reheats cleanly.",
            listOf(
                ing("ground-beef", "Ground beef", 1.0, "lb", MEAT),
                ing("rice", "White rice", 1.5, "cups", PANTRY),
                ing("black-beans", "Black beans", 1.0, "can", PANTRY),
                ing("corn", "Corn kernels", 8.0, "oz", FROZEN),
                ing("salsa", "Salsa", 1.0, "cup", PANTRY),
                ing("cheddar", "Shredded cheddar", 1.0, "cup", DAIRY),
                ing("taco-seasoning", "Taco seasoning", 2.0, "tbsp", PANTRY)
            ),
            listOf(
                step("Start rice", "Rinse rice and start the rice cooker.", "Rice cooker", 22 * 60),
                step("Brown beef", "Cook beef in a hot pan, breaking it into small pieces.", "Pan", 8 * 60),
                step("Season", "Drain excess fat, add taco seasoning and a splash of water.", "Pan", 3 * 60),
                step("Warm sides", "Stir in drained beans and corn until hot.", "Pan", 4 * 60),
                step("Build bowls", "Divide rice and beef mixture; top dinner with salsa and cheese.", "Bowls")
            )
        ),
        recipe(
            "tikka-masala", "Chicken tikka masala + rice", "Chicken tikka masala", 10, 28,
            listOf("Pan", "Rice cooker"), listOf("Saucy", "Great leftovers"),
            "This reheats especially well; cool rice quickly before refrigerating.",
            listOf(
                ing("chicken-thighs", "Boneless chicken thighs", 1.0, "lb", MEAT),
                ing("rice", "Basmati rice", 1.5, "cups", PANTRY),
                ing("tikka-sauce", "Tikka masala simmer sauce", 1.0, "jar", PANTRY),
                ing("yogurt", "Plain yogurt", 0.5, "cup", DAIRY),
                ing("naan", "Naan", 4.0, "pieces", BAKERY)
            ),
            listOf(
                step("Start rice", "Rinse basmati rice and start the rice cooker.", "Rice cooker", 22 * 60),
                step("Brown chicken", "Cut chicken into chunks and brown in a lightly oiled pan.", "Pan", 7 * 60),
                step("Simmer", "Add tikka masala sauce and simmer until chicken reaches 165°F.", "Pan", 10 * 60),
                step("Finish sauce", "Remove from heat and stir in yogurt for a milder finish.", "Pan"),
                step("Warm naan", "Toast naan and divide rice and curry into dinner and lunch portions.", "Toaster", 2 * 60)
            )
        ),
        recipe(
            "steak-rice-peppers", "Steak + rice + peppers", "Steak, rice + peppers", 8, 26,
            listOf("Ooni Volt or pan", "Rice cooker"), listOf("High heat"),
            "Slice only the lunch portion after resting; keep rice separate.",
            listOf(
                ing("sirloin", "Sirloin steak", 1.25, "lb", MEAT),
                ing("rice", "White rice", 1.5, "cups", PANTRY),
                ing("bell-peppers", "Bell peppers", 3.0, "count", PRODUCE),
                ing("onion", "Yellow onion", 1.0, "count", PRODUCE),
                ing("olive-oil", "Olive oil", 1.0, "tbsp", PANTRY, true)
            ),
            listOf(
                step("Start rice", "Rinse rice and start the rice cooker.", "Rice cooker", 22 * 60),
                step("Prep", "Slice peppers and onion. Pat steak dry and season.", "Prep board"),
                step("Cook vegetables", "Cook peppers and onion at high heat until browned but crisp.", "Ooni Volt or pan", 7 * 60),
                step("Cook steak", "Sear steak to preferred doneness.", "Ooni Volt or pan", 8 * 60),
                step("Rest and slice", "Rest steak before slicing across the grain.", "Cutting board", 5 * 60)
            )
        ),
        recipe(
            "beef-tacos", "Beef tacos", "Beef tacos", 10, 20,
            listOf("Pan", "Microwave"), listOf("Tacos", "Great leftovers"),
            "Store taco filling separately; reheat before assembling.",
            listOf(
                ing("ground-beef", "Ground beef", 1.0, "lb", MEAT),
                ing("tortillas-small", "Small tortillas", 12.0, "count", BAKERY),
                ing("taco-seasoning", "Taco seasoning", 2.0, "tbsp", PANTRY),
                ing("lettuce", "Shredded lettuce", 3.0, "cups", PRODUCE),
                ing("tomato", "Tomatoes", 2.0, "count", PRODUCE),
                ing("cheddar", "Shredded cheddar", 1.0, "cup", DAIRY),
                ing("salsa", "Salsa", 1.0, "cup", PANTRY)
            ),
            listOf(
                step("Prep toppings", "Chop tomatoes and set out lettuce, cheese, and salsa.", "Prep board"),
                step("Brown beef", "Cook beef in a hot pan, breaking it apart.", "Pan", 8 * 60),
                step("Season", "Drain excess fat and simmer with seasoning and water.", "Pan", 3 * 60),
                step("Warm tortillas", "Warm tortillas under a damp towel.", "Microwave", 45),
                step("Build", "Fill dinner tacos. Pack lunch filling and toppings separately.", "Counter")
            )
        ),
        recipe(
            "spaghetti-meatballs", "Spaghetti + meatballs", "Spaghetti + meatballs", 10, 30,
            listOf("CRISPi", "Pot"), listOf("Pasta", "Great leftovers"),
            "Toss lunch pasta with a little extra sauce before refrigerating.",
            listOf(
                ing("meatballs", "Frozen beef meatballs", 16.0, "count", FROZEN),
                ing("spaghetti", "Spaghetti", 12.0, "oz", PANTRY),
                ing("marinara", "Marinara sauce", 3.0, "cups", PANTRY),
                ing("parmesan", "Parmesan", 0.5, "cup", DAIRY)
            ),
            listOf(
                step("Start meatballs", "Air fry meatballs at 390°F until browned and hot.", "CRISPi", 12 * 60),
                step("Boil pasta", "Cook spaghetti in salted water until al dente.", "Pot", 10 * 60),
                step("Warm sauce", "Warm marinara in a second pot or microwave-safe bowl.", "Pot or microwave", 6 * 60),
                step("Combine", "Add meatballs to sauce and simmer briefly.", "Pot", 3 * 60),
                step("Serve", "Toss pasta with sauce and finish with Parmesan.", "Bowls")
            )
        ),
        recipe(
            "chicken-pesto-pasta", "Chicken pesto pasta", "Chicken pesto pasta", 10, 26,
            listOf("CRISPi", "Pot"), listOf("Pasta", "Great leftovers"),
            "Add a spoonful of pasta water to the lunch portion before chilling.",
            listOf(
                ing("chicken-breast", "Chicken breasts", 1.0, "lb", MEAT),
                ing("pasta", "Short pasta", 12.0, "oz", PANTRY),
                ing("pesto", "Basil pesto", 0.75, "cup", PANTRY),
                ing("cherry-tomatoes", "Cherry tomatoes", 1.0, "pint", PRODUCE),
                ing("parmesan", "Parmesan", 0.5, "cup", DAIRY)
            ),
            listOf(
                step("Cook chicken", "Season and air fry chicken at 400°F until 165°F.", "CRISPi", 14 * 60),
                step("Boil pasta", "Cook pasta until al dente; reserve some pasta water.", "Pot", 10 * 60),
                step("Halve tomatoes", "Cut cherry tomatoes while the pasta cooks.", "Prep board"),
                step("Sauce", "Drain pasta and toss off heat with pesto and enough pasta water to loosen.", "Pot"),
                step("Finish", "Slice chicken, fold in tomatoes, and top with Parmesan.", "Pot")
            )
        ),
        recipe(
            "pizza", "Pepperoni or margherita pizza", "Pizza night", 10, 20,
            listOf("Ooni Volt"), listOf("Pizza", "Weekend"),
            "Pizza is best fresh; save extra dough and toppings instead of cooked slices.",
            listOf(
                ing("pizza-dough", "Pizza dough balls", 2.0, "count", BAKERY),
                ing("pizza-sauce", "Pizza sauce", 1.0, "cup", PANTRY),
                ing("mozzarella", "Low-moisture mozzarella", 2.0, "cups", DAIRY),
                ing("pepperoni", "Pepperoni", 4.0, "oz", MEAT),
                ing("basil", "Fresh basil", 1.0, "bunch", PRODUCE)
            ),
            listOf(
                step("Preheat", "Preheat the Ooni Volt and stone for the selected pizza style.", "Ooni Volt", 20 * 60),
                step("Stretch dough", "Stretch one dough ball on a lightly floured surface.", "Counter"),
                step("Top lightly", "Add a thin layer of sauce, mozzarella, and pepperoni or basil.", "Pizza peel"),
                step("Bake", "Launch onto the stone and turn as needed until evenly browned.", "Ooni Volt", 5 * 60),
                step("Rest", "Rest for 2 minutes, slice, and start the second pizza.", "Cutting board", 2 * 60)
            )
        ),
        recipe(
            "lazy-chicken-sandwich", "Ultra-lazy chicken sandwich + fries", "Lazy chicken sandwich", 5, 20,
            listOf("CRISPi"), listOf("Ultra easy"),
            "Cook only what you need; the frozen components stay ready for another night.",
            listOf(
                ing("breaded-chicken", "Breaded chicken fillets", 4.0, "pieces", FROZEN),
                ing("burger-buns", "Burger buns", 4.0, "count", BAKERY),
                ing("frozen-fries", "Frozen fries", 1.0, "lb", FROZEN),
                ing("lettuce", "Lettuce", 4.0, "leaves", PRODUCE),
                ing("pickles", "Pickle slices", 0.5, "cup", PANTRY),
                ing("mayo", "Mayonnaise", 0.25, "cup", PANTRY)
            ),
            listOf(
                step("Start fries", "Air fry fries at 400°F, shaking twice.", "CRISPi basket 1", 18 * 60),
                step("Cook chicken", "Air fry chicken fillets in basket 2 until crisp and hot.", "CRISPi basket 2", 14 * 60),
                step("Toast buns", "Toast buns during the final minutes if desired.", "Toaster", 2 * 60),
                step("Build", "Spread mayo and add chicken, lettuce, and pickles.", "Counter"),
                step("Serve", "Salt fries and serve immediately.", "Plate")
            )
        )
    )

    fun recipe(id: String): Recipe? = recipes.firstOrNull { it.id == id }
}

private fun recipe(
    id: String,
    name: String,
    shortName: String,
    effort: Int,
    total: Int,
    equipment: List<String>,
    tags: List<String>,
    leftovers: String,
    ingredients: List<Ingredient>,
    steps: List<RecipeStep>
) = Recipe(id, name, shortName, effort, total, equipment = equipment, tags = tags, leftoverNote = leftovers, ingredients = ingredients, steps = steps)

private fun ing(
    id: String,
    name: String,
    amount: Double,
    unit: String,
    category: com.mealos.model.IngredientCategory,
    pantry: Boolean = false
) = Ingredient(id, name, amount, unit, category, pantry)

private fun step(
    title: String,
    instruction: String,
    equipment: String,
    timer: Int? = null,
    tip: String? = null
) = RecipeStep(title, instruction, equipment, timer, tip)
