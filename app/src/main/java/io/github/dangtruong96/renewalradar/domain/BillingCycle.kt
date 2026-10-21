package io.github.dangtruong96.renewalradar.domain

import java.time.LocalDate
import java.time.Period

/**
 * How often a subscription charges. [period] is the step applied to an
 * anchor date to reach the next (or previous) charge.
 */
enum class BillingCycle(val period: Period, val label: String) {
    WEEKLY(Period.ofWeeks(1), "Weekly"),
    MONTHLY(Period.ofMonths(1), "Monthly"),
    QUARTERLY(Period.ofMonths(3), "Quarterly"),
    YEARLY(Period.ofYears(1), "Yearly");

    fun next(date: LocalDate): LocalDate = date.plus(period)

    fun previous(date: LocalDate): LocalDate = date.minus(period)

    companion object {
        /** Parse a stored enum name; unknown values fall back to MONTHLY. */
        fun fromName(name: String): BillingCycle =
            entries.firstOrNull { it.name == name } ?: MONTHLY
    }
}
