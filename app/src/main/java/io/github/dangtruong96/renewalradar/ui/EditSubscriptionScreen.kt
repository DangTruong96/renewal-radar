package io.github.dangtruong96.renewalradar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.dangtruong96.renewalradar.data.Subscription
import io.github.dangtruong96.renewalradar.domain.BillingCycle
import io.github.dangtruong96.renewalradar.domain.Money
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val DISPLAY_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val REMIND_OPTIONS = listOf(1, 3, 7, 14)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSubscriptionScreen(
    existing: Subscription?,
    onSave: (Subscription) -> Unit,
    onDelete: (Subscription) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var currency by remember { mutableStateOf(existing?.currency ?: "USD") }
    var amountText by remember {
        mutableStateOf(existing?.let { Money.minorToPlainString(it.amountMinor, it.currency) } ?: "")
    }
    var cycle by remember {
        mutableStateOf(existing?.let { BillingCycle.fromName(it.cycle) } ?: BillingCycle.MONTHLY)
    }
    var anchorDate by remember {
        mutableStateOf(
            existing?.let { RenewalDate.parse(it.anchorDate) } ?: LocalDate.now()
        )
    }
    var isTrial by remember { mutableStateOf(existing?.isTrial ?: false) }
    var trialEndDate by remember {
        mutableStateOf(
            existing?.trialEndDate?.let { RenewalDate.parse(it) } ?: LocalDate.now().plusDays(7)
        )
    }
    var remindDays by remember { mutableIntStateOf(existing?.remindDaysBefore ?: 3) }
    var category by remember { mutableStateOf(existing?.category ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var cancelUrl by remember { mutableStateOf(existing?.cancelUrl ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Add subscription" else "Edit subscription") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name (e.g. Music streaming)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Price") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                CurrencyDropdown(
                    selected = currency,
                    onSelected = { currency = it },
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                if (Money.fractionDigits(currency) == 0)
                    "$currency has no minor unit — enter whole units (e.g. 1500)."
                else
                    "Enter the price as charged, e.g. 12.99 for $currency.",
                style = MaterialTheme.typography.bodySmall
            )

            CycleDropdown(
                selected = cycle,
                onSelected = { cycle = it },
                modifier = Modifier.fillMaxWidth()
            )

            DateField(
                label = "Next renewal (or a past charge date)",
                date = anchorDate,
                onDateChange = { anchorDate = it }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("This is a free trial", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = isTrial, onCheckedChange = { isTrial = it })
            }
            if (isTrial) {
                DateField(
                    label = "Trial ends (converts to paid)",
                    date = trialEndDate,
                    onDateChange = { trialEndDate = it }
                )
            }

            Text("Remind me this many days before", style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                REMIND_OPTIONS.forEach { days ->
                    FilterChip(
                        selected = remindDays == days,
                        onClick = { remindDays = days },
                        label = { Text("$days") }
                    )
                }
            }

            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = cancelUrl,
                onValueChange = { cancelUrl = it },
                label = { Text("Cancellation page URL (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    val amountMinor = Money.parseToMinor(amountText, currency)
                    when {
                        name.isBlank() -> error = "Give the subscription a name."
                        amountMinor == null -> error =
                            "Enter a valid price for $currency."
                        else -> onSave(
                            Subscription(
                                id = existing?.id ?: 0,
                                name = name.trim(),
                                amountMinor = amountMinor,
                                currency = currency,
                                cycle = cycle.name,
                                anchorDate = anchorDate.toString(),
                                isTrial = isTrial,
                                trialEndDate = if (isTrial) trialEndDate.toString() else null,
                                remindDaysBefore = remindDays,
                                category = category.trim(),
                                notes = notes.trim(),
                                cancelUrl = cancelUrl.trim()
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }

            if (existing != null) {
                Button(
                    onClick = { onDelete(existing) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete")
                }
            }
        }
    }
}

/** Small helper so this file and the calculator agree on ISO date parsing. */
private object RenewalDate {
    fun parse(iso: String): LocalDate? =
        runCatching { LocalDate.parse(iso) }.getOrNull()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyDropdown(
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Currency") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Money.SUPPORTED_CURRENCIES.forEach { code ->
                DropdownMenuItem(
                    text = { Text(code) },
                    onClick = {
                        onSelected(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CycleDropdown(
    selected: BillingCycle,
    onSelected: (BillingCycle) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Billing cycle") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            BillingCycle.entries.forEach { cycle ->
                DropdownMenuItem(
                    text = { Text(cycle.label) },
                    onClick = {
                        onSelected(cycle)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    label: String,
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = date.format(DISPLAY_DATE),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            TextButton(onClick = { showPicker = true }) { Text("Pick") }
        },
        modifier = Modifier.fillMaxWidth()
    )
    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onDateChange(
                                Instant.ofEpochMilli(millis)
                                    .atZone(ZoneOffset.UTC)
                                    .toLocalDate()
                            )
                        }
                        showPicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
