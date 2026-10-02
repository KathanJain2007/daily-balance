package com.spendtag.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// =====================================================================
// SpendTag Design System: Shapes & Radii
// 8dp grid system with rounded, tactile cards (16dp - 24dp)
// =====================================================================

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

// Reusable card & element shapes conforming to design specs
val SpendTagCardShape = RoundedCornerShape(16.dp)
val SpendTagHeroCardShape = RoundedCornerShape(24.dp)
val SpendTagBottomSheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
val SpendTagChipShape = RoundedCornerShape(10.dp)
val SpendTagPillShape = RoundedCornerShape(50)
