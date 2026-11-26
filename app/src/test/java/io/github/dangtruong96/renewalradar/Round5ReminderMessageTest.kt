package io.github.dangtruong96.renewalradar

import io.github.dangtruong96.renewalradar.data.Subscription
import io.github.dangtruong96.renewalradar.domain.ReminderMessage
import io.github.dangtruong96.renewalradar.domain.RenewalCalculator
import io.github.dangtruong96.renewalradar.domain.RenewalCalculator.ReminderKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

/**
 * Round 5 (QA loop, 2026-11-26): the reminder notification strings are
 * behavior, so they live in the pure domain layer ([ReminderMessage])
 * and are pinned here. The defect fixed: a renewal with a 0-day lead
 * (accepted by import validation — the editor only offers 1/3/7/14)
 * fires on the renewal day itself, but the worker rendered it as
 * "renews in 0 day(s)", as if the charge were still in the future.
 * The trial branch already rendered day 0 as TODAY.
 *
 * Locale is pinned to US so the price rendering is deterministic.
 */
class Round5ReminderMessageTest {

    private val locale = Locale.US

    private fun sub(lead: Int = 3) = Subscription(
        name = "Music",
        amountMinor = 1299,
        currency = "USD",
        cycle = "MONTHLY",
        anchorDate = "2026-01-15",
        remindDaysBefore = lead
    )

    private fun renewal(days: Long) =
        RenewalCalculator.ReminderDue(ReminderKind.RENEWAL, days, LocalDate.of(2026, 11, 26))

    private fun trial(days: Long) =
        RenewalCalculator.ReminderDue(ReminderKind.TRIAL, days, LocalDate.of(2026, 11, 26))

    @Test
    fun renewal_dayZeroSaysToday() {
        val (title, body) = ReminderMessage.render(sub(lead = 0), renewal(0), locale)
        assertEquals("Renewal coming up: Music", title)
        assertEquals("Music renews TODAY at \$12.99. Still using it?", body)
    }

    @Test
    fun renewal_otherDaysUnchanged() {
        val (_, oneDay) = ReminderMessage.render(sub(), renewal(1), locale)
        assertEquals("Music renews in 1 day(s) at \$12.99. Still using it?", oneDay)
        val (_, sixDays) = ReminderMessage.render(sub(), renewal(6), locale)
        assertEquals("Music renews in 6 day(s) at \$12.99. Still using it?", sixDays)
        val (_, fourteenDays) = ReminderMessage.render(sub(), renewal(14), locale)
        assertEquals("Music renews in 14 day(s) at \$12.99. Still using it?", fourteenDays)
    }

    @Test
    fun trial_messagesUnchanged() {
        val (title0, body0) = ReminderMessage.render(sub(), trial(0), locale)
        assertEquals("Free trial ending: Music", title0)
        assertTrue(body0.startsWith("Music converts to a paid plan TODAY at \$12.99."))
        val (_, body1) = ReminderMessage.render(sub(), trial(1), locale)
        assertEquals("Music converts to a paid plan TOMORROW at \$12.99.", body1)
        val (_, body3) = ReminderMessage.render(sub(), trial(3), locale)
        assertEquals("Music converts to a paid plan in 3 days at \$12.99.", body3)
        val (_, body7) = ReminderMessage.render(sub(), trial(7), locale)
        assertEquals("Music converts to a paid plan in 7 days at \$12.99.", body7)
    }
}
