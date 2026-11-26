package io.github.dangtruong96.renewalradar.domain

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Money conversion and formatting that respects each currency's real
 * number of fraction digits. USD/EUR/GBP/AUD/CAD use 2 (cents); JPY, KRW
 * and VND use 0 — amounts in those currencies are stored as whole units.
 */
object Money {

    /** Currencies offered in the UI. */
    val SUPPORTED_CURRENCIES: List<String> =
        listOf("USD", "EUR", "GBP", "JPY", "VND", "AUD", "CAD", "KRW")

    /** Minor units per major unit for [currencyCode] (100 for USD, 1 for JPY). */
    fun minorPerMajor(currencyCode: String): Int {
        val digits = fractionDigits(currencyCode)
        var factor = 1
        repeat(digits) { factor *= 10 }
        return factor
    }

    fun fractionDigits(currencyCode: String): Int = try {
        Currency.getInstance(currencyCode).defaultFractionDigits.let {
            if (it < 0) 2 else it
        }
    } catch (e: IllegalArgumentException) {
        2
    }

    /**
     * Disambiguate the comma in user-typed input. A comma is a decimal
     * separator when it is the only comma, there is no dot, and it is
     * followed by 1..[fractionDigits] digits ("12,99" EUR, "12,5" EUR):
     * in decimal-comma locales that is what the keyboard produces, and
     * stripping the comma as if it were a grouping separator silently
     * multiplied the amount by 100. Otherwise commas are grouping
     * separators and are stripped ("1,299" USD, "1,500" JPY).
     *
     * The decimal comma is the LAST comma, not necessarily the only one:
     * "1,299,50" (grouped thousands + decimal comma) reads as 1299.50,
     * and the integer part may be empty (",99" is 0.99) or carry a sign
     * ("+12,99" is 12.99). The earlier code required exactly one comma
     * with a non-empty all-digit integer part, so all three of those
     * fell through to the grouping branch and were stored 100x too
     * large — the same silent-corruption class as the decimal-comma bug
     * above, only reached through a different shape of input.
     *
     * When BOTH separators are present, the LAST one is the decimal
     * separator: "1,299.50" (US grouping) versus "1.299,50" (German
     * grouping). Treating every dot as decimal silently stored the
     * German form ~1000x too small ("1.299,50" EUR became 130 minor
     * instead of 129950) — the same silent-corruption class as the
     * decimal-comma bug above, only with grouping dots.
     */
    private fun normalizeCommas(text: String, currencyCode: String): String {
        val digits = fractionDigits(currencyCode)
        if (text.contains(".") && text.contains(",")) {
            return if (text.lastIndexOf(",") > text.lastIndexOf(".")) {
                text.replace(".", "").replace(",", ".")
            } else {
                text.replace(",", "")
            }
        }
        if (digits == 0 || text.contains(".")) return text.replace(",", "")
        val parts = text.split(",")
        if (parts.size < 2) return text
        val fraction = parts.last()
        if (fraction.length !in 1..digits || !fraction.all { it.isDigit() }) {
            return text.replace(",", "")
        }
        val head = parts.dropLast(1)
        val integer = when {
            head.size == 1 -> {
                val h = head[0]
                when {
                    h.isEmpty() -> "0"
                    h == "+" || h == "-" -> h + "0"
                    h.all { it.isDigit() } -> h
                    (h[0] == '+' || h[0] == '-') && h.drop(1).all { it.isDigit() } -> h
                    else -> return text.replace(",", "")
                }
            }
            head.all { it.isNotEmpty() && it.all { c -> c.isDigit() } } ->
                head.joinToString("")
            else -> return text.replace(",", "")
        }
        return "$integer.$fraction"
    }

    /**
     * "12.99" (USD) -> 1299; "1500" (JPY) -> 1500. Returns null on bad input.
     *
     * Only plain decimal notation is accepted. Non-breaking spaces
     * (U+00A0) and narrow non-breaking spaces (U+202F) are stripped as
     * grouping separators — French-locale formatting groups with them,
     * so a pasted or retyped French-formatted price was rejected
     * outright. Scientific notation ("1e3") is rejected: it is never a
     * typed price, and a huge exponent ("1e100000000") forced the
     * BigDecimal scaling to grind for 30+ seconds on the calling
     * thread (the edit form calls this on the main thread, so that
     * was an ANR from a 12-character paste).
     */
    fun parseToMinor(text: String, currencyCode: String): Long? {
        val cleaned = normalizeCommas(
            text.trim().replace(" ", "").replace("\u00A0", "").replace("\u202F", ""),
            currencyCode
        )
        if (cleaned.isEmpty()) return null
        if (cleaned.any { it == 'e' || it == 'E' }) return null
        val major = cleaned.toBigDecimalOrNull() ?: return null
        if (major.signum() < 0) return null
        return try {
            major.multiply(BigDecimal(minorPerMajor(currencyCode)))
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .longValueExact()
        } catch (e: ArithmeticException) {
            null
        }
    }

    /** 1299 (USD) -> "12.99"; 1500 (JPY) -> "1500". For form fields. */
    fun minorToPlainString(amountMinor: Long, currencyCode: String): String {
        val factor = minorPerMajor(currencyCode)
        if (factor == 1) return amountMinor.toString()
        val major = BigDecimal(amountMinor).divide(BigDecimal(factor))
        return major.stripTrailingZeros().toPlainString()
    }

    /** Locale-aware display: 1299 USD -> "$12.99", 1500 JPY -> "¥1,500". */
    fun format(amountMinor: Long, currencyCode: String, locale: Locale = Locale.getDefault()): String {
        val currency = try {
            Currency.getInstance(currencyCode)
        } catch (e: IllegalArgumentException) {
            return "$amountMinor $currencyCode"
        }
        val factor = minorPerMajor(currencyCode)
        val major = BigDecimal(amountMinor).divide(BigDecimal(factor))
        val format = NumberFormat.getCurrencyInstance(locale)
        format.currency = currency
        // setCurrency() alone does not reset the fraction digits the
        // instance inherited from the locale's own currency, so a JPY
        // amount in a US locale would render as "¥1,500.00". Pin the
        // digits to the amount currency's real ones.
        val digits = fractionDigits(currencyCode)
        format.minimumFractionDigits = digits
        format.maximumFractionDigits = digits
        return format.format(major)
    }
}
