package io.github.dangtruong96.renewalradar

import io.github.dangtruong96.renewalradar.domain.BillingCycle
import io.github.dangtruong96.renewalradar.domain.Money
import io.github.dangtruong96.renewalradar.domain.RenewalCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale
import kotlin.random.Random

/**
 * Round 5 (QA loop, 2026-11-26): documentation-vs-behavior +
 * error-message quality + cross-locale Money.format + fuzz
 * RenewalCalculator at month/leap boundaries.
 *
 * Two defects fixed here, both in the decimal-comma family:
 * (1) a leading decimal comma (",99") or a signed integer part
 * ("+12,99") or grouped thousands with a decimal comma ("1,299,50")
 * fell through to the grouping branch and was stored 100x too large;
 * (2) covered in Round5ReminderMessageTest.
 */
class Round5MoneyTest {

    // -- Decimal comma: leading / signed / grouped ----------------------

    @Test
    fun parseToMinor_leadingDecimalComma() {
        assertEquals(99L, Money.parseToMinor(",99", "EUR"))
        assertEquals(50L, Money.parseToMinor(",5", "EUR"))
        assertEquals(5L, Money.parseToMinor(",05", "EUR"))
        assertEquals(99L, Money.parseToMinor(",99", "USD"))
        // Guard: the explicit-zero form already worked.
        assertEquals(99L, Money.parseToMinor("0,99", "EUR"))
        assertEquals(99L, Money.parseToMinor(".99", "EUR"))
    }

    @Test
    fun parseToMinor_signedDecimalComma() {
        assertEquals(1299L, Money.parseToMinor("+12,99", "EUR"))
        assertEquals(1299L, Money.parseToMinor("+12,99", "USD"))
        // Negative still rejected, exactly as before.
        assertNull(Money.parseToMinor("-12,99", "EUR"))
    }

    @Test
    fun parseToMinor_groupedThousandsWithDecimalComma() {
        assertEquals(129950L, Money.parseToMinor("1,299,50", "EUR"))
        assertEquals(1299950L, Money.parseToMinor("12,999,50", "EUR"))
        // Guards: the both-separators forms already worked.
        assertEquals(129950L, Money.parseToMinor("1.299,50", "EUR"))
        assertEquals(129950L, Money.parseToMinor("1,299.50", "USD"))
        assertEquals(100000050L, Money.parseToMinor("1,000,000.50", "USD"))
    }

    @Test
    fun parseToMinor_groupingStillStripped() {
        // Last group has 3 digits, so the comma is grouping, not decimal.
        assertEquals(129900L, Money.parseToMinor("1,299", "EUR"))
        assertEquals(1299900L, Money.parseToMinor("12,999", "EUR"))
        assertEquals(129999900L, Money.parseToMinor("1,299,999", "EUR"))
        assertEquals(1299900L, Money.parseToMinor("12,999", "USD"))
        // Trailing comma: not a decimal comma, grouping branch as before.
        assertEquals(1200L, Money.parseToMinor("12,", "EUR"))
        // Zero-decimal currency: a comma is always grouping there.
        assertEquals(99L, Money.parseToMinor(",99", "JPY"))
        assertEquals(1500L, Money.parseToMinor("1,500", "JPY"))
    }

    @Test
    fun parseToMinor_singleDigitFractionStillDecimal() {
        assertEquals(1290L, Money.parseToMinor("12,9", "EUR"))
        assertEquals(150L, Money.parseToMinor("1,5", "EUR"))
        assertEquals(150L, Money.parseToMinor("1,50", "EUR"))
    }

    // -- Cross-locale format (verified correct this round; pinned) ------

    @Test
    fun format_zeroDecimalCurrencyInTwoDecimalLocale() {
        // JPY in a US locale must not render ".00" (pin from v0.1.1).
        val rendered = Money.format(1500, "JPY", Locale.US)
        assertTrue(rendered.contains("1,500"))
        assertFalse(rendered.contains(".00"))
    }

    @Test
    fun format_twoDecimalCurrencyInZeroDecimalLocale() {
        // USD in a Japan locale must keep its cents, not round to whole.
        assertEquals("$12.99", Money.format(1299, "USD", Locale.JAPAN))
    }

    @Test
    fun format_threeDecimalCurrency() {
        // BHD has 3 fraction digits; 123456 minor is 123.456.
        assertTrue(Money.format(123456, "BHD", Locale.US).contains("123.456"))
    }

    @Test
    fun format_decimalCommaLocale() {
        val rendered = Money.format(1299, "EUR", Locale.GERMANY)
        assertTrue(rendered.contains("12,99"))
    }

    @Test
    fun format_roundTripThroughPlainString() {
        for (ccy in listOf("USD", "EUR", "JPY", "VND", "BHD", "KRW")) {
            for (v in listOf(0L, 1L, 5L, 99L, 100L, 101L, 1299L, 123456L, 1000000L)) {
                assertEquals(v, Money.parseToMinor(Money.minorToPlainString(v, ccy), ccy))
            }
        }
    }

    // -- nextRenewal: property fuzz at month/leap boundaries -------------

    @Test
    fun nextRenewal_propertyFuzz() {
        val rnd = Random(20261126)
        repeat(2000) {
            val anchor = LocalDate.ofEpochDay(rnd.nextLong(-20000, 20000))
            val today = LocalDate.ofEpochDay(rnd.nextLong(-20000, 20000))
            val cycle = BillingCycle.entries[rnd.nextInt(4)]
            val res = RenewalCalculator.nextRenewal(anchor, cycle, today)
            assertFalse("result $res before today $today (anchor $anchor, $cycle)",
                res.isBefore(today))
            assertTrue("previous($res) not before today $today (anchor $anchor, $cycle)",
                cycle.previous(res).isBefore(today))
        }
    }

    @Test
    fun nextRenewal_leapDayAnchorDriftsAsDocumented() {
        // Feb 29 anchor clamps to Feb 28 in non-leap years and never
        // returns to Feb 29: documented clamp drift, pinned not changed.
        assertEquals(
            LocalDate.of(2026, 2, 28),
            RenewalCalculator.nextRenewal(
                LocalDate.of(2024, 2, 29), BillingCycle.YEARLY, LocalDate.of(2026, 2, 28)
            )
        )
        assertEquals(
            LocalDate.of(2029, 2, 28),
            RenewalCalculator.nextRenewal(
                LocalDate.of(2024, 2, 29), BillingCycle.YEARLY, LocalDate.of(2028, 3, 1)
            )
        )
    }

    @Test
    fun nextRenewal_jan31AnchorDriftsAsDocumented() {
        assertEquals(
            LocalDate.of(2026, 4, 28),
            RenewalCalculator.nextRenewal(
                LocalDate.of(2026, 1, 31), BillingCycle.MONTHLY, LocalDate.of(2026, 3, 30)
            )
        )
    }
}
