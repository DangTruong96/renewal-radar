package io.github.dangtruong96.renewalradar

import io.github.dangtruong96.renewalradar.data.Subscription
import io.github.dangtruong96.renewalradar.domain.BillingCycle
import io.github.dangtruong96.renewalradar.domain.RenewalCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * A free trial that has already ended has converted to a paid plan: the
 * first charge lands on the trial-end date and renewals step from there.
 * These tests pin that behaviour — before the fix, [RenewalCalculator]
 * returned the (past) trial-end date forever, so a converted trial
 * vanished from the upcoming list, kept a past "trial ends" date, never
 * got renewal reminders, and never counted in the spend totals.
 */
class TrialConversionTest {

    private fun date(iso: String): LocalDate = LocalDate.parse(iso)

    private fun trial(
        trialEnd: String?,
        cycle: BillingCycle = BillingCycle.MONTHLY,
        anchor: String = "2025-01-01"
    ) = Subscription(
        name = "Video app",
        amountMinor = 1299,
        currency = "USD",
        cycle = cycle.name,
        anchorDate = anchor,
        isTrial = true,
        trialEndDate = trialEnd
    )

    @Test
    fun expiredTrialEffectiveDateRollsToNextPaidRenewal() {
        // Trial ended 2025-01-10; monthly charges step from there:
        // Jan 10 (first charge), Feb 10, Mar 10 — so on Mar 1 the
        // effective date is the Mar 10 renewal, not the dead trial end.
        val result = RenewalCalculator.effectiveDate(
            trial(trialEnd = "2025-01-10"), date("2025-03-01")
        )
        assertEquals(date("2025-03-10"), result)
    }

    @Test
    fun expiredTrialOnChargeDayReturnsThatDay() {
        val result = RenewalCalculator.effectiveDate(
            trial(trialEnd = "2025-01-10"), date("2025-02-10")
        )
        assertEquals(date("2025-02-10"), result)
    }

    @Test
    fun expiredWeeklyTrialStepsByWeeksFromTrialEnd() {
        // Trial ended Fri 2025-01-03; weekly charges Jan 3, 10, 17, 24 —
        // on Jan 20 the next charge is Jan 24.
        val result = RenewalCalculator.effectiveDate(
            trial(trialEnd = "2025-01-03", cycle = BillingCycle.WEEKLY),
            date("2025-01-20")
        )
        assertEquals(date("2025-01-24"), result)
    }

    @Test
    fun trialEndingTodayIsStillTheTrialEnd() {
        val result = RenewalCalculator.effectiveDate(
            trial(trialEnd = "2025-03-01"), date("2025-03-01")
        )
        assertEquals(date("2025-03-01"), result)
    }

    @Test
    fun expiredTrialAppearsInUpcomingWithRenewalDate() {
        val today = date("2025-03-01")
        val converted = trial(trialEnd = "2025-01-10")
        val result = RenewalCalculator.upcoming(listOf(converted), today, withinDays = 30)
        assertEquals(1, result.size)
        assertEquals(date("2025-03-10"), result[0].date)
    }

    @Test
    fun trialPendingOnlyUntilTheTrialEnds() {
        val sub = trial(trialEnd = "2025-03-10")
        assertTrue(RenewalCalculator.isTrialPending(sub, date("2025-03-01")))
        assertTrue(RenewalCalculator.isTrialPending(sub, date("2025-03-10")))
        assertFalse(RenewalCalculator.isTrialPending(sub, date("2025-03-11")))
    }

    @Test
    fun trialWithoutEndDateIsNotPending() {
        val sub = trial(trialEnd = null)
        assertFalse(RenewalCalculator.isTrialPending(sub, date("2025-03-01")))
    }
}
