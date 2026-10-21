package io.github.dangtruong96.renewalradar.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One tracked subscription or free trial.
 *
 * Dates are stored as ISO-8601 strings (`yyyy-MM-dd`, [java.time.LocalDate]
 * format) and parsed in the domain layer. Storing plain strings keeps the
 * schema free of Room type converters, so there is nothing to misconfigure.
 *
 * Money is stored as [amountMinor]: the amount in the currency's smallest
 * unit (cents for USD/EUR, yen for JPY, đồng for VND). How many minor units
 * make one major unit is decided by `java.util.Currency.defaultFractionDigits`,
 * never by a hard-coded 100 — see `domain/Money.kt`.
 */
@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val amountMinor: Long,
    val currency: String = "USD",
    /** Name of a [io.github.dangtruong96.renewalradar.domain.BillingCycle] value. */
    val cycle: String = "MONTHLY",
    /** ISO date of a known renewal date (the anchor the cycle steps from). */
    val anchorDate: String,
    val isTrial: Boolean = false,
    /** ISO date the free trial ends and converts to paid; null for non-trials. */
    val trialEndDate: String? = null,
    /** How many days before the effective date to remind. */
    val remindDaysBefore: Int = 3,
    val category: String = "",
    val notes: String = "",
    /** Deep link to the service's cancellation page, if known. */
    val cancelUrl: String = ""
)
