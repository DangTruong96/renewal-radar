package io.github.dangtruong96.renewalradar

import io.github.dangtruong96.renewalradar.data.Subscription
import io.github.dangtruong96.renewalradar.domain.BillingCycle
import io.github.dangtruong96.renewalradar.domain.Money
import io.github.dangtruong96.renewalradar.domain.RenewalCalculator
import io.github.dangtruong96.renewalradar.domain.RenewalCalculator.ReminderKind
import io.github.dangtruong96.renewalradar.domain.SubscriptionImport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Round 4 (2026-11-14): the daily reminder decision, simulated day by
 * day through [RenewalCalculator.reminderDue] — the exact function the
 * worker now calls — plus validation of imported backup entries.
 *
 * Two reminder options the editor offers could silently never fire:
 * a trial's chosen 7/14-day lead time (the 3/1/0 countdown replaced it
 * instead of adding to it), and any 7/14-day lead on a weekly plan
 * (days-until-renewal is always 0..6, so the exact-day rule could never
 * match). A third hole was in import: any structurally-parseable entry
 * was accepted, so a garbage anchor date made a subscription invisible
 * to reminders and "Coming up" while its amount still counted in the
 * dashboard totals.
 */
class Round4ReminderTest {

    private fun date(iso: String): LocalDate = LocalDate.parse(iso)

    private fun trial(end: String, lead: Int, cycle: String = "MONTHLY") =
        Subscription(
            name = "Trial app", amountMinor = 1299, currency = "USD",
            cycle = cycle, anchorDate = "2026-01-01",
            isTrial = true, trialEndDate = end, remindDaysBefore = lead
        )

    private fun plan(anchor: String, cycle: String, lead: Int) =
        Subscription(
            name = "Plan", amountMinor = 999, currency = "USD",
            cycle = cycle, anchorDate = anchor, remindDaysBefore = lead
        )

    /** Every (date, due) pair in [from, to], using the real decision function. */
    private fun simulate(
        sub: Subscription, from: LocalDate, to: LocalDate
    ): List<Pair<LocalDate, RenewalCalculator.ReminderDue>> {
        val out = mutableListOf<Pair<LocalDate, RenewalCalculator.ReminderDue>>()
        var d = from
        while (!d.isAfter(to)) {
            RenewalCalculator.reminderDue(sub, d)?.let { out += d to it }
            d = d.plusDays(1)
        }
        return out
    }

    // --- Defect 1: a trial's chosen lead time was silently ignored ---

    @Test
    fun trialLead7FiresOnLeadDayPlusCountdown() {
        val sub = trial(end = "2026-03-15", lead = 7)
        val fired = simulate(sub, date("2026-03-01"), date("2026-03-15"))
        assertEquals(
            listOf(date("2026-03-08"), date("2026-03-12"), date("2026-03-14"), date("2026-03-15")),
            fired.map { it.first }
        )
        assertTrue(fired.all { it.second.kind == ReminderKind.TRIAL })
        assertEquals(7L, fired.first { it.first == date("2026-03-08") }.second.days)
    }

    @Test
    fun trialLead14FiresOnLeadDayPlusCountdown() {
        val sub = trial(end = "2026-03-20", lead = 14)
        val fired = simulate(sub, date("2026-03-01"), date("2026-03-20"))
        assertEquals(
            listOf(date("2026-03-06"), date("2026-03-17"), date("2026-03-19"), date("2026-03-20")),
            fired.map { it.first }
        )
        assertEquals(14L, fired.first { it.first == date("2026-03-06") }.second.days)
    }

    @Test
    fun trialLead3FiresExactlyOnceAtThreeDays() {
        // Lead 3 coincides with a countdown day: still one notification.
        val sub = trial(end = "2026-03-15", lead = 3)
        val fired = simulate(sub, date("2026-03-01"), date("2026-03-15"))
        assertEquals(
            listOf(date("2026-03-12"), date("2026-03-14"), date("2026-03-15")),
            fired.map { it.first }
        )
    }

    @Test
    fun trialLead1FiresAtOneDay() {
        val sub = trial(end = "2026-03-15", lead = 1)
        val fired = simulate(sub, date("2026-03-01"), date("2026-03-15"))
        assertEquals(
            listOf(date("2026-03-12"), date("2026-03-14"), date("2026-03-15")),
            fired.map { it.first }
        )
    }

    @Test
    fun trialShorterThanLeadStillGetsCountdownOnly() {
        // A 7-day trial can never be 14 days out; the countdown is the
        // safety net, and no day fires twice.
        val sub = trial(end = "2026-03-08", lead = 14)
        val fired = simulate(sub, date("2026-03-01"), date("2026-03-08"))
        assertEquals(
            listOf(date("2026-03-05"), date("2026-03-07"), date("2026-03-08")),
            fired.map { it.first }
        )
    }

    @Test
    fun trialEndingTodayWithLead7FiresOnce() {
        val due = RenewalCalculator.reminderDue(trial(end = "2026-03-15", lead = 7), date("2026-03-15"))
        assertEquals(RenewalCalculator.ReminderDue(ReminderKind.TRIAL, 0L, date("2026-03-15")), due)
    }

    // --- Defect 2: weekly plans could never fire a 7/14-day lead ---

    @Test
    fun weeklyLead7FiresOncePerCycleAtSixDaysOut() {
        // Mondays from 2026-03-02; the closest reachable day to a 7-day
        // lead is 6 days out — the day after each renewal.
        val sub = plan("2026-03-02", "WEEKLY", 7)
        val fired = simulate(sub, date("2026-03-02"), date("2026-04-30"))
        assertEquals(
            listOf(
                "2026-03-03", "2026-03-10", "2026-03-17", "2026-03-24", "2026-03-31",
                "2026-04-07", "2026-04-14", "2026-04-21", "2026-04-28"
            ).map { date(it) },
            fired.map { it.first }
        )
        assertTrue(fired.all { it.second.kind == ReminderKind.RENEWAL && it.second.days == 6L })
    }

    @Test
    fun weeklyLead14FiresSameDaysAsLead7() {
        val lead14 = simulate(plan("2026-03-02", "WEEKLY", 14), date("2026-03-02"), date("2026-04-30"))
        val lead7 = simulate(plan("2026-03-02", "WEEKLY", 7), date("2026-03-02"), date("2026-04-30"))
        assertEquals(lead7.map { it.first }, lead14.map { it.first })
    }

    @Test
    fun weeklyLead3Unchanged() {
        val sub = plan("2026-03-02", "WEEKLY", 3)
        val fired = simulate(sub, date("2026-03-02"), date("2026-04-30"))
        assertEquals(
            listOf(
                "2026-03-06", "2026-03-13", "2026-03-20", "2026-03-27",
                "2026-04-03", "2026-04-10", "2026-04-17", "2026-04-24"
            ).map { date(it) },
            fired.map { it.first }
        )
        assertTrue(fired.all { it.second.days == 3L })
    }

    @Test
    fun weeklyLead1Unchanged() {
        val sub = plan("2026-03-02", "WEEKLY", 1)
        val fired = simulate(sub, date("2026-03-02"), date("2026-04-30"))
        assertEquals(8, fired.size)
        assertTrue(fired.all { it.second.days == 1L })
    }

    // --- Leads the cycle can reach fire on exactly the lead day ---

    @Test
    fun monthlyLead14Unchanged() {
        val sub = plan("2026-01-15", "MONTHLY", 14)
        assertEquals(
            RenewalCalculator.ReminderDue(ReminderKind.RENEWAL, 14L, date("2026-03-15")),
            RenewalCalculator.reminderDue(sub, date("2026-03-01"))
        )
        assertNull(RenewalCalculator.reminderDue(sub, date("2026-03-02")))
    }

    @Test
    fun monthlyLead7Unchanged() {
        val sub = plan("2026-01-15", "MONTHLY", 7)
        assertEquals(
            RenewalCalculator.ReminderDue(ReminderKind.RENEWAL, 7L, date("2026-03-15")),
            RenewalCalculator.reminderDue(sub, date("2026-03-08"))
        )
        assertNull(RenewalCalculator.reminderDue(sub, date("2026-03-09")))
    }

    @Test
    fun yearlyLead14Unchanged() {
        val sub = plan("2026-01-15", "YEARLY", 14)
        assertEquals(
            RenewalCalculator.ReminderDue(ReminderKind.RENEWAL, 14L, date("2027-01-15")),
            RenewalCalculator.reminderDue(sub, date("2027-01-01"))
        )
    }

    @Test
    fun noReminderOnOrdinaryDay() {
        val sub = plan("2026-01-15", "MONTHLY", 7)
        assertNull(RenewalCalculator.reminderDue(sub, date("2026-03-10")))
    }

    @Test
    fun convertedTrialGetsRenewalReminderNotTrialReminder() {
        // Trial ended 2026-01-10; on 2026-03-07 the next paid renewal is
        // Mar 10, 3 days out — a renewal reminder, not a trial one.
        val sub = trial(end = "2026-01-10", lead = 3)
        assertEquals(
            RenewalCalculator.ReminderDue(ReminderKind.RENEWAL, 3L, date("2026-03-10")),
            RenewalCalculator.reminderDue(sub, date("2026-03-07"))
        )
    }

    @Test
    fun garbageAnchorYieldsNoReminder() {
        val sub = plan("not-a-date", "MONTHLY", 3)
        assertNull(RenewalCalculator.reminderDue(sub, date("2026-03-10")))
    }

    // --- Defect 3: import validation ---

    private fun validEntry(
        name: String = "Music",
        amountMinor: Long = 1299,
        currency: String = "USD",
        cycle: String = "MONTHLY",
        anchorDate: String = "2026-01-15",
        isTrial: Boolean = false,
        trialEndDate: String? = null,
        remindDaysBefore: Int = 3
    ) = Subscription(
        name = name, amountMinor = amountMinor, currency = currency,
        cycle = cycle, anchorDate = anchorDate, isTrial = isTrial,
        trialEndDate = trialEndDate, remindDaysBefore = remindDaysBefore
    )

    @Test
    fun exportShapedEntriesAreValid() {
        for (cycle in BillingCycle.entries) {
            for (currency in Money.SUPPORTED_CURRENCIES) {
                assertTrue(
                    "$cycle/$currency",
                    SubscriptionImport.isValid(validEntry(cycle = cycle.name, currency = currency))
                )
            }
        }
        assertTrue(SubscriptionImport.isValid(validEntry(isTrial = true, trialEndDate = "2026-04-01")))
        assertTrue(SubscriptionImport.isValid(validEntry(isTrial = true, trialEndDate = null)))
        assertTrue(SubscriptionImport.isValid(validEntry(amountMinor = 0))) // free plan
        assertTrue(SubscriptionImport.isValid(validEntry(remindDaysBefore = 0)))
        assertTrue(SubscriptionImport.isValid(validEntry(remindDaysBefore = 14)))
    }

    @Test
    fun blankNameIsInvalid() {
        assertFalse(SubscriptionImport.isValid(validEntry(name = "")))
        assertFalse(SubscriptionImport.isValid(validEntry(name = "   ")))
    }

    @Test
    fun negativeAmountIsInvalid() {
        assertFalse(SubscriptionImport.isValid(validEntry(amountMinor = -1)))
    }

    @Test
    fun unknownCurrencyIsInvalid() {
        assertFalse(SubscriptionImport.isValid(validEntry(currency = "USDD")))
        assertFalse(SubscriptionImport.isValid(validEntry(currency = "usd")))
        assertFalse(SubscriptionImport.isValid(validEntry(currency = "")))
    }

    @Test
    fun realButUnsupportedCurrencyIsValid() {
        // The boundary is "a real ISO 4217 code", not the editor's
        // 8-currency list: formatting handles any real code.
        assertTrue(SubscriptionImport.isValid(validEntry(currency = "CHF")))
    }

    @Test
    fun unknownCycleIsInvalid() {
        assertFalse(SubscriptionImport.isValid(validEntry(cycle = "YEARLYY")))
        assertFalse(SubscriptionImport.isValid(validEntry(cycle = "monthly")))
        assertFalse(SubscriptionImport.isValid(validEntry(cycle = "")))
    }

    @Test
    fun garbageAnchorDateIsInvalid() {
        assertFalse(SubscriptionImport.isValid(validEntry(anchorDate = "not-a-date")))
        assertFalse(SubscriptionImport.isValid(validEntry(anchorDate = "2026-13-40")))
        assertFalse(SubscriptionImport.isValid(validEntry(anchorDate = "")))
    }

    @Test
    fun garbageTrialEndDateIsInvalid() {
        assertFalse(SubscriptionImport.isValid(validEntry(isTrial = true, trialEndDate = "soon")))
        assertFalse(SubscriptionImport.isValid(validEntry(isTrial = true, trialEndDate = "")))
    }

    @Test
    fun negativeLeadIsInvalid() {
        assertFalse(SubscriptionImport.isValid(validEntry(remindDaysBefore = -1)))
    }
}
