package io.github.dangtruong96.renewalradar

import io.github.dangtruong96.renewalradar.domain.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * Deterministic unit tests for [Money]: currency fraction digits,
 * parsing user input into minor units, and formatting for display.
 * Locales are pinned where formatting is asserted, so nothing here
 * depends on the device or CI machine's default locale.
 */
class MoneyTest {

    // -- Fraction digits / minor-per-major -------------------------------

    @Test
    fun fractionDigits_twoDecimalCurrencies() {
        assertEquals(2, Money.fractionDigits("USD"))
        assertEquals(2, Money.fractionDigits("EUR"))
        assertEquals(2, Money.fractionDigits("GBP"))
    }

    @Test
    fun fractionDigits_zeroDecimalCurrencies() {
        assertEquals(0, Money.fractionDigits("JPY"))
        assertEquals(0, Money.fractionDigits("KRW"))
        assertEquals(0, Money.fractionDigits("VND"))
    }

    @Test
    fun fractionDigits_unknownOrUnitlessCodesFallBackToTwo() {
        // "XXX" is ISO 4217 for "no currency": fraction digits are -1.
        assertEquals(2, Money.fractionDigits("XXX"))
        // Not a currency code at all: getInstance throws, fallback is 2.
        assertEquals(2, Money.fractionDigits("NOTACURRENCY"))
    }

    @Test
    fun minorPerMajor_matchesFractionDigits() {
        assertEquals(100, Money.minorPerMajor("USD"))
        assertEquals(1, Money.minorPerMajor("JPY"))
        assertEquals(1, Money.minorPerMajor("VND"))
    }

    @Test
    fun supportedCurrencies_allResolve() {
        assertEquals(8, Money.SUPPORTED_CURRENCIES.size)
        for (code in Money.SUPPORTED_CURRENCIES) {
            val digits = Money.fractionDigits(code)
            assertTrue("$code digits in range", digits in 0..3)
        }
    }

    // -- Parsing ----------------------------------------------------------

    @Test
    fun parse_twoDecimalCurrency() {
        assertEquals(1299L, Money.parseToMinor("12.99", "USD"))
        assertEquals(1200L, Money.parseToMinor("12", "USD"))
        assertEquals(5L, Money.parseToMinor("0.05", "USD"))
        assertEquals(0L, Money.parseToMinor("0", "USD"))
    }

    @Test
    fun parse_stripsGroupingCommasAndSpaces() {
        assertEquals(129950L, Money.parseToMinor("1,299.50", "USD"))
        assertEquals(1299L, Money.parseToMinor(" 12.99 ", "USD"))
    }

    @Test
    fun parse_zeroDecimalCurrencyRoundsHalfUp() {
        assertEquals(1500L, Money.parseToMinor("1500", "JPY"))
        assertEquals(1501L, Money.parseToMinor("1500.6", "JPY"))
        assertEquals(1500L, Money.parseToMinor("1500.4", "JPY"))
    }

    @Test
    fun parse_extraPrecisionRoundsToNearestMinorUnit() {
        // 12.999 USD = 1299.9 cents -> 1300.
        assertEquals(1300L, Money.parseToMinor("12.999", "USD"))
    }

    @Test
    fun parse_rejectsBadInput() {
        assertNull(Money.parseToMinor("", "USD"))
        assertNull(Money.parseToMinor("   ", "USD"))
        assertNull(Money.parseToMinor("abc", "USD"))
        assertNull(Money.parseToMinor("12.99.1", "USD"))
        assertNull(Money.parseToMinor("-5", "USD"))
    }

    // -- Plain string (form fields) ---------------------------------------

    @Test
    fun minorToPlainString_twoDecimal() {
        assertEquals("12.99", Money.minorToPlainString(1299, "USD"))
        assertEquals("12", Money.minorToPlainString(1200, "USD"))
        assertEquals("12.5", Money.minorToPlainString(1250, "USD"))
        assertEquals("0.05", Money.minorToPlainString(5, "USD"))
    }

    @Test
    fun minorToPlainString_zeroDecimal() {
        assertEquals("1500", Money.minorToPlainString(1500, "JPY"))
        assertEquals("0", Money.minorToPlainString(0, "JPY"))
    }

    @Test
    fun parse_isInverseOfPlainString() {
        for (amount in listOf(0L, 5L, 1299L, 1200L, 999999L)) {
            val plain = Money.minorToPlainString(amount, "USD")
            assertEquals(amount, Money.parseToMinor(plain, "USD"))
        }
        for (amount in listOf(0L, 500L, 12000L)) {
            val plain = Money.minorToPlainString(amount, "JPY")
            assertEquals(amount, Money.parseToMinor(plain, "JPY"))
        }
    }

    // -- Display formatting -------------------------------------------------

    @Test
    fun format_usdInUsLocale() {
        assertEquals("$12.99", Money.format(1299, "USD", Locale.US))
    }

    @Test
    fun format_jpyHasNoDecimalsAndGroupsThousands() {
        val text = Money.format(1500, "JPY", Locale.US)
        assertTrue(text.contains("1,500"))
        assertTrue(!text.contains("."))
    }

    @Test
    fun format_unknownCurrencyFallsBackToRawAmount() {
        assertEquals("1299 NOTACURRENCY", Money.format(1299, "NOTACURRENCY", Locale.US))
    }
}
