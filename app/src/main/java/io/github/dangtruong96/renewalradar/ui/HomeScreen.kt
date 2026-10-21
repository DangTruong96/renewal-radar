package io.github.dangtruong96.renewalradar.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dangtruong96.renewalradar.data.Subscription
import io.github.dangtruong96.renewalradar.domain.Money
import io.github.dangtruong96.renewalradar.domain.RenewalCalculator
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val COMING_UP_WINDOW_DAYS = 30L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: RenewalViewModel,
    onAdd: () -> Unit,
    onEdit: (Subscription) -> Unit
) {
    val subscriptions by viewModel.subscriptions.collectAsState()
    val monthlyTotals by viewModel.monthlyTotalsByCurrency.collectAsState()
    val today = remember { LocalDate.now() }
    val comingUp = remember(subscriptions, today) {
        RenewalCalculator.upcoming(subscriptions, today, COMING_UP_WINDOW_DAYS)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("RenewalRadar") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DashboardCard(monthlyTotals = monthlyTotals)
            }

            if (subscriptions.isEmpty()) {
                item {
                    Text(
                        "No subscriptions yet. Tap + to add your first one — " +
                            "a streaming plan, a cloud bill, or that free trial you meant to cancel.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            if (comingUp.isNotEmpty()) {
                item {
                    Text("Coming up", style = MaterialTheme.typography.titleMedium)
                }
                items(comingUp, key = { "upcoming-${it.subscription.id}" }) { item ->
                    SubscriptionRow(
                        subscription = item.subscription,
                        effectiveDate = item.date,
                        today = today,
                        onClick = { onEdit(item.subscription) }
                    )
                }
            }

            if (subscriptions.isNotEmpty()) {
                item {
                    Text("All subscriptions", style = MaterialTheme.typography.titleMedium)
                }
                items(subscriptions, key = { "all-${it.id}" }) { sub ->
                    SubscriptionRow(
                        subscription = sub,
                        effectiveDate = RenewalCalculator.effectiveDate(sub, today),
                        today = today,
                        onClick = { onEdit(sub) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardCard(monthlyTotals: Map<String, Long>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "What your subscriptions cost",
                style = MaterialTheme.typography.titleMedium
            )
            if (monthlyTotals.isEmpty()) {
                Text(
                    "Add a subscription to see your monthly total.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                monthlyTotals.toSortedMap().forEach { (currency, monthlyMinor) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${Money.format(monthlyMinor, currency)} / month")
                        Text("${Money.format(monthlyMinor * 12, currency)} / year")
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionRow(
    subscription: Subscription,
    effectiveDate: LocalDate?,
    today: LocalDate,
    onClick: () -> Unit
) {
    val daysLeft = effectiveDate?.let { RenewalCalculator.daysUntil(it, today) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(subscription.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    Money.format(subscription.amountMinor, subscription.currency),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (subscription.isTrial) {
                    AssistChip(onClick = onClick, label = { Text("Free trial") })
                }
                if (effectiveDate != null && daysLeft != null) {
                    val label = when {
                        subscription.isTrial && daysLeft == 0L -> "Trial ends today"
                        subscription.isTrial -> "Trial ends ${effectiveDate.format(DATE_FORMAT)}"
                        daysLeft == 0L -> "Renews today"
                        daysLeft == 1L -> "Renews tomorrow"
                        else -> "Renews ${effectiveDate.format(DATE_FORMAT)}"
                    }
                    AssistChip(onClick = onClick, label = { Text(label) })
                    AssistChip(
                        onClick = onClick,
                        label = {
                            Text(
                                when (daysLeft) {
                                    0L -> "today"
                                    1L -> "1 day left"
                                    else -> "$daysLeft days left"
                                }
                            )
                        },
                        colors = if (daysLeft <= 3) {
                            AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                labelColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        } else {
                            AssistChipDefaults.assistChipColors()
                        }
                    )
                }
            }
            if (subscription.category.isNotBlank()) {
                Text(
                    subscription.category,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
