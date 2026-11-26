package io.github.dangtruong96.renewalradar.domain

import io.github.dangtruong96.renewalradar.data.Subscription
import java.util.Locale

/**
 * Pure rendering of a due reminder into the notification title and body.
 * Kept in the domain layer so the exact strings the worker posts are
 * unit-testable on the JVM — the worker only hands them to the OS.
 * This mirrors why the reminder decision itself lives in
 * [RenewalCalculator.reminderDue]: the strings are behavior, and the
 * worker must not be the only place they exist.
 *
 * Day 0 renders as "today", never as "in 0 day(s)": a renewal with a
 * 0-day lead (reachable through an imported backup — the editor only
 * offers 1/3/7/14, but import accepts any non-negative lead) fires on
 * the renewal day itself, and "renews in 0 day(s)" reads as if the
 * renewal were still in the future. The trial branch already rendered
 * day 0 as TODAY; the renewal branch did not.
 */
object ReminderMessage {

    /** Title and body for [reminder] about [subscription]. */
    fun render(
        subscription: Subscription,
        reminder: RenewalCalculator.ReminderDue,
        locale: Locale = Locale.getDefault()
    ): Pair<String, String> {
        val price = Money.format(subscription.amountMinor, subscription.currency, locale)
        val name = subscription.name
        return when (reminder.kind) {
            RenewalCalculator.ReminderKind.TRIAL -> {
                val title = "Free trial ending: $name"
                val body = when (reminder.days) {
                    0L -> "$name converts to a paid plan TODAY at $price. " +
                        "Cancel now if you don't want it."
                    1L -> "$name converts to a paid plan TOMORROW at $price."
                    else -> "$name converts to a paid plan in ${reminder.days} " +
                        "days at $price."
                }
                title to body
            }
            RenewalCalculator.ReminderKind.RENEWAL -> {
                val title = "Renewal coming up: $name"
                val body = if (reminder.days == 0L) {
                    "$name renews TODAY at $price. Still using it?"
                } else {
                    "$name renews in ${reminder.days} day(s) at $price. " +
                        "Still using it?"
                }
                title to body
            }
        }
    }
}
