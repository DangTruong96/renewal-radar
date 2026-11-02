package io.github.dangtruong96.renewalradar

import io.github.dangtruong96.renewalradar.domain.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Round-3 adversarial tests for [Money.parseToMinor]: grouped
 * decimal-comma input, non-breaking-space grouping, and scientific
 * notation. The edit form feeds raw user text to [Money.parseToMinor]
 * on the main thread, so a mis-parse here silently stores the wrong
 * price and a pathological input freezes the UI.
 */
class Round3MoneyTest {

    // -- dot grouping + comma decimal ("1.299,50") ------------------------

    @Test
    fun parse_dotGroupedDecimalCommaIsNotSilentlyShrunk() {
        // A user in a dot-grouping locale types the natural grouped
        // format. Before the fix "1.299,50" EUR returned 130 minor
        // (the comma was stripped and "1.29950" read as 1.30 EUR) --
        // a silent ~1000x error, the same corruption class as the
        // decimal-comma bug fixed in 0.1.3.
        assertEquals(129950L, Money.parseToMinor("1.299,50", "EUR"))
        assertEquals(1234567L, Money.parseToMinor("12.345,67", "EUR"))
        assertEquals(129950050L, Money.parseToMinor("1.299.500,50", "EUR"))
        assertEquals(129950L, Money.parseToMinor("1.299,5", "EUR"))
    }

    @Test
    fun parse_dotGroupedDecimalCommaKeepsUsGroupedInputWorking() {
        // When the dot is last it stays the decimal separator, as before.
        assertEquals(129950L, Money.parseToMinor("1,299.50", "USD"))
        assertEquals(129999950L, Money.parseToMinor("1,299,999.50", "USD"))
        // Ungrouped and single-comma forms are unchanged.
        assertEquals(1299L, Money.parseToMinor("12,99", "EUR"))
        assertEquals(129900L, Money.parseToMinor("1,299", "USD"))
    }

    // -- non-breaking-space grouping ---------------------------------------

    @Test
    fun parse_nonBreakingSpaceGroupingIsAccepted() {
        // French-locale formatting groups thousands with a non-breaking
        // space (U+00A0, or narrow U+202F on newer platforms). Before
        // the fix both were rejected outright; a plain space already
        // worked, so the grouped value itself was never in question.
        assertEquals(129950L, Money.parseToMinor("1\u00A0299,50", "EUR"))
        assertEquals(129950L, Money.parseToMinor("1\u202F299,50", "EUR"))
        assertEquals(129950L, Money.parseToMinor("1 299,50", "EUR"))
    }

    // -- scientific notation -----------------------------------------------

    @Test
    fun parse_scientificNotationIsRejected() {
        // A price field takes plain decimal notation only; "1e3" is
        // never a typed price. Before the fix it was accepted as 1000.
        assertNull(Money.parseToMinor("1e3", "USD"))
        assertNull(Money.parseToMinor("12e2", "USD"))
        assertNull(Money.parseToMinor("1E3", "USD"))
        // Before the fix this 12-character input stalled the caller
        // for 30+ seconds (BigDecimal scaling materialising a
        // hundred-million-digit number); post-fix it returns at once.
        assertNull(Money.parseToMinor("1e100000000", "USD"))
    }
}
