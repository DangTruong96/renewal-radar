package io.github.dangtruong96.renewalradar.domain

import io.github.dangtruong96.renewalradar.data.Subscription
import java.util.Currency

/**
 * Validation for a subscription entry coming from a JSON backup.
 *
 * `RenewalViewModel.importJson` documents "returns false on bad input",
 * but it used to accept any structurally-parseable entry and replace the
 * whole library with it. An entry with a garbage anchor date is then
 * invisible everywhere it matters — `effectiveDate` is null, so it never
 * appears in "Coming up" and never gets a reminder — while the dashboard
 * still sums its amount into the totals. A typo'd cycle (`YEARLYY`)
 * silently fell back to MONTHLY, inflating that plan's monthly total
 * 12x; a negative amount put negative spend on the dashboard.
 *
 * The boundary is "what the app can honestly hold": every field the
 * editor can produce, plus any real ISO 4217 currency (a hand-edited
 * backup may use a code the editor doesn't offer, and formatting and
 * fraction digits handle any real code). Anything outside that is bad
 * input and the whole document is rejected, so a corrupt backup can
 * never wipe a good library.
 */
object SubscriptionImport {

    /** True if [subscription] is a well-formed entry an import may store. */
    fun isValid(subscription: Subscription): Boolean {
        if (subscription.name.isBlank()) return false
        if (subscription.amountMinor < 0) return false
        if (!isRealCurrency(subscription.currency)) return false
        if (BillingCycle.entries.none { it.name == subscription.cycle }) return false
        if (RenewalCalculator.parseDate(subscription.anchorDate) == null) return false
        val trialEnd = subscription.trialEndDate
        if (trialEnd != null && RenewalCalculator.parseDate(trialEnd) == null) return false
        if (subscription.remindDaysBefore < 0) return false
        return true
    }

    private fun isRealCurrency(code: String): Boolean = try {
        Currency.getInstance(code)
        true
    } catch (e: IllegalArgumentException) {
        false
    }
}
