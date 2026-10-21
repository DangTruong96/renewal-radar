package io.github.dangtruong96.renewalradar.domain

import io.github.dangtruong96.renewalradar.data.Subscription
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Pure date/money math for renewals. No Android dependencies — everything
 * here is `java.time` and is covered by unit tests.
 */
object RenewalCalculator {

    /**
     * The first renewal date on or after [today], stepping from [anchor].
     *
     * [anchor] is a date on which the subscription is known to renew (for
     * example, the last charge or the sign-up date projected forward).
     * The cycle is applied repeatedly until the result is >= [today].
     *
     * Month-end note: [LocalDate.plusMonths] clamps to the last day of a
     * shorter month, so an anchor of Jan 31 steps to Feb 28/29 and then to
     * Mar 28/29 — the day drifts after a clamp. This mirrors how most
     * billing systems behave (they charge on the last day of short months)
     * and is accepted deliberately rather than special-cased.
     */
    fun nextRenewal(anchor: LocalDate, cycle: BillingCycle, today: LocalDate): LocalDate {
        var date = anchor
        // Step forward until the date reaches today…
        while (date.isBefore(today)) date = cycle.next(date)
        // …then, if the anchor started in the future, step back while the
        // previous cycle date is still today or later. Either way the
        // result is the first cycle date >= today.
        while (!cycle.previous(date).isBefore(today)) date = cycle.previous(date)
        return date
    }

    /** Days from [today] until [date]; negative if [date] is in the past. */
    fun daysUntil(date: LocalDate, today: LocalDate): Long =
        ChronoUnit.DAYS.between(today, date)

    /**
     * What this subscription costs per month, in minor units.
     * WEEKLY: amount * 52 / 12; MONTHLY: as-is; QUARTERLY: / 3;
     * YEARLY: / 12. Rounded to the nearest minor unit.
     */
    fun monthlyEquivalentMinor(amountMinor: Long, cycle: BillingCycle): Long = when (cycle) {
        BillingCycle.WEEKLY -> Math.round(amountMinor * 52.0 / 12.0)
        BillingCycle.MONTHLY -> amountMinor
        BillingCycle.QUARTERLY -> Math.round(amountMinor / 3.0)
        BillingCycle.YEARLY -> Math.round(amountMinor / 12.0)
    }

    /** Parse an ISO date string, returning null instead of throwing on bad input. */
    fun parseDate(iso: String?): LocalDate? =
        iso?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    /**
     * The date that matters for reminders: a trial's end date while it is a
     * trial, otherwise the next renewal computed from the anchor.
     */
    fun effectiveDate(subscription: Subscription, today: LocalDate): LocalDate? {
        if (subscription.isTrial) {
            parseDate(subscription.trialEndDate)?.let { return it }
        }
        val anchor = parseDate(subscription.anchorDate) ?: return null
        return nextRenewal(anchor, BillingCycle.fromName(subscription.cycle), today)
    }

    /** A subscription paired with its effective date, for lists and totals. */
    data class UpcomingItem(val subscription: Subscription, val date: LocalDate)

    /**
     * Items whose effective date falls in [today, today + withinDays],
     * sorted by date ascending (soonest first).
     */
    fun upcoming(
        subscriptions: List<Subscription>,
        today: LocalDate,
        withinDays: Long
    ): List<UpcomingItem> =
        subscriptions.mapNotNull { sub ->
            effectiveDate(sub, today)?.let { date ->
                val days = daysUntil(date, today)
                if (days in 0..withinDays) UpcomingItem(sub, date) else null
            }
        }.sortedBy { it.date }
}
