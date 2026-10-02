package com.spendtag.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.spendtag.app.SpendTagApplication
import com.spendtag.app.core.database.entity.PaymentEntity
import com.spendtag.app.core.database.entity.TagHistoryEntity
import com.spendtag.app.core.model.PaymentDirection
import com.spendtag.app.core.model.PaymentSource
import com.spendtag.app.core.model.PaymentStatus
import com.spendtag.app.core.model.PaymentUiModel
import com.spendtag.app.core.preferences.PreferencesManager
import com.spendtag.app.feature.addcash.AddCashBottomSheet
import com.spendtag.app.feature.detail.PaymentDetailBottomSheet
import com.spendtag.app.feature.detail.component.SplitBillBottomSheet
import com.spendtag.app.feature.friends.FriendsScreen
import com.spendtag.app.feature.friends.FriendsViewModel
import com.spendtag.app.feature.home.HomeScreen
import com.spendtag.app.feature.home.HomeViewModel
import com.spendtag.app.feature.insights.InsightsScreen
import com.spendtag.app.feature.insights.InsightsViewModel
import com.spendtag.app.feature.onboarding.OnboardingScreen
import com.spendtag.app.feature.payments.PaymentsScreen
import com.spendtag.app.feature.payments.PaymentsViewModel
import com.spendtag.app.feature.pending.PendingScreen
import com.spendtag.app.feature.pending.PendingViewModel
import com.spendtag.app.feature.settings.SettingsScreen
import com.spendtag.app.feature.settings.SettingsViewModel
import com.spendtag.app.ui.theme.CategoryPalette
import kotlinx.coroutines.launch

@Composable
fun SpendTagNavHost(
    navController: NavHostController,
    preferencesManager: PreferencesManager
) {
    val context = LocalContext.current
    val app = context.applicationContext as SpendTagApplication
    val coroutineScope = rememberCoroutineScope()

    val homeViewModel = remember {
        HomeViewModel(
            paymentRepository = app.paymentRepository,
            splitDao = app.database.splitDao()
        )
    }

    val pendingViewModel = remember {
        PendingViewModel(
            paymentRepository = app.paymentRepository,
            database = app.database
        )
    }

    val paymentsViewModel = remember {
        PaymentsViewModel(paymentRepository = app.paymentRepository)
    }

    val insightsViewModel = remember {
        InsightsViewModel(paymentRepository = app.paymentRepository)
    }

    val friendsViewModel = remember {
        FriendsViewModel(
            splitRepository = app.splitRepository,
            paymentRepository = app.paymentRepository
        )
    }

    val settingsViewModel = remember {
        SettingsViewModel(
            preferencesManager = preferencesManager,
            database = app.database
        )
    }

    var showAddCashSheet by remember { mutableStateOf(false) }
    var showSplitSheetForPayment by remember { mutableStateOf<PaymentUiModel?>(null) }
    val allFriends by app.splitRepository.allFriends.collectAsState(initial = emptyList())
    var selectedDetailPaymentId by remember { mutableStateOf<Long?>(null) }
    var selectedDetailPayment by remember { mutableStateOf<PaymentUiModel?>(null) }

    fun openPaymentDetail(paymentId: Long) {
        coroutineScope.launch {
            val entity = app.paymentRepository.getPaymentById(paymentId)
            if (entity != null) {
                selectedDetailPayment = PaymentUiModel(
                    id = entity.id,
                    amountPaise = entity.amountPaise,
                    mySharePaise = entity.myShare,
                    direction = entity.direction,
                    upiId = entity.upiId,
                    refNo = entity.refNo,
                    source = entity.source,
                    createdAt = entity.createdAt,
                    categoryId = entity.categoryId,
                    category = CategoryPalette.getCategory(entity.categoryId),
                    status = entity.status,
                    payeeName = entity.note ?: entity.upiId ?: "Payment",
                    note = entity.note
                )
            }
        }
    }

    val startDestination = if (preferencesManager.isOnboardingCompleted) {
        Screen.Home.route
    } else {
        Screen.Onboarding.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onOnboardingFinished = {
                    preferencesManager.isOnboardingCompleted = true
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToPending = { navController.navigate(Screen.Pending.route) },
                onNavigateToPayments = { navController.navigate(Screen.Payments.route) },
                onNavigateToDetail = { paymentId -> openPaymentDetail(paymentId) },
                onNavigateToInsights = { navController.navigate(Screen.Insights.route) },
                onNavigateToFriends = { navController.navigate(Screen.Friends.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onOpenAddCash = { showAddCashSheet = true }
            )
        }

        composable(Screen.Pending.route) {
            PendingScreen(
                viewModel = pendingViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Payments.route) {
            PaymentsScreen(
                viewModel = paymentsViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { paymentId -> openPaymentDetail(paymentId) }
            )
        }

        composable(Screen.Insights.route) {
            InsightsScreen(
                viewModel = insightsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Friends.route) {
            FriendsScreen(
                viewModel = friendsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }

    // Modal Bottom Sheet: Add Cash Expense (< 5 Seconds)
    if (showAddCashSheet) {
        AddCashBottomSheet(
            onDismiss = { showAddCashSheet = false },
            onSaveExpense = { amountPaise, categoryId, note ->
                coroutineScope.launch {
                    val manualPayment = PaymentEntity(
                        amountPaise = amountPaise,
                        direction = PaymentDirection.OUT,
                        upiId = null,
                        refNo = null,
                        source = PaymentSource.MANUAL,
                        createdAt = System.currentTimeMillis(),
                        categoryId = categoryId,
                        myShare = amountPaise,
                        status = PaymentStatus.TAGGED,
                        note = note
                    )
                    app.paymentRepository.insertIfUnique(manualPayment)
                }
            }
        )
    }

    // Modal Bottom Sheet: Payment Detail
    selectedDetailPayment?.let { payment ->
        PaymentDetailBottomSheet(
            payment = payment,
            onDismiss = { selectedDetailPayment = null },
            onChangeCategory = { newCategoryId ->
                coroutineScope.launch {
                    app.paymentRepository.tagPayment(payment.id, newCategoryId)
                    if (payment.upiId != null) {
                        app.database.counterpartyDao().recordCategoryChoice(
                            upiId = payment.upiId,
                            categoryId = newCategoryId,
                            fallbackAlias = payment.payeeName
                        )
                    }
                    app.database.tagHistoryDao().insertTagHistory(
                        TagHistoryEntity(
                            paymentId = payment.id,
                            upiId = payment.upiId,
                            categoryId = newCategoryId,
                            isSilent = false
                        )
                    )
                    openPaymentDetail(payment.id)
                }
            },
            onSaveAlias = { newAlias ->
                coroutineScope.launch {
                    if (payment.upiId != null) {
                        app.database.counterpartyDao().updateAlias(payment.upiId, newAlias)
                    }
                    val current = app.paymentRepository.getPaymentById(payment.id)
                    if (current != null) {
                        app.paymentRepository.updatePayment(current.copy(note = newAlias))
                    }
                    openPaymentDetail(payment.id)
                }
            },
            onSaveNote = { newNote ->
                coroutineScope.launch {
                    val current = app.paymentRepository.getPaymentById(payment.id)
                    if (current != null) {
                        app.paymentRepository.updatePayment(current.copy(note = newNote))
                    }
                }
            },
            onOpenSplitSheet = {
                showSplitSheetForPayment = payment
            },
            onDelete = if (payment.source == PaymentSource.MANUAL) {
                {
                    coroutineScope.launch {
                        val current = app.paymentRepository.getPaymentById(payment.id)
                        if (current != null) {
                            app.paymentRepository.deletePayment(current)
                        }
                        selectedDetailPayment = null
                    }
                }
            } else null
        )
    }

    // Modal Bottom Sheet: Split Bill
    showSplitSheetForPayment?.let { paymentToSplit ->
        SplitBillBottomSheet(
            payment = paymentToSplit,
            allFriends = allFriends,
            onDismiss = { showSplitSheetForPayment = null },
            onAddNewFriend = { friendName ->
                coroutineScope.launch {
                    app.splitRepository.addFriend(friendName)
                }
            },
            onConfirmSplit = { mySharePaise, friendShares ->
                coroutineScope.launch {
                    app.splitRepository.recordSplit(paymentToSplit.id, mySharePaise, friendShares)
                    openPaymentDetail(paymentToSplit.id)
                }
            }
        )
    }
}
