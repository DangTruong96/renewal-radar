package io.github.dangtruong96.renewalradar

import io.github.dangtruong96.renewalradar.data.Subscription
import io.github.dangtruong96.renewalradar.domain.BillingCycle
import io.github.dangtruong96.renewalradar.domain.RenewalCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Round-2 adversarial tests: window boundaries of [RenewalCalculator.upcoming],
 * sorting stability, garbage stored data, [BillingCycle.fromName] fallback,
 * and rounding of [RenewalCalculator.monthlyEquivalentMinor].
 */
class Round2DomainTest {

    private fun date(iso: String): LocalDate = LocalDate.parse(iso)

    private fun sub(
        name: String,
        anchor: String,
        cycle: BillingCycle = BillingCycle.MONTHLY,
        isTrial: Boolean = false,
        trialEnd: String? = null
    ) = Subscription(
        name = name,
        amountMinor = 1299,
        currency = "USD",
        cycle = cycle.name,
        anchorDate = anchor,
        isTrial = isTrial,
        trialEndDate = trialEnd
    )

    private val today: LocalDate = date("2025-03-10")

    // -- upcoming() window boundaries -------------------------------------

    @Test
    fun upcoming_includesItemExactlyAtWindowEnd() {
        // Anchor steps to exactly today + 30.
        val edge = sub(name = "Edge", anchor = "2025-04-09")
        val result = RenewalCalculator.upcoming(listOf(edge), today, withinDays = 30)
        assertEquals(listOf("Edge"), result.map { it.subscription.name })
        assertEquals(date("2025-04-09"), result[0].date)
    }

    @Test
    fun upcoming_excludesItemOneDayPastWindowEnd() {
        // YEARLY so the previous cycle date (2024-04-10) is before today
        // and the effective date really is today + 31. (With MONTHLY an
        // Apr-10 anchor would project back to a Mar-10 renewal — the
        // documented step-back behaviour — and be inside the window.)
        val past = sub(name = "Past", anchor = "2025-04-10", cycle = BillingCycle.YEARLY)
        val result = RenewalCalculator.upcoming(listOf(past), today, withinDays = 30)
        assertTrue(result.isEmpty())
    }

    @Test
    fun upcoming_includesItemRenewingToday() {
        val now = sub(name = "Now", anchor = "2025-03-10")
        val result = RenewalCalculator.upcoming(listOf(now), today, withinDays = 30)
        assertEquals(listOf("Now"), result.map { it.subscription.name })
    }

    @Test
    fun upcoming_zeroWindowOnlyIncludesToday() {
        val now = sub(name = "Now", anchor = "2025-03-10")
        val tomorrow = sub(name = "Tomorrow", anchor = "2025-03-11")
        val result = RenewalCalculator.upcoming(listOf(now, tomorrow), today, withinDays = 0)
        assertEquals(listOf("Now"), result.map { it.subscription.name })
    }

    @Test
    fun upcoming_negativeWindowIsEmpty() {
        val now = sub(name = "Now", anchor = "2025-03-10")
        val result = RenewalCalculator.upcoming(listOf(now), today, withinDays = -1)
        assertTrue(result.isEmpty())
    }

    // -- sorting stability --------------------------------------------------

    @Test
    fun upcoming_equalDatesKeepInputOrder() {
        val first = sub(name = "First", anchor = "2025-03-15")
        val second = sub(name = "Second", anchor = "2025-03-15")
        val third = sub(name = "Third", anchor = "2025-03-15")
        val result = RenewalCalculator.upcoming(
            listOf(first, second, third), today, withinDays = 30
        )
        assertEquals(
            listOf("First", "Second", "Third"),
            result.map { it.subscription.name }
        )
    }

    // -- garbage stored data ------------------------------------------------

    @Test
    fun upcoming_unparseableAnchorIsExcludedButValidOnesSurvive() {
        val broken = sub(name = "Broken", anchor = "not-a-date")
        val empty = sub(name = "Empty", anchor = "")
        val good = sub(name = "Good", anchor = "2025-03-12")
        val result = RenewalCalculator.upcoming(
            listOf(broken, good, empty), today, withinDays = 30
        )
        assertEquals(listOf("Good"), result.map { it.subscription.name })
    }

    @Test
    fun effectiveDate_trialWithGarbageTrialEndFallsBackToAnchor() {
        val plan = sub(
            name = "Trial", anchor = "2025-01-15",
            isTrial = true, trialEnd = "garbage"
        )
        assertEquals(
            date("2025-03-15"),
            RenewalCalculator.effectiveDate(plan, today)
        )
    }

    @Test
    fun effectiveDate_trialWithGarbageTrialEndAndAnchorIsNull() {
        val plan = sub(
            name = "Trial", anchor = "also-garbage",
            isTrial = true, trialEnd = "garbage"
        )
        assertEquals(null, RenewalCalculator.effectiveDate(plan, today))
    }

    // -- BillingCycle.fromName ----------------------------------------------

    @Test
    fun fromName_garbageFallsBackToMonthly() {
        assertEquals(BillingCycle.MONTHLY, BillingCycle.fromName("DAILY"))
        assertEquals(BillingCycle.MONTHLY, BillingCycle.fromName(""))
        assertEquals(BillingCycle.MONTHLY, BillingCycle.fromName("weekly"))
        assertEquals(BillingCycle.MONTHLY, BillingCycle.fromName(" WEEKLY"))
        assertEquals(BillingCycle.WEEKLY, BillingCycle.fromName("WEEKLY"))
        assertEquals(BillingCycle.YEARLY, BillingCycle.fromName("YEARLY"))
    }

    // -- monthlyEquivalentMinor rounding ------------------------------------

    @Test
    fun monthlyEquivalent_roundingAcrossCycles() {
        // WEEKLY: amount * 52 / 12.
        assertEquals(433L, RenewalCalculator.monthlyEquivalentMinor(100, BillingCycle.WEEKLY))
        assertEquals(4L, RenewalCalculator.monthlyEquivalentMinor(1, BillingCycle.WEEKLY))
        assertEquals(4329L, RenewalCalculator.monthlyEquivalentMinor(999, BillingCycle.WEEKLY))
        assertEquals(0L, RenewalCalculator.monthlyEquivalentMinor(0, BillingCycle.WEEKLY))
        // QUARTERLY: / 3.
        assertEquals(33L, RenewalCalculator.monthlyEquivalentMinor(100, BillingCycle.QUARTERLY))
        assertEquals(34L, RenewalCalculator.monthlyEquivalentMinor(101, BillingCycle.QUARTERLY))
        assertEquals(0L, RenewalCalculator.monthlyEquivalentMinor(1, BillingCycle.QUARTERLY))
        // YEARLY: / 12; 6/12 = 0.5 rounds half-up to 1 (Math.round).
        assertEquals(1L, RenewalCalculator.monthlyEquivalentMinor(6, BillingCycle.YEARLY))
        assertEquals(0L, RenewalCalculator.monthlyEquivalentMinor(5, BillingCycle.YEARLY))
        assertEquals(83333L, RenewalCalculator.monthlyEquivalentMinor(999999, BillingCycle.YEARLY))
        // Negative amounts (should not occur, but math must not blow up).
        assertEquals(-1200L, RenewalCalculator.monthlyEquivalentMinor(-1200, BillingCycle.MONTHLY))
        assertEquals(-433L, RenewalCalculator.monthlyEquivalentMinor(-100, BillingCycle.WEEKLY))
    }
}
