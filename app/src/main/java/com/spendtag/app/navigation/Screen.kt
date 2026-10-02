package com.spendtag.app.navigation

/**
 * SpendTag Navigation Routes.
 */
sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Pending : Screen("pending")
    object Payments : Screen("payments")
    object PaymentDetail : Screen("payment_detail/{paymentId}") {
        fun createRoute(paymentId: Long): String = "payment_detail/$paymentId"
    }
    object Insights : Screen("insights")
    object Friends : Screen("friends")
    object Settings : Screen("settings")
}
