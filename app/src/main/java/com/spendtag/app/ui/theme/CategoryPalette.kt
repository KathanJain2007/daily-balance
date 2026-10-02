package com.spendtag.app.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// =====================================================================
// SpendTag Design System: Category Palette
// Consistent colors and icons across chips, canvas charts, and list rows.
// =====================================================================

data class CategoryDefinition(
    val id: Long,
    val name: String,
    val colorHex: String,
    val color: Color,
    val iconName: String,
    val icon: ImageVector,
    val isDefault: Boolean = true
)

object CategoryPalette {

    val FoodAndDining = CategoryDefinition(
        id = 1L,
        name = "Food & Dining",
        colorHex = "#FF7043",
        color = Color(0xFFFF7043),
        iconName = "restaurant",
        icon = Icons.Rounded.Restaurant
    )

    val Groceries = CategoryDefinition(
        id = 2L,
        name = "Groceries",
        colorHex = "#43A047",
        color = Color(0xFF43A047),
        iconName = "shopping_cart",
        icon = Icons.Rounded.ShoppingCart
    )

    val TravelAndCommute = CategoryDefinition(
        id = 3L,
        name = "Travel & Commute",
        colorHex = "#0288D1",
        color = Color(0xFF0288D1),
        iconName = "directions_car",
        icon = Icons.Rounded.DirectionsCar
    )

    val BillsAndUtilities = CategoryDefinition(
        id = 4L,
        name = "Bills & Utilities",
        colorHex = "#8E24AA",
        color = Color(0xFF8E24AA),
        iconName = "receipt_long",
        icon = Icons.Rounded.ReceiptLong
    )

    val Shopping = CategoryDefinition(
        id = 5L,
        name = "Shopping",
        colorHex = "#E91E63",
        color = Color(0xFFE91E63),
        iconName = "shopping_bag",
        icon = Icons.Rounded.ShoppingBag
    )

    val Entertainment = CategoryDefinition(
        id = 6L,
        name = "Entertainment",
        colorHex = "#5E35B1",
        color = Color(0xFF5E35B1),
        iconName = "movie",
        icon = Icons.Rounded.Movie
    )

    val HealthAndMedical = CategoryDefinition(
        id = 7L,
        name = "Health & Medical",
        colorHex = "#D32F2F",
        color = Color(0xFFD32F2F),
        iconName = "local_hospital",
        icon = Icons.Rounded.LocalHospital
    )

    val RentAndHousing = CategoryDefinition(
        id = 8L,
        name = "Rent & Housing",
        colorHex = "#6D4C41",
        color = Color(0xFF6D4C41),
        iconName = "home",
        icon = Icons.Rounded.Home
    )

    val Investments = CategoryDefinition(
        id = 9L,
        name = "Investments",
        colorHex = "#00897B",
        color = Color(0xFF00897B),
        iconName = "trending_up",
        icon = Icons.Rounded.TrendingUp
    )

    val PersonalCare = CategoryDefinition(
        id = 10L,
        name = "Personal Care",
        colorHex = "#F06292",
        color = Color(0xFFF06292),
        iconName = "spa",
        icon = Icons.Rounded.Spa
    )

    val Transfers = CategoryDefinition(
        id = 11L,
        name = "Transfers",
        colorHex = "#1976D2",
        color = Color(0xFF1976D2),
        iconName = "swap_horiz",
        icon = Icons.Rounded.SwapHoriz
    )

    val Miscellaneous = CategoryDefinition(
        id = 12L,
        name = "Miscellaneous",
        colorHex = "#607D8B",
        color = Color(0xFF607D8B),
        iconName = "category",
        icon = Icons.Rounded.Category
    )

    val defaultCategories: List<CategoryDefinition> = listOf(
        FoodAndDining,
        Groceries,
        TravelAndCommute,
        BillsAndUtilities,
        Shopping,
        Entertainment,
        HealthAndMedical,
        RentAndHousing,
        Investments,
        PersonalCare,
        Transfers,
        Miscellaneous
    )

    private val categoryById = defaultCategories.associateBy { it.id }
    private val categoryByName = defaultCategories.associateBy { it.name.lowercase() }

    fun getCategory(id: Long?): CategoryDefinition {
        return categoryById[id] ?: Miscellaneous
    }

    fun getCategoryByName(name: String?): CategoryDefinition {
        if (name.isNullOrBlank()) return Miscellaneous
        return categoryByName[name.trim().lowercase()] ?: Miscellaneous
    }

    /**
     * Parses a hex color string (e.g. #FF7043 or #FFFF7043) safely with fallback.
     */
    fun parseColorHex(hex: String, fallback: Color = Miscellaneous.color): Color {
        return try {
            val cleanHex = hex.removePrefix("#")
            when (cleanHex.length) {
                6 -> Color(cleanHex.toLong(16) or 0x00000000FF000000)
                8 -> Color(cleanHex.toLong(16))
                else -> fallback
            }
        } catch (_: Exception) {
            fallback
        }
    }
}
