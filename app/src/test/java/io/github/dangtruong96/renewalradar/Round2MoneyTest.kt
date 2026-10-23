package io.github.dangtruong96.renewalradar

import io.github.dangtruong96.renewalradar.domain.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Round-2 adversarial tests for [Money]: a locale x currency formatting
 * matrix (the formatted number must carry exactly the currency's value),
 * and decimal-comma input from locales where "," is the natural decimal
 * separator (the edit form feeds raw user text to [Money.parseToMinor]).
 */
class Round2MoneyTest {

    private val locales = listOf(
        Locale.US, Locale.UK, Locale.GERMANY, Locale.FRANCE, Locale.JAPAN
    )

    /** Extract the numeric value from a locale-formatted currency string. */
    private fun numericValue(formatted: String, locale: Locale): BigDecimal {
        val syms = DecimalFormatSymbols(locale)
        val sb = StringBuilder()
        for (ch in formatted) {
            when {
                ch.isDigit() -> sb.append(ch)
                ch == syms.decimalSeparator -> sb.append('.')
                ch == syms.groupingSeparator -> Unit
                ch == '-' -> sb.append('-')
            }
        }
        return BigDecimal(sb.toString())
    }

    @Test
    fun format_valueSurvivesAcrossLocaleCurrencyMatrix() {
        val amounts = listOf(0L, 5L, 1299L, 123456789L)
        for (code in Money.SUPPORTED_CURRENCIES) {
            val factor = BigDecimal(Money.minorPerMajor(code))
            for (locale in locales) {
                for (minor in amounts) {
                    val text = Money.format(minor, code, locale)
                    val value = numericValue(text, locale)
                    val expected = BigDecimal(minor).divide(factor)
                    assertEquals(
                        "format($minor, $code, $locale) = '$text'",
                        0, value.compareTo(expected)
                    )
                }
            }
        }
    }

    @Test
    fun format_zeroDecimalCurrenciesNeverShowFractionInAnyLocale() {
        for (code in listOf("JPY", "KRW", "VND")) {
            for (locale in locales) {
                val text = Money.format(1500, code, locale)
                val syms = DecimalFormatSymbols(locale)
                // No decimal separator followed by digits (i.e. no ".00").
                val idx = text.indexOf(syms.decimalSeparator)
                if (idx >= 0) {
                    val after = text.substring(idx + 1).filter { it.isDigit() }
                    assertEquals("format(1500, $code, $locale) = '$text'", "", after)
                }
            }
        }
    }

    // -- decimal comma ------------------------------------------------------

    @Test
    fun parse_decimalCommaIsNotSilentlyInflated() {
        // A user in a decimal-comma locale types the natural format.
        // Before the fix this returned 129900 (the comma was stripped as
        // if it were a grouping separator) — a silent 100x error.
        assertEquals(1299L, Money.parseToMinor("12,99", "EUR"))
        assertEquals(1250L, Money.parseToMinor("12,5", "EUR"))
        assertEquals(5L, Money.parseToMinor("0,05", "EUR"))
    }

    @Test
    fun parse_groupingCommasStillWork() {
        // Three digits after the comma cannot be a 2-digit fraction, so
        // the comma stays a grouping separator.
        assertEquals(129900L, Money.parseToMinor("1,299", "USD"))
        assertEquals(129950L, Money.parseToMinor("1,299.50", "USD"))
        // Zero-decimal currencies: commas are always grouping.
        assertEquals(1500L, Money.parseToMinor("1,500", "JPY"))
        assertEquals(1299L, Money.parseToMinor("12.99", "USD"))
    }

    @Test
    fun parse_overflowReturnsNull() {
        assertNull(Money.parseToMinor("99999999999999999999999999999", "USD"))
    }
}
