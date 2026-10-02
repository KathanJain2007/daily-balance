package com.spendtag.app.feature.pending

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendtag.app.core.common.CurrencyFormatter
import com.spendtag.app.core.common.DateTimeUtils
import com.spendtag.app.ui.theme.AmountLargeStyle
import com.spendtag.app.ui.theme.CategoryDefinition
import com.spendtag.app.ui.theme.CategoryPalette
import com.spendtag.app.ui.theme.IncomeGreenLight
import com.spendtag.app.ui.theme.SpendTagCardShape
import com.spendtag.app.ui.theme.SpendTagChipShape
import com.spendtag.app.ui.theme.SpendTagHeroCardShape

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PendingScreen(
    viewModel: PendingViewModel,
    onNavigateBack: () -> Unit
) {
    val pendingCards by viewModel.pendingCards.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (pendingCards.isEmpty()) "Pending" else "Pending (${pendingCards.size})",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (pendingCards.isEmpty()) {
                AllCaughtUpCelebration(onNavigateBack = onNavigateBack)
            } else {
                val currentCard = pendingCards.first()

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Card Stack item
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = SpendTagHeroCardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = DateTimeUtils.formatShortDateTime(currentCard.payment.createdAt),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = CurrencyFormatter.formatPaise(currentCard.payment.amountPaise),
                                style = AmountLargeStyle,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = currentCard.payment.payeeName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Select Category:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.Start)
                            )

                            // Category Chips with best guess highlighted
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CategoryPalette.defaultCategories.forEach { category ->
                                    val isBestGuess = category.id == currentCard.bestGuessCategory.id
                                    PendingCategoryChip(
                                        category = category,
                                        isBestGuess = isBestGuess,
                                        onClick = {
                                            viewModel.tagPayment(
                                                currentCard.payment.id,
                                                category.id,
                                                currentCard.payment.upiId
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Swipe/Action Shortcuts at Bottom
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.skipPayment(currentCard.payment.id)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = SpendTagCardShape
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Skip",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Skip")
                        }

                        Button(
                            onClick = {
                                viewModel.acceptGuess(
                                    currentCard.payment.id,
                                    currentCard.bestGuessCategory,
                                    currentCard.payment.upiId
                                )
                            },
                            modifier = Modifier
                                .weight(2f)
                                .height(52.dp),
                            shape = SpendTagCardShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = currentCard.bestGuessCategory.color
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Accept",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Accept ${currentCard.bestGuessCategory.name}",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PendingCategoryChip(
    category: CategoryDefinition,
    isBestGuess: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = SpendTagChipShape,
        color = if (isBestGuess) category.color else category.color.copy(alpha = 0.12f),
        contentColor = if (isBestGuess) Color.White else category.color,
        modifier = Modifier
            .clickable(onClick = onClick)
            .then(
                if (isBestGuess) Modifier.border(2.dp, category.color, SpendTagChipShape)
                else Modifier
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = category.name,
                modifier = Modifier.size(15.dp),
                tint = if (isBestGuess) Color.White else category.color
            )
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isBestGuess) FontWeight.Bold else FontWeight.Medium,
                color = if (isBestGuess) Color.White else category.color
            )
        }
    }
}

@Composable
fun AllCaughtUpCelebration(
    onNavigateBack: () -> Unit
) {
    val scale = remember { Animatable(0.4f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(24.dp)
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(scale.value)
                .clip(CircleShape)
                .background(IncomeGreenLight.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.DoneAll,
                contentDescription = "All Caught Up",
                tint = IncomeGreenLight,
                modifier = Modifier.size(54.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "All Caught Up!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Every transaction is tagged and organized.\nNew expenses will pop up here as they occur.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onNavigateBack,
            shape = SpendTagCardShape
        ) {
            Text(text = "Back to Home")
        }
    }
}
