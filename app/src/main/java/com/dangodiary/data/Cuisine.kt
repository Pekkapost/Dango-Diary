package com.dangodiary.data

/** Top-level grouping shown as a section header in the cuisine picker. */
enum class CuisineGroup(val label: String) {
    RESTAURANT("Restaurants"),
    CAFE("Cafés"),
    BAR("Bars"),
}

/**
 * A picker option. `id` is what we persist (stable across label changes); `label` is what we show.
 * Keep ids snake_case and ASCII so existing rows survive a label edit.
 */
data class Cuisine(val id: String, val label: String, val group: CuisineGroup)

object CuisineCatalog {
    val all: List<Cuisine> = listOf(
        // Restaurants
        Cuisine("american", "American", CuisineGroup.RESTAURANT),
        Cuisine("bbq", "BBQ", CuisineGroup.RESTAURANT),
        Cuisine("burger", "Burger", CuisineGroup.RESTAURANT),
        Cuisine("chinese", "Chinese", CuisineGroup.RESTAURANT),
        Cuisine("french", "French", CuisineGroup.RESTAURANT),
        Cuisine("greek", "Greek", CuisineGroup.RESTAURANT),
        Cuisine("indian", "Indian", CuisineGroup.RESTAURANT),
        Cuisine("italian", "Italian", CuisineGroup.RESTAURANT),
        Cuisine("japanese", "Japanese", CuisineGroup.RESTAURANT),
        Cuisine("korean", "Korean", CuisineGroup.RESTAURANT),
        Cuisine("mediterranean", "Mediterranean", CuisineGroup.RESTAURANT),
        Cuisine("mexican", "Mexican", CuisineGroup.RESTAURANT),
        Cuisine("pizza", "Pizza", CuisineGroup.RESTAURANT),
        Cuisine("ramen", "Ramen", CuisineGroup.RESTAURANT),
        Cuisine("seafood", "Seafood", CuisineGroup.RESTAURANT),
        Cuisine("spanish", "Spanish", CuisineGroup.RESTAURANT),
        Cuisine("steakhouse", "Steakhouse", CuisineGroup.RESTAURANT),
        Cuisine("sushi", "Sushi", CuisineGroup.RESTAURANT),
        Cuisine("thai", "Thai", CuisineGroup.RESTAURANT),
        Cuisine("vietnamese", "Vietnamese", CuisineGroup.RESTAURANT),
        // Cafés
        Cuisine("bakery", "Bakery", CuisineGroup.CAFE),
        Cuisine("boba", "Boba", CuisineGroup.CAFE),
        Cuisine("coffee", "Coffee", CuisineGroup.CAFE),
        Cuisine("dessert", "Dessert", CuisineGroup.CAFE),
        Cuisine("ice_cream", "Ice cream", CuisineGroup.CAFE),
        // Bars
        Cuisine("bar", "Bar", CuisineGroup.BAR),
        Cuisine("brewery", "Brewery", CuisineGroup.BAR),
        Cuisine("cocktail_bar", "Cocktail bar", CuisineGroup.BAR),
        Cuisine("pub", "Pub", CuisineGroup.BAR),
        Cuisine("wine_bar", "Wine bar", CuisineGroup.BAR),
    )

    private val byId: Map<String, Cuisine> = all.associateBy { it.id }

    /** Ordered by CuisineGroup declaration order, then by the catalog order within each group. */
    val grouped: List<Pair<CuisineGroup, List<Cuisine>>> =
        CuisineGroup.entries.map { g -> g to all.filter { it.group == g } }

    fun labelFor(id: String?): String? = id?.let { byId[it]?.label }

    /** The supertype an entry's cuisine belongs to, or null if [id] is unknown / not set. */
    fun groupFor(id: String?): CuisineGroup? = id?.let { byId[it]?.group }
}
