package io.github.dangtruong96.renewalradar

import io.github.dangtruong96.renewalradar.data.Subscription
import io.github.dangtruong96.renewalradar.domain.BillingCycle
import io.github.dangtruong96.renewalradar.domain.RenewalCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Deterministic unit tests for the renewal math. Every date is fixed;
 * nothing here depends on the wall clock.
 */
class RenewalCalculatorTest {

    private fun date(iso: String): LocalDate = LocalDate.parse(iso)

    private fun sub(
        name: String,
        anchor: String,
        cycle: BillingCycle = BillingCycle.MONTHLY,
        isTrial: Boolean = false,
        trialEnd: String? = null,
        amountMinor: Long = 1299,
        currency: String = "USD"
    ) = Subscription(
        name = name,
        amountMinor = amountMinor,
        currency = currency,
        cycle = cycle.name,
        anchorDate = anchor,
        isTrial = isTrial,
        trialEndDate = trialEnd
    )

    @Test
    fun monthlyNextRenewalRollsForward() {
        val result = RenewalCalculator.nextRenewal(
            date("2025-01-15"), BillingCycle.MONTHLY, date("2025-03-10")
        )
        assertEquals(date("2025-03-15"), result)
    }

    @Test
    fun anchorTodayReturnsToday() {
        val today = date("2025-03-10")
        val result = RenewalCalculator.nextRenewal(today, BillingCycle.MONTHLY, today)
        assertEquals(today, result)
    }

    @Test
    fun pastAnchorAdvancesMultipleCycles() {
        val result = RenewalCalculator.nextRenewal(
            date("2024-01-05"), BillingCycle.MONTHLY, date("2025-06-20")
        )
        assertEquals(date("2025-07-05"), result)
    }

    @Test
    fun futureAnchorStepsBackToFirstDateOnOrAfterToday() {
        val result = RenewalCalculator.nextRenewal(
            date("2025-12-15"), BillingCycle.MONTHLY, date("2025-03-10")
        )
        assertEquals(date("2025-03-15"), result)
    }

    @Test
    fun yearlyFeb29AnchorClampsToFeb28InNonLeapYear() {
        val result = RenewalCalculator.nextRenewal(
            date("2024-02-29"), BillingCycle.YEARLY, date("2025-01-10")
        )
        assertEquals(date("2025-02-28"), result)
    }

    @Test
    fun weeklyStepsBySevenDays() {
        val result = RenewalCalculator.nextRenewal(
            date("2025-03-03"), BillingCycle.WEEKLY, date("2025-03-12")
        )
        assertEquals(date("2025-03-17"), result)
    }

    @Test
    fun quarterlyStepsByThreeMonths() {
        val result = RenewalCalculator.nextRenewal(
            date("2025-01-31"), BillingCycle.QUARTERLY, date("2025-05-01")
        )
        assertEquals(date("2025-07-30"), result)
    }

    @Test
    fun daysUntilCountsDays() {
        assertEquals(5L, RenewalCalculator.daysUntil(date("2025-03-15"), date("2025-03-10")))
        assertEquals(0L, RenewalCalculator.daysUntil(date("2025-03-10"), date("2025-03-10")))
        assertEquals(-2L, RenewalCalculator.daysUntil(date("2025-03-08"), date("2025-03-10")))
    }

    @Test
    fun monthlyEquivalentWeekly() {
        // 1000 minor/week -> 1000 * 52 / 12 = 4333.33 -> 4333
        assertEquals(4333L, RenewalCalculator.monthlyEquivalentMinor(1000, BillingCycle.WEEKLY))
    }

    @Test
    fun monthlyEquivalentMonthlyIsIdentity() {
        assertEquals(1299L, RenewalCalculator.monthlyEquivalentMinor(1299, BillingCycle.MONTHLY))
    }

    @Test
    fun monthlyEquivalentQuarterly() {
        assertEquals(1000L, RenewalCalculator.monthlyEquivalentMinor(3000, BillingCycle.QUARTERLY))
    }

    @Test
    fun monthlyEquivalentYearly() {
        assertEquals(1000L, RenewalCalculator.monthlyEquivalentMinor(12000, BillingCycle.YEARLY))
        assertEquals(833L, RenewalCalculator.monthlyEquivalentMinor(9999, BillingCycle.YEARLY))
    }

    @Test
    fun trialEffectiveDateUsesTrialEnd() {
        val trial = sub(
            name = "Video app trial",
            anchor = "2025-01-01",
            isTrial = true,
            trialEnd = "2025-04-01"
        )
        val result = RenewalCalculator.effectiveDate(trial, date("2025-03-01"))
        assertEquals(date("2025-04-01"), result)
    }

    @Test
    fun nonTrialEffectiveDateUsesNextRenewal() {
        val plan = sub(name = "Music", anchor = "2025-01-15")
        val result = RenewalCalculator.effectiveDate(plan, date("2025-03-10"))
        assertEquals(date("2025-03-15"), result)
    }

    @Test
    fun upcomingSortsAscendingAndFiltersWindow() {
        val today = date("2025-03-10")
        val inFiveDays = sub(name = "A", anchor = "2025-03-15")
        val inFortyDays = sub(name = "B", anchor = "2025-04-19", cycle = BillingCycle.YEARLY)
        val inTwoDays = sub(name = "C", anchor = "2025-03-12")

        val result = RenewalCalculator.upcoming(
            listOf(inFiveDays, inFortyDays, inTwoDays), today, withinDays = 30
        )

        assertEquals(listOf("C", "A"), result.map { it.subscription.name })
        assertTrue(result.all { RenewalCalculator.daysUntil(it.date, today) in 0..30 })
    }
}
