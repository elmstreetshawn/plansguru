package com.plansguru.agent

object FoodKnowledgeGraph {

    data class FoodEntity(
        val name: String,
        val category: String, // "CUISINE", "DISH", "DIETARY", "VIBE", "LOCATION"
        val parentCuisine: String? = null,
        val synonyms: List<String> = emptyList()
    )

    private val entities = listOf(
        // Cuisines & Dishes
        FoodEntity("Mexican", "CUISINE", synonyms = listOf("mexican", "tex-mex", "taco", "tacos", "burrito", "burritos", "fajitas", "enchiladas", "quesadilla")),
        FoodEntity("Italian", "CUISINE", synonyms = listOf("italian", "pizza", "pizzas", "pasta", "primavera", "lasagna", "trattoria", "risotto", "pesto")),
        FoodEntity("Japanese", "CUISINE", synonyms = listOf("japanese", "sushi", "ramen", "sashimi", "tempura", "teriyaki", "izakaya", "miso")),
        FoodEntity("Chinese", "CUISINE", synonyms = listOf("chinese", "dim sum", "dumplings", "fried rice", "lo mein")),
        FoodEntity("Thai", "CUISINE", synonyms = listOf("thai", "tai", "pad thai", "green curry", "tom yum")),
        FoodEntity("Indian", "CUISINE", synonyms = listOf("indian", "curry", "tikka masala", "naan", "biryani", "samosa")),
        FoodEntity("American", "CUISINE", synonyms = listOf("american", "burger", "burgers", "fries", "diner", "steak", "steakhouse")),
        FoodEntity("BBQ", "CUISINE", synonyms = listOf("bbq", "barbecue", "brisket", "ribs", "pulled pork", "smokehouse")),
        FoodEntity("French", "CUISINE", synonyms = listOf("french", "bistro", "bourguignon", "crepe")),
        FoodEntity("Greek", "CUISINE", synonyms = listOf("greek", "gyro", "souvlaki", "tzatziki")),
        FoodEntity("Mediterranean", "CUISINE", synonyms = listOf("mediterranean", "hummus", "falafel", "shawarma", "pita")),
        FoodEntity("Vietnamese", "CUISINE", synonyms = listOf("vietnamese", "pho", "banh mi")),
        FoodEntity("Korean", "CUISINE", synonyms = listOf("korean", "korean bbq", "kimchi", "bibimbap")),
        FoodEntity("Bakery", "CUISINE", synonyms = listOf("bakery", "cafe", "donuts", "coffee", "pastry", "espresso")),
        FoodEntity("Nuts & Peanuts", "CUISINE", synonyms = listOf("nut", "nuts", "peanut", "peanuts", "cashew", "almond")),

        // Dietary Restrictions
        FoodEntity("Vegan", "DIETARY", synonyms = listOf("vegan", "plant-based", "no dairy", "no meat")),
        FoodEntity("Vegetarian", "DIETARY", synonyms = listOf("vegetarian", "veggie", "meatless")),
        FoodEntity("Gluten-Free", "DIETARY", synonyms = listOf("gluten", "gluten-free", "celiac")),
        FoodEntity("Dairy-Free", "DIETARY", synonyms = listOf("dairy", "dairy-free", "lactose")),
        FoodEntity("Halal", "DIETARY", synonyms = listOf("halal")),
        FoodEntity("Kosher", "DIETARY", synonyms = listOf("kosher")),
        FoodEntity("Nut-Free", "DIETARY", synonyms = listOf("nut-free", "peanut-free", "nut allergy", "peanut allergy", "no nuts", "allergic to nuts")),

        // Atmosphere & Vibe
        FoodEntity("Quiet / Low Crowds", "VIBE", synonyms = listOf("quiet", "peaceful", "calm", "cozy", "intimate")),
        FoodEntity("Vibrant / Moderate", "VIBE", synonyms = listOf("lively", "vibrant", "patio", "outdoor seating", "sports bar")),
        FoodEntity("Loud / Party", "VIBE", synonyms = listOf("loud", "busy", "crowd", "crowds", "packed", "party")),

        // Greater Austin Places
        FoodEntity("Georgetown", "LOCATION", synonyms = listOf("georgetown", "78626")),
        FoodEntity("Round Rock", "LOCATION", synonyms = listOf("round rock", "78681", "78664")),
        FoodEntity("The Domain", "LOCATION", synonyms = listOf("domain", "rock rose", "78758")),
        FoodEntity("North Austin", "LOCATION", synonyms = listOf("north austin", "78753")),
        FoodEntity("Central Austin", "LOCATION", synonyms = listOf("central austin", "hyde park", "ut", "guadalupe", "78705", "78751")),
        FoodEntity("Downtown Austin", "LOCATION", synonyms = listOf("downtown", "78701")),
        FoodEntity("East Austin", "LOCATION", synonyms = listOf("east austin", "east 6th", "east 11th", "78702")),
        FoodEntity("South Congress", "LOCATION", synonyms = listOf("south congress", "soco", "south lamar", "barton springs", "78704")),
        FoodEntity("Manchaca", "LOCATION", synonyms = listOf("manchaca", "south austin", "78652", "78748"))
    )

    fun isFoodOrGeneralTerm(word: String): Boolean {
        val lower = word.lowercase().trim()
        if (lower.length <= 1) return true
        return entities.any { entity ->
            entity.synonyms.any { syn -> lower == syn || lower == "${syn}s" || syn.contains(lower) }
        }
    }

    fun lookupCuisineForTerm(term: String): String? {
        val lower = term.lowercase().trim()
        val match = entities.find { e -> e.category == "CUISINE" && e.synonyms.contains(lower) }
        return match?.name
    }

    fun lookupDietaryForTerm(term: String): String? {
        val lower = term.lowercase().trim()
        val match = entities.find { e -> e.category == "DIETARY" && e.synonyms.contains(lower) }
        return match?.name
    }

    fun lookupLocationForTerm(term: String): String? {
        val lower = term.lowercase().trim()
        val match = entities.find { e -> e.category == "LOCATION" && e.synonyms.contains(lower) }
        return match?.name
    }
}
